package org.edu_sharing.service.collection;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.search.impl.solr.ESSearchParameters;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.edu_sharing.restservices.CollectionDao;
import org.edu_sharing.restservices.CollectionDao.Scope;
import org.edu_sharing.restservices.CollectionDao.SearchScope;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceInterceptor;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;
import org.edu_sharing.service.notification.Status;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.remote.RemoteObjectService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;


public class CollectionServiceImpl implements CollectionService {

    private NotificationService notificationService;

    public static CollectionService build(String appId) {
        CollectionServiceConfig config = (CollectionServiceConfig) ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
        return new CollectionServiceImpl(appId, config.getPattern(), config.getPath());
    }


    Logger logger = Logger.getLogger(CollectionServiceImpl.class);

    String pattern;

    String path;


    ApplicationInfo appInfo = null;

    MCAlfrescoAPIClient client = null;

    AuthenticationTool authTool = null;

    BehaviourFilter policyBehaviourFilter;

    Map<String, String> authInfo;

    SearchService searchService;
    NodeService nodeService;

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    TransactionService transactionService = serviceRegistry.getTransactionService();

    ToolPermissionService toolPermissionService;

    org.edu_sharing.service.permission.PermissionService permissionService;

    public CollectionServiceImpl(String appId, String pattern, String path) {
        try {
            this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);

            this.authTool = RepoFactory.getAuthenticationToolInstance(appId);

			String guestUn = ApplicationInfoList.getHomeRepository().getGuest_username();

            //fix for running in runas user mode
            if ((AuthenticationUtil.isRunAsUserTheSystemUser()
                    || "admin".equals(AuthenticationUtil.getRunAsUser()))
					|| Context.getCurrentInstance().getCurrentInstance() == null
					|| (guestUn != null
					&& guestUn.equals(AuthenticationUtil.getFullyAuthenticatedUser()) )) {
                logger.debug("starting in runas user mode");
                this.authInfo = new HashMap<>();
                this.authInfo.put(CCConstants.AUTH_USERNAME, AuthenticationUtil.getRunAsUser());
            } else {
                this.authInfo = this.authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
            }
            try {
                this.client = new MCAlfrescoAPIClient();
            } catch (net.sf.acegisecurity.AuthenticationCredentialsNotFoundException e) {
                //when remote auth
                logger.warn(e.getMessage());
            }
            this.searchService = SearchServiceFactory.getSearchService(appId);
            this.nodeService = NodeServiceFactory.getNodeService(appId);
            this.pattern = pattern;
            this.path = path;
            this.toolPermissionService = ToolPermissionServiceFactory.getInstance();
            this.permissionService = PermissionServiceFactory.getPermissionService(appId);
            this.notificationService = NotificationServiceFactoryUtility.getNotificationService(appId);
            ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
            policyBehaviourFilter = (BehaviourFilter) appContext.getBean("policyBehaviourFilter");

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String addToCollection(String collectionId, String refNodeId, boolean allowDuplicate) throws Throwable {

        try {
            /**
             * use original
             */
            String nodeId = refNodeId;
            String originalNodeId;
			if(AuthenticationUtil.runAsSystem(() -> nodeService.hasAspect(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),refNodeId,CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
                originalNodeId = client.getProperty(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), refNodeId, CCConstants.CCM_PROP_IO_ORIGINAL);
            } else {
                originalNodeId = refNodeId;
            }
            NodeRef originalNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, originalNodeId);

			// user must have CC_PUBLISH on either the original, a reference object or it is already proposed
            if (!client.hasPermissions(originalNodeId, new String[]{CCConstants.PERMISSION_CC_PUBLISH})
					&& !client.hasPermissions(nodeId, new String[]{CCConstants.PERMISSION_CC_PUBLISH})
					// we allow adding if it is a proposal since it was proposed from someone with CC_PUBLISH
					// DESP-989
					&& !getChildrenProposalIntern(collectionId).stream().anyMatch(
					c -> c.getTargetRef().equals(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, originalNodeId))
			)){
                String message = I18nServer.getTranslationDefaultResourcebundleNoException("collection_no_publish_permission");
                throw new Exception(message);
            }
            NodeRef collectionRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, collectionId);
            boolean collectionIsPublic = false;
            PermissionService alfPermissionService = serviceRegistry.getPermissionService();
            Set<AccessPermission> permissions = alfPermissionService.getAllSetPermissions(collectionRef);
            for (AccessPermission accessPermission : permissions) {
                if (PermissionService.ALL_AUTHORITIES.equals(accessPermission.getAuthority())) {
                    if (PermissionService.READ.equals(accessPermission.getPermission())
                            || PermissionService.CONSUMER.equals(accessPermission.getPermission())
                            || "ConsumerMetadata".equals(accessPermission.getPermission())
                            || PermissionService.EDITOR.equals(accessPermission.getPermission())
                            || PermissionService.CONTRIBUTOR.equals(accessPermission.getPermission())
                            || PermissionService.COORDINATOR.equals(accessPermission.getPermission())) {
                        collectionIsPublic = true;
                    }
                }

            }

