package org.edu_sharing.service.bulk;


import com.typesafe.config.Config;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.EduSharingLockHelper;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BulkServiceImpl implements BulkService, ApplicationListener<RefreshScopeRefreshedEvent> {
	public static final String PRIMARY_FOLDER_NAME = "SYNC_OBJ";
	public static final String NEW_DATA_FOLDER_NAME = "NEW";

	/**
	 * these internal properties will be ignored from the mds and never touched by the bulk service sync method
	 */
	private static final List<String> IGNORE_PROPERTIES = Stream.of(
			ContentModel.PROP_NODE_UUID,
			ContentModel.PROP_VERSION_LABEL,
			ContentModel.PROP_INITIAL_VERSION,
			ContentModel.PROP_VERSION_TYPE
	).map(QName::toString).collect(Collectors.toList());
	static NodeService nodeServiceAlfresco = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
	static final ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	static VersionService versionServiceAlfresco = serviceRegistry.getVersionService();
	static Repository repositoryHelper = (Repository) AlfAppContextGate.getApplicationContext().getBean("repositoryHelper");
	NodeService dbNodeService = (NodeService)AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");

	private static Logger logger = Logger.getLogger(BulkServiceImpl.class);
	private final List<String> propsToClean;
	private NodeRef primaryFolder;
	private List<BulkServiceInterceptorInterface> interceptors;


	/**
	 * get or create the folder
	 * @param parent
	 * @param name
	 * @param propertiesNative Provide the properties of the created child. This will be taken into account when setting the metadataset id of the folder
	 * @return
	 */

	public NodeRef getOrCreate(NodeRef parent, String name, Map<String, Object> propertiesNative){
		name = NodeServiceHelper.cleanupCmName(name);
		NodeRef replicationFolder = getOrCreateFolderInternal(parent, name, propertiesNative);
		BulkUpdateBehaviour behaviour = getBehaviourConfig(replicationFolder);
		if(behaviour.equals(BulkUpdateBehaviour.None)) {
			return replicationFolder;
		} else if(behaviour.equals(BulkUpdateBehaviour.SeparateViaFolder)) {
			NodeRef newFolder = getOrCreateFolderInternal(replicationFolder, NEW_DATA_FOLDER_NAME, propertiesNative);
			copyPermissions(replicationFolder, newFolder);
			String dateName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			return getOrCreateFolderInternal(newFolder, dateName, propertiesNative);
		}
		throw new IllegalArgumentException("unkown behaviour: " + behaviour);
	}

	private void copyPermissions(NodeRef fromFolder, NodeRef toFolder) {
		PermissionService permissionService = serviceRegistry.getPermissionService();
		Set<AccessPermission> permissions = permissionService.getAllSetPermissions(fromFolder).stream().filter(p -> !p.getAuthorityType().equals(AuthorityType.EVERYONE) && !p.isInherited()).collect(Collectors.toSet());
		permissionService.deletePermissions(toFolder);
		permissionService.setInheritParentPermissions(toFolder, false);
		permissions.forEach(p -> permissionService.setPermission(toFolder, p.getAuthority(), p.getPermission(), p.getAccessStatus().equals(AccessStatus.ALLOWED)));
	}

	private static NodeRef getOrCreateFolderInternal(NodeRef parent, String name, Map<String, Object> propertiesNative) {
		NodeRef node = nodeServiceAlfresco.getChildByName(parent, ContentModel.ASSOC_CONTAINS, name);
		if (node == null) {
			return createFolder(parent, name, propertiesNative);
		}
		return node;
	}

	private static NodeRef createFolder(NodeRef parent, String name, Map<String, Object> propertiesNative) {
		Map<QName, Serializable> props = new HashMap<>();
		props.put(ContentModel.PROP_NAME, name);
		if (propertiesNative != null) {
			props.put(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET),
					(Serializable) propertiesNative.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
			props.put(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET),
					(Serializable) propertiesNative.get(CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET));
		}
		return serviceRegistry.getNodeService().createNode(parent, ContentModel.ASSOC_CONTAINS,
				QName.createQName(name),
				QName.createQName(CCConstants.CCM_TYPE_MAP),
				props).getChildRef();
	}

	private BulkUpdateBehaviour getBehaviourConfig(NodeRef primaryFolder) {
		String value = (String) serviceRegistry.getNodeService().getProperty(primaryFolder, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONFOLDERUPDATE));
		if(value == null) {
			return BulkUpdateBehaviour.None;
		}
		return BulkUpdateBehaviour.valueOf(value);
	}

	@Override
	public NodeRef getPrimaryFolder() {
		return primaryFolder;
	}

	public BulkServiceImpl(){
		primaryFolder = getOrCreateFolderInternal(repositoryHelper.getCompanyHome(), PRIMARY_FOLDER_NAME, null);


		// all props which might be overriden through the user, reset them and clean them
		propsToClean = new ArrayList<>();
		propsToClean.addAll(Arrays.asList(NodeCustomizationPolicies.SAFE_PROPS));
		propsToClean.addAll(Arrays.asList(NodeCustomizationPolicies.LICENSE_PROPS));
		propsToClean.addAll(CCConstants.getLifecycleContributerPropsMap().values());
		propsToClean.addAll(CCConstants.getMetadataContributerPropsMap().values());
		/*
		for(QName aspect : dictionaryService.getAllAspects()){
			propsToClean.addAll(
					dictionaryService.getAspect(aspect).getProperties().keySet().stream()
							.map(QName::toString).collect(Collectors.toList())
			);
		}*/
		refresh();
	}

	@Override
	public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
		refresh();
	}

	private void refresh() {
		Config config = LightbendConfigLoader.get().getConfig("repository.bulk");
		interceptors = config.getStringList("interceptors").stream().map(i -> {
			try {
				return (BulkServiceInterceptorInterface)Class.forName(i).getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
					 NoSuchMethodException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
	}



	@Override
	public NodeRef sync(String group, List<String> match, List<String> groupBy, String type, List<String> aspects, Map<String, String[]> properties, boolean resetVersion) throws Throwable {
		if(match == null || match.size() == 0){
			throw new IllegalArgumentException("match should contain at least 1 property");
		}
		Map<String, String[]> propertiesFiltered = properties.entrySet().stream().filter((e) -> match.contains(e.getKey())).
				collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(a, b) -> b, HashMap::new));
		if(propertiesFiltered.size() != match.size()){
			throw new IllegalArgumentException("match contained property names that did not exist in the properties section. Please check that you provide data for all match properties.");
		}
		properties = NodeServiceHelper.transformShortToLongProperties(properties);
		Map<String, Object> propertiesNative = NodeServiceHelper.getPropertiesSinglevalue(properties);
		propertiesNative.put(CCConstants.CM_NAME, NodeServiceHelper.cleanupCmName((String)(propertiesNative.get(CCConstants.CM_NAME))) + "_" + System.currentTimeMillis());
		propertiesNative.remove(CCConstants.CCM_PROP_IO_VERSION_COMMENT);
		for (BulkServiceInterceptorInterface interceptor : interceptors) {
			propertiesNative = interceptor.preprocessProperties(propertiesNative);
		}
		// filter for valid/declared properties to store
		Map<String, Object> rawProperties = new HashMap<>(propertiesNative);
		propertiesNative = propertiesNative.entrySet().stream().filter(property -> {
			QName prop = QName.createQName(property.getKey());
			return serviceRegistry.getDictionaryService().getProperty(prop) != null;
		}).collect(
				HashMap::new,
				(map, e) -> map.put(e.getKey(), e.getValue()),
				Map::putAll
		);
		String lockId = propertiesFiltered.values().stream().filter(Objects::nonNull).map(v -> v[0]).collect(Collectors.joining(","));
		Map<String, Object> finalPropertiesNative = propertiesNative;
		NodeRef result = EduSharingLockHelper.runSingleton(BulkServiceImpl.class,"sync_"  + lockId, () -> {
			NodeRef existing = null;
			try {
				existing = find(propertiesFiltered);
				Map<String, Object> propertiesNativeMapped = finalPropertiesNative;
				if (existing == null) {
					NodeRef groupFolder = getOrCreate(primaryFolder, group, finalPropertiesNative);
					if (groupBy != null && groupBy.size() > 0) {
						if (groupBy.size() == 1) {
							groupFolder = getOrCreate(groupFolder, rawProperties.get(CCConstants.getValidGlobalName(groupBy.get(0))).toString(), finalPropertiesNative);
						} else {
							throw new IllegalArgumentException("groupBy currently only supports exactly one value");
						}
					}
					// clean up and remove "null" values since they will result in weird data otherwise
					propertiesNativeMapped = new HashMap<>(finalPropertiesNative.entrySet().stream().filter((e) -> e.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
					// add a default comment for bulk import
					propertiesNativeMapped.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, CCConstants.VERSION_COMMENT_BULK_CREATE);
					existing = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
							NodeServiceFactory.getLocalService().createNodeBasic(
									groupFolder.getId(),
									CCConstants.getValidGlobalName(type),
									propertiesNativeMapped
							));
					// 2. versioning (use the regular service for proper versioning)
					NodeServiceFactory.getLocalService().createVersion(existing.getId());
				} else {
					String blocked = NodeServiceHelper.getProperty(existing, CCConstants.CCM_PROP_IO_IMPORT_BLOCKED);
					if (Boolean.parseBoolean(blocked)) {
						throw new IllegalStateException("The given node was blocked for any updates and should not be reimported");
					}
					Map<String, Object> propertiesKeep = checkInternalOverrides(propertiesNativeMapped, existing);
					if (resetVersion) {
						versionServiceAlfresco.deleteVersionHistory(existing);
					}
					propertiesNativeMapped = getCleanProps(existing, finalPropertiesNative);
					propertiesNativeMapped.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, resetVersion ? CCConstants.VERSION_COMMENT_BULK_CREATE : CCConstants.VERSION_COMMENT_BULK_UPDATE);
					NodeServiceFactory.getLocalService().updateNodeNative(existing.getId(), propertiesNativeMapped);
					// version the previous state
					NodeServiceFactory.getLocalService().createVersion(existing.getId());
					if (propertiesKeep != null) {
						propertiesKeep = getCleanProps(existing, propertiesKeep);
						propertiesKeep.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, CCConstants.VERSION_COMMENT_BULK_UPDATE_RESYNC);
						NodeServiceFactory.getLocalService().updateNodeNative(existing.getId(), propertiesKeep);
						// 2. versioning
						NodeServiceFactory.getLocalService().createVersion(existing.getId());
					}
				}
			}catch (Exception e) {
				throw new RuntimeException(e);
			}
			return existing;
		});
		if(aspects != null) {
			aspects.forEach((a) -> NodeServiceFactory.getLocalService().addAspect(result.getId(), CCConstants.getValidGlobalName(a)));
		}
		for (BulkServiceInterceptorInterface interceptor : interceptors) {
			interceptor.onNodeCreated(result, propertiesNative);
		}
		return result;

	}
	private List<String> getAllAvailableProperties(NodeRef nodeRef) throws Exception {

		/*Map<String, Serializable> cleanProps = new HashMap<>();
		propsToClean.forEach((k) -> cleanProps.put(k, null));
		return cleanProps;*/
		return Stream.concat(MetadataHelper.getWidgetsByNode(nodeRef, false).stream().map((w) -> CCConstants.getValidGlobalName(w.getId())).
								filter(Objects::nonNull).
								filter(id -> !IGNORE_PROPERTIES.contains(id)).
								filter(id -> !id.startsWith("{virtualproperty}")),
						propsToClean.stream())
				.collect(Collectors.toList());
	}
	/**
	 * alfresco will not override "removed" props, so we try to clean up via the mds
	 * @return
	 */
	private Map<String, Object> getCleanProps(NodeRef nodeRef, Map<String, Object> props) throws Exception {
		Map<String, Object> mergedProps = new HashMap<>();
		getAllAvailableProperties(nodeRef).forEach((k) -> mergedProps.put(k, null));
		mergedProps.putAll(props);
		mergedProps.put(CCConstants.CCM_PROP_IO_CREATE_VERSION, false);
		return mergedProps;
	}
	/**
	 * This method takes new properties as an input, and checks which properties might be already "touched" internally
	 * (meaning not by the bulk import itself) and will always keep the latest version of the internal ones
	 * @param propertiesIn
	 * @param nodeRef
	 * @return
	 */
	private Map<String, Object> checkInternalOverrides(Map<String, Object> propertiesIn, NodeRef nodeRef) throws Exception {
		VersionHistory history = versionServiceAlfresco.getVersionHistory(nodeRef);
		if(history == null){
			return null;
		}
		List<Version> versions = new ArrayList<>(history.getAllVersions());
		Collections.reverse(versions);
		logger.debug(propertiesIn.get(CCConstants.CM_NAME));
		Map<String, Serializable> importerProps = null;
		Map<String, Object> modifiedProps = new HashMap<>();
		boolean changed = false;
		for(Version version : versions){
			Map<String, Serializable> currentProps = version.getVersionProperties();
			String vname = (String) currentProps.get(CCConstants.CCM_PROP_IO_VERSION_COMMENT);
			if(vname.equals(CCConstants.VERSION_COMMENT_BULK_CREATE)){
				importerProps = version.getVersionProperties();
				continue;
			}
			if(importerProps != null){
				if(vname.equals(CCConstants.VERSION_COMMENT_BULK_UPDATE)){
					Map<String, Object> diffs = getPropDiffs(importerProps, currentProps);
					for (String diff : diffs.keySet()) {
						importerProps.put(diff, currentProps.get(diff));
					}
				} else if(vname.equals(CCConstants.VERSION_COMMENT_BULK_UPDATE_RESYNC)){
					// we do nothing for these, these are just resynced once from previous changes
				} else {
					modifiedProps.putAll(getPropDiffs(importerProps, currentProps));
				}
			}
		}
		Map<String, Object> returnProps = new HashMap<>(propertiesIn);
		if(importerProps!=null) {
			Collection<String> widgets = getAllAvailableProperties(nodeRef);
			// copy all new props which are untouched
			for (Map.Entry<String, Object> entry : modifiedProps.entrySet()) {
				if(!widgets.contains(entry.getKey())){
					continue;
				}
				logger.info("Property " + entry.getKey() + " ignored since it was modified outside of the bulk api");
				returnProps.put(entry.getKey(), entry.getValue());
				changed = true;
			}
		}
		if(changed) {
			return returnProps;
		}
		return null;
	}

	/**
	 * Return all props that have a different value inside the "diff" map
	 */
	private Map<String, Object> getPropDiffs(Map<String, Serializable> base, Map<String, Serializable> diff) {
		Map<String, Object> diffs = new HashMap<>();
		//diffs.putAll(getPropDiffsOneWay(base,diff));
		//diffs.putAll(getPropDiffsOneWay(diff,base));
		for(Map.Entry<String, Serializable> entry : diff.entrySet()){
			Serializable entryDiff = base.get(entry.getKey());
			if(Objects.equals(entryDiff,entry.getValue())){
				continue;
			}
			diffs.put(entry.getKey(),entry.getValue());
		}
		return diffs;
	}

	@Override
	public NodeRef find(Map<String, String[]> properties) throws Exception {
		CMISSearchHelper.CMISSearchData data = new CMISSearchHelper.CMISSearchData();
		// this uses SOLR and is not synchronized!
		// data.inTree = primaryFolder.getId();

		List<NodeRef> result = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO,
				NodeServiceHelper.getPropertiesSinglevalue(
						NodeServiceHelper.transformShortToLongProperties(properties)
				),data);
		result = filterCMISResult(result, primaryFolder);
		if(result.size()==1){
			return result.get(0);
		}else if(result.size()>1){
			StringBuilder props = new StringBuilder();
			for (Map.Entry<String, String[]> entry : properties.entrySet()) {
				props.append(entry.getKey()).append(":").append(entry.getValue()[0]).append(" ");
			}
			throw new Exception("The given properties ("+props+") matched more than 1 node (" + result.size() + "). Please check your criterias and make sure they match unique data");
		}
		return null;
	}

	@Override
	public List<BulkRun> runs(String replicationsource, BulkRun.RunState filterByState) throws DAOException {
		String name = NodeServiceHelper.cleanupCmName(replicationsource);
		NodeRef replicationFolder = getOrCreateFolderInternal(getPrimaryFolder(), name, null);
		if(replicationFolder == null) {
			return null;
		}
		NodeRef newFolder = serviceRegistry.getNodeService().getChildByName(replicationFolder, ContentModel.ASSOC_CONTAINS, NEW_DATA_FOLDER_NAME);
		if(newFolder == null) {
			return Collections.emptyList();
		}
		return serviceRegistry.getNodeService().getChildAssocs(newFolder, Collections.singleton(QName.createQName(CCConstants.CCM_TYPE_MAP))).stream().map(
				child -> {
					try {
						BulkRun.RunState state = serviceRegistry.getPermissionService().getAllSetPermissions(child.getChildRef()).stream()
								.anyMatch((p) -> p.getAuthorityType().equals(AuthorityType.EVERYONE) && p.getAccessStatus().equals(AccessStatus.ALLOWED)) ?
								BulkRun.RunState.Published : BulkRun.RunState.New;
						if(filterByState == null || state.equals(filterByState)) {
							return new BulkRun(
									NodeDao.getNode(RepositoryDao.getHomeRepository(), child.getChildRef().getId(), Filter.createShowAllFilter()).asNode(),
									state
							);
						} else {
							return null;
						}
					} catch (DAOException e) {
						throw new RuntimeException(e);
					}
				}
		).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static List<NodeRef> filterCMISResult(List<NodeRef> result, NodeRef primaryFolder){
		NodeService dbNodeService = (NodeService)AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
		return result.stream().filter((r) -> {
			Path path = dbNodeService.getPath(r);
			for (Path.Element p : path) {
				if (p instanceof Path.ChildAssocElement) {
					NodeRef ref = ((Path.ChildAssocElement) p).getRef().getParentRef();
					if (primaryFolder.equals(ref)) {
						return true;
					}
				}
			}
			return false;
		}).collect(Collectors.toList());
	}

}
