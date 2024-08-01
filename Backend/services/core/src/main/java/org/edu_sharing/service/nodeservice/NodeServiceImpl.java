package org.edu_sharing.service.nodeservice;

import com.typesafe.config.Config;
import lombok.Setter;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.policy.OnCopyIOPolicy;
import org.edu_sharing.service.handleservice.HandleService;
import org.edu_sharing.service.handleservice.HandleServiceFactory;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.handleservicedoi.DOIService;
import org.edu_sharing.service.handleservicedoi.FeatureInfoDoiService;
import org.edu_sharing.service.handleservice.FeatureInfoHandleService;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.service.permission.HandleMode;
import org.edu_sharing.service.permission.HandleParam;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.rendering.RenderingTool;
import org.edu_sharing.service.search.Suggestion;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class NodeServiceImpl implements org.edu_sharing.service.nodeservice.NodeService {

	protected String appId;
	protected ContentService contentService;
	protected DictionaryService dictionaryService;
	protected final BehaviourFilter policyBehaviourFilter;
	protected String repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
	protected ServiceRegistry serviceRegistry = null;
	protected NodeService nodeService = null;
	protected NodeService nodeServiceAlfresco = null;
	protected VersionService versionService;
	@Setter
	protected HandleServiceFactory handleServiceFactory;

	Logger logger = Logger.getLogger(NodeServiceImpl.class);

	Repository repositoryHelper = null;

	MCAlfrescoAPIClient apiClient;

	public NodeServiceImpl() {
		this(ApplicationInfoList.getHomeRepository().getAppId());
	}

	public ApplicationInfo getApplication(){
		return ApplicationInfoList.getRepositoryInfoById(appId);
	}

	public NodeServiceImpl(String appId) {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		nodeService = serviceRegistry.getNodeService();
		nodeServiceAlfresco = (NodeService) applicationContext.getBean("alfrescoDefaultDbNodeService");
		policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
		contentService = serviceRegistry.getContentService();
		versionService = serviceRegistry.getVersionService();
		dictionaryService = serviceRegistry.getDictionaryService();
		repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
		this.appId=appId;
		Map<String, String> homeAuthInfo = null;
		if(!ApplicationInfoList.getRepositoryInfoById(repositoryId).ishomeNode()){
			homeAuthInfo = new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession());
		}
		try{
			apiClient = (MCAlfrescoAPIClient) RepoFactory.getInstance(repositoryId, homeAuthInfo);
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}

	}
	public void updateNode(String nodeId, Map<String, String[]> props) throws Throwable{
		String nodeType = getType(nodeId);
		String[] aspects = getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
		String parentId = nodeService.getPrimaryParent(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId)).getParentRef().getId();
		Map<String,Object> toSafeProps = getToSafeProps(props,nodeType,aspects, nodeId, parentId,null);
		updateNodeNative(nodeId, toSafeProps);
	}

	@Override
	public void createAssoc(String parentId, String childId, String assocName) {
		nodeService.createAssociation(
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId),
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, childId),
				QName.createQName(assocName)
		);
	}

	public NodeRef copyNode(String nodeId, String toNodeId, boolean copyChildren) throws Throwable {
		NodeRef result = serviceRegistry.getRetryingTransactionHelper().doInTransaction(()->{
			NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
			throwIfRestrictedAccessPresent(nodeRef);

			CopyService copyService = serviceRegistry.getCopyService();

			// copy and rename has a weird naming scheme
			String originalName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			NodeRef copyNodeRef = copyService.copyAndRename(nodeRef,
					new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, toNodeId),
					QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS),
					QName.createQName(originalName), copyChildren);

			int renameCounter = 1;
			while(true) {
				try {
					String name = originalName;
					if(renameCounter > 1){
						name = NodeServiceHelper.renameNode(originalName, renameCounter);
					}
					nodeServiceAlfresco.setProperty(copyNodeRef, QName.createQName(CCConstants.CM_NAME), name);
					break;
				} catch (DuplicateChildNodeNameException e){
					renameCounter++;
				}
			}
			resetVersion(copyNodeRef);
			return copyNodeRef;
		});

		return result;
	}

	private void throwIfRestrictedAccessPresent(NodeRef nodeRef) {
		Boolean restrictedAccess = (Boolean) NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.CCM_PROP_RESTRICTED_ACCESS);
		if(restrictedAccess != null && restrictedAccess) {
			if(!serviceRegistry.getPermissionService().hasPermission(nodeRef, CCConstants.PERMISSION_CHANGEPERMISSIONS).equals(AccessStatus.ALLOWED)) {
				throw new SecurityException("Node has restricted access and no permission " + CCConstants.PERMISSION_CHANGEPERMISSIONS + " available");
			}
		}
	}

	private void resetVersion(NodeRef nodeRef) {
		if(CCConstants.CCM_TYPE_IO.equals(getType(nodeRef.getId()))) {
			Map<String, Object> props = new HashMap<>();
			props.put(CCConstants.LOM_PROP_LIFECYCLE_VERSION,"1.0");
			updateNodeNative(nodeRef.getId(), props);
		}
	}

	public String createNode(String parentId, String nodeType, Map<String, String[]> props) throws Throwable{
		Map<String,Object> toSafeProps = getToSafeProps(props,nodeType,null, null,parentId,null);
		return createNodeBasic(parentId, nodeType, toSafeProps);
	}

	@Override
	public String createNode(String parentId, String nodeType, Map<String, String[]> props, String childAssociation)
			throws Throwable {
		Map<String,Object> toSafeProps = getToSafeProps(props,nodeType,null, null,parentId,null);
		return this.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId, nodeType,childAssociation, toSafeProps);
	}
	@Override
	public String createNodeBasic(String parentID, String nodeTypeString, Map<String, ?> _props) {
		return this.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentID, nodeTypeString,CCConstants.CM_ASSOC_FOLDER_CONTAINS, _props);
	}
	@Override
	public String createNodeBasic(StoreRef store, String parentID, String nodeTypeString, String childAssociation, Map<String, ?> _props) {
		childAssociation = (childAssociation == null) ? CCConstants.CM_ASSOC_FOLDER_CONTAINS : childAssociation;

		NodeRef parentNodeRef = new NodeRef(store, parentID);
		QName nodeType = QName.createQName(nodeTypeString);

		String assocName = (String) _props.get(CCConstants.CM_NAME);
		if (assocName == null) {
			assocName = "defaultAssociationName";
		} else {

			// assco name must have be smaller than a maxlength
			// https://issues.alfresco.com/jira/browse/MNT-2417
			assocName = QName.createValidLocalName(assocName);
		}
		assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;
		Map<String, Object> propsConverted = new HashMap<>();
		propsConverted.putAll(_props);
		for (PropertiesSetInterceptor i : PropertiesInterceptorFactory.getPropertiesSetInterceptors()) {
			try {
				propsConverted = i.beforeSetProperties(
						PropertiesInterceptorFactory.getPropertiesContext(
								null,
								propsConverted,
								Collections.emptyList(),
								null, null
						)
				);
			} catch (Throwable e) {
				logger.warn("Error while calling interceptor " + i.getClass().getName() + ": " + e.toString());
			}
		}
		Map<QName, Serializable> properties = transformPropMap(propsConverted);


		ChildAssociationRef childRef = nodeService.createNode(parentNodeRef, QName.createQName(childAssociation), QName.createQName(assocName), nodeType,
				properties);
		if(childAssociation.equals(CCConstants.CCM_ASSOC_CHILDIO)){
			new RepositoryCache().remove(parentID);
		}
		return childRef.getChildRef().getId();
	}

	@Override
	public String getPrimaryParent(String nodeId) {
		return nodeService.getPrimaryParent(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId)).getParentRef().getId();
	}

	@Override
	public String getCompanyHome(){
		return repositoryHelper.getCompanyHome().getId();
	}

	private Map<String,Object> getToSafeProps(Map<String, String[]> props, String nodeType, String[] aspects, String nodeId, String parentId,String templateName) throws Throwable{
		Map<String, String[]> propsCopy = new HashMap<>(props);
		String[] metadataSetIdArr = propsCopy.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET);

		String metadataSetId = (metadataSetIdArr != null && metadataSetIdArr.length > 0) ? metadataSetIdArr[0] : null;
		if(metadataSetId == null && nodeId != null) {
			// check if the node already as a metadata set id -> in this case, skip overwriting
			metadataSetId = (String) getPropertyNative(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
			if("default".equals(metadataSetId)) {
				metadataSetId = null;
			}
		}
		if(metadataSetId == null) {
			// allow to run as admin since user might don't have access to the parent ref
			metadataSetId = AuthenticationUtil.runAsSystem(() -> {
				Boolean forceMds = false;
				NodeRef parentRef = new NodeRef(MCAlfrescoAPIClient.storeRef, parentId);
				if (nodeService.exists(parentRef)) {
					try {
						forceMds = (Boolean) nodeService.getProperty(parentRef, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET));
						if (forceMds == null) forceMds = false;
					} catch (Throwable t) {
					}
				}
				if (forceMds) {
					return (String) nodeService.getProperty(parentRef, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
				} else {
					String mdsId;
					if(HttpContext.getCurrentMetadataSet() != null && HttpContext.getCurrentMetadataSet().trim().length() > 0) {
						mdsId = HttpContext.getCurrentMetadataSet();
					}else {
						mdsId = CCConstants.metadatasetdefault_id;
					}
					propsCopy.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, new String[] {mdsId});
					return mdsId;
				}
			});
		}

		MetadataSet mds = MetadataHelper.getMetadataset(getApplication(), metadataSetId);
		Map<String,Object> toSafe = new HashMap<>();
		for (MetadataWidget widget : (templateName==null ?
				mds.getWidgetsByNode(nodeType,Arrays.asList(ArrayUtils.nullToEmpty(aspects)), false) :
				mds.getWidgetsByTemplate(templateName))) {
			String id=widget.getId();
			if(!MetadataHelper.checkConditionTrue(widget.getCondition())) {
				logger.info("widget "+id+" skipped because condition failed");
				logger.info("condition that should match: "+widget.getCondition().getType()+" "+(widget.getCondition().isNegate() ? "!=" : "=" )+" "+widget.getCondition().getValue());
				continue;
			}
			id=CCConstants.getValidGlobalName(id);
			String[] propsValue = propsCopy.get(id);
			List<Serializable> values = propsValue != null ? Arrays.asList((Serializable[])propsValue) : null;
			if("range".equals(widget.getType())){
				String [] valuesFrom = propsCopy.get(id+"_from");
				String [] valuesTo = propsCopy.get(id+"_to");
				if(valuesFrom==null || valuesTo==null)
					continue;
				toSafe.put(id+"_from",valuesFrom[0]);
				toSafe.put(id+"_to",valuesTo[0]);
			}
			else if("defaultvalue".equals(widget.getType())) {
				logger.info("will save property "+widget.getId()+" with predefined defaultvalue "+widget.getDefaultvalue());
				toSafe.put(id,widget.getDefaultvalue());
				continue;
			} else if("date".equals(widget.getType()) && props.containsKey(id)){
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					values = Arrays.stream(propsCopy.get(id)).map((p) -> {
						try {
							return sdf.parse(p);
						} catch (ParseException e) {
							throw new RuntimeException(e);
						}
					}).collect(Collectors.toList());

				}catch(Throwable t){
					logger.info("Could not parse date for widget id " + widget.getId() + ": " + t.getMessage());
					values = new ArrayList<>();
				}
			}else if("datetime".equals(widget.getType()) && props.containsKey(id)){
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
					values = Arrays.stream(props.get(id)).map((p) -> {
						try {
							return sdf.parse(p);
						} catch (ParseException e) {
							throw new RuntimeException(e);
						}
					}).collect(Collectors.toList());

				}catch(Throwable t){
					logger.info("Could not parse date for widget id " + widget.getId() + ": " + t.getMessage());
					values = new ArrayList<>();
				}
			}
			// changed: otherwise reset values for multivalue fields is not possible
			// if(values==null || values.length==0)
			if(values==null)
				continue;
			if(!widget.isMultivalue() && values.size()>1)
				throw new IllegalArgumentException("Multiple values given for a non-multivalue widget: ID "+id+", widget type "+widget.getType());
			if(widget.isMultivalue()){
				toSafe.put(id,values.size()==0 ? null : new ArrayList<>(values));
			}
			else{
				toSafe.put(id,values.size()==0 ? null : values.get(0));
			}

			if(widget.getSuggestDisplayProperty() != null){
				String[] keys = propsCopy.get(id);
				if(keys != null) {
					Set<String> displayStrings = new HashSet<>();
					for (String key : keys) {
						List<? extends Suggestion> suggestions = MetadataSearchHelper.getSuggestions(getApplication().getAppId(), mds, "ngsearch", widget.getId(), key, null);
						displayStrings.add(suggestions.get(0).getDisplayString());
					}
					toSafe.put(CCConstants.getValidGlobalName(widget.getSuggestDisplayProperty()),displayStrings.size()==0 ? null : new ArrayList<>(displayStrings));
				}
			}
		}

		for(String property : getAllSafeProps()){
			if(!propsCopy.containsKey(property)) continue;

			NodeServiceHelper.convertMutlivaluePropToGeneric(propsCopy.get(property), toSafe, property);
		}
		// removed in 5.1
		/*
		if (isSubOf(nodeType, CCConstants.CM_TYPE_OBJECT)) {
			// only when there is an title
			String[] cmNameReadableNameArr = (String[])props.get(CCConstants.CM_NAME);
			String cmNameReadableName = (cmNameReadableNameArr != null && cmNameReadableNameArr.length > 0)  ? cmNameReadableNameArr[0] : null;
			String titleProp = null;
			if(cmNameReadableName == null){
				String[] lomTitleArr =  (String[]) props.get(CCConstants.LOM_PROP_GENERAL_TITLE);
				cmNameReadableName = (lomTitleArr != null && lomTitleArr.length > 0) ? lomTitleArr[0] : null;
				titleProp = CCConstants.LOM_PROP_GENERAL_TITLE;
				if (cmNameReadableName == null) {
					String[] cmTitleArr = (String[]) props.get(CCConstants.CM_PROP_C_TITLE);
					cmNameReadableName = (cmTitleArr != null && cmTitleArr.length > 0) ? cmTitleArr[0] : null;
					titleProp = CCConstants.CM_PROP_C_TITLE;
				}
			}
			if (cmNameReadableName != null) {
	
				// replace ie fakepath like C:\fakepath\test.png
				String replaceFakePathPrefixRegEx = "^[A-Za-z]:\\\\fakepath\\\\";
				String fakePaceCleanedString = cmNameReadableName.replaceFirst(replaceFakePathPrefixRegEx, "");
				if (fakePaceCleanedString.length() > 0) {
					cmNameReadableName = fakePaceCleanedString;
				}
	
				cmNameReadableName = NodeServiceHelper.cleanupCmName(cmNameReadableName);
	
				toSafe.put(CCConstants.CM_NAME, cmNameReadableName);	
			}
		}
		*/
		return toSafe;
	}
	//transient Logger logger = Logger.getLogger(MetadataWidget.class);

	private static Iterable<String> getAllSafeProps() {
		List<String> safe=new ArrayList<>();
		safe.addAll(Arrays.asList(NodeCustomizationPolicies.SAFE_PROPS));
		safe.addAll(Arrays.asList(NodeCustomizationPolicies.LICENSE_PROPS));
		safe.addAll(CCConstants.getLifecycleContributerPropsMap().values());
		safe.addAll(CCConstants.getMetadataContributerPropsMap().values());
		return safe;
	}
	@Override
	public Map<String, String[]> getNameProperty(String name) {
		Map<String, String[]> map= new HashMap<>();
		map.put(CCConstants.CM_NAME, new String[]{name});
		return map;
	}

	/**
	 * returns the child with the given name, or null if no child matched
	 */
	@Override
	public String findNodeByName(String parentId, String name){
		List<ChildAssociationRef> children = this.getChildAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId));
		for(ChildAssociationRef child : children){
			String childName=(String) nodeService.getProperty(child.getChildRef(), QName.createQName(CCConstants.CM_NAME));
			if(childName.equals(name))
				return child.getChildRef().getId();
		}
		return null;
	}
	private List<ChildAssociationRef> getChildrenAssocsByType(StoreRef store, String nodeId, String type) {
		List<ChildAssociationRef> childAssocList = this.getChildAssocs(new NodeRef(store, nodeId), Collections.singleton(QName.createQName(type)));
		return childAssocList;
	}

	public Map<String, Map<String, Object>> getChildrenByType(StoreRef store, String nodeId, String type) {
		Map<String, Map<String, Object>> result = new HashMap<>();
		List<ChildAssociationRef> childAssocList = getChildrenAssocsByType(store,nodeId,type);
		for (ChildAssociationRef child : childAssocList) {
			Map<String, Object> resultProps = getPropertiesWithoutChildren(child.getChildRef());
			String childNodeId = child.getChildRef().getId();
			result.put(childNodeId, resultProps);
		}
		return result;
	}
	@Override
	public List<NodeRef> getChildrenRecursive(StoreRef store, String nodeId,List<String> types,RecurseMode recurseMode) {
		// this method uses nodeServiceAlfresco instead of nodeService
		// to prevent that recursive fetch data of user homes will fetch (and also produce duplicates) of the shared org folders

		List<NodeRef> result = new ArrayList<>();
		try {
			NodeRef nodeRef = new NodeRef(store, nodeId);
			//logger.info("nodeRef:"+ nodeRef +"path: " + nodeServiceAlfresco.getPath(nodeRef).toDisplayPath(serviceRegistry.getNodeService(),serviceRegistry.getPermissionService()));
			if(logger.isDebugEnabled()) {
				logger.debug("nodeRef:" + nodeRef + "path: " + nodeServiceAlfresco.getPath(nodeRef).toPrefixString(serviceRegistry.getNamespaceService()));
			}
			List<ChildAssociationRef> assocs;
			if (types == null) {
				assocs = nodeServiceAlfresco.getChildAssocs(nodeRef);
			} else {
				Set<QName> typesConverted = types.stream().map(QName::createQName).collect(Collectors.toSet());
				assocs = nodeServiceAlfresco.getChildAssocs(nodeRef, typesConverted);
			}

			for (ChildAssociationRef assoc : assocs) {
				if(assoc.isPrimary()){
					result.add(assoc.getChildRef());
				}else{
					logger.warn("ignoring non primary association parent:" + assoc.getParentRef() +" child:"+assoc.getChildRef());
				}
			}
			List<ChildAssociationRef> maps;
			if (recurseMode.equals(RecurseMode.Folders)) {
				maps = nodeServiceAlfresco.getChildAssocs(nodeRef, new HashSet<>(Arrays.asList(QName.createQName(CCConstants.CCM_TYPE_MAP), QName.createQName(CCConstants.CM_TYPE_FOLDER))));
			} else if (recurseMode.equals(RecurseMode.All)) {
				// in theory, every object may have children, so we need to access all of them
				maps = nodeServiceAlfresco.getChildAssocs(nodeRef);
			} else {
				throw new IllegalArgumentException("invalid RecurseMode");
			}
			String user = AuthenticationUtil.getFullyAuthenticatedUser();
			// run in parallel to increase performance
			maps.parallelStream().forEach((map) -> {
				if(map.isPrimary()){
					AuthenticationUtil.runAs(() -> result.addAll(getChildrenRecursive(store, map.getChildRef().getId(), types, recurseMode))
							, user);
				}else{
					logger.warn("ignoring non primary association for recursive traversing parent:" + map.getParentRef() +" child:"+map.getChildRef());
				}
			});


			logger.info("Get children recursive finished with " + result.size() + " nodes");
			return result;
		}catch (Exception e){
			logger.error(e.getMessage(),e);
			return result;
		}
	}

	@Override
	public NodeRef getChild(StoreRef store, String parentId, String type, String property, Serializable value) {
		List<ChildAssociationRef> children = this.getChildrenAssocsByType(store, parentId, type);
		for (ChildAssociationRef child : children) {
			Serializable propValue = nodeService.getProperty(child.getChildRef(),QName.createQName(property));
			if (propValue != null && propValue.equals(value))
				return child.getChildRef();
		}
		return null;
	}



	private Map<String, Object> getPropertiesWithoutChildren(NodeRef nodeRef) {
		Map<QName, Serializable> childPropMap = nodeService.getProperties(nodeRef);
		Map<String, Object> resultProps = new HashMap<>();

		String nodeType = nodeService.getType(nodeRef).toString();

		for (QName qname : childPropMap.keySet()) {

			Serializable object = childPropMap.get(qname);

			String metadataSetId = (String) childPropMap.get(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));

			String value = formatData(nodeType, qname.toString(), object, metadataSetId);
			resultProps.put(qname.toString(), value);

			// VCard
			String type = nodeService.getType(nodeRef).toString();
			Map<String, Object> vcard = VCardConverter.getVCardMap(type, qname.toString(), value);
			if (vcard != null && vcard.size() > 0)
				resultProps.putAll(vcard);

		}

		resultProps.put(CCConstants.REPOSITORY_ID, repositoryId);
		resultProps.put(CCConstants.REPOSITORY_CAPTION, ApplicationInfoList.getHomeRepository().getAppCaption());

		return resultProps;
	}

	public String formatData(String type, String key, Object value, String metadataSetId) {
		String returnValue = null;
		if (key != null && value != null) {

			// value is date than put a String with a long value so that it can
			// be formated with userInfo later
			if (value instanceof Date) {

				Date date = (Date) value;

				if (date != null) {
					returnValue = new Long(date.getTime()).toString();
				}

			} else {
				returnValue = getValue(type, key, value, metadataSetId);
			}

			// like de_DE=null in gui
			if (returnValue == null && value != null && !(value instanceof MLText || value instanceof List)) {
				returnValue = value.toString();
			}
		}
		return returnValue;
	}

	protected String getValue(String type, String prop, Object _value, String metadataSetId) {

		//MetadataSetModelProperty mdsmProp = getMetadataSetModelProperty(metadataSetId, type, prop);

		if (_value instanceof List && ((List) _value).size() > 0) {
			String result = null;
			for (Object value : (List) _value) {
				if (result != null)
					result += CCConstants.MULTIVALUE_SEPARATOR;
				if (value != null) {
					if (value instanceof MLText) {
						String tmpStr = getMLTextString(value);
						if (result != null)
							result += tmpStr;
						else
							result = tmpStr;
					} else {
						if (result != null)
							result += value.toString();
						else
							result = value.toString();
					}
				}
			}

			return result;
		} else if (_value instanceof List && ((List) _value).size() == 0) {
			// cause empty list toString returns "[]"
			return "";
		} else if (_value instanceof String) {
			return (String) _value;
		} else if (_value instanceof Number) {
			return _value.toString();
		} else if (_value instanceof MLText) {
			return getMLTextString(_value);
		} else {
			return _value.toString();
		}

	}

	protected String getMLTextString(Object _mlText) {

		if (_mlText instanceof MLText) {

			MLText mlText = (MLText) _mlText;

			if (true /*mdsmp == null || (mdsmp != null && !mdsmp.getMultilang())*/) {
				return mlText.getDefaultValue();
			}

			String mlValueString = null;

			for (Locale locale : mlText.getLocales()) {
				String mlValue = mlText.getValue(locale);

				String localeStr = (locale.toString().equals(".default")) ? CCConstants.defaultLocale : locale.toString();

				if (mlValueString == null) {
					// for props that are declared multilang in alfresco model
					// but not in cc metadataset then props are saved as default.
					if (mlText.getLocales().size() == 1 && localeStr.equals(CCConstants.defaultLocale)) {
						mlValueString = mlValue;
					} else {
						mlValueString = localeStr + "=" + mlValue;
					}
				} else {
					mlValueString += "[,]" + localeStr + "=" + mlValue;
				}
			}
			if (mlValueString != null && !mlValueString.trim().equals("") && !mlValueString.contains(CCConstants.defaultLocale)) {
				mlValueString += "[,]default=" + mlText.getDefaultValue();
			}

			return mlValueString;
		} else {
			return _mlText.toString();
		}
	}

	@Override
	public void updateNodeNative(String nodeId, Map<String, ?> _props) {
		this.updateNodeNative(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, _props);
	}

	public void updateNodeNative(StoreRef store, String nodeId, Map<String, ?> _props) {

		try {
			NodeRef nodeRef = new NodeRef(store, nodeId);
			Map<QName, Serializable> props = transformPropMap(_props);
			Map<String, Object> propsConverted = new HashMap<>();
			Set<QName> propsNull = new HashSet<>();

			// do in transaction to disable behaviour
			// otherwise interceptors might be called multiple times -> the final update props is enough!
			for(Map.Entry<QName, Serializable> prop : props.entrySet()){
				// instead of storing props as null (which can cause solr erros), remove them completely from the node!
				if(prop.getValue()==null) {
					propsNull.add(prop.getKey());
				}
				propsConverted.put(prop.getKey().toString(),prop.getValue());
			}

			// don't do this cause it's slow:
			/*
			 * for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
			 * nodeService.setProperty(nodeRef, entry.getKey(),
			 * entry.getValue()); }
			 */

			// prevent overwriting of properties that don't come with param _props
			Set<String> changedProps = propsConverted.keySet();
			Map<QName, Serializable> currentProps = nodeService.getProperties(nodeRef);
			for (Map.Entry<QName, Serializable> entry : currentProps.entrySet()) {
				if (!changedProps.contains(entry.getKey().toString())) {
					propsConverted.put(entry.getKey().toString(), entry.getValue());
				}
			}

			Map<String, Object> propsFinal = propsConverted;

			for (PropertiesSetInterceptor i : PropertiesInterceptorFactory.getPropertiesSetInterceptors()) {
				try {
					propsFinal = i.beforeSetProperties(PropertiesInterceptorFactory.getPropertiesContext(
							nodeRef,
							propsFinal,
							Arrays.asList(getAspects(store.getProtocol(), store.getIdentifier(), nodeId)), null, null));
				} catch (Throwable e) {
					logger.warn("Error while calling interceptor " + i.getClass().getName() + ": " + e.toString());
				}
			}
			Map<QName, Serializable> propsStore = propsFinal.entrySet().stream().collect(
					HashMap::new,
					(m,entry)-> m.put(QName.createQName(entry.getKey()), (Serializable) entry.getValue()),
					HashMap::putAll
			);
			nodeService.setProperties(nodeRef, propsStore);
			// do in transaction to disable behaviour
			// otherwise interceptors might be called multiple times -> the final update props is enough!
			// to it AFTER set properties so the values can be sent as NULL-Values into setProperties to be read by interceptors
			serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
				policyBehaviourFilter.disableBehaviour(nodeRef);
				for(QName prop : propsNull){
					nodeService.removeProperty(nodeRef, prop);
				}
				policyBehaviourFilter.enableBehaviour(nodeRef);
				return null;
			});
		} catch (Exception e) {
			// this occurs sometimes in workspace
			// it seems it is an alfresco bug:
			// https://issues.alfresco.com/jira/browse/ETHREEOH-2461
			logger.error("Thats maybe an alfreco bug: https://issues.alfresco.com/jira/browse/ETHREEOH-2461", e);
		}

	}

	Map<QName, Serializable> transformPropMap(Map<String, ?> map) {
		Map<QName, Serializable> result = new HashMap<>();
		for (Object key : map.keySet()) {

			try {
				Object value = map.get(key);
				if (value instanceof Map) {
					value = getMLText((Map) value);
				} else if (value instanceof List) {
					List transformedList = new ArrayList();
					for (Object valCol : (List) value) {
						if (valCol instanceof Map) {
							transformedList.add(getMLText((Map) valCol));
						} else {
							transformedList.add(valCol);
						}
					}
					value = transformedList;
				}
				result.put(QName.createQName((String) key), (Serializable) value);
			} catch (ClassCastException e) {
				logger.error("this prop has a wrong value:" + key + " val:" + map.get(key));
				logger.error(e.getMessage(), e);
			}
		}
		return result;
	}

	private MLText getMLText(Map<String, String> i18nMap) {
		MLText mlText = new MLText();
		for (String locale : i18nMap.keySet()) {
			mlText.addValue(new Locale(locale), i18nMap.get(locale));
		}
		return mlText;
	}

	@Override
	public String getType(String storeProtocol,String storeId,String nodeId) {
		return nodeService.getType(new NodeRef(new StoreRef(storeProtocol,storeId), nodeId)).toString();
	}

	public ChildAssociationRef getParent(NodeRef nodeRef){
		return nodeService.getPrimaryParent(nodeRef);
	}

	public boolean isSubOf(String type, String parentType) throws Throwable {

		boolean isSubOf = serviceRegistry.getDictionaryService().isSubClass(QName.createQName(type), QName.createQName(parentType));
		return isSubOf;
	}

	public void setOwner(String nodeId, String username) {
		serviceRegistry.getOwnableService().setOwner(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), username);
	}


	/**
	 * set's permission for one authority, leaves permissions already set for the authority
	 *
	 * check ToolPermissions in the callers
	 */
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission) throws Exception {

		PermissionService permissionsService = this.serviceRegistry.getPermissionService();
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		if (inheritPermission != null) {
			logger.info("setInheritParentPermissions " + inheritPermission);
			permissionsService.setInheritParentPermissions(nodeRef, inheritPermission);
		}

		if (permissions != null) {
			for (String permission : permissions) {
				permissionsService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), authority, permission, true);
			}
		}

	}
	@Override
	public String getUserInbox(boolean createIfNotExists) {
		NodeRef userhome=repositoryHelper.getUserHome(repositoryHelper.getPerson());
		List<ChildAssociationRef> inbox = nodeService.getChildAssocsByPropertyValue(userhome, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE), CCConstants.CCM_VALUE_MAP_TYPE_USERINBOX);
		if(inbox!=null && inbox.size()>0)
			return inbox.get(0).getChildRef().getId();
		if(createIfNotExists) {
			Map<String, Object> properties = new HashMap<>();
			properties.put(CCConstants.CM_NAME, "Inbox");
			properties.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_USERINBOX);
			return createNodeBasic(userhome.getId(), CCConstants.CCM_TYPE_MAP, properties);
		}
		return null;
	}
	@Override
	public String getUserSavedSearch(boolean createIfNotExists) {
		NodeRef userhome=repositoryHelper.getUserHome(repositoryHelper.getPerson());
		List<ChildAssociationRef> savedSearch = nodeService.getChildAssocsByPropertyValue(userhome, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE), CCConstants.CCM_VALUE_MAP_TYPE_USERSAVEDSEARCH);
		if(savedSearch!=null && savedSearch.size()>0)
			return savedSearch.get(0).getChildRef().getId();
		if(createIfNotExists) {
			Map<String, Object> properties = new HashMap<>();
			properties.put(CCConstants.CM_NAME, "SavedSearch");
			properties.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_USERSAVEDSEARCH);
			return createNodeBasic(userhome.getId(), CCConstants.CCM_TYPE_MAP, properties);
		}
		return null;
	}

	/**
	 * Supported values for filter:
	 * special: show all files which are usually not displayed anyway (usage, share, thumbnail)
	 * files: return only files
	 * @param list
	 * @param filter
	 * @param sortDefinition
	 * @param <T>
	 * @return
	 */
	@Override
	public <T>List<T> sortNodeRefList(List<T> list,List<String> filter, SortDefinition sortDefinition){
		// make a copy so we have a modifiable list object
		list=new ArrayList<>(list);
		List<T> filtered = new ArrayList<>();
		for(T obj : list){
			if(!EduSharingNodeHelper.shouldFilter(getAsNode(obj),filter)){
				filtered.add(obj);
			}
		}
		list=filtered;

		Map<String,Object> cache=new HashMap<>();
		Collections.sort(list, (o1, o2) -> sortNodes(cache,getAsNode(o1),getAsNode(o2),sortDefinition));
		return list;
	}

	private <T> NodeRef getAsNode(T obj) {
		NodeRef node;
		if(obj instanceof ChildAssociationRef){
			node=((ChildAssociationRef) obj).getChildRef();
		}
		else if(obj instanceof AssociationRef){
			node=((AssociationRef) obj).getTargetRef();
		}
		else if(obj instanceof NodeRef){
			node= (NodeRef) obj;
		}
		else{
			throw new IllegalArgumentException("Given type not supported");
		}
		return node;
	}

	private int sortNodes(Map<String, Object> cache, NodeRef n1, NodeRef n2, SortDefinition sortDefinition) {
		String keyType1=n1.toString()+"_TYPE";
		String keyType2=n2.toString()+"_TYPE";

		String type1,type2;
		if(cache.containsKey(keyType1)){
			type1= (String) cache.get(keyType1);
		}
		else{
			type1=nodeServiceAlfresco.getType(n1).toString();
			cache.put(keyType1,type1);
		}
		if(cache.containsKey(keyType2)){
			type2= (String) cache.get(keyType2);
		}
		else{
			type2=nodeServiceAlfresco.getType(n2).toString();
			cache.put(keyType2,type2);
		}
		if(EduSharingNodeHelper.typeIsDirectory(type1)!=EduSharingNodeHelper.typeIsDirectory(type2)){
			return EduSharingNodeHelper.typeIsDirectory(type1) ? -1 : 1;
		}
		if(!sortDefinition.hasContent())
			return 0;
		for(SortDefinition.SortDefinitionEntry entry : sortDefinition.getSortDefinitionEntries()){
			QName prop = QName.createQName(CCConstants.getValidGlobalName(entry.getProperty()));
			Object prop1,prop2;
			String key1=n1.toString()+prop.toString();
			String key2=n2.toString()+prop.toString();
			if(cache.containsKey(key1)){
				prop1=cache.get(key1);
			}
			else{
				prop1 = getSortPropertyValue(n1, prop);
				cache.put(key1,prop1);
			}
			if(cache.containsKey(key2)){
				prop2=cache.get(key2);
			}
			else{
				prop2 = getSortPropertyValue(n2, prop);
				cache.put(key2,prop2);
			}
			int compare=0;
			if(prop1==null && prop2!=null) {
				try {
					prop1=prop2.getClass().getConstructor().newInstance();
				}catch(Throwable t){}

			}
			else if(prop1!=null && prop2==null) {
				try {
					prop2=prop1.getClass().getConstructor().newInstance();
				}catch(Throwable t){}
			}
			if(prop1==null && prop2==null){
				continue;
			}
			else {
				// some int fields are parsed as string. make sure to compare them correctly
				// e.g. for collection sorting

				String fieldType = dictionaryService.getProperty(prop).getDataType().getJavaClassName();
				if (fieldType.equals(Integer.class.getName())) {
					if (prop1 instanceof String && prop2 instanceof String) {
						compare = Integer.compare(Integer.parseInt((String) prop1), Integer.parseInt((String) prop2));
					}
				}

				if (compare == 0) {
					// cast ml text to string
					if(prop1 instanceof MLText) {
						prop1 = ((MLText) prop1).getDefaultValue();
					}
					if(prop2 instanceof MLText) {
						prop2 = ((MLText) prop2).getDefaultValue();
					}
					if (prop1 instanceof String && prop2 instanceof String) {
						// normalize umlauts
						prop1 = StringUtils.stripAccents((String)prop1);
						prop2 = StringUtils.stripAccents((String)prop2);
						compare = ((String) prop1).compareToIgnoreCase((String) prop2);
					} else if (prop1 instanceof Date && prop2 instanceof Date) {
						compare = ((Date) prop1).compareTo((Date) prop2);
					} else if (prop1 instanceof Comparable && prop2 instanceof Comparable) {
						compare = ((Comparable) prop1).compareTo((Comparable) prop2);
					}
				}
			}
			if(!entry.isAscending())
				compare*=-1;
			if(compare!=0)
				return compare;
		}
		return 0;
	}

	private Serializable getSortPropertyValue(NodeRef ref, QName prop) {
		Serializable value = nodeServiceAlfresco.getProperty(ref, prop);
		//dbnodeservice returns mltext
		if(value instanceof MLText){
			value = ((MLText)value).getDefaultValue();
		}

		if(prop.toString().equals(CCConstants.LOM_PROP_GENERAL_TITLE) && StringUtils.isBlank((String)value)) {
			return nodeServiceAlfresco.getProperty(ref, ContentModel.PROP_NAME);
		}
		return value;
	}

	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRefAssoc(String parentID, String assocName, List<String> filter, SortDefinition sortDefinition){
		NodeRef parentNodeRef = getParentRef(parentID);
		List<ChildAssociationRef> result;
		if(assocName==null || assocName.isEmpty()){
			result=this.getChildAssocs(parentNodeRef);
		}
		else{
			// special handling for series objects inside collections
			if(assocName.equals(CCConstants.CCM_ASSOC_CHILDIO)) {
				if(hasAspect(StoreRef.PROTOCOL_WORKSPACE,
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
						parentID,
						CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)
				) {
					return AuthenticationUtil.runAsSystem(() -> {
						try {
							return sortNodeRefList(
									this.getChildAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
													(String) getPropertyNative(StoreRef.PROTOCOL_WORKSPACE,
															StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
															parentID,
															CCConstants.CCM_PROP_IO_ORIGINAL
													)),
											QName.createQName(assocName), RegexQNamePattern.MATCH_ALL),
									filter,
									sortDefinition
							);
						}catch(Throwable ignored) {
							// original might be deleted!
						}
						return Collections.emptyList();
					});
				}
			}
			result=this.getChildAssocs(parentNodeRef,QName.createQName(assocName),RegexQNamePattern.MATCH_ALL);
		}
		result=sortNodeRefList(result,filter,sortDefinition);
		return result;
	}


	private NodeRef getParentRef(String parentID) {
		if (parentID == null) {

			String startParentId = apiClient.getRootNodeId();
			if (startParentId == null || startParentId.trim().equals("")) {
				parentID = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE).getId();
			} else {
				parentID = startParentId;
			}
		}

		return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentID);
	}
	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRefType(String parentID,String childType){
		NodeRef parentNodeRef = getParentRef(parentID);
		if(childType==null) {
			return this.getChildAssocs(parentNodeRef);
		}
		else {
			return this.getChildAssocs(parentNodeRef,new HashSet<>(Arrays.asList(QName.createQName(childType))));
		}

	}

	private List<ChildAssociationRef> getChildAssocs(NodeRef parentNodeRef, QName qName, QNamePattern matchAll) {
		return nodeService.getChildAssocs(parentNodeRef, qName, matchAll);
	}
	private List<ChildAssociationRef> getChildAssocs(NodeRef parentNodeRef) {
		return nodeService.getChildAssocs(mapParentNodeLink(parentNodeRef));
	}
	private List<ChildAssociationRef> getChildAssocs(NodeRef parentNodeRef, Set<QName> qNames) {
		return nodeService.getChildAssocs(mapParentNodeLink(parentNodeRef),qNames);
	}

	private NodeRef mapParentNodeLink(NodeRef parentNodeRef) {
		if (parentNodeRef != null && nodeService.hasAspect(parentNodeRef, QName.createQName(CCConstants.CCM_ASPECT_MAP_REF))) {
			parentNodeRef = (NodeRef) nodeService.getProperty(parentNodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_REF_TARGET));
		}
		return parentNodeRef;
	}

	public void createVersion(String nodeId) throws Exception{
		apiClient.createVersion(nodeId);
	}

	@Override
	public void deleteVersionHistory(String nodeId) throws Exception {
		versionService.deleteVersionHistory(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId));
	}

	@Override
	public void writeContent(StoreRef store, String nodeID, InputStream content, String mimetype, String _encoding,
							 final String property) throws Exception {
		// if trying to write to an io ref -> switch to original (this can cause permission denied!)
		if(hasAspect(store.getProtocol(), store.getIdentifier(), nodeID, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
			nodeID = getProperty(store.getProtocol(), store.getIdentifier(), nodeID, CCConstants.CCM_PROP_IO_ORIGINAL);
		}
		String finalNodeID = nodeID;
		apiClient.writeContent(store, nodeID, content, mimetype, _encoding, property, () -> {
			if (property == null || property.equals(ContentModel.PROP_CONTENT.toString())) {
				RenderingTool.buildRenderingCache(finalNodeID);
			}
		});
	}

	@Override
	public void removeNode(String nodeID, String fromID){
		this.removeNode(nodeID, fromID, true);
	}

	@Override
	public void removeNode(String nodeId, String parentId, boolean recycle) {
		List<ChildAssociationRef> assocs = nodeService.getParentAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
		for(ChildAssociationRef assoc : assocs){
			if(assoc.getTypeQName().toString().equals(CCConstants.CCM_ASSOC_CHILDIO)){
				new RepositoryCache().remove(assoc.getParentRef().getId());
			}
		}
		apiClient.removeNode(nodeId, parentId, recycle);
	}

	@Override
	public void removeNodeForce(String storeProtocol, String storeId, String nodeId, boolean recycle) {
		NodeRef nodeRef = new NodeRef(new StoreRef(storeProtocol,storeId),nodeId);
		if(!recycle){
			nodeServiceAlfresco.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
		}
		//serviceRegistry.getRetryingTransactionHelper().doInTransaction(()->{
		Method method = null;
		try {
			method = nodeServiceAlfresco.getClass().getDeclaredMethod("deleteNode", NodeRef.class,boolean.class);
			method.setAccessible(true);

			Object r = method.invoke(nodeServiceAlfresco,nodeRef,false);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		/*	return null;
		});*/
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable{
		return apiClient.getProperties(storeProtocol, storeId, nodeId);
	}

	@Override
	public Map<String, Object> getPropertiesDynamic(String storeProtocol, String storeId, String nodeId) throws Throwable{
		throw new NotImplementedException("getPropertiesDynamic may not be called for the local repository (was the remote repo removed?)");
	}
	@Override
	public Map<String, Object> getPropertiesPersisting(String storeProtocol, String storeId, String nodeId) throws Throwable{
		throw new NotImplementedException("getPropertiesPersisting may not be called for the local repository (was the remote repo removed or is it missing the remote_provider setting?)");
	}

	@Override
	public String getProperty(String storeProtocol, String storeId, String nodeId, String property){
		return apiClient.getProperty(new StoreRef(storeProtocol,storeId), nodeId, property);
	}
	@Override
	public Serializable getPropertyNative(String storeProtocol, String storeId, String nodeId, String property){
		return nodeService.getProperty(new NodeRef(new StoreRef(storeProtocol,storeId), nodeId), QName.createQName(property));
	}

	@Override
	public void keepModifiedDate(String storeProtocol, String storeId, String nodeId, Runnable task) {
		serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
			NodeRef nodeRef = new NodeRef(new StoreRef(storeProtocol, storeId), nodeId);
			// disable behaviour so no version data is altered externally
			policyBehaviourFilter.disableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);
			task.run();
			policyBehaviourFilter.enableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);
			return null;
		});
	}

	@Override
	public String publishCopy(String nodeId, HandleParam handleParam) throws Throwable {
		ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_PUBLISH_COPY);
		if(PermissionServiceFactory.getLocalService().hasAllPermissions(StoreRef.PROTOCOL_WORKSPACE,
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
						nodeId,
						new String[]{CCConstants.PERMISSION_READ, CCConstants.PERMISSION_CHANGEPERMISSIONS}).
				values().stream().anyMatch((v) -> !v)){
			throw new SecurityException("No " + CCConstants.PERMISSION_CHANGEPERMISSIONS + " on node " + nodeId);
		}
		String parent, pattern,owner;
		try {
			Config config = LightbendConfigLoader.get();
			parent = config.getString("publish.node");
			pattern = config.getString("publish.nodePattern");
			owner = config.getString("publish.owner");
		} catch(Throwable t){
			throw new RuntimeException("Invalid configuration for publishing. Please check the repository config", t);
		}
		return serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
			// permissions are checked beforehand,
			return AuthenticationUtil.runAsSystem(() -> {
				String currentVersion = getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId, CCConstants.LOM_PROP_LIFECYCLE_VERSION);
				List<String> currentCopies = getPublishedCopies(nodeId);
				if(currentCopies.stream().anyMatch((c) -> currentVersion.equals(getProperty(
						StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), c, CCConstants.LOM_PROP_LIFECYCLE_VERSION
				)))) {
					throw new IllegalArgumentException("The version " + currentVersion + " is already published!");
				}
				String container = NodeServiceHelper.getContainerId(parent, pattern);
				NodeRef oldNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
				NodeRef newNode;
				try {
					newNode = copyNode(nodeId, container, true);
					OnCopyIOPolicy.removeCopiedUsages(nodeService, newNode);
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
				return serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
					// disable behaviour so no version data is altered externally
					policyBehaviourFilter.disableBehaviour(newNode);
					// replace owner, creator & modifier
					setPublishedCopyProperties(oldNodeRef, newNode, owner);
					setProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), oldNodeRef.getId(), CCConstants.CCM_PROP_IO_PUBLISHED_MODE, "copy", false);
					setProperty(newNode.getStoreRef().getProtocol(), newNode.getStoreRef().getIdentifier(), newNode.getId(), CCConstants.CCM_PROP_IO_PUBLISHED_DATE, new Date(), false);
					setProperty(newNode.getStoreRef().getProtocol(), newNode.getStoreRef().getIdentifier(), newNode.getId(), CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL,
							oldNodeRef, false);
					NodeServiceHelper.copyProperty(oldNodeRef, newNode, CCConstants.LOM_PROP_LIFECYCLE_VERSION);
					NodeServiceHelper.copyProperty(oldNodeRef, newNode, CCConstants.CCM_PROP_IO_VERSION_COMMENT);
					//deleteVersionHistory(newNode.getId());
					//createVersion(newNode.getId());
					for (ChildAssociationRef child : this.getChildAssocs(newNode)) {
						policyBehaviourFilter.disableBehaviour(child.getChildRef());
						setPublishedCopyProperties(oldNodeRef, child.getChildRef(), owner);
						policyBehaviourFilter.enableBehaviour(child.getChildRef());
					}
					serviceRegistry.getPermissionService().deletePermissions(newNode);
					setPermissions(newNode.getId(), CCConstants.AUTHORITY_GROUP_EVERYONE,
							new String[]{CCConstants.PERMISSION_CONSUMER, CCConstants.PERMISSION_CC_PUBLISH},
							true);
					if(handleParam != null) {
						ApplicationContext eduAppContext = ApplicationContextFactory.getApplicationContext();
						if(handleParam.handleService != null){
							try{
								eduAppContext.getBean(FeatureInfoHandleService.class);
								createHandle(newNode, currentCopies,
										handleServiceFactory.instance(HandleServiceFactory.IMPLEMENTATION.handle),
										handleParam.handleService );
							}catch (NoSuchBeanDefinitionException e){
								logger.error("handle service not enabled");
							}
						}
						if(handleParam.doiService != null){
							try{
								eduAppContext.getBean(FeatureInfoDoiService.class);
								createHandle(newNode, currentCopies,
										handleServiceFactory.instance(HandleServiceFactory.IMPLEMENTATION.doi),
										handleParam.doiService );
							}catch (NoSuchBeanDefinitionException e){
								logger.error("doi service not enabled");
							}
						}
					}

					policyBehaviourFilter.enableBehaviour(newNode);
					return newNode.getId();
				});
			});
		});

	}


	@Override
	public void createHandle(NodeRef nodeRef, List<String> publishedCopies, HandleService handleService, HandleMode handleMode) throws Exception {
		ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_HANDLESERVICE);
		try {
			/**
			 * test handleservice to prevent property handleid isset but can not be pushed to handleservice cause of configration problems
			 */
			if(!handleService.available()) throw new Exception("handleservice unavailable");
		} catch (Exception e) {
			// DEBUG ONLY
			//handle = "test/" + Math.random();
			throw new RuntimeException("Handle service throwed an error: " + e.getMessage(), e);
		}
		String currentHandle = null;
		// fetch the last given handle from the currently existing copies
		if(handleMode.equals(HandleMode.update) && publishedCopies.size() > 0 ){
			Set<String> handles = publishedCopies.stream().filter((c) ->
					getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), c, handleService.getHandleIdProperty()) != null
			).map((c) ->
					getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), c, handleService.getHandleIdProperty())
			).distinct().collect(Collectors.toSet());
			if(handles.size() != 1){
				throw new IllegalStateException("Multiple handles found but handleMode " + handleMode + " was requested");
			}
			currentHandle = handles.iterator().next();
		}

		String generated = null;
		String handle = null;

		Map<QName, Serializable> publishedProps = new HashMap<>();

		try {
			if (handleMode.equals(HandleMode.distinct)) {
				try {
					generated = handleService.generateId();
					handle = generated;

				} catch (Exception e) {
					logger.error("Internal error while creating handle id", e);
					// DEBUG ONLY
					//handle = "test/" + Math.random();
					throw new RuntimeException("Handle generation throwed an error: " + e.getMessage(), e);
				}
			} else {
				if (currentHandle == null) {
					throw new IllegalArgumentException("Handle Mode " + handleMode + " requested but no handle assigned yet");
				}
				handle = currentHandle;
			}


			publishedProps.put(QName.createQName(CCConstants.CCM_PROP_PUBLISHED_DATE), new Date());

			if (handle != null) {
				publishedProps.put(QName.createQName(handleService.getHandleIdProperty()), handle);
			}

			if (!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_PUBLISHED))) {
				nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_PUBLISHED), publishedProps);
			} else {
				for (Map.Entry<QName, Serializable> entry : publishedProps.entrySet()) {
					nodeService.setProperty(nodeRef, entry.getKey(), entry.getValue());
				}
			}

			/**
			 * create version for the published node
			 * NO: NOT NEEDED ANYMORE!
			 * The version is implicitly correct because a copied node has exact ONE version!
			 *
			 */
				/*
				Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
				props.put(QName.createQName(CCConstants.CCM_PROP_IO_VERSION_COMMENT), NODE_PUBLISHED);
				try {
					new MCAlfrescoAPIClient().createVersion(_nodeId);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					logger.error(e1.getMessage(), e1);
				}*/
			if (handle != null) {
				String contentLink = URLHelper.getNgRenderNodeUrl(nodeRef.getId(), null, false);
				Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
				if (handleMode.equals(HandleMode.distinct)) {
					logger.info("Create handle " + handle + ", " + contentLink);
					handle = handleService.create(handle, nodeRef.getId(), properties);
					if(handleService instanceof DOIService) {
						properties.put(QName.createQName(CCConstants.CCM_PROP_PUBLISHED_DOI_ID), handle);
					} else {
						properties.put(QName.createQName(CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID), handle);
					}
				} else if (handleMode.equals(HandleMode.update)) {
					logger.info("Update handle " + handle + ", " + contentLink);
					handleService.update(handle, nodeRef.getId(), properties);
				}
			}
		}catch (Exception e){
			//cleanup draft
			if(generated != null){
				handleService.delete(generated, nodeRef.getId());
			}
			throw e;
		}

	}

	private void setPublishedCopyProperties(NodeRef oldNodeRef, NodeRef newNode, String owner) {
		setOwner(newNode.getId(), owner);
		NodeServiceHelper.copyProperty(oldNodeRef, newNode, CCConstants.CM_PROP_C_CREATOR);
		NodeServiceHelper.copyProperty(oldNodeRef, newNode, CCConstants.CM_PROP_C_MODIFIER);
	}

	@Override
	public List<String> getPublishedCopies(String nodeId) {
		nodeId = this.getOriginalNode(nodeId).getId();
		Map<String, Object> filters = new HashMap<>();
		filters.put(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
		List<NodeRef> nodes = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO, filters);
		return nodes.stream().
				// filter any collection ref to prevent duplicated published versions
						filter(n -> !NodeServiceHelper.hasAspect(n, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)).
				map(NodeRef::getId).collect(Collectors.toList());
	}

	@Override
	public String getPreviewUrl(String storeProtocol, String storeId, String nodeId, String version) {
		String previewURL = URLHelper.getBaseUrl(true);
		previewURL += "/preview?nodeId="+nodeId+"&storeProtocol="+storeProtocol+"&storeId="+storeId+"&dontcache="+System.currentTimeMillis();
		if(version!=null){
			previewURL +="&version="+version;
		}
		previewURL =  URLTool.addOAuthAccessToken(previewURL);
		return previewURL;
	}

	@Override
	public String getTemplateNode(String nodeId,boolean create) throws Throwable {
		if(!getType(nodeId).equals(CCConstants.CCM_TYPE_MAP) && !getType(nodeId).equals(CCConstants.CM_TYPE_FOLDER)){
			throw new IllegalArgumentException("Setting templates for nodes is only supported for type "+CCConstants.CCM_TYPE_MAP);
		}

		QName assocQName=QName.createQName(CCConstants.CCM_ASSOC_METADATA_PRESETTING_TEMPLATE);
		//List<AssociationRef> result = nodeService.getTargetAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),qname);
		NodeRef result = nodeService.getChildByName(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),ContentModel.ASSOC_CONTAINS,CCConstants.TEMPLATE_NODE_NAME);

		if(result!=null)
			return result.getId();
		if(!create)
			return null;

		addAspect(nodeId,CCConstants.CCM_ASPECT_METADATA_PRESETTING);
		Map<String,String[]> props = new HashMap<>();
		props.put(CCConstants.CM_NAME,new String[]{CCConstants.TEMPLATE_NODE_NAME});
		String id=createNode(nodeId,CCConstants.CCM_TYPE_IO, props);
		nodeService.createAssociation(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,id),
				assocQName);
		addAspect(id,CCConstants.CCM_ASPECT_METADATA_PRESETTING_TEMPLATE);
		return id;
	}
	@Override
	public void setTemplateProperties(String nodeId, Map<String, String[]> props) throws Throwable {
		//updateNode(getOrCreateTemplateNode(nodeId),props);
		String template = getTemplateNode(nodeId,true);
		String nodeType = getType(template);
		Map<String, Object> toSafeProps = getToSafeProps(props, nodeType, null, template, nodeId, "io_template");
		updateNodeNative(template, toSafeProps);
	}

	@Override
	public void setTemplateStatus(String nodeId, Boolean enable) throws Throwable{
		addAspect(nodeId,CCConstants.CCM_ASPECT_METADATA_PRESETTING);
		nodeService.setProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),
				QName.createQName(CCConstants.CCM_PROP_METADATA_PRESETTING_STATUS),
				enable);

	}

	@Override
	public Long getContentLength(String storeProtocol, String storeId, String nodeId, String version, String contentProp) throws Throwable {
		ContentReader reader = getContentReader(storeProtocol, storeId, nodeId, version, contentProp);
		if(reader != null && reader.getContentData() != null) {
			return reader.getContentData().getSize();
		}
		return null;
	}

	@Override
	public ContentReader getContentReader(String storeProtocol, String storeId, String nodeId, String version, String contentProp){
		NodeRef nodeRef=new NodeRef(new StoreRef(storeProtocol, storeId), nodeId);
		if(version==null) {
			ContentReader cr = contentService.getReader(nodeRef, QName.createQName(contentProp));
			if(cr != null) {
				return cr.getReader();
			}else return null;
		}
		else{
			VersionHistory versionHistory = serviceRegistry.getVersionService().getVersionHistory(nodeRef);
			Version versionObj = versionHistory.getVersion(version);
			ContentReader cr = contentService.getReader(versionObj.getFrozenStateNodeRef(), QName.createQName(contentProp)).getReader();
			if(cr != null) {
				return cr.getReader();
			}else return null;
		}
	}
	@Override
	public InputStream getContent(String storeProtocol, String storeId, String nodeId,String version,String contentProp) throws Throwable{
		ContentReader cr = getContentReader(storeProtocol,storeId,nodeId,version,contentProp);
		if(cr != null) {
			return cr.getContentInputStream();
		}else return null;
	}

	@Override
	public String getContentHash(String storeProtocol, String storeId, String nodeId, String version, String contentProp) {
		try{

			return ""+getContentReader(storeProtocol,storeId,nodeId,version,contentProp).getContentData().hashCode();
		}catch(Throwable t){
			return null;
		}
	}

	@Override
	public void addAspect(String nodeId, String aspect) {
		apiClient.addAspect(nodeId, aspect);
	}

	@Override
	public void removeAspect(String nodeId, String aspect) {
		apiClient.removeAspect(nodeId, aspect);
	}
	@Override
	public void removeProperty(String storeProtocol, String storeId, String nodeId, String property) {
		// when interceptors are active, use set instead to trigger interceptors
		if(PropertiesInterceptorFactory.getPropertiesSetInterceptors().size() > 0) {
			setProperty(storeProtocol, storeId, nodeId, property, null, true);
		} else {
			nodeService.removeProperty(new NodeRef(new StoreRef(storeProtocol, storeId), nodeId), QName.createQName(property));
		}
	}
	@Override
	public String[] getAspects(String storeProtocol, String storeId, String nodeId){
		return apiClient.getAspects(new NodeRef(new StoreRef(storeProtocol,storeId),nodeId));
	}

	@Override
	public void moveNode(String newParentId, String childAssocType, String nodeId) {
		try{
			apiClient.moveNode(newParentId, childAssocType, nodeId);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public void revertVersion(String nodeId, String verLbl) throws Exception {
		try{
			apiClient.revertVersion(nodeId, verLbl);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Map<String, Object>> getVersionHistory(String nodeId) throws Throwable {
		return apiClient.getVersionHistory(nodeId);
	}

	@NotNull
	@Override
	public List<String> getVersionLabelsHistory(String nodeId){
		return Optional.of(versionService)
				.map(x->x.getVersionHistory(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId)))
				.map(VersionHistory::getAllVersions)
				.map(x->x.stream()
						.map(Version::getVersionLabel)
						.collect(Collectors.toList()))
				.orElse(new ArrayList<>());
	}

	@Override
	public String importNode(String nodeId,String localParent) throws Throwable {
		throw new Exception("Not supported for local repository");
	}

	@Override
	public User getOwner(String storeId, String storeProtocol, String nodeId){
		return apiClient.getOwner(storeId, storeProtocol, nodeId);
	}

	@Override
	public void removeNode(String protocol, String store, String nodeId) {
		apiClient.removeNode(new StoreRef(protocol, store), nodeId);
	}
	@Override
	public boolean exists(String protocol, String store, String nodeId) {
		return nodeService.exists(new NodeRef(new StoreRef(protocol, store), nodeId));
	}

	@Override
	public String getContentMimetype(String protocol, String storeId, String nodeId) {
		if(PermissionServiceFactory.getPermissionService(appId).hasPermission(protocol,storeId,nodeId,CCConstants.PERMISSION_READ))
			return new MCAlfrescoAPIClient().getAlfrescoMimetype(new NodeRef(protocol,storeId,nodeId));
		else
			throw new AccessDeniedException("No "+CCConstants.PERMISSION_READ+" permission on node "+nodeId);
	}
	@Override
	public List<AssociationRef> getNodesByAssoc(String nodeId, AssocInfo assoc) {
		NodeRef ref = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
		if(assoc.getDirection().equals(AssocInfo.Direction.SOURCE)) {
			return nodeService.getSourceAssocs(ref,QName.createQName(assoc.getAssocName()));
		}
		else{
			return nodeService.getTargetAssocs(ref,QName.createQName(assoc.getAssocName()));
		}
	}

	public void setProperty(String protocol, String storeId, String nodeId, String property, Serializable value, boolean skipDefinitionChecks) {
		NodeRef nodeRef = new NodeRef(new StoreRef(protocol, storeId), nodeId);
		property = NameSpaceTool.transformToLongQName(property);
		QName prop = QName.createQName(property);
		if(!skipDefinitionChecks) {
			PropertyDefinition propertyDefinition = dictionaryService.getProperty(prop);
			if (propertyDefinition == null) {
				logger.error("property" + property + " is not defined in content model");
				return;
			}

			if (!propertyDefinition.isMultiValued() && value instanceof Collection) {
				if (((Collection) value).stream().iterator().hasNext()) {
					value = (Serializable) ((Collection) value).stream().iterator().next();
				} else {
					value = null;
				}
			}
		}
		Map<String, Object> properties = null;
		if(PropertiesInterceptorFactory.getPropertiesSetInterceptors().size() > 0) {
			try {
				properties = nodeService.getProperties(nodeRef).entrySet().stream().collect(
						HashMap::new,
						(m,entry)-> m.put(entry.getKey().toString(), entry.getValue()),
						HashMap::putAll
				);
				properties.put(property, value);
				for (PropertiesSetInterceptor i : PropertiesInterceptorFactory.getPropertiesSetInterceptors()) {
					try {
						properties = i.beforeSetProperties(PropertiesInterceptorFactory.getPropertiesContext(
								nodeRef,
								properties,
								Arrays.asList(getAspects(protocol, storeId, nodeId)), null, null)
						);
					} catch (Throwable e) {
						logger.warn("Error while calling interceptors " + i.getClass().getName() + ": " + e);
					}
				}
			} catch (Throwable e) {
				logger.warn("Error while handling set interceptors: " + e);
			}
		}
		if(properties != null) {
			updateNodeNative(nodeRef.getStoreRef(), nodeRef.getId(), properties);
			nodeService.setProperties(nodeRef, properties.entrySet().stream().collect(
					HashMap::new,
					(m,entry)-> m.put(QName.createQName(entry.getKey()), (Serializable) entry.getValue()),
					HashMap::putAll
			));
		} else {
			nodeService.setProperty(nodeRef, prop, value);
		}
	}

	@Override
	public GetPreviewResult getPreview(String storeProtocol, String storeIdentifier, String nodeId, Map<String, Object> nodeProps, String version){
		boolean isIcon;
		if(nodeProps == null) {
			NodeRef nodeRef = new NodeRef(new StoreRef(storeProtocol, storeIdentifier), nodeId);
			isIcon = NodeServiceHelper.getProperty(nodeRef,CCConstants.CCM_PROP_MAP_ICON) != null && NodeServiceHelper.getProperty(nodeRef,CCConstants.CM_ASSOC_THUMBNAILS) != null;
		} else {
			isIcon = nodeProps.get(CCConstants.CCM_PROP_MAP_ICON) != null && nodeProps.get(CCConstants.CM_ASSOC_THUMBNAILS) != null;
		}
		return new GetPreviewResult(getPreviewUrl(storeProtocol,storeIdentifier,nodeId,version),isIcon);
	}

	@Override
	public String getPrimaryParent(String protocol, String store, String nodeId) {
		return nodeService.getPrimaryParent(new NodeRef(new StoreRef(protocol, store), nodeId)).getParentRef().getId();
	}

	@Override
	public Collection<org.edu_sharing.service.model.NodeRef> getFrontpageNodes() throws Throwable {
		return new NodeFrontpage().getNodesForCurrentUserAndConfig();
	}


	/**
	 * maps to the original node id for:
	 * collection refs
	 * published copies
	 * @param nodeId the source node to map
	 * @return the mapped node ref
	 */
	@Override
	public NodeRef getOriginalNode(String nodeId) {
		if(!nodeService.exists(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId))) {
			return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		}
		// Handle io references (i.e. collection refs)
		// use the nodeServiceAlfresco since the nodeRef might be null if original was deleted
		if(hasAspect(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
			nodeId = (String) nodeServiceAlfresco.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));

			if(!nodeService.exists(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId))) {
				return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
			}
		}
		// handle copied nodes
		NodeRef original = ((NodeRef)nodeServiceAlfresco.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), QName.createQName(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL)));
		if(original != null) {
			nodeId = original.getId();
		}
		return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
	}
}