            if (collectionIsPublic && !toolPermissionService.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES)
                    && !client.isOwner(collectionId, AuthenticationUtil.getFullyAuthenticatedUser())) {
                throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES);
            }

            String originalNodeType = client.getNodeType(originalNodeId);
            if (!originalNodeType.equals(CCConstants.CCM_TYPE_IO)) {
                throw new Exception("Only Files are allowed to be added!");
            }
			//NodeRef child = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, collectionId, CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_ORIGINAL, originalNodeId);
			if(!allowDuplicate) {
				List<NodeRef> child = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO, new HashMap<>() {{
                            put(CCConstants.CCM_PROP_IO_ORIGINAL, originalNodeId);
                        }},
						CMISSearchHelper.CMISSearchData.builder().inFolder(collectionId).build()
				);
				if (!child.isEmpty()) {
                throw new DuplicateNodeException("Node is already in collection");
            }
			}
            /*
            for(ChildAssociationRef node : nodeService.getChildrenChildAssociationRef(collectionId)){
                // TODO: Maybe we can find a faster way to determine it?
                String nodeRef = client.getProperty(node.getChildRef().getStoreRef().getProtocol(),node.getChildRef().getStoreRef().getIdentifier(),node.getChildRef().getId(), CCConstants.CCM_PROP_IO_ORIGINAL);
                if(originalNodeId.equals(nodeRef)){
                    String message = I18nServer.getTranslationDefaultResourcebundle("collection_already_in", locale);

                    throw new DuplicateNodeException(message);
                }
            }
            */

            // we need to copy as system because the user may just has full access to the ref (which may has different metadata)
            // we check the add children permissions before continuing
            if (!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionId, CCConstants.PERMISSION_ADD_CHILDREN)) {
                throw new SecurityException("No permissions to add childrens to collection");
            }

            Map<String, Object> props = AuthenticationUtil.runAsSystem(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put(CCConstants.CM_PROP_VERSIONABLELABEL, NodeServiceHelper.getProperty(originalNodeRef, CCConstants.CM_PROP_VERSIONABLELABEL));
                    data.put(CCConstants.LOM_PROP_TECHNICAL_SIZE, NodeServiceHelper.getProperty(originalNodeRef, CCConstants.LOM_PROP_TECHNICAL_SIZE));
                    return data;
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });
            String versLabel = (String) props.get(CCConstants.CM_PROP_VERSIONABLELABEL);

            permissionService.addToRecentProperty(CCConstants.CCM_PROP_PERSON_RECENT_COLLECTIONS, collectionRef);

			return AuthenticationUtil.runAsSystem(() -> {
				/**
				 * make a copy of the original.
				 * OnCopyCollectionRefPolicy cares about
				 * - not duplicating the content
				 * - ignore childs: usage and license data
				 * - the preview child will be copied
				 */
				String refId = client.copyNode(originalNodeId, collectionId, false);

                permissionService.setPermissions(refId, null, true);
                client.addAspect(refId, CCConstants.CCM_ASPECT_POSITIONABLE);


                /*
                 * write content, so that the index tracking will be triggered
                 * the overwritten NodeContentGet class checks if it' s an collection ref object
                 * and switches the nodeId to original node, which is used for indexing
                 * Do a transaction and disable all policy filters to prevent quota exceptions to kick in
                 *
                 * Alfresco 5.2 Set mimetype 5.2 to prevent thumbnail transformation to crash with different mimetype warning
                 * "Transformation has not taken place because the declared mimetype (image/jpeg) does not match the detected mimetype (text/plain)"
                 */
                NodeServiceInterceptor.ignoreQuota(() -> {
                    client.writeContent(refId, "1".getBytes(), "text/plain", "utf-8", CCConstants.CM_PROP_CONTENT);
                    return null;
                });

                //set to original size
                client.setProperty(refId, CCConstants.LOM_PROP_TECHNICAL_SIZE, (String) props.get(CCConstants.LOM_PROP_TECHNICAL_SIZE));

                // run setUsage as admin because the user may not has cc_publish on the original node (but on the ref)
                new Usage2Service().setUsageInternal(appInfo.getAppId(),
                        authInfo.get(CCConstants.AUTH_USERNAME),
                        appInfo.getAppId(),
                        collectionId,
                        originalNodeId, null, null, null, -1, versLabel, refId, null);

                String  colllectionType = null;
                List<String> collectionAspects;
                Map<String, Object> collectionProperties;
                try {
                    colllectionType = nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionId);
                    collectionAspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionId));
                    collectionProperties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionId);
                } catch (Throwable ignored) {
                    collectionAspects = new ArrayList<>();
                    collectionProperties = new HashMap<>();
                }

                String nodeType = null;
                List<String> nodeAspects;
                Map<String, Object> nodeProperties;
                try {
                    nodeType = nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), refNodeId);
                    nodeAspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), refNodeId));
                    nodeProperties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), refNodeId);
                } catch (Throwable ignored) {
                    nodeProperties = new HashMap<>();
                    nodeAspects = new ArrayList<>();
                }
                notificationService.notifyAddCollection(collectionId, refNodeId, colllectionType, collectionAspects, collectionProperties, nodeType, nodeAspects, nodeProperties, Status.ADDED);
                return refId;
            });

        } catch (Throwable e) {
            throw e;
        }
    }

    @Override
    public List<AssociationRef> getChildrenProposal(String parentId) throws Exception {
        if (!PermissionServiceHelper.hasPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId), CCConstants.PERMISSION_WRITE)) {
            throw new InsufficientPermissionException("No " + CCConstants.PERMISSION_WRITE + " on collection " + parentId);
        }
		return getChildrenProposalIntern(parentId);
	}

	private static List<AssociationRef> getChildrenProposalIntern(String parentId) {
        return NodeServiceFactory.getLocalService().getChildrenChildAssociationRefType(parentId, CCConstants.CCM_TYPE_COLLECTION_PROPOSAL).stream().map((proposal) -> {
            NodeRef target = new NodeRef(NodeServiceHelper.getProperty(proposal.getChildRef(), CCConstants.CCM_PROP_COLLECTION_PROPOSAL_TARGET));
            return new AssociationRef(proposal.getChildRef(), ContentModel.ASSOC_ORIGINAL, target);
        }).collect(Collectors.toList());
    }

    @Override
    public void proposeForCollection(String collectionId, String originalNodeId, String sourceRepositoryId)
            throws DuplicateNodeException, Throwable {
        String finalId = mapNodeId(originalNodeId, sourceRepositoryId);

        /*
        if(!PermissionServiceHelper.isNodePublic(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, finalId))) {
            throw new IllegalArgumentException("Suggested node is required to be publicly accessible");
        }
        */
        ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PROPOSAL);
        if (!PermissionServiceHelper.hasPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, collectionId), CCConstants.PERMISSION_READ)) {
            throw new InsufficientPermissionException("No " + CCConstants.PERMISSION_READ + " permissions for collection " + collectionId);
        }
        if (!PermissionServiceHelper.hasPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, finalId), CCConstants.PERMISSION_CC_PUBLISH)) {
            throw new InsufficientPermissionException("No " + CCConstants.PERMISSION_CC_PUBLISH + " permissions for collection " + collectionId);
        }
        AuthenticationUtil.runAsSystem(() -> {
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, finalId);
            if (getChildren(collectionId, null, new SortDefinition(), Collections.singletonList("files")).stream().anyMatch((ref) ->
                    nodeRef.getId().equals(NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_IO_ORIGINAL))
            )) {
                throw new DuplicateNodeException("Node id " + nodeRef.getId() + " is already in this collection");
            }
            if (getChildrenProposal(collectionId).stream().anyMatch((assoc) -> assoc.getTargetRef().equals(nodeRef))) {
                throw new DuplicateNodeException("Node id " + nodeRef.getId() + " is already proposed for this collection");
            }
            Map<String, Serializable> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, NodeServiceHelper.getProperty(nodeRef, CCConstants.CM_NAME));
            props.put(CCConstants.CCM_PROP_COLLECTION_PROPOSAL_TARGET, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, finalId));
            props.put(CCConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS, CCConstants.PROPOSAL_STATUS.PENDING);
            String refId = NodeServiceFactory.getLocalService().createNodeBasic(collectionId,
                    CCConstants.CCM_TYPE_COLLECTION_PROPOSAL,
                    props
            );

            String  colllectionType = null;
            List<String> collectionAspects;
            Map<String, Object> collectionProperties;
            try {
                colllectionType = nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionId);
                collectionAspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionId));
                collectionProperties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionId);
            } catch (Throwable ignored) {
                collectionAspects = new ArrayList<>();
                collectionProperties = new HashMap<>();
            }

            String nodeType = null;
            List<String> nodeAspects;
            Map<String, Object> nodeProperties;
            try {
                nodeType = nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), originalNodeId);
                nodeAspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), originalNodeId));
                nodeProperties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), originalNodeId);
            } catch (Throwable ignored) {
                nodeProperties = new HashMap<>();
                nodeAspects = new ArrayList<>();
            }
            notificationService.notifyProposeForCollection(collectionId, originalNodeId, colllectionType, collectionAspects, collectionProperties, nodeType, nodeAspects, nodeProperties, Status.ADDED);

            return refId;
        });
    }

    public String mapNodeId(String originalNodeId, String sourceRepositoryId) throws Throwable {
        if (sourceRepositoryId != null) {
            ApplicationInfo rep = ApplicationInfoList.getRepositoryInfoById(sourceRepositoryId);
            if (rep.ishomeNode() || ApplicationInfo.REPOSITORY_TYPE_LOCAL.equals(rep.getRepositoryType())) {
                return originalNodeId;
            }
            return RemoteObjectService.getOrCreateRemoteMetadataObject(sourceRepositoryId, originalNodeId);
        }
        return originalNodeId;
    }

    @Override
    public String addToCollection(String collectionId, String originalNodeId, String sourceRepositoryId, boolean allowDuplicate)
            throws DuplicateNodeException, Throwable {
        originalNodeId = mapNodeId(originalNodeId, sourceRepositoryId);
        return addToCollection(collectionId, originalNodeId, allowDuplicate);
    }

    @Override
    public Collection create(String parentId, Collection collection) throws Throwable {

        String currentUsername = null;
        ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS);
        if (Context.getCurrentInstance() != null) {
            currentUsername = authTool.validateAuthentication(Context.getCurrentInstance().getRequest().getSession()).get(CCConstants.AUTH_USERNAME);
        } else {
            if (AuthenticationUtil.getRunAsUser() != null) {
                currentUsername = AuthenticationUtil.getRunAsUser();
            }
        }

        final String fcurrentUsername = currentUsername;

        if (fcurrentUsername != null) {
            return AuthenticationUtil.runAsSystem(new RunAsWork<Collection>() {

                @Override
                public Collection doWork() throws Exception {
                    String parentIdLocal = parentId;
                    if (parentIdLocal == null) {

                        collection.setLevel0(true);

                        parentIdLocal = getHomePath();
                    }

                    Map<String, Object> props = asProps(collection);
                    try {
                        new DuplicateFinder().transformToSafeName(client.getChildren(parentIdLocal), props);
                    } catch (Throwable e) {
                        throw new Exception(e);
                    }

                    String collectionId = client.createNode(parentIdLocal, CCConstants.CCM_TYPE_MAP, props);
                    client.addAspect(collectionId, CCConstants.CCM_ASPECT_COLLECTION);
                    client.addAspect(collectionId, CCConstants.CCM_ASPECT_POSITIONABLE);

                    client.setOwner(collectionId, fcurrentUsername);
                    collection.setNodeId(collectionId);
                    return collection;
                }
            });
        } else {
            throw new Exception("not authenticated");
        }

    }

	/**
	 * return the main folder where ALL collections are stored
	 */
	@Override
	public String getCollectionHomeParent() {
		return AuthenticationUtil.runAsSystem(() -> {
			try {
				return NodeServiceHelper.getContainerRootPath(path);
			} catch (Throwable e) {
				throw new RuntimeException();
			}
		});
	}

    /**
     * return the folder id to the collection home where new collections should be created
     */
    @Override
    public String getHomePath() {
        return AuthenticationUtil.runAsSystem(() -> NodeServiceHelper.getContainerIdByPath(path, pattern));
    }

    @Override
    public Collection createAndSetScope(String parentId, Collection collection) throws Throwable {
        Collection col = create(parentId, collection);
        setScope(col);
        return col;
    }

    /**
     * @TODO not ready yet, set usage again, refId must be safed by addToCollection
     * set level0 to false when moving an root collection to an sub one
     * set level0 to true when moving an childcollection to root
     */
    @Override
    public void move(String toCollection, String toMove) {
        try {

            if (CCConstants.CCM_TYPE_IO.equals(client.getNodeType(toMove))) {
                String parent = client.getParents(toMove, true).keySet().iterator().next();
                client.moveNode(toCollection, CCConstants.CM_ASSOC_FOLDER_CONTAINS, toMove);

                Map<String, Map<String, Object>> assocNode = client.getAssocNode(toMove, CCConstants.CM_ASSOC_ORIGINAL);
                String originalNodeId = (String) assocNode.entrySet().iterator().next().getValue().get(CCConstants.SYS_PROP_NODE_UID);

                /**
                 * set the usage for the new collection
                 */
                Usage2Service usageService = new Usage2Service();
                Usage usage = usageService.getUsage(this.appInfo.getAppId(), parent, originalNodeId, toMove);
                client.removeNode(usage.getNodeId(), originalNodeId);
                usageService.setUsage(appInfo.getAppId(),
                        authInfo.get(CCConstants.AUTH_USERNAME),
                        appInfo.getAppId(),
                        toCollection,
                        originalNodeId, null, null, null, -1, usage.getUsageVersion(), toMove, null);

            } else {
                client.moveNode(toCollection, CCConstants.CM_ASSOC_FOLDER_CONTAINS, toMove);
                /**
                 * handle level0
                 */
                Map<String, Object> properties = client.getProperties(toMove);
                if (Boolean.parseBoolean((String) properties.get(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0))) {
                    if (Arrays.asList(client.getAspects(toCollection)).contains(CCConstants.CCM_ASPECT_COLLECTION)) {
                        client.setProperty(toMove, CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0, "false");
                    }
                } else {
                    if (!Arrays.asList(client.getAspects(toCollection)).contains(CCConstants.CCM_ASPECT_COLLECTION)) {
                        client.setProperty(toMove, CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0, "true");
                    }
                }
            }

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

    }

    @Override
    public void remove(String collectionId) {

        try {
            /**
             * first remove the children so that the usages from the original are also removed
             */
            // Moved to @BeforeDeleteIOPolicy

            /**
             * remove the collection
             */
            // maybe it is a collection on root and someone else already created a collection that day
            // this will result in an error, so we need to fetch the parent id as system
            String parent = AuthenticationUtil.runAsSystem(() -> {
                try {
                    return client.getParents(collectionId, true).keySet().iterator().next();
                } catch (Throwable throwable) {
                    logger.warn(throwable);
                    return null;
                }
            });
            client.removeNode(collectionId, parent);

        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void removeFromCollection(String collectionId, String nodeId) {
        try {
            String originalNodeId = nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.CCM_PROP_IO_ORIGINAL);
            client.removeNode(nodeId, collectionId);

            if (originalNodeId == null) {
                logger.warn("reference object " + nodeId + " has no originId, can not remove usage");
                return;
            }

            // Usage handling is now handled in @BeforeDeleteIOPolicy

            try {
                new RepositoryCache().remove(originalNodeId);
            } catch (Throwable t) {
                // may fail if original has no access, however, this is not an issue
            }

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void update(Collection collection) {
        Map<String, Object> props = asProps(collection);
        props.remove(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0);
        client.updateNode(collection.getNodeId(), props);
    }

    @Override
    public void updateAndSetScope(Collection collection) throws Exception {
        update(collection);
        if (permissionService.hasPermission(
                StoreRef.PROTOCOL_WORKSPACE,
                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
                collection.getNodeId(),
                CCConstants.PERMISSION_CHANGEPERMISSIONS
        )) {
            setScope(collection);
        }
    }

    public Map<String, Object> asProps(Collection collection) {
        Map<String, Object> props = new HashMap<>();
        if (collection.getNodeId() != null) {
            props.put(CCConstants.SYS_PROP_NODE_UID, collection.getNodeId());
        }
        props.put(CCConstants.CM_PROP_TITLE, collection.getTitle());
        props.put(CCConstants.CM_NAME, NodeServiceHelper.cleanupCmName(collection.getTitle()));
        props.put(CCConstants.CM_PROP_DESCRIPTION, collection.getDescription());
        props.put(CCConstants.CCM_PROP_MAP_X, collection.getX());
        props.put(CCConstants.CCM_PROP_MAP_Y, collection.getY());
        props.put(CCConstants.CCM_PROP_MAP_Z, collection.getZ());
        props.put(CCConstants.CCM_PROP_MAP_COLLECTIONSCOPE, collection.getScope());

        props.put(CCConstants.CCM_PROP_MAP_COLLECTIONCOLOR, collection.getColor());
        props.put(CCConstants.CCM_PROP_MAP_COLLECTIONTYPE, collection.getType());
        props.put(CCConstants.CCM_PROP_MAP_COLLECTIONVIEWTYPE, collection.getViewtype());
        props.put(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0, collection.isLevel0());
        if (collection.getAuthorFreetext() != null && !collection.getAuthorFreetext().isEmpty()) {
            ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CHANGE_OWNER);
            props.put(CCConstants.CCM_PROP_MAP_COLLECTION_AUTHOR_FREETEXT, collection.getAuthorFreetext());
        }
        return props;
    }

    public Collection asCollection(Map<String, Object> props) {
        Collection collection = new Collection();
        collection.setNodeId((String) props.get(CCConstants.SYS_PROP_NODE_UID));

        collection.setTitle((String) props.get(CCConstants.CM_PROP_TITLE));
        collection.setDescription((String) props.get(CCConstants.CM_PROP_DESCRIPTION));

        String x = (String) props.get(CCConstants.CCM_PROP_MAP_X);
        if (x != null) collection.setX(Integer.parseInt(x));

        String y = (String) props.get(CCConstants.CCM_PROP_MAP_Y);
        if (y != null) collection.setY(Integer.parseInt(y));

        String z = (String) props.get(CCConstants.CCM_PROP_MAP_Z);
        if (z != null) collection.setZ(Integer.parseInt(z));

        collection.setColor((String) props.get(CCConstants.CCM_PROP_MAP_COLLECTIONCOLOR));
        collection.setType((String) props.get(CCConstants.CCM_PROP_MAP_COLLECTIONTYPE));
        collection.setViewtype((String) props.get(CCConstants.CCM_PROP_MAP_COLLECTIONVIEWTYPE));
        collection.setScope((String) props.get(CCConstants.CCM_PROP_MAP_COLLECTIONSCOPE));
        collection.setOrderAscending(false);
        String[] order = NodeServiceHelper.getPropertiesMultivalue(props).get(CCConstants.CCM_PROP_MAP_COLLECTION_ORDER_MODE);
        if (order != null) {
            collection.setOrderMode(order[0]);
            // since 6.1, we store 2 values
            if (order.length > 1) {
                collection.setOrderAscending(Boolean.parseBoolean(order[1]));
            }
        }
        collection.setAuthorFreetext((String) props.get(CCConstants.CCM_PROP_MAP_COLLECTION_AUTHOR_FREETEXT));
        if (props.containsKey(CCConstants.CCM_PROP_COLLECTION_PINNED_STATUS))
            collection.setPinned( Boolean.parseBoolean((String) props.get(CCConstants.CCM_PROP_COLLECTION_PINNED_STATUS)));

        return collection;
    }

    protected void addCollectionCountProperties(NodeRef nodeRef, Collection collection) {
        String path = serviceRegistry.getNodeService().getPath(nodeRef).toPrefixString(serviceRegistry.getNamespaceService());
        SearchParameters params = new ESSearchParameters();
        params.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        params.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_LUCENE);
        params.setMaxItems(0);

        params.setQuery("TYPE:" + QueryParser.escape(CCConstants.CCM_TYPE_IO) + " AND NOT ASPECT:" + QueryParser.escape(CCConstants.CCM_ASPECT_IO_CHILDOBJECT) + " AND PATH:\"" + QueryParser.escape(path) + "//*\"");
        collection.setChildReferencesCount((int) serviceRegistry.getSearchService().query(params).getNumberFound());
        params.setQuery("TYPE:" + QueryParser.escape(CCConstants.CCM_TYPE_MAP) + " AND PATH:\"" + QueryParser.escape(path) + "//*\"");
        collection.setChildCollectionsCount((int) serviceRegistry.getSearchService().query(params).getNumberFound());
    }

    @Override
	public Collection get(org.edu_sharing.service.model.NodeRef nodeRef, boolean fetchCounts) {
        try {
			Map<String,Object> props = nodeRef.getProperties() == null ? nodeService.getProperties(nodeRef.getStoreProtocol(),nodeRef.getStoreId(),nodeRef.getNodeId()) : nodeRef.getProperties();
			throwIfNotACollection(nodeRef.getStoreProtocol(),nodeRef.getStoreId(),nodeRef.getNodeId());

            Collection collection = asCollection(props);

            // using solr to count all underlying refs recursive
            if (fetchCounts) {
				addCollectionCountProperties(new NodeRef(new StoreRef(nodeRef.getStoreProtocol(), nodeRef.getStoreId()), nodeRef.getNodeId()), collection);
            }
            //collection.setChildReferencesCount(client.getChildAssociationByType(storeProtocol,storeId,collectionId, CCConstants.CCM_TYPE_IO).size());
            //collection.setChildCollectionsCount(client.getChildAssociationByType(storeProtocol,storeId,collectionId, CCConstants.CCM_TYPE_MAP).size());

			User owner = client.getOwner(nodeRef.getStoreId(),nodeRef.getStoreProtocol(),nodeRef.getNodeId());

            String currentUser = client.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME);
            if (!currentUser.equals(owner.getUsername()) && !client.isAdmin(currentUser)) {
                //leave out username
                owner.setUsername(null);
                owner.setEmail(null);
                owner.setNodeId(null);
            }
            collection.setFromUser(currentUser.equals(owner.getUsername()));

            collection.setOwner(owner);

            String parentId = (String) props.get(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID);
            AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

                @Override
                public Void doWork() throws Exception {
					if(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(new StoreRef(nodeRef.getStoreProtocol(), nodeRef.getStoreId()))){
						if(Arrays.asList(client.getAspects(nodeRef.getStoreProtocol(), nodeRef.getStoreId(),parentId)).contains(CCConstants.CCM_ASPECT_COLLECTION)){
                            collection.setLevel0(false);
                        } else {
                            collection.setLevel0(true);
                        }
                    }
                    return null;
                }
            });
			detectAndSetCollectionScope(nodeRef,collection);
            return collection;

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

	private void detectAndSetCollectionScope(org.edu_sharing.service.model.NodeRef nodeRef, Collection collection) {
        if (!CollectionDao.Scope.CUSTOM.name().equals(collection.getScope())) {
            return;
        }
        AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
				if(nodeRef.getPublic() != null) {
					 if(nodeRef.getPublic().equals(true)) {
						 collection.setScope(CollectionDao.Scope.CUSTOM_PUBLIC.name());
					 }
					return null;
				}
				ACL permissions = permissionService.getPermissions(nodeRef.getNodeId());
                for (ACE acl : permissions.getAces()) {
                    if (acl.getAuthority().equals(CCConstants.AUTHORITY_GROUP_EVERYONE)) {
                        collection.setScope(CollectionDao.Scope.CUSTOM_PUBLIC.name());
                        break;
                    }
                }
                return null;
            }
        });
    }

    @Override
    public List<NodeRef> getChildren(String parentId, String scope) {

        try {
            List<NodeRef> returnVal = new ArrayList<>();
            if (parentId == null) {

                /**
                 * @TODO owner + inherit off -> node will be found even if search is done in edu-group context
                 * level 0 nodes -> maybe cache level 0 with an node property
                 */
                String queryString = "ASPECT:\"" + CCConstants.CCM_ASPECT_COLLECTION + "\"" + " AND @ccm\\:collectionlevel0:true";
                MCAlfrescoAPIClient.ContextSearchMode mode = getContextModeForScope(scope);
                if (SearchScope.MY.name().equals(scope)) {
                    queryString += " AND OWNER:\"" + authInfo.get(CCConstants.AUTH_USERNAME) + "\"";
                }
                SearchParameters token = new SearchParameters();
                token.setQuery(queryString);
                List<NodeRef> searchResult = client.searchNodeRefs(token, mode);
                for (NodeRef entry : searchResult) {
                    String parent = nodeService.getPrimaryParent(entry.getStoreRef().getProtocol(), entry.getStoreRef().getIdentifier(), entry.getId());
                    if (Arrays.asList(client.getAspects(parent)).contains(CCConstants.CCM_ASPECT_COLLECTION)) {
                        continue;
                    }
                    returnVal.add(entry);
                }
            } else {
                List<ChildAssociationRef> list = nodeService.getChildrenChildAssociationRef(parentId);
                for (ChildAssociationRef entry : list) {
                    returnVal.add(entry.getChildRef());
                }
            }
            return returnVal;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<NodeRef> getChildren(String parentId, String scope, SortDefinition sortDefinition, List<String> filter) {
        try {
            if (parentId == null) {
                SearchParameters searchParams = new SearchParameters();
                sortDefinition.applyToSearchParameters(searchParams);

                MetadataSet mds = MetadataHelper.getMetadataset(appInfo, CCConstants.metadatasetdefault_id);

                String queryId = null;
                switch (SearchScope.valueOf(scope)) {
                    case MY:
                        queryId = "collections_scope_my";
                        break;
                    case EDU_ALL:
                        queryId = "collections_scope_public";
                        break;
                    case EDU_GROUPS:
                        queryId = "collections_scope_shared";
                        break;
                    case TYPE_EDITORIAL:
                        queryId = "collections_scope_editorial";
                        break;
                    case TYPE_MEDIA_CENTER:
                        queryId = "collections_scope_media_center";
                        break;
                    case RECENT:
                        return permissionService.getRecentProperty(CCConstants.CCM_PROP_PERSON_RECENT_COLLECTIONS);
                }
                String queryString = mds.findQuery(queryId, MetadataReader.QUERY_SYNTAX_LUCENE).findBasequery(null);
                /**
                 * @TODO owner + inherit off -> node will be found even if search is done in edu-group context
                 */
                MCAlfrescoAPIClient.ContextSearchMode mode = getContextModeForScope(scope);

                List<NodeRef> returnVal = new ArrayList<>();
                searchParams.setQuery(queryString);
                List<NodeRef> nodeRefs = client.searchNodeRefs(searchParams, mode);
                for (NodeRef nodeRef : nodeRefs) {
                    if (isSubCollection(nodeRef)) {
                        continue;
                    }
                    returnVal.add(nodeRef);
                }
                return returnVal;
            } else {
                List<ChildAssociationRef> children = nodeService.getChildrenChildAssociationRefAssoc(parentId, null, filter, sortDefinition);
                List<NodeRef> returnVal = new ArrayList<>();
                for (ChildAssociationRef child : children) {
                    returnVal.add(child.getChildRef());
                }
                return returnVal;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private MCAlfrescoAPIClient.ContextSearchMode getContextModeForScope(String scope) {
        MCAlfrescoAPIClient.ContextSearchMode mode = MCAlfrescoAPIClient.ContextSearchMode.Default;
        if (Scope.EDU_GROUPS.name().equals(scope)) {
            mode = MCAlfrescoAPIClient.ContextSearchMode.UserAndGroups;
        } else if (Scope.EDU_ALL.name().equals(scope)) {
            mode = MCAlfrescoAPIClient.ContextSearchMode.Public;
        }
        return mode;
    }

    private boolean isSubCollection(NodeRef nodeRef) {
        return AuthenticationUtil.runAsSystem(() -> {
            String parent = client.getParent(nodeRef).getParentRef().getId();
            return Arrays.asList(client.getAspects(parent)).contains(CCConstants.CCM_ASPECT_COLLECTION);
        });
    }

    @Override
    public void updateScope(NodeRef ref, List<ACE> permissions) {
        Scope result = Scope.MY;
        String creator = serviceRegistry.getOwnableService().getOwner(ref);
        for (ACE ace : permissions) {
            if (ace.getAuthority().equals(creator)) {
                // nothing is done
            } else if (ace.getAuthority().equals(CCConstants.AUTHORITY_GROUP_EVERYONE)) {
                result = Scope.CUSTOM_PUBLIC;
            } else if (result.equals(Scope.MY)) {
                result = Scope.CUSTOM;
            }
        }
        nodeService.setProperty(ref.getStoreRef().getProtocol(),
                ref.getStoreRef().getIdentifier(),
                ref.getId(),
                CCConstants.CCM_PROP_MAP_COLLECTIONSCOPE,
                result.toString(),
                false);
    }

    public void setScope(Collection collection) throws Exception {
        String collectionId = collection.getNodeId();
        String scope = collection.getScope();
        boolean custom = (scope == null || scope.equals(Scope.CUSTOM.name()));
        org.edu_sharing.repository.client.rpc.ACL acl = new org.edu_sharing.repository.client.rpc.ACL();

        List<org.edu_sharing.repository.client.rpc.ACE> aces = new ArrayList<>();
        if (acl.getAces() != null)
            aces.addAll(Arrays.asList(acl.getAces()));
        if (CCConstants.COLLECTIONTYPE_MEDIA_CENTER.equals(collection.getType())) {
            List<String> mediacenters = searchService.getAllMediacenters().stream().filter((m) -> AuthorityServiceFactory.getLocalService().hasAdminAccessToMediacenter(m)).collect(Collectors.toList());
            if (mediacenters.size() != 1) {
                throw new IllegalArgumentException("Current user is assigned to " + mediacenters.size() + " mediacenters, but must be assigned to exactly 1");
            }
            ACE ace = new ACE();
            ace.setAuthority(mediacenters.get(0));
            ace.setAuthorityType(Authority.Type.GROUP.name());
            ace.setPermission(CCConstants.PERMISSION_CONSUMER);
            aces.add(ace);
            ACE ace2 = new ACE();
            ace2.setAuthority(PermissionService.GROUP_PREFIX + AuthorityService.getGroupName(org.edu_sharing.alfresco.service.AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP, mediacenters.get(0)));
            ace2.setAuthorityType(Authority.Type.GROUP.name());
            ace2.setPermission(CCConstants.PERMISSION_COORDINATOR);
            aces.add(ace2);

            permissionService.setPermissions(collectionId, aces, false);
        } else if (custom) {

            if (collection.isLevel0()) { // don't allow inherition on root level
                permissionService.setPermissionInherit(collectionId, false);
                return;
            }

        } else {
            acl.setInherited(false);
            if (scope.equals(Scope.MY.name())) {

            } else if (scope.equals(Scope.EDU_ALL.name())) {
                org.edu_sharing.repository.client.rpc.ACE ace2 = new org.edu_sharing.repository.client.rpc.ACE();
                ace2.setAuthority(CCConstants.AUTHORITY_GROUP_EVERYONE);
                ace2.setAuthorityType(Authority.Type.EVERYONE.name());
                ace2.setPermission(CCConstants.PERMISSION_CONSUMER);
                aces.add(ace2);
            } else if (scope.equals(Scope.EDU_GROUPS.name())) {

                List<EduGroup> groups = searchService.getAllOrganizations(false).getData();
                for (EduGroup group : groups) {
                    org.edu_sharing.repository.client.rpc.ACE ace2 = new org.edu_sharing.repository.client.rpc.ACE();
                    ace2 = new org.edu_sharing.repository.client.rpc.ACE();
                    ace2.setAuthority(group.getGroupname());
                    ace2.setAuthorityType(Authority.Type.GROUP.name());
                    ace2.setPermission(CCConstants.PERMISSION_CONSUMER);
                    aces.add(ace2);

                }
            }
        }

        final ACL aclFinal = acl;
        if (scope != null && scope.equals(Scope.MY.name())) {
            // We need to set inherition
            AuthenticationUtil.runAsSystem((RunAsWork<Void>) () -> {
                permissionService.setPermissions(collectionId, aces, aclFinal.isInherited());
                return null;
            });
        } else if (!custom) {
            permissionService.setPermissions(collectionId, aces, aclFinal.isInherited());
        }
    }

    /**
     * unpin all current collections and set the list of collection ids pinned
     */
    @Override
    public void setPinned(String[] collections) {
        SearchToken searchToken = new SearchToken();
        searchToken.setContentType(ContentType.COLLECTIONS);
        searchToken.setLuceneString("ASPECT:" + QueryParser.escape(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION_PINNED)));
        searchToken.setMaxResult(Integer.MAX_VALUE);
        List<org.edu_sharing.service.model.NodeRef> currentPinned = searchService.search(searchToken).getData();
        for (org.edu_sharing.service.model.NodeRef pinned : currentPinned) {
            nodeService.removeAspect(pinned.getNodeId(), CCConstants.CCM_ASPECT_COLLECTION_PINNED);
            nodeService.removeProperty(pinned.getStoreProtocol(), pinned.getStoreId(), pinned.getNodeId(), CCConstants.CCM_PROP_COLLECTION_PINNED_STATUS);
            nodeService.removeProperty(pinned.getStoreProtocol(), pinned.getStoreId(), pinned.getNodeId(), CCConstants.CCM_PROP_COLLECTION_PINNED_ORDER);
        }
        int order = 0;
        for (String collection : collections) {
            throwIfNotACollection(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collection);
            nodeService.addAspect(collection, CCConstants.CCM_ASPECT_COLLECTION_PINNED);

            Map<String, Object> props = new HashMap<>();
            props.put(CCConstants.CCM_PROP_COLLECTION_PINNED_STATUS, true);
            props.put(CCConstants.CCM_PROP_COLLECTION_PINNED_ORDER, order);
            nodeService.updateNodeNative(collection, props);

            order++;
        }
    }

    private void throwIfNotACollection(String storeProtocol, String storeId, String collection) {
        String[] aspects = nodeService.getAspects(storeProtocol, storeId, collection);
        if (!Arrays.asList(aspects).contains(CCConstants.CCM_ASPECT_COLLECTION)) {
            throw new IllegalArgumentException("Node " + collection + " is not a collection (Aspect " + CCConstants.CCM_ASPECT_COLLECTION + " not found)");
        }

    }

    @Override
    public void writePreviewImage(String collectionId, InputStream is, String mimeType) throws Exception {
        ImageTool.VerifyResult result = ImageTool.verifyAndPreprocessImage(is, ImageTool.MAX_THUMB_SIZE);
        client.writeContent(MCAlfrescoAPIClient.storeRef, collectionId, result.getInputStream(), result.getMediaType().toString(), null, CCConstants.CCM_PROP_MAP_ICON);
        ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        PreviewCache.purgeCache(collectionId);
    }

    @Override
    public void removePreviewImage(String collectionId) throws Exception {
        NodeServiceFactory.getLocalService().removeProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionId, CCConstants.CCM_PROP_MAP_ICON);
        PreviewCache.purgeCache(collectionId);
    }

    @Override
    public void setOrder(String parentId, String[] nodes) {
		List<NodeRef> refs=getChildren(parentId, null, new SortDefinition(),Arrays.asList("files", "folders"));
		AtomicInteger order=new AtomicInteger(0);

        Map<String, Object> collectionProps = new HashMap<>();
        nodeService.updateNodeNative(parentId, collectionProps);

		if(nodes == null)
            return;
        for (String node : nodes) {
			transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
            NodeRef ref = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node);
				policyBehaviourFilter.disableBehaviour(ref, ContentModel.ASPECT_AUDITABLE);
            if (!refs.contains(ref))
                throw new IllegalArgumentException("Node id " + node + " is not a children of the collection " + parentId);

            nodeService.addAspect(node, CCConstants.CCM_ASPECT_COLLECTION_ORDERED);
            Map<String, Object> props = new HashMap<>();
				props.put(CCConstants.CCM_PROP_COLLECTION_ORDERED_POSITION, order.get());
            nodeService.updateNodeNative(node, props);
				policyBehaviourFilter.enableBehaviour(ref, ContentModel.ASPECT_AUDITABLE);
				return ref;
			});
			order.incrementAndGet();
        }
    }

    /**
     * Get all reference objects for a given node
     * Uses solr
     *
     * @param nodeId
     * @return
     */
    @Override
    public List<org.edu_sharing.service.model.NodeRef> getReferenceObjects(String nodeId) {
        SearchToken token = new SearchToken();
        token.setMaxResult(Integer.MAX_VALUE);
        token.setContentType(ContentType.ALL);
        token.setLuceneString("ASPECT:\"ccm:collection_io_reference\" AND @ccm\\:original:" + QueryParser.escape(nodeId) + " AND NOT @sys\\:node-uuid:" + QueryParser.escape(nodeId));
        return SearchServiceFactory.getSearchService(appInfo.getAppId()).search(token).getData();
    }

    @Override
    public List<NodeRef> getReferenceObjectsSync(String nodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put(CCConstants.CCM_PROP_IO_ORIGINAL, nodeId);

        List<String> aspects = new ArrayList<>();
        aspects.add(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE);
        logger.debug("cmis helper start");
        List<org.alfresco.service.cmr.repository.NodeRef> nodes = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO, map, aspects, null, 100000, (StoreRef) null);
        logger.debug("cmis helper finished");
        return nodes;
    }

    @Override
    public List<NodeRef> getCollectionProposals(String nodeId, CCConstants.PROPOSAL_STATUS status) {
        Map<String, Object> filters = new HashMap<>();
        filters.put(CCConstants.CCM_PROP_COLLECTION_PROPOSAL_TARGET, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId).toString());
        filters.put(CCConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS, status.toString());
        List<NodeRef> collections = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_COLLECTION_PROPOSAL, filters);
        return collections.stream().map(
                (ref) -> serviceRegistry.getNodeService().getPrimaryParent(ref).getParentRef()
        ).filter((ref) ->
                PermissionServiceHelper.hasPermission(ref, CCConstants.PERMISSION_ADD_CHILDREN)
        ).collect(Collectors.toList());

    }
}
