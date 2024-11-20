/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.nodeservice.*;
import org.edu_sharing.service.util.CSVTool;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Batch edit property for multiple nodes
 * Required parameters:
 * property: The property name to replace
 * value: the target value to set
 * OR copy: the source property to copy the value of
 * startFolder: The id of the folder to start (recursively processing all children)
 * mode: The mode, see enum
 * types: the types of nodes to process, e.g. ccm:io (comma seperated string)
 *
 */
@JobDescription(description = "Bulk change metadata of nodes")
public class BulkEditNodesJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(BulkEditNodesJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	@JobFieldDescription(description = "folder id to start from")
	private String startFolder;
	@JobFieldDescription(description = "Lucene query to fetch the nodes that shall be processed. When used, the 'startFolder' parameter is ignored")
	private String lucene;
	@JobFieldDescription(description = "Mode to use")
	private Mode mode;
	@JobFieldDescription(description = "property to modify, e.g. cm:name", sampleValue = "cm:name")
	private String property;
	@JobFieldDescription(description = "Value to replace target property with")
	private Serializable value;
	@JobFieldDescription(description = "property to copy value from, if mode == Replace. Hint: use \"parent::\" prefix to copy data from the primary parent. Also supports special attributes like _DISPLAYNAME (if available for the source property)", sampleValue = "cclom:title")
	private String copy;
	private boolean copyParent;
	@JobFieldDescription(description = "token to replace, if mode == ReplaceToken")
	private String searchToken;
	@JobFieldDescription(description = "Token to replace with, if mode == ReplaceToken")
	private String replaceToken;
	@JobFieldDescription(description = "Element types to modify (comma seperated list), e.g. ccm:map,ccm:io", sampleValue = "ccm:map,ccm:io")
	private List<String> types;
	@JobFieldDescription(description = "RecurseMode to use")
	private RecurseMode recurseMode;
	@JobFieldDescription(file = true, description = "Mapping list (csv with oldValue + newValue headers), only if mode == ReplaceMapping")
	private String data;
	private CSVTool.CSVResult csv;
	@JobFieldDescription(description = "classname, if mode == Custom. Must be a subclass of java.util.function.Consumer<NodeRef>")
	private String customClass;

	@JobFieldDescription(description = "use archive. default is \"false\"")
	private String archive;

	@JobFieldDescription(description = "use versionStore. default is \"false\". overwrites archive param.")
	private String versionStore;
	private PropertiesSetInterceptor setInterceptor;

	private enum Mode{
		@JobFieldDescription(description = "Replace a property with a fixed string")
		Replace,
		@JobFieldDescription(description = "Search and Replace a string value inside the properties")
		ReplaceToken,
		@JobFieldDescription(description = "Find specific keys in the list and map it to new ones (list must contain oldValue and newValue as csv-headings!)")
		ReplaceMapping,
		@JobFieldDescription(description = "Add value to an multivalue property. use \"value\" param for the new value")
		Append,
		@JobFieldDescription(description = "Remove the property. Use with searchtoken: one value must be equal, than the property is removed. When no search token is given, the property will be removed on every node")
		Remove,
		@JobFieldDescription(description = "Remove the property. Use with searchtoken: if a found value is equal, than it is removed, but other values will stay stored (useful for multivalue fields).")
		SetInterceptor,
		@JobFieldDescription(description = "Run a SetInterceptor for all nodes (provide the class name of the interceptor in the \"customClass\" attribute). The interceptor must be of type \"PropertiesSetInterceptor\"")
		RemoveSingle,
		@JobFieldDescription(description = "Remove Duplicates in multivalue properties.")
		RemoveDuplicates,
		@JobFieldDescription(description = "Use a class that implements custom handling. defined by customClass param")
		Custom
	};

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();

		try {
			mode = Mode.valueOf((String) context.getJobDetail().getJobDataMap().get("mode"));
		} catch (Throwable t) {
			throw new IllegalArgumentException("Missing or invalid value for required parameter 'mode'", t);
		}


		if(!Arrays.asList(Mode.Custom, Mode.SetInterceptor).contains(mode)) {
			property = prepareParam(context, "property", true);
			property = CCConstants.getValidGlobalName(property);
		}

		copy = prepareParam(context, "copy", false);
		if (copy != null) {
			if(copy.startsWith("parent::")) {
				copyParent = true;
				copy = copy.substring("parent::".length());
			} else {
				copyParent = false;
			}
			copy = CCConstants.getValidGlobalName(copy);
		}
		value = prepareParam(context, "value", false);
		if (mode.equals(Mode.Replace)) {
			if (copy == null && value == null) {
				throwMissingParam("'value' or 'copy'");
			}
			if (copy != null && value != null) {
				throw new IllegalArgumentException("Only one of parameters 'value' and 'copy' may be set");
			}
		}
		if (mode.equals(Mode.ReplaceToken)) {
			searchToken = prepareParam(context, "searchToken", true);
			replaceToken = prepareParam(context, "replaceToken", true);
		}

		if (mode.equals(Mode.Remove) || mode.equals(Mode.RemoveSingle)) {
			searchToken = prepareParam(context, "searchToken", false);
		}

		lucene = prepareParam(context, "lucene", false);

		startFolder = prepareParam(context, "startFolder", true);
		archive = prepareParam(context, "archive", false);
		versionStore = prepareParam(context,"versionStore", false);

		try {
			types = Arrays.stream(((String) context.getJobDetail().getJobDataMap().get("types")).
					split(",")).map(String::trim).map(CCConstants::getValidGlobalName).
					collect(Collectors.toList());
		} catch (Throwable ignored) {
		}
		if (types == null || types.isEmpty()) {
			throwMissingParam("types");
		}
		recurseMode = RecurseMode.Folders;
		try {
			if (context.getJobDetail().getJobDataMap().get("recurseMode") != null) {
				recurseMode = RecurseMode.valueOf((String) context.getJobDetail().getJobDataMap().get("recurseMode"));
			}
		} catch (Throwable t) {
			throw new IllegalArgumentException("Missing or invalid value for parameter 'recurseMode'", t);
		}
		data = (String) context.getJobDetail().getJobDataMap().get(JobHandler.FILE_DATA);
		if (mode.equals(Mode.ReplaceMapping)) {
			csv = readCSVMapping(data);
		}

		NodeRunner runner = new NodeRunner();

		if (mode.equals(Mode.SetInterceptor)) {
			customClass = prepareParam(context, "customClass", true);
			try {
				setInterceptor = (PropertiesSetInterceptor)Class.forName(customClass).getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		if (mode.equals(Mode.Custom)) {
			customClass = prepareParam(context, "customClass", true);
			Class clazz = null;
			try {
				clazz = Class.forName(customClass);
				Consumer<NodeRef> consumer = (Consumer<NodeRef>) clazz.getConstructor(new Class[]{}).newInstance(new Object[]{});
				runner.setTask(consumer);
			} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new JobExecutionException(e);
			}
		}else{
			runner.setTask((ref) -> {
				org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreRef(), ref.getId());
				logger.info("Bulk edit metadata for node " + ref.getId());
				if (copy != null) {
					try {
						if (copyParent) {
							value = NodeServiceHelper.getPropertyNativeWithMapping(
									nodeService.getPrimaryParent(nodeRef).getParentRef(),
									copy
							);
						} else {
							value = NodeServiceHelper.getPropertyNativeWithMapping(nodeRef, copy);
						}
					} catch (Throwable t) {
						logger.warn("Could not read copy property from source node: "+ t.getMessage());
					}
				}
				if (mode.equals(Mode.Replace)) {
					setProperty(nodeRef, QName.createQName(property), value);
				} else if (mode.equals(Mode.Remove)) {
					if (searchToken != null) {
						Serializable current = nodeService.getProperty(nodeRef, QName.createQName(property));
						if (current != null) {
							boolean remove = false;
							if (current instanceof String) {
								if (searchToken.equals((String) current)) {
									remove = true;
								}
							} else if (current instanceof List) {
								for (Object o : (List) current) {
									if (searchToken.equals(o)) {
										remove = true;
									}
								}
							}
							if (remove) {
								removeProperty(nodeRef, QName.createQName(property));
							}
						}
					} else {
						removeProperty(nodeRef, QName.createQName(property));
					}
				} else if (mode.equals(Mode.RemoveSingle)) {
					Serializable current = nodeService.getProperty(nodeRef, QName.createQName(property));
					if (current != null) {
						if (current instanceof String) {
							if (searchToken.equals(current)) {
								removeProperty(nodeRef, QName.createQName(property));
							}
						} else if (current instanceof List) {
							ArrayList<String> newList = new ArrayList<>((List<String>) current);
							newList.removeIf((entry) -> entry.equals(searchToken));
							if(((List<?>) current).size() != newList.size()) {
								if(newList.size() == 0) {
									NodeServiceHelper.removeProperty(nodeRef, property);
								} else {
									setProperty(nodeRef,QName.createQName(property),newList);
								}
							}
						} else {
							logger.error("Could not process property of node " + nodeRef + ": Unsupported type " + current.getClass().getName());
						}
					}
				} else if (mode.equals(Mode.ReplaceToken) || mode.equals(Mode.ReplaceMapping)) {
					Serializable current = nodeService.getProperty(nodeRef, QName.createQName(property));
					if (current != null) {
						if (current instanceof String) {
							setProperty(nodeRef, QName.createQName(property), processPropertyValue((String) current));
						} else if (current instanceof List) {
							setProperty(nodeRef, QName.createQName(property), (Serializable) ((List) current).stream().map((v) -> {
								if (v instanceof String) {
									return processPropertyValue((String) v);
								} else {
									return v;
								}
							}).collect(Collectors.toList()));
						} else {
							logger.info("Can not replace property " + property + "for node " + nodeRef + ": current data is not of type String/List");
						}
					}
				} else if (mode.equals(Mode.RemoveDuplicates)) {
					Serializable current = nodeService.getProperty(nodeRef, QName.createQName(property));
					if (current != null && current instanceof List) {
						if (((List) current).stream().distinct().count() != ((List) current).size()) {
							setProperty(nodeRef, QName.createQName(property), (Serializable) ((List) current).stream().distinct().collect(Collectors.toList()));
						}
					}
				} else if(mode.equals(Mode.Append)){
					if(value == null ){
						logger.warn("can not append value cause its's null");
					}else if(!(value instanceof String)){
						logger.warn("only strings as value supported");
					}else{
						Serializable current = nodeService.getProperty(nodeRef, QName.createQName(property));
						if(current == null){
							logger.info("current is null");
						}else if(!(current instanceof List)){
							logger.info("property "+property +" is no list");
						}else{
							ArrayList newList = new ArrayList((List)current);
							if(newList.contains(value)){
								logger.info(nodeRef + " already contains "+value +" in "+property);
							}else{
								newList.add(value);
								setProperty(nodeRef, QName.createQName(property),newList);
							}
						}
					}

				}  else if(mode.equals(Mode.SetInterceptor)){
					HashMap<String, Object> properties = nodeService.getProperties(nodeRef).entrySet().stream().collect(
							HashMap::new,
							(m, entry) -> m.put(entry.getKey().toString(), entry.getValue()),
							HashMap::putAll
					);
					Map<String, Object> propertiesMapped = setInterceptor.beforeSetProperties(PropertiesInterceptorFactory.getPropertiesContext(
							nodeRef,
							properties,
							nodeService.getAspects(nodeRef).stream().map(QName::toString).collect(Collectors.toList()),
							null,
							null
					));
					nodeService.setProperties(nodeRef,  NodeServiceImpl.convertToFinalProperties(nodeRef, propertiesMapped));
				} else {
					throw new IllegalArgumentException("Mode " + mode + " is currently not supported");
				}
			});
		}
		runner.setTypes(types);
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setRecurseMode(recurseMode);
		runner.setStartFolder(startFolder);
		runner.setLucene(lucene);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		if(new Boolean(archive)){
			runner.setStartFolderStore(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
			runner.setLuceneStore(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
		}
		if(new Boolean(versionStore)){
			runner.setStartFolderStore(new StoreRef("workspace","version2Store"));
			if(StringUtils.isBlank(startFolder)) {
				runner.setStartFolder(AuthenticationUtil.runAsSystem(
						() -> nodeService.getRootNode(new StoreRef("workspace","version2Store")).getId())
				);
			}
			if(lucene != null && !lucene.trim().isEmpty()) throw new IllegalArgumentException("lucene can not be used with version store");
		}
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}

	public static CSVTool.CSVResult readCSVMapping(String data) {
		if(data==null) {
			throw new IllegalArgumentException("Missing or invalid value for parameter 'data'");
		}
		CSVTool.CSVResult csv = CSVTool.readCSV(new BufferedReader(new StringReader(data)), ',');
		if(csv == null){
			throw new IllegalArgumentException("Could not read csv");
		}
		if(!csv.getHeaders().contains("oldValue") || !csv.getHeaders().contains("newValue")){
			throw new IllegalArgumentException("Provided csv must contain oldValue and newValue headers");
		}
		return csv;
	}

	private Serializable processPropertyValue(String value) {
		if(mode.equals(Mode.ReplaceToken)) {
		return value.replace(searchToken, replaceToken);
		} else if (mode.equals(Mode.ReplaceMapping)) {
			List<Map<String, String>> filtered = csv.getLines().stream().
					filter((l) -> !l.get("oldValue").isEmpty()).
					filter((l) -> l.get("oldValue").trim().equals(value.trim())).collect(Collectors.toList());
			if(filtered.size() == 0){
				logger.warn("No mapping found for '" + value + "'! Check your csv data if this value should be mapped");
				return value;
			}
			if(filtered.size() == 1){
				return filtered.get(0).get("newValue");
			}
			logger.warn("Multiple matches for value '" + value + "'! No mapping applied. Make sure your csv data is valid");
			return value;
		}
		return null;
	}

	private String prepareParam(JobExecutionContext context, String param, boolean required) {
		String value = (String) context.getJobDetail().getJobDataMap().get(param);
		if(value==null && required) {
			throwMissingParam(param);
		}
		return value;

	}

	private void throwMissingParam(String param) {
		throw new IllegalArgumentException("Missing required parameter(s) '" + param + "'");
	}

	public void run() {

	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}

	private void setProperty(NodeRef nodeRef,QName qName, Serializable serializable){
		NodeServiceHelper.setProperty(nodeRef, qName.toString(), serializable, true);

		if(new Boolean(versionStore)){
			QName qnameV = QName.createQName(Version2Model.NAMESPACE_URI, Version2Model.PROP_METADATA_PREFIX+qName.toString());
			nodeService.setProperty(nodeRef,qnameV,serializable);
		}
	}


	private void removeProperty(NodeRef nodeRef,QName qName){
		nodeService.removeProperty(nodeRef,qName);
		if(new Boolean(versionStore)){
			QName qnameV = QName.createQName(Version2Model.NAMESPACE_URI, Version2Model.PROP_METADATA_PREFIX+qName.toString());
			nodeService.removeProperty(nodeRef,qnameV);
		}
	}
}
