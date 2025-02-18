package org.edu_sharing.restservices;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigCache;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.guest.GuestConfig;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.repository.server.tools.security.JwtTokenUtil;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.collection.v1.model.CollectionReference;
import org.edu_sharing.restservices.collection.v1.model.CollectionRelationReference;
import org.edu_sharing.restservices.node.v1.model.*;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.shared.NodeSearch.Facet;
import org.edu_sharing.restservices.shared.SearchResult;
import org.edu_sharing.restservices.shared.NodeSearch.Facet.Value;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.model.CollectionRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.*;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;
import org.edu_sharing.service.permission.HandleParam;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.rating.RatingDetails;
import org.edu_sharing.service.rating.RatingServiceFactory;
import org.edu_sharing.service.remote.RemoteObjectService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SharedToMeType;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeDao {
    private static Logger logger = Logger.getLogger(NodeDao.class);
    private static final StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    /**
     * also check @PermissionServiceHelper.PERMISSIONS
     */
    private static final String[] DAO_PERMISSIONS = new String[]{
            org.alfresco.service.cmr.security.PermissionService.READ,
            org.alfresco.service.cmr.security.PermissionService.ADD_CHILDREN,
            org.alfresco.service.cmr.security.PermissionService.CHANGE_PERMISSIONS,
            org.alfresco.service.cmr.security.PermissionService.WRITE,
            org.alfresco.service.cmr.security.PermissionService.DELETE,
            CCConstants.PERMISSION_COMMENT,
            CCConstants.PERMISSION_EMBED,
            CCConstants.PERMISSION_DOWNLOAD_CONTENT,
            CCConstants.PERMISSION_FEEDBACK,
            CCConstants.PERMISSION_CC_PUBLISH,
            CCConstants.PERMISSION_READ_ALL
    };
    final List<String> access;
    private final org.edu_sharing.service.model.NodeRef.Preview previewData;
    // true if the current Dao is the collection home folder
    private final boolean isCollectionHomePath;
    private final String ownerUsername;
    private final Map<NodeRefImpl.Relation, NodeDao> relations = new HashMap<>();
    private org.edu_sharing.service.model.NodeRef nodeRef = null;
    private CollectionRef collectionRef;
    private final List<Contributor> contributors = new ArrayList<>();
    /*
    whether this node dao is supposed to fetch collection counts (more expensive when true)
     */
    boolean fetchCounts = true;
    /**
     * whether to resolve owner, creator and modifier full username
     * Disabling is recommended when not needed to improve performance
     */
    public boolean resolveUsernames = true;
    // id of the object by the remote repository (null if not a remote object)
    private String remoteId;
    private RepositoryDao remoteRepository;

    private String version;

    private static ThreadLocal<Boolean> isGlobalAdmin = new ThreadLocal<>();
    private boolean isPublic;
    /**
     * temporary variable to reduce rebuilds for query in search context + collections
     */
    BoolQuery readPermissionsQuery;
    private Set<TimedPermissionAuthority> authPerm;

    private NodeDao(org.alfresco.service.cmr.repository.NodeRef nodeRef) throws Throwable {
        this.nodeId = nodeRef.getId();
        storeId = nodeRef.getStoreRef().getIdentifier();
        storeProtocol = nodeRef.getStoreRef().getProtocol();
        filter = Filter.createShowAllFilter();
        repoDao = RepositoryDao.getHomeRepository();
        access = new ArrayList<>();
        type = NodeServiceHelper.getType(nodeRef);
        aspects = Arrays.asList(NodeServiceHelper.getAspects(nodeRef));
        nodeProps = NodeServiceHelper.getProperties(nodeRef);
        previewData = null;
        ownerUsername = null;
        isCollectionHomePath = false; // TODO do we need to resolve this here?
    }

    public String getVersion() {
        return version;
    }

    public static NodeStatsEntry.NodeStats getStats(NodeDao node) throws DAOException {
        if (!node.isFromRemoteRepository()) {
            if (!node.access.contains(PermissionService.CHANGE_PERMISSIONS)) {
                throw new DAORestrictedAccessException(new SecurityException("Requires " + PermissionService.CHANGE_PERMISSIONS), node.getId());
            }
            try {
                NodeStatsEntry.NodeStats stats = new NodeStatsEntry.NodeStats();
                StatisticEntry total = TrackingServiceFactory.getTrackingService().getSingleNodeData(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node.getId()), null, null);
                stats.setTotal(total.getCounts());
                return stats;
            } catch (Throwable e) {
                logger.warn("Error while resolving stats for " + node.getRef().getId(), e);
                return null;
            }
        }
        return null;
    }

    public org.edu_sharing.service.model.NodeRef getNodeRef() {
        if (this.nodeRef != null) {
            return this.nodeRef;
        }
        return new org.edu_sharing.service.model.NodeRefImpl(repoDao.getId(), storeProtocol, storeId, nodeId);
    }

    public static NodeDao getNodeWithVersion(RepositoryDao repoDao, String nodeId, String versionLabel) throws DAOException {
        if (versionLabel != null && versionLabel.equals("-1"))
            versionLabel = null;
        NodeDao nodeDao = getNode(repoDao, nodeId, Filter.createShowAllFilter());

        // published nodes don't have version histories!
        if (nodeDao.isPublishedCopy()) {
            versionLabel = null;
        }
        if (versionLabel != null) {
            nodeDao.version = versionLabel;
            nodeDao.nodeProps = nodeDao.getNodeHistory().get(nodeDao.version);
            if (nodeDao.nodeProps == null)
                throw new DAOMissingException(new Exception("Node " + nodeId + " does not have this version: " + versionLabel));
        }
        return nodeDao;
    }

    public NodeDao copyProperties(NodeDao fromDao) throws DAOException {
        Map<String, String[]> props = fromDao.getAllProperties();
        props.remove(CCConstants.getValidLocalName(CCConstants.CM_NAME));
        try {
            MetadataSet mds = MetadataHelper.getMetadataset(new NodeRef(repoDao, getId()));
            HashSet<String> defaultProps = mds.getWidgetsByNode(this.type, getAspectsNative(), false).stream().map(MetadataWidget::getId).collect(Collectors.toCollection(HashSet::new));
            defaultProps.addAll(mds.getWidgetsByNode(this.type, getAspectsNative(), false).stream().map(MetadataWidget::getSuggestDisplayProperty).filter(Objects::nonNull).collect(Collectors.toSet()));
            defaultProps.addAll(Arrays.stream(NodeCustomizationPolicies.SAFE_PROPS).map(CCConstants::getValidLocalName).collect(Collectors.toList()));
            defaultProps.addAll(Arrays.stream(NodeCustomizationPolicies.LICENSE_PROPS).map(CCConstants::getValidLocalName).collect(Collectors.toList()));

            // don't remove published only properties
            Stream.of(
                            CCConstants.CCM_PROP_PUBLISHED_DOI_ID,
                            CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID,
                            CCConstants.CCM_PROP_PUBLISHED_DATE,
                            CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL
                    )
                    .map(CCConstants::getValidLocalName)
                    .forEach(defaultProps::remove);

            for (String prop : defaultProps) {
                if (!props.containsKey(prop) && CCConstants.getValidGlobalName(prop) != null) {
                    // delete removed properties
                    nodeService.removeProperty(getStoreProtocol(), getStoreIdentifier(), getId(), CCConstants.getValidGlobalName(prop));
                }
            }
            // copy version
            List<String> copyNative = Arrays.asList(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
            for (String prop : copyNative) {
                nodeService.setProperty(getStoreProtocol(), getStoreIdentifier(), getId(),
                        prop,
                        nodeService.getPropertyNative(fromDao.getStoreProtocol(), fromDao.getStoreIdentifier(), fromDao.getId(), prop),
                        false);
            }

            // fix duplicate child name issues in publishing folder
            props.remove(CCConstants.getValidLocalName(CCConstants.CM_NAME));

            // fix published mode only for original
            props.remove(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_PUBLISHED_MODE));

            return changeProperties(props);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public NodeDao revokeNode(RevokeDetails details) {
        try {
            NodeServiceFactory.getLocalService().revokeNode(storeProtocol, storeId, getId(), details);
            return getNode(repoDao, getId(), filter);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public String getSignedNode() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Node node = asNode();
            String serializedNode = objectMapper.writeValueAsString(node);

            Signing signing = new Signing();
            PrivateKey privateKey = signing.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);
            byte[] signedNodeData = signing.sign(privateKey, serializedNode, CCConstants.SECURITY_SIGN_ALGORITHM);

            SignedNode signedNode = new SignedNode(node, new String(signedNodeData));
            String serializedSignedNode = objectMapper.writeValueAsString(signedNode);
            Base64.Encoder encoder = Base64.getEncoder();
            return encoder.encodeToString(serializedSignedNode.getBytes());
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }


    enum ExistingMode {
        // Fallback if the original node does not exist
        IfNotExists,
        // Fallback if the user has no read permissions on original node
        IfNoReadPermissions
    }

    /**
     * find any "existing" node
     * this means that if an original node is delivered, you might get a published copy if the original is deleted
     *
     * @param repoDao the repo, this method will only return published copies for the home repo, not for remotes!
     * @param nodeId
     * @return
     * @throws DAOException
     */
    public static NodeDao getAnyExistingNode(RepositoryDao repoDao, List<ExistingMode> mode, String nodeId)
            throws DAOException {
        if (repoDao.isHomeRepo()) {
            boolean fetchCopy = false;
            boolean exists = NodeServiceHelper.exists(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
            boolean permission = PermissionServiceHelper.hasPermission(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), CCConstants.PERMISSION_READ);
            if (mode.contains(ExistingMode.IfNotExists) && !exists) {
                fetchCopy = true;
            } else if (exists && mode.contains(ExistingMode.IfNoReadPermissions) && !permission) {
                fetchCopy = true;
            }
            if (fetchCopy) {
                // try to fetch a published copy
                SortDefinition sort = new SortDefinition();
                sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(
                        CCConstants.getValidLocalName(CCConstants.LOM_PROP_LIFECYCLE_VERSION), false
                ));
                String finalNodeId = nodeId;
                List<org.alfresco.service.cmr.repository.NodeRef> list = AuthenticationUtil.runAsSystem(() -> NodeServiceFactory.getLocalService().getPublishedCopies(finalNodeId).stream().map(
                                id -> new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, id)
                        ).collect(Collectors.toList())
                );
                if (!list.isEmpty()) {
                    list = NodeServiceFactory.getLocalService().sortNodeRefList(list,
                            null,
                            sort
                    );
                    nodeId = list.get(0).getId();
                } else {
                    throw new DAOMissingException(new IllegalArgumentException("No remaining node found for id " + nodeId));
                }
            }
        }
        return getNode(repoDao, nodeId, Filter.createShowAllFilter());
    }

    public static NodeDao getNode(RepositoryDao repoDao, String nodeId)
            throws DAOException {
        return getNode(repoDao, nodeId, Filter.createShowAllFilter());
    }

    public static NodeDao getNode(RepositoryDao repoDao, org.edu_sharing.service.model.NodeRef nodeRef)
            throws DAOException {
        return new NodeDao(repoDao, nodeRef, Filter.createShowAllFilter());
    }

    /**
     * get node via shared link
     **/
    public static NodeDao getNode(RepositoryDao repoDao, String nodeId, String token)
            throws DAOException {
        return AuthenticationUtil.runAsSystem(new RunAsWork<NodeDao>() {
            @Override
            public NodeDao doWork() throws Exception {
                ShareServiceImpl service = new ShareServiceImpl(PermissionServiceFactory.getPermissionService(repoDao.getId()));
                Share share = service.getShare(nodeId, token);
                if (share == null) {
                    throw new Exception("No share found for nodeId and token");
                }
                if (share.getExpiryDate() != ShareService.EXPIRY_DATE_UNLIMITED) {
                    if (new Date(System.currentTimeMillis()).after(new Date(share.getExpiryDate()))) {
                        throw new Exception("Share expired");
                    }
                }
                share.setDownloadCount((share.getDownloadCount() + 1));
                service.updateShare(share);

                return getNode(repoDao, nodeId, new Filter());
            }
        });
    }

    public static NodeDao getNode(RepositoryDao repoDao, String nodeId, Filter filter)
            throws DAOException {

        try {
            if (filter == null) filter = new Filter();
            return NodeDao.getNode(repoDao, null, null, nodeId, filter);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public static NodeDao getNode(RepositoryDao repoDao, String storeProtocol, String storeId, String nodeId, Filter filter)
            throws DAOException {
        try {

            return new NodeDao(repoDao, storeProtocol, storeId, nodeId, filter);

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public static NodeSearch search(RepositoryDao repoDao,
                                    org.edu_sharing.service.search.model.SearchToken searchToken) throws DAOException {
        SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getId());
        return transform(repoDao, searchService.search(searchToken));
    }

    public static NodeSearch search(RepositoryDao repoDao,
                                    org.edu_sharing.service.search.model.SearchToken searchToken, boolean scoped) throws DAOException {
        SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getId());
        return transform(repoDao, searchService.search(searchToken, scoped));
    }

    public static NodeSearch search(RepositoryDao repoDao, MdsDao mdsDao,
                                    String query, List<MdsQueryCriteria> criterias, SearchToken token, Filter filter, Function<NodeDao, NodeDao> transform) throws DAOException {
        SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getId());
        Map<String, String[]> criteriasMap = MetadataSearchHelper.convertCriterias(criterias);
        try {
            BoolQuery readPermissionsQuery = null;
            if (searchService instanceof SearchServiceElastic) {
                // improve performance by caching the relatively expensive query
                readPermissionsQuery = ((SearchServiceElastic) searchService).getReadPermissionsQuery(new BoolQuery.Builder()).build();
            }
            NodeSearch result = transform(repoDao, searchService.search(mdsDao.getMds(), query, criteriasMap, token), filter, transform, readPermissionsQuery);
            if (result.getCount() == 0) {
                // try to search for ignorable properties to be null
                List<String> removed;
                if (searchService instanceof SearchServiceElastic) {
                    try {
                        removed = slackCriteriasMap(criteriasMap, mdsDao.getMds().findQuery(query, MetadataReader.QUERY_SYNTAX_DSL));
                    } catch (IllegalArgumentException e) {
                        // query not available via dsl, so no slacking is done
                        return result;
                    }
                } else {
                    removed = slackCriteriasMap(criteriasMap, mdsDao.getMds().findQuery(query, MetadataReader.QUERY_SYNTAX_LUCENE));
                }
                result = transform(repoDao, searchService.search(mdsDao.getMds(), query, criteriasMap, token), filter, transform);
                result.setIgnored(removed);
                return result;
            }
            return result;
        } catch (Throwable e) {
            throw DAOException.mapping(e);
        }
    }

    public static NodeSearch searchFacets(RepositoryDao repoDao, MdsDao mdsDao, String query,
                                          List<MdsQueryCriteria> criterias,
                                          SearchToken token) throws DAOException {
        SearchService ss = SearchServiceFactory.getSearchService(repoDao.getId());
        try {
            if (!(ss instanceof SearchServiceElastic)) {
                throw new Exception("not implemented for non elastic searchengine:" + ss.getClass().getName());
            }
            SearchServiceElastic searchService = (SearchServiceElastic) ss;
            Map<String, String[]> criteriasMap = MetadataSearchHelper.convertCriterias(criterias);
            SearchResultNodeRef searchResultNodeRef = searchService.searchFacets(mdsDao.getMds(), query, criteriasMap, token);
            NodeSearch nodeSearch = transform(repoDao, searchResultNodeRef);
            return nodeSearch;
        } catch (Throwable e) {
            throw DAOException.mapping(e);
        }
    }

    private static List<String> slackCriteriasMap(Map<String, String[]> criteriasMap, MetadataQuery metadataQuery) {
        List<String> removed = new ArrayList<>();
        for (MetadataQueryParameter param : metadataQuery.getParameters()) {
            if (param.getIgnorable() > 0 && criteriasMap.containsKey(param.getName())) {
                criteriasMap.put(param.getName(), null);
                removed.add(param.getName());
            }
        }
        return removed;
    }

    public static NodeSearch searchFingerprint(RepositoryDao repoDao, String nodeId, Filter filter) throws DAOException {
        SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getId());
        try {
            return transform(repoDao, searchService.searchFingerPrint(nodeId), filter, null);
        } catch (Throwable e) {
            throw DAOException.mapping(e);
        }
    }

    public static NodeSearch search(RepositoryDao repoDao, String query,
                                    int startIdx, int nrOfresults, List<String> facets,
                                    int facetsMinCount, int facetsLimit, Function<NodeDao, NodeDao> transform) throws DAOException {

        try {

            SearchResultNodeRef search = ((MCAlfrescoAPIClient) repoDao.getBaseClient()).searchSolrNodeRef(query,
                    startIdx, nrOfresults, facets, facetsMinCount,
                    facetsLimit);

            return transform(repoDao, search);

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public static NodeSearch getMetadata(RepositoryDao repoDao, List<String> nodeIds, Filter filter) throws DAOException {
        SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getId());
        try {
            return transform(repoDao, searchService.getMetadata(nodeIds), filter, null);
        } catch (Throwable e) {
            throw DAOException.mapping(e);
        }
    }

    public static NodeSearch transform(RepositoryDao repoDao, SearchResultNodeRef search) {
        return transform(repoDao, search, null, null);
    }

    public static NodeSearch transform(RepositoryDao repoDao, SearchResultNodeRef search, Filter filter, Function<NodeDao, NodeDao> transform) {
        return transform(repoDao, search, filter, transform, null);
    }

    public static NodeSearch transform(RepositoryDao repoDao, SearchResultNodeRef search, Filter filter, Function<NodeDao, NodeDao> transform, BoolQuery readPermissionsQuery) {
        NodeSearch result = new NodeSearch();

        List<NodeRef> data = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        result.setResult(data);
        result.setNodes(nodes);

        if (search == null || search.getData() == null)
            return result;
        for (org.edu_sharing.service.model.NodeRef nodeRef : search.getData()) {
            NodeRef ref = new NodeRef();
            ref.setRepo(repoDao.getId());
            ref.setId(nodeRef.getNodeId());

            String storeProtocol = nodeRef.getStoreProtocol();
            if (StoreRef.PROTOCOL_ARCHIVE.equals(storeProtocol)) {
                ref.setArchived(true);
            }

            data.add(ref);

            if (nodeRef.getProperties() != null) {

                try {
                    NodeDao nodeDao = new NodeDao(repoDao, nodeRef, filter);
                    nodeDao.readPermissionsQuery = readPermissionsQuery;
                    if (transform != null) {
                        nodeDao = transform.apply(nodeDao);
                    }
                    nodes.add(nodeDao.asNode());
                } catch (DAOException e) {

                }
            }
        }
        result.setCount(search.getNodeCount());
        result.setSkip(search.getStartIDX());


        if (search.getFacets() != null) {
            for (Facet facet : search.getFacets()) {
                if (facet.getValues() != null) {
                    Collections.sort(facet.getValues(), new Comparator<Value>() {
                        @Override
                        public int compare(Value o1, Value o2) {
                            return o2.getCount().compareTo(o1.getCount());
                        }
                    });
                }
            }
            result.setFacets(search.getFacets());
        }

        if (search.getSuggests() != null) {
            Collections.sort(search.getSuggests(), new Comparator<NodeSearch.Suggest>() {
                @Override
                public int compare(NodeSearch.Suggest suggest, NodeSearch.Suggest t1) {
                    return new Float(t1.getScore()).compareTo(new Float(suggest.getScore()));
                }
            });
            result.setSuggests(search.getSuggests());
        }

        return result;
    }

    public static void delete(String protocol, String store, String nodeId) {
        NodeService nodeService = NodeServiceFactory.getNodeService(null);
        nodeService.removeNode(protocol, store, nodeId);
    }

    private final RepositoryDao repoDao;
    private final String nodeId;

    private Map<String, Object> nodeProps;
    private Map<String, Map<String, Object>> nodeHistory;

    private Map<String, Boolean> hasPermissions;

    private final String type;
    private final List<String> aspects;

    private final String storeProtocol;

    private final String storeId;

    NodeService nodeService;
    AuthorityService authorityService;
    CollectionService collectionService;
    CommentService commentService;
    GuestService guestService;
    final Filter filter;

    private org.edu_sharing.service.permission.PermissionService permissionService;

    public static final String defaultStoreProtocol = "workspace";
    public static final String defaultStoreId = "SpacesStore";

    public static final String archiveStoreProtocol = "archive";
    public static final String archiveStoreId = "SpacesStore";

    List<NodeDao> usedInCollections = new ArrayList<>();

    private NodeDao(RepositoryDao repoDao, String nodeId) throws Throwable {
        this(repoDao, nodeId, new Filter());
    }

    /**
     * return a node by a given name inside a parent folder
     *
     * @param repoDao
     * @param parentId the folder to search
     * @param type     the NodeType to find
     * @param name     the CM_NAME to find
     * @return
     * @throws Throwable
     */
    public static NodeDao getByParent(RepositoryDao repoDao, String parentId, String type, String name) throws Throwable {
        NodeService nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
        org.alfresco.service.cmr.repository.NodeRef ref = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId, type, CCConstants.CM_NAME, name);
        return new NodeDao(repoDao, ref.getId());
    }

    private NodeDao(RepositoryDao repoDao, String nodeId, Filter filter) throws Throwable {

        this(repoDao, null, null, nodeId, filter);
    }

    private NodeDao(RepositoryDao repoDao, String storeProtocol, String storeId, String nodeId, Filter filter) throws DAOException {
        this(repoDao, new org.edu_sharing.service.model.NodeRefImpl(repoDao.getId(),
                        storeProtocol != null ? storeProtocol : defaultStoreProtocol,
                        storeId != null ? storeId : defaultStoreId,
                        nodeId),
                filter);
    }

    public static String mapNodeConstants(RepositoryDao repoDao, String node) throws DAOException {
        return mapNodeConstants(repoDao, node, true);
    }


    public static String mapNodeConstants(RepositoryDao repoDao, String node, boolean createIfNotExists) throws DAOException {
        try {
            if ("-userhome-".equals(node)) {
                node = repoDao.getUserHome();
            }
            if ("-inbox-".equals(node)) {
                node = repoDao.getUserInbox(createIfNotExists);
            }
            if ("-saved_search-".equals(node)) {
                node = repoDao.getUserSavedSearch(createIfNotExists);
            }
            return node;
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    /**
     * create an empty, Node dummy interface
     *
     * @param nodeRef
     * @return
     */
    public static <T extends Node> T createEmptyDummy(Class<T> clazz, NodeRef nodeRef) throws IllegalAccessException, InstantiationException {
        T node = clazz.newInstance();
        node.setRef(nodeRef);
        node.setName(nodeRef.getId());
        node.setPreview(new Preview());
        // allow fetching as admin to properly resolve the url
        AuthenticationUtil.runAsSystem(() -> {
            try {
                node.getPreview().setUrl(
                        URLTool.getPreviewServletUrl(nodeRef.getId(), StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier())
                );
            } catch (Throwable ignored) {
            }
            return null;
        });
        return node;
    }

    public SearchResult<Node> runSavedSearch(int skipCount, int maxItems, SearchService.ContentType contentType, SortDefinition sort, List<String> facets) throws DAOException {
        try {
            if (!CCConstants.getValidLocalName(CCConstants.CCM_TYPE_SAVED_SEARCH).equals(getType())) {
                throw new IllegalArgumentException("The given node must be of type " + CCConstants.CCM_TYPE_SAVED_SEARCH);
            }
            Map<String, Object> props = getNativeProperties();
            RepositoryDao repoDao = RepositoryDao
                    .getRepository((String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY));
            MdsDao mdsDao = MdsDao.getMds(repoDao, (String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_MDS));

            SearchToken token = new SearchToken();
            token.setFacets(facets);
            token.setSortDefinition(sort);
            token.setFrom(skipCount);
            token.setMaxResult(maxItems);
            token.setContentType(contentType);

            ObjectMapper mapper = new ObjectMapper();
            List<MdsQueryCriteria> parameters = Arrays.asList(mapper.readValue(
                    (String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS), MdsQueryCriteria[].class));
            NodeSearch search = NodeDao.search(repoDao, mdsDao,
                    (String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_QUERY), parameters, token, filter, null);

            List<Node> data;
            if (search.getNodes().size() < search.getResult().size()) {
                //searched repo deliveres only nodeRefs by query time
                data = new ArrayList<>();
                for (NodeRef ref : search.getResult()) {
                    data.add(NodeDao.getNode(repoDao, ref.getId(), filter).asNode());
                }
            } else {
                //searched repo delivered properties by query time
                data = search.getNodes();
            }


            Pagination pagination = new Pagination();
            pagination.setFrom(search.getSkip());
            pagination.setCount(data.size());
            pagination.setTotal(search.getCount());


            SearchResult<Node> response = new SearchResult<>();
            response.setNodes(data);
            response.setPagination(pagination);
            response.setFacets(search.getFacets());

            return response;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public static NodeSearch getRelevantNodes(RepositoryDao repoDao, int skipCount, int maxItems) throws DAOException {
        try {
            return transform(repoDao, SearchServiceFactory.getSearchService(repoDao.getId()).getRelevantNodes(skipCount, maxItems));
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    private int getCommentCount() {
        if (nodeProps.containsKey(CCConstants.VIRT_PROP_COMMENTCOUNT)) {
            Object count = nodeProps.get(CCConstants.VIRT_PROP_COMMENTCOUNT);
            return (count instanceof Integer) ? (int) count : Integer.parseInt((String) count);
        }
        return 0;
    }

    private NodeDao(RepositoryDao repoDao, org.edu_sharing.service.model.NodeRef nodeRef, Filter filter) throws DAOException {
        try {

            if (nodeRef.getNodeId().equals("-collectionhome-")) {
                isCollectionHomePath = true;
                nodeRef.setNodeId(CollectionServiceFactory.getLocalService().getHomePath());
            } else {
                isCollectionHomePath = false;
            }

            this.nodeRef = nodeRef;

            this.repoDao = repoDao;
            this.nodeId = nodeRef.getNodeId();

            this.storeProtocol = nodeRef.getStoreProtocol();
            this.storeId = nodeRef.getStoreId();

            this.nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
            this.guestService = AlfAppContextGate.getApplicationContext().getBean(GuestService.class);
            this.permissionService = PermissionServiceFactory.getPermissionService(repoDao.getId());
            try {
                this.authorityService = AuthorityServiceFactory.getAuthorityService(repoDao.getId());
            } catch (RuntimeException e) {
            }
            /**
             * call getProperties on demand
             */
            if (nodeRef.getProperties() == null || nodeRef.getProperties().isEmpty()) {
                this.nodeProps = this.nodeService.getProperties(this.storeProtocol, this.storeId, this.nodeId);

            } else {
                this.nodeProps = nodeRef.getProperties();
            }
            if (nodeRef instanceof CollectionRef) {
                this.collectionRef = (CollectionRef) nodeRef;
            }
            this.previewData = nodeRef.getPreview();

            if (nodeRef.getContributors() != null) {
                this.contributors.addAll(nodeRef.getContributors());
            }
            //when nodeservice or solr was used
            if (this.contributors.size() == 0) {
                for (String prop : this.nodeProps.keySet()) {
                    String key = CCConstants.getValidLocalName(prop);
                    if (key == null) continue;
                    if (key.matches("ccm:[a-zA-Z]*contributer_[a-zA-Z_-]*")) {
                        String value = (String) this.nodeProps.get(prop);
                        for (String v : ValueTool.getMultivalue(value)) {
                            ArrayList<Map<String, Object>> vcds = VCardConverter.vcardToMap(v);
                            if (vcds.size() > 0) {
                                Contributor contributor = new Contributor();
                                Map<String, Object> vcd = vcds.get(0);
                                contributor.setFirstname((String) vcd.get(CCConstants.VCARD_GIVENNAME));
                                contributor.setLastname((String) vcd.get(CCConstants.VCARD_SURNAME));
                                contributor.setProperty(key);
                                contributor.setEmail(CCConstants.VCARD_EMAIL);
                                contributor.setOrg((String) vcd.get(CCConstants.VCARD_ORG));
                                contributor.setVcard(v);
                                this.contributors.add(contributor);
                            }
                        }

                    }
                }

            }

            if (nodeProps.containsKey(CCConstants.NODETYPE)) {
                this.type = (String) nodeProps.get(CCConstants.NODETYPE);
            } else {
                this.type = CCConstants.CCM_TYPE_IO;
            }
            if (nodeRef.getAspects() == null) {
                String[] aspects = nodeService.getAspects(this.storeProtocol, this.storeId, nodeId);
                this.aspects = (aspects != null) ? Arrays.asList(aspects) : new ArrayList<>();
            } else {
                this.aspects = nodeRef.getAspects();
            }
            refreshPermissions(nodeRef);
            this.access = PermissionServiceHelper.getPermissionsAsString(hasPermissions);
            // replace all data if its an remote object
            if (this.type.equals(CCConstants.CCM_TYPE_REMOTEOBJECT)) {
                this.remoteId = (String) this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID);
                this.remoteRepository = RepositoryDao.getRepository((String) this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID));
                this.nodeService = NodeServiceFactory.getNodeService(this.remoteRepository.getId());
                this.permissionService = PermissionServiceFactory.getPermissionService(this.remoteRepository.getId());
                this.nodeProps = this.nodeService.getProperties(null, null, this.remoteId);
            } else if (this.aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)) {
                // just fetch dynamic data which needs to be fetched, because the local io already has metadata
                String originalNodeId = this.getReferenceOriginalId();
                Map<String, Map<String, Object>> history = AuthenticationUtil.runAsSystem(() -> {
                    try {
                        return this.nodeService.getVersionHistory(originalNodeId);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
                Optional<Entry<String, Map<String, Object>>> entry = history == null ? Optional.empty() : history.entrySet().stream().findFirst();
                if (!entry.isPresent() || CCConstants.VERSION_COMMENT_REMOTE_OBJECT_INIT.equals(entry.get().getValue().get(CCConstants.CCM_PROP_IO_VERSION_COMMENT))) {
                    try {
                        NodeService nodeServiceRemote = NodeServiceFactory.getNodeService((String) this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID));
                        Map<String, Object> nodePropsReplace = nodeServiceRemote.getPropertiesDynamic(
                                null, null, (String) this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID));
                        nodePropsReplace.remove(CCConstants.SYS_PROP_NODE_UID);
                        nodePropsReplace.remove(CCConstants.SYS_PROP_NODE_DBID);
                        nodePropsReplace.remove(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID);
                        nodePropsReplace.remove(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID);
                        nodePropsReplace.remove(CCConstants.CCM_PROP_IO_ORIGINAL);
                        this.nodeProps.putAll(nodePropsReplace);
                    } catch (Throwable t) {
                        logger.warn("Error while fetching properties for node id " + getId() + ": Node is a remote node and calling remote " + (String) this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID) + " failed", t);
                    }
                }
            }

            this.filter = filter;
            if (nodeRef.getRelations() != null) {
                nodeRef.getRelations().forEach((relation, ref) -> {
                    try {
                        relations.put(relation,
                                new NodeDao(repoDao, ref, filter)
                        );
                    } catch (DAOException e) {
                        logger.info("Can not add relation to node: " + e.getMessage());
                    }
                });
            }
            for (org.edu_sharing.service.model.CollectionRef usedInCollection : nodeRef.getUsedInCollections()) {
                NodeDao collection = new NodeDao(repoDao, usedInCollection, filter);
                if (usedInCollection.getRelationType() != null) {
                    if (usedInCollection.getRelationType().equals(CollectionRef.RelationType.Proposal)) {
                        // proposals should only be visible if the current user is organizer of the particular collection
                        if (collection.access.contains(CCConstants.PERMISSION_WRITE)) {
                            usedInCollections.add(collection);
                        }
                    } else {
                        usedInCollections.add(collection);
                    }
                }
            }

            this.ownerUsername = nodeRef.getOwner();

        } catch (Throwable t) {
            throw DAOException.mapping(t, nodeRef.getNodeId());
        }
    }

    public boolean isFromRemoteRepository() {
        return remoteId != null || !this.repoDao.isHomeRepo() || this.aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY);
    }

    public void refreshPermissions(org.edu_sharing.service.model.NodeRef nodeRef) {
        boolean isRemoteCopy = !this.isCollectionReference() && aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY);
        org.edu_sharing.service.permission.PermissionService usedPermissionService = isRemoteCopy ? PermissionServiceFactory.getLocalService() : permissionService;
        if (nodeRef != null && nodeRef.getPublic() != null) {
            this.isPublic = nodeRef.getPublic();
        } else {
            GuestConfig guestConfig = guestService.getCurrentGuestConfig();
            if (guestConfig != null && guestConfig.isEnabled()) {
                try {
                    this.isPublic = usedPermissionService.hasPermission(
                            storeProtocol,
                            storeId,
                            nodeId,
                            guestConfig.getUsername(),
                            CCConstants.PERMISSION_READ_ALL
                    );
                } catch (Throwable t) {
                    logger.info("Unexpected error while resolving isPublic for node " + nodeId, t);
                }
            }
        }
        if (nodeRef != null && nodeRef.getPermissions() != null && !nodeRef.getPermissions().isEmpty()) {
            this.hasPermissions = nodeRef.getPermissions();
        } else {
            this.hasPermissions = usedPermissionService.hasAllPermissions(storeProtocol, storeId, nodeId, DAO_PERMISSIONS);
        }
    }

    public static NodeEntries convertToRest(RepositoryDao repoDao,
                                            Filter propFilter,
                                            List<NodeRef> children,
                                            Integer skipCount,
                                            Integer maxItems) throws DAOException {
        return convertToRest(repoDao, propFilter, children, skipCount, maxItems, null);
    }

    public static NodeEntries convertToRest(RepositoryDao repoDao,
                                            Filter propFilter,
                                            List<NodeRef> children,
                                            Integer skipCount,
                                            Integer maxItems,
                                            Function<NodeDao, NodeDao> transform

    ) throws DAOException {

        NodeEntries result = new NodeEntries();
        List<NodeRef> slice = children.subList(skipCount, Math.min(children.size(), skipCount + maxItems));

        List<Node> nodes = convertToRest(repoDao, slice, propFilter, transform);
        int removedNodes = slice.size() - nodes.size();
        Pagination pagination = new Pagination();
        pagination.setFrom(skipCount);
        pagination.setCount(nodes.size());
        pagination.setTotal(children.size() - removedNodes);
        result.setPagination(pagination);
        result.setNodes(nodes);
        return result;
    }

    public static List<Node> convertToRest(RepositoryDao repoDao, List<NodeRef> list, Filter propFilter, Function<NodeDao, NodeDao> transform) {
        if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
            return list.stream().map(nodeRef -> {
                try {
                    NodeDao nodeDao = NodeDao.getNode(repoDao, nodeRef.getId(), propFilter);
                    if (transform != null) {
                        nodeDao = transform.apply(nodeDao);
                    }
                    return nodeDao.asNode();
                } catch (DAOMissingException daoException) {
                    logger.warn("Missing node " + nodeRef.getId() + " tried to fetch, skipping fetch", daoException);
                    return null;
                } catch (DAOException e) {
                    throw new RuntimeException(e);
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        final String user = AuthenticationUtil.getFullyAuthenticatedUser();
        final Context context = Context.getCurrentInstance();
        final String scope = NodeServiceInterceptor.getEduSharingScope();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Node> nodes = null;
        java.util.Collection<Callable<Node>> tasks = list.stream().map(
                (nodeRef) -> (Callable<Node>) () -> AuthenticationUtil.runAs(() -> {
                    try {
                        // apply thread variables to keep state of thread
                        Context.setInstance(context);
                        NodeServiceInterceptor.setEduSharingScope(scope);
                        NodeDao nodeDao = NodeDao.getNode(repoDao, nodeRef.getId(), propFilter);
                        if (transform != null) {
                            nodeDao = transform.apply(nodeDao);
                        }
                        return nodeDao.asNode();
                    } catch (DAOMissingException daoException) {
                        logger.warn("Missing node " + nodeRef.getId() + " tried to fetch, skipping fetch", daoException);
                        return null;
                    } catch (DAOToolPermissionException daoException) {
                        logger.info("Toolpermission exception for node " + nodeRef.getId() + " tried to fetch, skipping fetch", daoException);
                        return null;
                    } finally {
                        Context.release();
                    }
                }, user)
        ).collect(Collectors.toList());
        try {
            nodes = executor.invokeAll(tasks).stream()
                    .filter(Objects::nonNull)
                    .map((f) -> {
                        try {
                            return f.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toList());
            executor.shutdown();
            return nodes;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<NodeRef> sortApiNodeRefs(RepositoryDao repoDao, List<NodeRef> refList, List<String> filter, SortDefinition sortDefinition) {
        return NodeDao.convertAlfrescoNodeRef(repoDao, NodeDao.sortAlfrescoRefs(NodeDao.convertApiNodeRef(refList), filter, sortDefinition));
    }

    public NodeDao createChild(String type, List<String> aspects,
                               Map<String, String[]> properties, boolean renameIfExists) throws DAOException {
        return this.createChild(type, aspects, properties, renameIfExists, null);
    }

    public NodeDao createChild(String type, List<String> aspects,
                               Map<String, String[]> properties, boolean renameIfExists, String childAssoc) throws DAOException {

        try {
            NameSpaceTool<String> nameSpaceTool = new NameSpaceTool<String>();
            type = nameSpaceTool.transformToLongQName(type);
            if (childAssoc != null)
                childAssoc = CCConstants.getValidGlobalName(childAssoc);
            Map<String, String[]> props = transformProperties(properties);
            String childId;

            String originalNameArr[] = props.get(CCConstants.CM_NAME);
            String originalName = (originalNameArr != null && originalNameArr.length > 0) ? originalNameArr[0] : null;
            if (originalName == null) throw new Exception("missing name");

            // escape invalid characters of the name
            originalName = EduSharingNodeHelper.cleanupCmName(originalName);
            props.put(CCConstants.CM_NAME, new String[]{originalName});

            int i = 2;
            while (true) {
                try {
                    childId = this.nodeService.createNode(nodeId, type, props, childAssoc);
                    break;
                } catch (DuplicateChildNodeNameException e) {
                    if (renameIfExists) {
                        props.put(CCConstants.CM_NAME, new String[]{NodeServiceHelper.renameNode(originalName, i)});
                        i++;
                        continue;
                    }
                    throw e;
                }
            }

            if (aspects != null) {
                for (String aspect : aspects) {
                    aspect = NameSpaceTool.transformToLongQName(aspect);
                    nodeService.addAspect(childId, aspect);
                }
            }

            return new NodeDao(repoDao, childId, Filter.createShowAllFilter());

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }

    }

    public NodeDao createChildByMove(String sourceId) throws DAOException {

        try {

            nodeService.moveNode(nodeId, CCConstants.CM_ASSOC_FOLDER_CONTAINS,
                    sourceId);
            // set for the given collection level 0 to true to support search
            if (isCollectionHomePath) {
                NodeServiceHelper.setProperty(
                        new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, sourceId),
                        CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0,
                        true, false);
            }
            return new NodeDao(repoDao, sourceId, Filter.createShowAllFilter());

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public NodeDao createChildByCopy(String sourceId, boolean withChildren)
            throws DAOException {

        try {
            org.alfresco.service.cmr.repository.NodeRef newNode = nodeService.copyNode(sourceId, nodeId, withChildren);
            permissionService.createNotifyObject(newNode.getId(), AuthenticationUtil.getFullyAuthenticatedUser(), CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD);
            return new NodeDao(repoDao, newNode.getId(), Filter.createShowAllFilter());

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public List<NodeRef> getChildrenSubobjects() throws DAOException {
        SortDefinition sort = new SortDefinition();
        sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_CHILDOBJECT_ORDER), true));
        sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CM_NAME), true));
        List<String> filter = new ArrayList<>();
        filter.add("files");
        return getChildren(null, filter, sort);
    }

    public List<NodeRef> getChildren() throws DAOException {
        return getChildren(null, null, new SortDefinition());
    }

    public List<NodeRef> getChildren(String assocName, List<String> filter, SortDefinition sortDefinition) throws DAOException {

        try {
            List<NodeRef> result = new ArrayList<>();


            if (assocName != null && !assocName.isEmpty()) {
                assocName = CCConstants.getValidGlobalName(assocName);
            }
            for (ChildAssociationRef childRef : nodeService.getChildrenChildAssociationRefAssoc(getId(), assocName, filter, sortDefinition)) {

                NodeRef ref = new NodeRef();
                ref.setRepo(this.repoDao.getId());
                ref.setHomeRepo(this.repoDao.isHomeRepo());
                ref.setId(childRef.getChildRef().getId());

                String storeProtocol = childRef.getChildRef().getStoreRef().getProtocol();
                if (StoreRef.PROTOCOL_ARCHIVE.equals(storeProtocol)) {
                    ref.setArchived(true);
                }

                result.add(ref);
            }

            return result;

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public List<NodeRef> getAssocs(AssocInfo assoc, List<String> filter, SortDefinition sortDefinition) throws DAOException {

        try {
            List<NodeRef> result = new ArrayList<>();
            assoc.setAssocName(CCConstants.getValidGlobalName(assoc.getAssocName()));
            List<AssociationRef> assocs = nodeService.getNodesByAssoc(getId(), assoc);
            assocs = ((NodeServiceImpl) NodeServiceFactory.getLocalService()).sortNodeRefList(assocs, filter, sortDefinition);
            for (AssociationRef childRef : assocs) {

                NodeRef ref = new NodeRef();
                ref.setRepo(this.repoDao.getId());
                ref.setHomeRepo(this.repoDao.isHomeRepo());
                ref.setId(childRef.getTargetRef().getId());

                String storeProtocol = childRef.getTargetRef().getStoreRef().getProtocol();
                if (StoreRef.PROTOCOL_ARCHIVE.equals(storeProtocol)) {
                    ref.setArchived(true);
                }

                result.add(ref);
            }

            return result;

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public NodeDao changeProperties(Map<String, String[]> properties)
            throws DAOException {

        try {

            this.nodeService.updateNode(nodeId, transformProperties(properties));

            return new NodeDao(repoDao, nodeId, Filter.createShowAllFilter());

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public NodeDao changePropertiesWithVersioning(
            Map<String, String[]> properties, String comment) throws DAOException {

        // Throws ConcurrencyFailureException if the previous call changes the preview (DESP-851)
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {

            try {
                mergeVersionComment(properties, comment);

                // 1. update
                this.nodeService.updateNode(nodeId, transformProperties(properties));

                // 2. versioning
                this.nodeService.createVersion(nodeId);

            } catch (Throwable t) {
                throw DAOException.mapping(t);
            }
            return null;
        });
        // don't do this in transaction since it could cause rollbacks!
        try {
            return new NodeDao(repoDao, nodeId, Filter.createShowAllFilter());
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public NodeDao changePreview(InputStream is, String mimetype, boolean version) throws DAOException {

        try {
            ImageTool.VerifyResult result = ImageTool.verifyAndPreprocessImage(is, ImageTool.MAX_THUMB_SIZE);

            Map<String, String[]> props = new HashMap<>();
            if (version) {
                props.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, new String[]{CCConstants.VERSION_COMMENT_PREVIEW_CHANGED});
                //mergeVersionComment(props, versionComment);
            }
            props.put(CCConstants.CCM_PROP_IO_CREATE_VERSION, new String[]{new Boolean(version).toString()});
            nodeService.updateNode(nodeId, props);
            nodeService.writeContent(storeRef, nodeId, result.getInputStream(), result.getMediaType().toString(), null,
                    isDirectory() ? CCConstants.CCM_PROP_MAP_ICON : CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW);
            PreviewCache.purgeCache(nodeId);
            return new NodeDao(repoDao, nodeId);

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public NodeDao deletePreview() throws DAOException {

        try {
            nodeService.removeProperty(storeProtocol, storeId, nodeId, isDirectory() ? CCConstants.CCM_PROP_MAP_ICON : CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW);
            PreviewCache.purgeCache(nodeId);
            return new NodeDao(repoDao, nodeId);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public NodeDao changeContent(InputStream is, String mimetype,
                                 String versionComment) throws DAOException {

        try {
            Map<String, String[]> props = new HashMap<>();

            boolean version = versionComment != null && !versionComment.isEmpty();
            // 1. update
            if (version) {
                props.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, new String[]{versionComment});
                //mergeVersionComment(props, versionComment);
            }
            props.put(CCConstants.CCM_PROP_IO_CREATE_VERSION, new String[]{new Boolean(version).toString()});
            nodeService.updateNode(nodeId, props);


            // 2. change content (automatic versioning)
            nodeService.writeContent(storeRef, nodeId, is, mimetype, null,
                    CCConstants.CM_PROP_CONTENT);

            return new NodeDao(repoDao, nodeId);

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    private void mergeVersionComment(Map<String, String[]> properties,
                                     String versionComment) {

        properties.remove(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_VERSION_COMMENT));

        properties.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, new String[]{versionComment});
    }

    public void delete(boolean recycle) throws DAOException {
        try {
            nodeService.removeNode(nodeId, getParentId(), recycle);
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    public List<NodeVersion> getHistory() throws DAOException {
        List<NodeVersion> history = new ArrayList<>();
        for (Entry<String, Map<String, Object>> version : getNodeHistory().entrySet()) {
            NodeVersion nodeVersion = convertVersionProps(version.getKey(), version.getValue());
            history.add(nodeVersion);
        }
        // Sort by version
        Collections.sort(history, (o1, o2) -> {
            if (o1.getVersion().getMajor() == o2.getVersion().getMajor())
                return o1.getVersion().getMinor() > o2.getVersion().getMinor() ? 1 : -1;
            return o1.getVersion().getMajor() > o2.getVersion().getMajor() ? 1 : -1;
        });
        return history;
    }

    public List<NodeVersionRef> getNodeRefHistory() throws DAOException {
        try {
            return nodeService.getVersionLabelsHistory(nodeId)
                    .stream()
                    .map(this::transformVersion)
                    .sorted((lhs, rhs) -> {
                        if (lhs.getMajor() == rhs.getMajor()) {
                            return Integer.compare(lhs.getMinor(), rhs.getMinor());
                        }
                        return Integer.compare(lhs.getMajor(), rhs.getMajor());
                    })
                    .collect(Collectors.toList());
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public NodeDao revertHistory(int major, int minor) throws DAOException {

        try {

            String versionLabel = getVersionLabel(major, minor);

            Map<String, Object> versionProps = getNodeHistory().get(versionLabel);

            if (versionProps == null) {
                return null;
            }

            nodeService.revertVersion(nodeId, versionLabel);

            return new NodeDao(repoDao, nodeId);

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public List<String> getAspectsNative() {
        return this.aspects;
    }

    public Node asNode() throws DAOException {
        return asNode(true);
    }

    public Node asNode(boolean fetchReference) throws DAOException {
        if (this.version != null) {
            VersionedNode node = new VersionedNode();
            fillNodeObject(node, true, true);
            VersionedNode.Version version = new VersionedNode.Version();
            version.setComment((String) nodeProps.get(CCConstants.CCM_PROP_IO_VERSION_COMMENT));
            node.setVersion(version);
            return node;
        }
        Node data = new Node();
        if (collectionRef != null) {
            data = new CollectionRelationReference();
            ((CollectionRelationReference) data).setRelationType(collectionRef.getRelationType());
        } else if (isCollectionReference() && fetchReference) {
            data = new CollectionReference();
            fillNodeReference((CollectionReference) data);
        }
        fillNodeObject(data, true, true);
        return data;
    }

    private boolean isCollectionReference() {
        return aspects.contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE);
    }

    private boolean isPublishedCopy() {
        return getNativeProperties().get(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL) != null;
    }

    private void fillNodeReference(CollectionReference reference) throws DAOException {
        final String originalId = getReferenceOriginalId();
        reference.setOriginalId(originalId);
        try {
            reference.setAccessOriginal(NodeDao.getNode(repoDao, originalId).asNode(false).getAccess());
        } catch (Throwable t) {
            // user may has no access to the original or it is deleted, this is okay
        }
        // not supported and used by remote repositories, BUT is supported for local copies
        if (isFromRemoteRepository() && !aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)) {
            return;
        }
        AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                try {
                    NodeDao nodeDaoOriginal = NodeDao.getNode(repoDao, originalId);
                    Serializable originalAccess = NodeServiceHelper.getPropertyNative(
                            new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, originalId)
                            , CCConstants.CCM_PROP_RESTRICTED_ACCESS);
                    List<String> restrictedPermissions = (List<String>) NodeServiceHelper.getPropertyNative(
                            new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, originalId)
                            , CCConstants.CCM_PROP_RESTRICTED_ACCESS_PERMISSIONS);
                    reference.setCreatedBy(nodeDaoOriginal.asNode(false).getCreatedBy());
                    reference.setOriginalRestrictedAccess(originalAccess != null && (Boolean) originalAccess);
                    reference.setAccessEffective(getEffectiveAccess(reference, restrictedPermissions));
                } catch (InvalidNodeRefException t) {
                    reference.setOriginalId(null);
                    // original maybe deleted
                } catch (Throwable t) {
                    logger.warn("Could not fetch original node for id " + nodeId, t);
                    reference.setOriginalId(null);
                }
                return null;
            }

        });
    }

    private java.util.Collection<String> getEffectiveAccess(CollectionReference reference, List<String> restrictedPermissions) {
        Set<String> result = new HashSet<>();
        if (reference.getAccessOriginal() != null) {
            result.addAll(reference.getAccessOriginal());
        }
        boolean restrictedAccess = reference.isOriginalRestrictedAccess();
        result.addAll(PermissionServiceHelper.getEffectivePermissions(restrictedPermissions, restrictedAccess));
        return result;
    }

    private String getReferenceOriginalId() throws DAOException {
        final String originalId = (String) getNativeProperties().get(CCConstants.CCM_PROP_IO_ORIGINAL);
        return originalId;
    }

    public <T extends Node> void fillNodeObject(T data, boolean fillOwner, boolean fillContent) throws DAOException {
        data.setRef(getRef());
        data.setParent(getParentRef());
        data.setRemote(getRemote());

        data.setType(getType());
        data.setDirectory(isDirectory());
        data.setAspects(NameSpaceTool.transFormToShortQName(this.aspects));

        data.setName(getName());
        data.setTitle(getTitle());

        data.setCreatedAt(getCreatedAt());
        data.setCreatedBy(getCreatedBy());
        if (fillOwner) {
            data.setOwner(getOwner());
        }

        data.setModifiedAt(getModifiedAt());
        data.setModifiedBy(getModifiedBy());

        if (fillContent) {
            data.setContent(getContent(data));
        }

        data.setDownloadUrl(getDownloadUrl());
        data.setMetadataset(getMetadataSet());

        data.setProperties(getProperties());

        data.setAccess(access);
        // set access effective for original elements only
        if (!(data instanceof CollectionReference) && Objects.equals(CallSourceHelper.CallSource.Render, CallSourceHelper.getCallSource())) {
            java.util.Collection<String> permissions = org.edu_sharing.service.nodeservice.NodeServiceInterceptor.getIndirectPermissions(getId(), List.of(DAO_PERMISSIONS));
            // since it's an original: we join the permissions with the original ones
            permissions = new HashSet<>(permissions);
            permissions.addAll(access);
            data.setAccessEffective(permissions);
        }
        data.setPublic(isPublic);

        data.setMimetype(getMimetype());
        data.setMediatype(getMediatype());
        data.setIconURL(getIconURL());
        data.setCommentCount(getCommentCount());
        data.setLicense(getLicense());
        data.setSize(getSize(data));

        data.setContributors(contributors);

        data.setRating(getRating());
        try {
            data.setPreview(getPreview());
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        data.setRepositoryType(getRepositoryType());

        if (isCollection()) {
            Collection collection = new CollectionDao(repoDao, getRef().getId(), this, data).asCollection();
            data.setCollection(collection);
        }

        for (NodeDao nodeDao : usedInCollections) {
            data.getUsedInCollections().add(nodeDao.asNode());
        }
        if (!relations.isEmpty()) {
            Map<NodeRefImpl.Relation, Node> relationsConverted = new HashMap<>();
            relations.forEach((relation, dao) -> {
                try {
                    relationsConverted.put(relation, dao.asNode(false));
                } catch (DAOException e) {
                    logger.info("Could not convert relation: " + e.getMessage());
                }
            });
            data.setRelations(relationsConverted);
        }
    }

    public RepositoryDao getRepositoryDao() {
        return remoteRepository != null ? remoteRepository : repoDao;
    }

    private Remote getRemote() throws DAOException {
        if (!isFromRemoteRepository())
            return null;
        Remote remote = new Remote();
        String remoteObjectRepositoryId = (String) this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID);
        if (aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY) && remoteObjectRepositoryId != null) {
            remote.setId((String) this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID));
            try {
                remote.setRepository(RepositoryDao.getRepository(remoteObjectRepositoryId).asRepo());
            } catch (DAOMissingException e) {
                logger.warn("Repository " + remoteObjectRepositoryId + " is not present anymore: " + e.getMessage());
            }
        } else if (remoteId != null) {
            remote.setId(remoteId);
            remote.setRepository(remoteRepository.asRepo());
        } else {
            // this is the case if NodeDao was already called via a remote ref (and not a shadow object)
            remote.setId(getId());
            remote.setRepository(repoDao.asRepo());
        }
        return remote;
    }

    private License getLicense() {
        if (isDirectory())
            return null;
        License license = new License();
        license.setIcon(new LicenseService().getIconUrl((String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)));
        license.setUrl(new LicenseService().getLicenseUrl(
                (String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY),
                (String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE),
                (String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION)
        ));
        return license;
    }

    private Content getContent(Node data) throws DAOException {
        Content content = new Content();
        content.setUrl(getContentUrl());
        // skip hash + version for search cause of performance penalties
        if (Arrays.asList(CallSourceHelper.CallSource.Search, CallSourceHelper.CallSource.Sitemap).contains(CallSourceHelper.getCallSource())) {
            return content;
        }
        content.setVersion(getContentVersion(data));

        if (isFromRemoteRepository()) {
            // @TODO: not available here
        } else if (data instanceof CollectionReference && ((CollectionReference) data).getOriginalId() != null) {
            content.setHash(AuthenticationUtil.runAsSystem(() ->
                    nodeService.getContentHash(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
                            ((CollectionReference) data).getOriginalId(), this.version, org.alfresco.model.ContentModel.PROP_CONTENT.toString())
            ));
        } else {
            content.setHash(nodeService.getContentHash(storeProtocol, storeId, nodeId, this.version, org.alfresco.model.ContentModel.PROP_CONTENT.toString()));
        }
        return content;
    }

    public String getNativeType() {
        return this.type;
    }

    public String getType() {
        return NameSpaceTool.transformToShortQName(this.type);
    }

    private String getMetadataSet() {
        return (String) nodeProps.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
    }

    private String getIconURL() {
        return new MimeTypesV2().getIcon(type, nodeProps, aspects);
    }

    public List<String> getPermissions(String authority) throws DAOException {
        try {
            return permissionService.getPermissionsForAuthority(nodeId, authority);
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    public org.edu_sharing.repository.client.rpc.ACL getRawPermissions() throws DAOException {
        if (!AuthorityServiceHelper.isAdmin()) {
            throw new AccessDeniedException("admin role required.");
        }
        try {
            org.edu_sharing.repository.client.rpc.ACL permissions = null;
            try {
                permissions = permissionService.getPermissions(nodeId);
            } catch (org.alfresco.repo.security.permissions.AccessDeniedException accessDenied) {
                //than you don't have the permission no ask for
                return null;
            }

            return permissions;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public NodePermissions getPermissions() throws DAOException {

        try {
            org.edu_sharing.repository.client.rpc.ACL permissions;
            try {
                permissions = permissionService.getPermissions(nodeId);
            } catch (org.alfresco.repo.security.permissions.AccessDeniedException accessDenied) {
                //than you don't have the permission no ask for
                return null;
            }

            if (permissions == null) {
                return null;
            }

            ACL local = new ACL();
            local.setInherited(permissions.isInherited());
            local.setPermissions(new ArrayList<>());

            NodePermissions result = new NodePermissions();
            result.setLocalPermissions(local);
            result.setInheritedPermissions(new ArrayList<>());

            org.edu_sharing.repository.client.rpc.ACE[] aces = permissions.getAces();

            if (aces == null) {
                return result;
            }

            Set<TimedPermissionAuthority> authPerm = new HashSet<>();
            Map<Authority, List<String>> authPermInherited = new HashMap<>();
            for (org.edu_sharing.repository.client.rpc.ACE ace : aces) {

                if (!"acepted".equals(ace.getAccessStatus())) {
                    continue;
                }

                Authority authority = (Authority.Type.valueOf(ace.getAuthorityType()) == Authority.Type.GROUP) ? new Group(ace) : new Authority(ace);
                if (ace.isInherited()) {
                    List<String> tmpPerms = authPermInherited.get(authority);
                    if (tmpPerms == null) {
                        tmpPerms = new ArrayList<>();
                    }
                    // do not duplicate existing permissions
                    if (!tmpPerms.contains(ace.getPermission())) {
                        tmpPerms.add(ace.getPermission());
                    }
                    authPermInherited.put(authority, tmpPerms);
                } else {
                    TimedPermissionAuthority timedPermission = new TimedPermissionAuthority(authority, ace.getFrom(), ace.getTo(), new HashSet<>(Collections.singleton(ace.getPermission())));
                    Optional<TimedPermissionAuthority> existingAuthority = authPerm.stream().filter(a -> a.equalsIgnorePermissions(timedPermission)).findAny();
                    Optional<TimedPermissionAuthority> existingPermission = authPerm.stream().filter(a -> a.equalsIgnoreFromTo(timedPermission)).findAny();
                    // do not duplicate existing permissions
                    if (existingAuthority.isPresent() && !existingAuthority.get().getPermissions().contains(ace.getPermission())) {
                        existingAuthority.get().getPermissions().add(ace.getPermission());
                    } else if (existingPermission.isPresent()) {
                        if (timedPermission.isTimed()) {
                            existingPermission.get().setFrom(timedPermission.getFrom());
                            existingPermission.get().setTo(timedPermission.getTo());
                        }
                    } else {
                        authPerm.add(timedPermission);
                    }
                }

            }


            for (TimedPermissionAuthority entry : authPerm) {
                ACE ace = getACEAsSystem(entry.getAuthority());
                ace.setPermissions(new ArrayList<>(entry.getPermissions()));
                ace.setFrom(entry.getFrom());
                ace.setTo(entry.getTo());
                ace.setEditable(entry.getAuthority().isEditable());
                result.getLocalPermissions().getPermissions().add(ace);
            }

            for (Map.Entry<Authority, List<String>> entry : authPermInherited.entrySet()) {
                ACE ace = getACEAsSystem(entry.getKey());
                ace.setPermissions(entry.getValue());
                result.getInheritedPermissions().add(ace);
            }


            return result;

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }

    }

    private ACE getACEAsSystem(Authority key) {
        return AuthenticationUtil.runAsSystem(new RunAsWork<ACE>() {

            @Override
            public ACE doWork() throws Exception {
                try {
                    if (key.getAuthorityType().name().equals("GROUP")) {
                        GroupProfile group = GroupDao.getGroup(repoDao, key.getAuthorityName()).asGroup().getProfile();
                        return new ACE(key, null, group);
                    } else if (key.getAuthorityType().name().equals("EVERYONE")) {
                        //@TODO check if authority must be changed
                        GroupProfile group = GroupDao.getGroup(repoDao, key.getAuthorityName()).asGroup().getProfile();
                        return new ACE(key, null, group);
                    } else if (key.getAuthorityName().equals("ROLE_OWNER")) {
                        return new ACE(key, null, null);
                    } else {
                        UserProfile user = PersonDao.getPerson(repoDao, key.getAuthorityName()).asPerson().getProfile();
                        return new ACE(key, user, null);
                    }
                } catch (DAOException e) {
                    // this may happens for a virtual user, e.g. GROUP_EVERYONE
                    return new ACE(key, null, null);
                }
            }
        });
    }

    public void setPermissions(ACL permissions, String mailText, Boolean sendMail, Boolean sendCopy) throws DAOException {

        try {
            List<org.edu_sharing.repository.client.rpc.ACE> aces = getAceList(permissions);
            org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(repoDao.getId());
            permissionService.setPermissions(
                    nodeId,
                    aces,
                    permissions.isInherited(),
                    mailText, sendMail, sendCopy
            );


        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    private static @NotNull List<org.edu_sharing.repository.client.rpc.ACE> getAceList(ACL permissions) {
        List<org.edu_sharing.repository.client.rpc.ACE> aces = new ArrayList<>();
        for (ACE permission : permissions.getPermissions()) {

            for (String tmpPerm : permission.getPermissions()) {
                org.edu_sharing.repository.client.rpc.ACE ace = new org.edu_sharing.repository.client.rpc.ACE();

                ace.setAccessStatus("accepted");

                ace.setAuthority(permission.getAuthority().getAuthorityName());
                ace.setAuthorityType(permission.getAuthority().getAuthorityType().name());

                ace.setFrom(permission.getFrom());
                ace.setTo(permission.getTo());

                ace.setPermission(tmpPerm);
                aces.add(ace);
            }
        }
        return aces;
    }

    private Map<String, String[]> transformProperties(
            Map<String, String[]> properties) {
        return NodeServiceHelper.transformShortToLongProperties(properties);
    }

    private String getId() {

        return (String) nodeProps.get(CCConstants.SYS_PROP_NODE_UID);
    }

    public boolean isDirectory() {
        return MimeTypesV2.isDirectory(nodeProps, type);
    }

    public boolean isCollection() {
        return MimeTypesV2.isCollection(aspects, nodeProps);
    }

    public NodeRef getRef() {

        NodeRef nodeRef = createNodeRef(repoDao, nodeId);
        String storeProtocol = (String) this.nodeProps.get(CCConstants.SYS_PROP_STORE_PROTOCOL);
        if (StoreRef.PROTOCOL_ARCHIVE.equals(storeProtocol)) {
            nodeRef.setArchived(true);
        }

        return nodeRef;
    }

    private NodeRef getParentRef() {
        return createNodeRef(repoDao, getParentId());
    }

    public static NodeRef createNodeRef(RepositoryDao repoDao, String nodeId) {
        if (repoDao.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)) {
            try {
                repoDao = RepositoryDao.getHomeRepository();
            } catch (DAOException e) {
                throw new RuntimeException(e);
            }
        }
        NodeRef nodeRef = new NodeRef();
        nodeRef.setRepo(repoDao.getId());
        nodeRef.setId(nodeId);
        nodeRef.setHomeRepo(repoDao.isHomeRepo());
        return nodeRef;
    }

    public String getStoreProtocol() {
        return (String) nodeProps.get(CCConstants.SYS_PROP_STORE_PROTOCOL);
    }

    public String getStoreIdentifier() {
        return (String) nodeProps.get(CCConstants.SYS_PROP_STORE_IDENTIFIER);
    }

    private String getParentId() {

        return (String) nodeProps
                .get(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID);
    }

    private String getName() {

        return (String) nodeProps.get(CCConstants.CM_NAME);
    }

    private String getTitle() {
        return (String) (nodeProps.get(CCConstants.LOM_PROP_GENERAL_TITLE) != null ?
                nodeProps.get(CCConstants.LOM_PROP_GENERAL_TITLE) :
                nodeProps.get(CCConstants.CM_PROP_TITLE));
    }

    public Date getCreatedAt() {

        String key = CCConstants.CM_PROP_C_CREATED
                + CCConstants.LONG_DATE_SUFFIX;

        return nodeProps.containsKey(key) ? new Date(
                Long.parseLong((String) nodeProps.get(key))) : null;
    }

    public Person getCreatedBy() {

        Person ref = new Person();
        if (nodeProps.get(CCConstants.NODECREATOR_FIRSTNAME) != null && nodeProps.get(CCConstants.NODECREATOR_LASTNAME) != null) {
            ref.setFirstName((String) nodeProps
                    .get(CCConstants.NODECREATOR_FIRSTNAME));
            ref.setLastName((String) nodeProps
                    .get(CCConstants.NODECREATOR_LASTNAME));
            if (resolveUsernames && this.checkUserHasPermissionToSeeMail((String) nodeProps.get(CCConstants.CM_PROP_C_CREATOR)))
                ref.setMailbox((String) nodeProps.get(CCConstants.NODECREATOR_EMAIL));
        } else if (resolveUsernames && authorityService != null) {
            User user = authorityService.getUser((String) nodeProps.get(CCConstants.CM_PROP_C_CREATOR));
            if (user != null) {
                ref.setFirstName(user.getGivenName());
                ref.setLastName(user.getSurname());
                if (this.checkUserHasPermissionToSeeMail((String) nodeProps.get(CCConstants.CM_PROP_C_CREATOR)))
                    ref.setMailbox(ref.getMailbox());
            }
        }
        return ref;
    }

    private Person getOwner() {
        if (resolveUsernames) {
            User owner = null;
            if (ownerUsername != null && !ownerUsername.trim().equals("")) {
                if (authorityService != null) {
                    owner = authorityService.getUser(ownerUsername);
                }
            }
            owner = (owner == null) ? nodeService.getOwner(storeId, storeProtocol, nodeId) : owner;
            if (owner == null)
                return null;
            Person ref = new Person();
            ref.setFirstName(owner.getGivenName());
            ref.setLastName(owner.getSurname());
            if (this.checkUserHasPermissionToSeeMail(owner.getUsername()))//only admin can see even if users have hide email
                ref.setMailbox(owner.getEmail());

            return ref;
        }
        return null;
    }


    /**
     * Check if normal USER has permision to see email
     * ADMIN can see the email even if the email is private for specific USER
     *
     * @param (String) userName  of Person,
     * @return true || false
     */
    public boolean checkUserHasPermissionToSeeMail(String userName) {
        try {
            if (LightbendConfigCache.getBoolean("repository.privacy.filterMetadataEmail") &&
                    (access == null || !access.contains(CCConstants.PERMISSION_WRITE))) {
                return false;
            }
            if (this.isCurrentUserAdminOrSameUserAsUserName(userName)) // if is ADMIN or sameUser, don't need to countinue;
                return true;
            else {
                Map<String, Serializable> profileSettings = AuthorityServiceFactory.getLocalService().getProfileSettingsProperties(userName, CCConstants.CCM_PROP_PERSON_SHOW_EMAIL);
                // default value is true for backward compatibility reasons
                boolean isEmailPublic = true;
                if (profileSettings.containsKey(CCConstants.CCM_PROP_PERSON_SHOW_EMAIL)) {
                    isEmailPublic = (boolean) profileSettings.get(CCConstants.CCM_PROP_PERSON_SHOW_EMAIL);
                }
                return isEmailPublic;
            }

        } catch (Exception e) {
            logger.debug("Cannot check if current User has permision to see email or not  : " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Method which check if:
     * -  login User is ADMIN or simple USER,
     * -  login User is similar with userName
     *
     * @param userName get a userName as parameter to controll
     * @return TRUE if is ADMIN or Same USER || FALSE in other cases
     */
    public boolean isCurrentUserAdminOrSameUserAsUserName(String userName) {
        boolean globalAdmin;
        String runAsUser = AuthenticationUtil.getRunAsUser();
        String fullyUser = AuthenticationUtil.getFullyAuthenticatedUser();
        if (runAsUser.equals(fullyUser)
                && !AuthenticationUtil.isRunAsUserTheSystemUser()) {
            if (isGlobalAdmin.get() == null) {
                globalAdmin = AuthorityServiceFactory.getLocalService().isGlobalAdmin();
                isGlobalAdmin.set(globalAdmin);
            } else {
                globalAdmin = isGlobalAdmin.get();
            }
        } else {
            globalAdmin = AuthorityServiceFactory.getLocalService().isGlobalAdmin();
        }

        if (globalAdmin) // if userLogin is ADMIN, don't need to countinue;
            return true;
        else {
            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            return currentUser.equals(userName);
        }
    }


    private Date getModifiedAt() {

        String key = CCConstants.CM_PROP_C_MODIFIED
                + CCConstants.LONG_DATE_SUFFIX;

        return nodeProps.containsKey(key) ? new Date(
                Long.parseLong((String) nodeProps.get(key))) : null;
    }

    private Person getModifiedBy() {

        Person ref = new Person();
        if (nodeProps.get(CCConstants.NODEMODIFIER_FIRSTNAME) != null && nodeProps.get(CCConstants.NODEMODIFIER_LASTNAME) != null) {
            ref.setFirstName((String) nodeProps
                    .get(CCConstants.NODEMODIFIER_FIRSTNAME));
            ref.setLastName((String) nodeProps
                    .get(CCConstants.NODEMODIFIER_LASTNAME));
            if (resolveUsernames && this.checkUserHasPermissionToSeeMail((String) nodeProps.get(CCConstants.CM_PROP_C_MODIFIER)))
                ref.setMailbox((String) nodeProps.get(CCConstants.NODEMODIFIER_EMAIL));
        } else if (resolveUsernames && authorityService != null) {
            User user = authorityService.getUser((String) nodeProps.get(CCConstants.CM_PROP_C_MODIFIER));
            if (user != null) {
                ref.setFirstName(user.getGivenName());
                ref.setLastName(user.getSurname());
                if (this.checkUserHasPermissionToSeeMail((String) nodeProps.get(CCConstants.CM_PROP_C_MODIFIER)))
                    ref.setMailbox(ref.getMailbox());
            }
        }
        return ref;
    }

    private String getContentVersion(Node data) throws DAOException {
        if (data instanceof CollectionReference && ((CollectionReference) data).getOriginalId() != null && !isFromRemoteRepository()) {
            try {
                return AuthenticationUtil.runAsSystem(() ->
                        nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,
                                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
                                ((CollectionReference) data).getOriginalId(), CCConstants.CM_PROP_VERSIONABLELABEL)
                );
            } catch (Throwable t) {
                logger.warn("Error while fetching original node version from " + nodeId + ":" + t.getMessage());
            }
        }
        String version = (String) nodeProps.get(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
        if (version == null) {
            version = (String) nodeProps.get(CCConstants.CM_PROP_VERSIONABLELABEL);
        }
        if (version == null) {
            version = this.version;
        }
        return version;
    }

    private String getContentUrl() {
        return (String) nodeProps.get(CCConstants.CONTENTURL);
    }

    private RatingDetails getRating() {
        try {
            if (access.contains(CCConstants.PERMISSION_RATE_READ)) {
                // skip permission checks, this can be useful if the user might have indirect access via collection
                return AuthenticationUtil.runAsSystem(() -> RatingServiceFactory.getRatingService(repoDao.getId()).getAccumulatedRatings(getNodeRef(), null));
            } else {
                return RatingServiceFactory.getRatingService(repoDao.getId()).getAccumulatedRatings(getNodeRef(), null);
            }
        } catch (Throwable t) {
            if (t.getCause() instanceof InsufficientPermissionException) {
                // ignored
                return null;
            }
            logger.info("Can not fetch ratings for node " + nodeId + ": " + t.getMessage());
            logger.debug(t);
            return null;
        }
    }

    private boolean isLink() {
        return nodeProps.containsKey(CCConstants.CCM_PROP_IO_WWWURL);
    }

    public String getDownloadUrl() {
        // the download servlet can may passthrough the TECHNICAL_LOCATION, so it depends on the actual state of the DOWNLOADURL
        //if(isLink())
        //	return null;

        // no download url if user can not access the content anyways
        if (!access.contains(CCConstants.PERMISSION_READ_ALL) || !access.contains(CCConstants.PERMISSION_DOWNLOAD_CONTENT))
            return null;
        return (String) nodeProps.get(CCConstants.DOWNLOADURL);
    }

    public NodeVersion getVersion(int major, int minor) throws DAOException {

        String versionLabel = getVersionLabel(major, minor);
        Map<String, Object> versionProps = getNodeHistory().get(versionLabel);

        return convertVersionProps(versionLabel, versionProps);
    }

    private NodeVersion convertVersionProps(String versionLabel, Map<String, Object> versionProps) throws DAOException {
        if (versionProps == null) {
            return null;
        }

        NodeVersion result = new NodeVersion();

        result.setVersion(transformVersion(versionLabel));

        result.setComment((String) versionProps
                .get(CCConstants.CCM_PROP_IO_VERSION_COMMENT));
        result.setContentUrl((String) versionProps.get(CCConstants.CONTENTURL));

        String keyCreated = CCConstants.CM_PROP_C_MODIFIED;
        result.setModifiedAt(versionProps.containsKey(keyCreated) ? ((String) versionProps.get(keyCreated)) : null);
        result.setProperties(convertProperties(filter, versionProps));
        Person ref = new Person();

        ref.setFirstName((String) versionProps
                .get(CCConstants.NODEMODIFIER_FIRSTNAME));
        ref.setLastName((String) versionProps
                .get(CCConstants.NODEMODIFIER_LASTNAME));
        ref.setMailbox((String) versionProps.get(CCConstants.NODEMODIFIER_EMAIL));

        result.setModifiedBy(ref);

        result.setProperties(getProperties(versionLabel));

        return result;
    }

    private static String getVersionLabel(int major, int minor) {
        return major + "." + minor;
    }

    private NodeVersionRef transformVersion(String versionLabel) {

        try {
            String[] versionTokens = versionLabel.split("\\.");

            NodeVersionRef version = new NodeVersionRef();
            version.setNode(getRef());
            version.setMajor(Integer.parseInt(versionTokens[0]));
            version.setMinor(Integer.parseInt(versionTokens[1]));
            version.setMinor(Integer.parseInt(versionTokens[1]));
            return version;
        } catch (Throwable e) {
            logger.warn("Could not parse version for node " + nodeId, e);
            NodeVersionRef version = new NodeVersionRef();
            version.setNode(getRef());
            return version;
        }
    }

    public Map<String, Object> getNativeProperties() {
        return nodeProps;
    }

    public void setNativeProperties(Map<String, Object> nodeProps) {
        this.nodeProps = nodeProps;
    }

    public Map<String, String[]> getAllProperties() throws DAOException {
        return getProperties(null, Filter.createShowAllFilter());
    }

    private Map<String, String[]> getProperties() throws DAOException {
        return getProperties(null);
    }

    private Map<String, String[]> getProperties(String versionLabel) throws DAOException {
        return getProperties(versionLabel, filter);
    }

    public Map<String, Object> getNativeProperties(String versionLabel) throws DAOException {
        return versionLabel != null ? getNodeHistory()
                .get(versionLabel) : nodeProps;
    }

    public void addWorkflowHistory(WorkflowHistory history, boolean sendMail) throws DAOException {
        Map<String, Object> properties = getNativeProperties();
        String nodeType = nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
        List<String> aspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId));

        Object protocol = properties.get(CCConstants.CCM_PROP_WF_PROTOCOL);
        List<String> data = (List<String>) NodeServiceHelper.getPropertyNative(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),
                CCConstants.CCM_PROP_WF_PROTOCOL
        );
        ArrayList<String> list = new ArrayList<>();
        if (data != null) {
            list = new ArrayList<>(data);
        }
        try {
            ArrayList<String> receivers = Arrays.stream(history.getReceiver()).map(Authority::getAuthorityName).collect(Collectors.toCollection(ArrayList::new));
            JSONObject json = new JSONObject();
            json.put("comment", history.getComment());
            json.put("editor", (history.getEditor() != null && !history.getEditor().getAuthorityName().isEmpty()) ? history.getEditor().getAuthorityName() : AuthenticationUtil.getFullyAuthenticatedUser());
            json.put("receiver", receivers);
            json.put("status", history.getStatus());
            json.put("time", history.getTime() > 0 ? history.getTime() : System.currentTimeMillis());
            list.add(0, json.toString());
            org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                    nodeId);
            NodeServiceHelper.setProperty(nodeRef, CCConstants.CCM_PROP_WF_INSTRUCTIONS, history.getComment(), false);
            NodeServiceHelper.setProperty(nodeRef, CCConstants.CCM_PROP_WF_RECEIVER, receivers, false);
            NodeServiceHelper.setProperty(nodeRef, CCConstants.CCM_PROP_WF_STATUS, history.getStatus(), false);
            NodeServiceHelper.setProperty(nodeRef, CCConstants.CCM_PROP_WF_PROTOCOL, list, false);
            if (sendMail) {
                NotificationService localService = NotificationServiceFactoryUtility.getLocalService();
                receivers.forEach((receiver) -> localService
                        .notifyWorkflowChanged(nodeId, nodeType, aspects, properties, receiver, history.getComment(), history.getStatus()));
            }
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public List<WorkflowHistory> getWorkflowHistory() throws DAOException {
        List<WorkflowHistory> workflow = new ArrayList<>();
        List<String> data = (List<String>) NodeServiceHelper.getPropertyNative(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),
                CCConstants.CCM_PROP_WF_PROTOCOL
        );
        if (data == null)
            return workflow;
        try {
            for (String entry : data) {
                JSONObject json = new JSONObject(entry);
                WorkflowHistory history = new WorkflowHistory();
                if (json.has("comment"))
                    history.setComment(json.getString("comment"));
                try {
                    history.setEditor(new PersonDao(repoDao, json.getString("editor")).asPersonSimple(false));
                } catch (Throwable t) {
                    // The user may has no permission or entry deleted
                    history.setEditor(new UserSimple());
                    history.getEditor().setAuthorityName(json.getString("editor"));
                }
                JSONArray arr = json.getJSONArray("receiver");
                Authority[] list = new Authority[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        String authority = arr.getString(i);
                        if (authority.startsWith(PermissionService.GROUP_PREFIX)) {
                            list[i] = new GroupDao(repoDao, authority).asGroup(false);
                        } else {
                            list[i] = new PersonDao(repoDao, authority).asPersonSimple(false);
                        }
                    } catch (Throwable t) {
                        // The user may has no permission or entry deleted
                        list[i] = new UserSimple();
                        list[i].setAuthorityName(arr.getString(i));
                    }
                }
                history.setReceiver(list);
                history.setStatus(json.getString("status"));
                Object time = json.get("time");
                if (time instanceof String) {
                    history.setTime(Long.parseLong((String) time));
                } else if (time instanceof Long) {
                    history.setTime((Long) time);
                } else {
                    logger.info("Can not cast time: " + time + ":" + json.toString());
                }

                workflow.add(history);
            }
            return workflow;

        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    private Map<String, String[]> getProperties(String versionLabel, Filter filter) throws DAOException {
        Map<String, Object> props = getNativeProperties(versionLabel);
        return convertProperties(filter, props);
    }

    private Map<String, String[]> convertProperties(Filter filter, Map<String, Object> props) {
        if (props == null) {
            return null;
        }

        if (filter.getProperties().size() == 0) {
            return new HashMap<>();
        }

        Map<String, String[]> properties = new HashMap<>();
        if (LightbendConfigCache.getBoolean("repository.privacy.filterVCardEmail")) {
            List<String> cleanup = new ArrayList<>();
            for (Entry<String, Object> entry : props.entrySet()) {
                if (CCConstants.getLifecycleContributerPropsMap().containsValue(entry.getKey()) || CCConstants.getMetadataContributerPropsMap().containsValue(entry.getKey())) {
                    if (access == null || !access.contains(CCConstants.PERMISSION_WRITE)) {
                        entry.setValue(VCardConverter.removeEMails(StringUtils.join(ValueTool.getMultivalue(entry.getValue().toString()), "\n")));
                        cleanup.add(entry.getKey() + CCConstants.VCARD_EMAIL);
                    }
                }
            }
            cleanup.forEach(props::remove);
        }
        for (Entry<String, Object> entry : props.entrySet()) {

            if (entry.getKey() == null) {
                logger.info(nodeId + " null property has value " + entry.getValue());
                continue;
            }
            String shortPropName = NameSpaceTool.transformToShortQName(entry.getKey());

            if (shortPropName != null) {

                if (filter.getProperties().size() > 0 &&
                        !filter.getProperties().contains(Filter.ALL)
                        && !filter.getProperties().contains(shortPropName)) {
                    continue;
                }
                List<String> values = getPropertyValues(entry.getValue());

                if (props.containsKey(entry.getKey() + CCConstants.LONG_DATE_SUFFIX)) {
                    values = getPropertyValues(props.get(entry.getKey() + CCConstants.LONG_DATE_SUFFIX));
                    properties.put(shortPropName, values.toArray(new String[values.size()]));
                } else {
                    properties.put(shortPropName, values.toArray(new String[values.size()]));
                }
            }

        }

        return properties;
    }

    private List<String> getPropertyValues(Object value) {
        List<String> values = new ArrayList<>();
        if (value != null) {
            if (value instanceof Date) {
                values.add(String.valueOf(((Date) value).getTime()));
                return values;
            }
            for (String mv : ValueTool.getMultivalue(value.toString())) {
                values.add(mv);
            }
        }
        return values;
    }

    public String getJWT() throws GeneralSecurityException {
        org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getLocalService();

//        ToolPermissionService toolPermissionService = ToolPermissionServiceFactory.getInstance();
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        List<String> nodePermissions = null;
        try {
            nodePermissions = permissionService.getPermissionsForAuthority(nodeId, user);
        } catch (InsufficientPermissionException e) {
            throw new RuntimeException(e);
        }
//        java.util.Collection<String> toolPermissions = toolPermissionService.getAllToolPermissions(false);

        String replicationSource = Arrays.stream(getProperties()
                .getOrDefault(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE), new String[0]))
                .findFirst()
                .orElse(null);

        String resourceType = Arrays.stream(getProperties().getOrDefault(CCConstants.getValidLocalName(CCConstants.CCM_PROP_CCRESSOURCETYPE), new String[0]))
                .findFirst()
                .orElse(null);

        return JwtTokenUtil.generateToken(user, nodeId, nodePermissions, getMimetype(), getMediatype(), replicationSource, resourceType);
    }

    private String getMimetype() {
        return MimeTypesV2.getMimeType(nodeProps, type);
    }

    public String getMediatype() {
        return MimeTypesV2.getNodeType(type, nodeProps, aspects);
    }

    private String getSize(Node data) {
        if (data instanceof CollectionReference && ((CollectionReference) data).getOriginalId() != null && !isFromRemoteRepository()) {
            return AuthenticationUtil.runAsSystem(() ->
                    nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
                            ((CollectionReference) data).getOriginalId(), CCConstants.LOM_PROP_TECHNICAL_SIZE));
        }
        return nodeProps.containsKey(CCConstants.LOM_PROP_TECHNICAL_SIZE) ? (String) nodeProps
                .get(CCConstants.LOM_PROP_TECHNICAL_SIZE) : null;
    }

    private String getRepositoryType() {
        return repoDao.getApplicationInfo().getRepositoryType();
    }

    private Preview getPreview() {
        if (previewData != null) {
            return new Preview(getStoreProtocol(),
                    getStoreIdentifier(),
                    remoteId != null ? remoteId : getRef().getId(),
                    previewData
            );
        }
        Preview result = new Preview(nodeService,
                getStoreProtocol(),
                getStoreIdentifier(),
                remoteId != null ? remoteId : getRef().getId(),
                this.version,
                type,
                nodeProps);

        return result;
    }

    private String getPreviewImage() {
        return new MimeTypesV2(repoDao.getApplicationInfo()).getPreview(type, nodeProps, aspects);
    }

    public Map<String, Map<String, Object>> getNodeHistory() throws DAOException {
        try {
            if (nodeHistory == null) {
                this.nodeHistory = new HashMap<>();

                Map<String, Map<String, Object>> versionHistory = nodeService
                        .getVersionHistory(nodeId);

                if (versionHistory != null) {

                    for (Map<String, Object> version : versionHistory.values()) {
                        nodeHistory.put((String) version
                                .get(CCConstants.CM_PROP_VERSIONABLELABEL), version);
                    }
                }
            }
            return nodeHistory;
        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public List<NotifyEntry> getNotifys() throws DAOException {
        try {
            throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
            List<Notify> notifys = permissionService.getNotifyList(nodeId);
            List<NotifyEntry> result = new ArrayList<>(notifys.size());
            for (Notify notify : notifys) {
                result.add(new NotifyEntry(notify));
            }
            return result;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    private void throwIfPermissionIsMissing(String permission) throws DAOSecurityException {
        if (!permissionService.hasPermission(storeProtocol, storeId, nodeId, permission)) {
            throw new DAOSecurityException(new SecurityException("Current user has no " + permission + " on node " + nodeId));
        }
    }

    public static SearchResult<NodeDao> getFilesSharedByMe(RepositoryDao repoDao, List<String> filter, Filter propertyFilter, SortDefinition sortDefinition, Integer skipCount, Integer maxItems) throws DAOException {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
            SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
            try {
                SearchResultNodeRef result = searchService.getFilesSharedByMe(sortDefinition, mapFilterToContentType(filter), skipCount.intValue(), maxItems == null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems.intValue());
                return NodeDao.convertResultSet(repoDao, propertyFilter, result);
            } catch (Exception e) {
                throw DAOException.mapping(e);
            }
        });
    }

    private static SearchService.ContentType mapFilterToContentType(List<String> filter) {
        if (filter == null || filter.size() == 0)
            return SearchService.ContentType.ALL;
        if (filter.contains("files") && filter.contains("folders")) {
            return SearchService.ContentType.FILES_AND_FOLDERS;
        }
        if (filter.contains("files")) {
            return SearchService.ContentType.FILES;
        }
        if (filter.contains("folders")) {
            return SearchService.ContentType.FOLDERS;
        }
        logger.warn("Could not map the given filter value(s) " + filter.get(0) + " to a given ContentType, will use ContentType.ALL");
        return SearchService.ContentType.ALL;
    }

    public static List<org.alfresco.service.cmr.repository.NodeRef> sortAlfrescoRefs(List<org.alfresco.service.cmr.repository.NodeRef> refs, List<String> filter, SortDefinition sortDefinition) {
        return NodeServiceFactory.getLocalService().sortNodeRefList(refs, filter, sortDefinition);
    }

    /**
     * All files the current user is a receiver of the workflow
     *
     * @param repoDao
     * @param skipCount
     * @param maxItems
     * @return
     * @throws DAOException
     */
    public static SearchResult<NodeDao> getWorkflowReceive(RepositoryDao repoDao, List<String> filter, SortDefinition sortDefinition, Integer skipCount, Integer maxItems) throws DAOException {
        SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
        try {
            SearchResultNodeRef result = searchService.getWorkflowReceive(
                    AuthenticationUtil.getFullyAuthenticatedUser(),
                    sortDefinition, mapFilterToContentType(filter),
                    skipCount.intValue(),
                    maxItems == null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems.intValue()
            );
            return NodeDao.convertResultSet(repoDao, Filter.createShowAllFilter(), result);

        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    public static SearchResult<NodeDao> getFilesSharedToMe(RepositoryDao repoDao, SharedToMeType shareType, List<String> filter, Filter propertyFilter, SortDefinition sortDefinition, Integer skipCount, Integer maxItems) throws DAOException {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
            SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
            try {
                SearchResultNodeRef result = searchService.getFilesSharedToMe(shareType, sortDefinition, mapFilterToContentType(filter), skipCount.intValue(), maxItems == null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems.intValue());
                return NodeDao.convertResultSet(repoDao, propertyFilter, result);
            } catch (Exception e) {
                throw DAOException.mapping(e);
            }
        });
    }

    private static SearchResult<NodeDao> convertResultSet(RepositoryDao repoDao, Filter propFilter, SearchResultNodeRef result) {
        SearchResult<NodeDao> converted = new SearchResult<>();
        Pagination pagination = new Pagination();
        pagination.setFrom(result.getStartIDX());
        pagination.setTotal(result.getNodeCount());
        pagination.setCount(result.getData().size());
        converted.setPagination(pagination);
        converted.setNodes(result.getData().stream().map((ref) -> {
            try {
                return new NodeDao(repoDao, ref, propFilter);
            } catch (DAOException e) {
                logger.warn(e.getMessage(), e);
            }
            return null;
        }).collect(Collectors.toList()));
        return converted;
    }

    public List<NodeShare> getShares(String email) throws DAOSecurityException {
        throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
        ShareServiceImpl service = new ShareServiceImpl(PermissionServiceFactory.getPermissionService(repoDao.getId()));
        List<NodeShare> entries = new ArrayList<>();
        for (Share share : service.getShares(this.nodeId)) {
            if (email == null || email.equals(share.getEmail()))
                entries.add(new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef, this.nodeId), share));
        }
        return entries;
    }

    public NodeShare createShare(long expiryDate, String password) throws DAOException {
        ShareServiceImpl service = new ShareServiceImpl(PermissionServiceFactory.getPermissionService(repoDao.getId()));
        try {
            throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
            return new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef, this.nodeId), service.createShare(nodeId, expiryDate, password));
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    public void removeShare(String shareId) throws DAOException {
        throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
        ShareServiceImpl service = new ShareServiceImpl(PermissionServiceFactory.getPermissionService(repoDao.getId()));
        for (Share share : service.getShares(this.nodeId)) {
            if (share.getNodeId().equals(shareId)) {
                service.removeShare(this.nodeId, shareId);
                return;
            }
        }
        throw DAOException.mapping(new Exception("share " + shareId + " was not found on node " + nodeId));
    }

    public NodeShare updateShare(String shareId, long expiryDate, String password) throws DAOException {
        throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
        ShareServiceImpl service = new ShareServiceImpl(PermissionServiceFactory.getPermissionService(repoDao.getId()));
        for (Share share : service.getShares(this.nodeId)) {
            if (share.getNodeId().equals(shareId)) {
                share.setExpiryDate(expiryDate);
                share.setPassword(password);
                service.updateShare(share);
                return new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef, this.nodeId), share);
            }
        }
        throw DAOException.mapping(new Exception("share " + shareId + " was not found on node " + nodeId));
    }

    public NodeDao importNode(String parent) throws DAOException {
        try {
            String result = nodeService.importNode(this.nodeId, parent);
            return new NodeDao(RepositoryDao.getRepository(RepositoryDao.HOME), result);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public void createVersion(String comment) throws DAOException, Exception {
        this.changePropertiesWithVersioning(getAllProperties(), comment);
    }

    public static List<NodeRef> convertAlfrescoNodeRef(java.util.Collection<org.alfresco.service.cmr.repository.NodeRef> refs) {
        return refs.stream().map((ref) -> {
            try {
                return new NodeRef(RepositoryDao.getHomeRepository().getId(), ref.getId());
            } catch (DAOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public static List<org.alfresco.service.cmr.repository.NodeRef> convertApiNodeRef(List<NodeRef> refs) {
        List<org.alfresco.service.cmr.repository.NodeRef> converted = new ArrayList<>(refs.size());
        for (NodeRef ref : refs) {
            converted.add(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ref.getId()));
        }
        return converted;
    }

    public static List<NodeRef> convertAlfrescoNodeRef(RepositoryDao repoDao, List<org.alfresco.service.cmr.repository.NodeRef> refs) {
        List<NodeRef> converted = new ArrayList<>(refs.size());
        for (org.alfresco.service.cmr.repository.NodeRef ref : refs) {
            converted.add(new NodeRef(repoDao.getId(), ref.getId()));
        }
        return converted;
    }

    public static List<NodeRef> convertEduNodeRef(RepositoryDao repoDao, List<org.edu_sharing.service.model.NodeRef> refs) {
        return refs.stream().map(ref -> new NodeRef(repoDao, ref.getNodeId())).collect(Collectors.toList());
    }

    public void reportNode(NotificationService.NotifyMode mode, String reason, String userEmail, String userComment) throws DAOException {
        try {
            String type = nodeService.getType(nodeId);
            Map<String, Object> properties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
            List<String> aspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId));
            NotificationServiceFactoryUtility.getLocalService()
                    .notifyNodeIssue(nodeId, mode, reason, type, aspects, properties, userEmail, userComment);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }

    }

    /**
     * store a new search node
     *
     * @return
     */
    public static NodeDao saveSearch(String repoId, String mdsId, String query, String name,
                                     List<MdsQueryCriteria> parameters, boolean replace) throws DAOException {
        try {
            String parent = RepositoryDao.getHomeRepository().getUserSavedSearch(true);
            NodeDao parentDao = new NodeDao(RepositoryDao.getHomeRepository(), parent);
            Map<String, String[]> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, new String[]{NodeServiceHelper.cleanupCmName(name)});
            props.put(CCConstants.LOM_PROP_GENERAL_TITLE, new String[]{name});
            props.put(CCConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY, new String[]{repoId});
            props.put(CCConstants.CCM_PROP_SAVED_SEARCH_MDS, new String[]{mdsId});
            props.put(CCConstants.CCM_PROP_SAVED_SEARCH_QUERY, new String[]{query});
            props.put(CCConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS, new String[]{Json.pretty(parameters)});
            props.put(CCConstants.CCM_PROP_IO_CREATE_VERSION, new String[]{"true"});
            try {
                return parentDao.createChild(CCConstants.CCM_TYPE_SAVED_SEARCH, null, props, false);
            } catch (DAOException e) {
                if (e.getCause() instanceof DuplicateChildNodeNameException && replace) {
                    NodeDao old = NodeDao.getByParent(RepositoryDao.getHomeRepository(), parent, CCConstants.CCM_TYPE_SAVED_SEARCH, NodeServiceHelper.cleanupCmName(name));
                    old.changeProperties(props);
                    return old;
                }
                throw e;
            }
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }

    }

    public static NodeRemote prepareUsage(String repId, String nodeId) throws DAOException, Throwable {

        String tmpNodeId = new RemoteObjectService().getRemoteObject(repId, nodeId);

        NodeRemote nodeRemote = new NodeRemote();
        nodeRemote.setNode(new NodeDao(RepositoryDao.getRepository(repId), nodeId, Filter.createShowAllFilter()).asNode());
        if (!tmpNodeId.equals(nodeId)) {
            nodeRemote.setRemote(new NodeDao(RepositoryDao.getRepository(RepositoryDao.HOME), tmpNodeId, Filter.createShowAllFilter()).asNode());
        }

        return nodeRemote;

    }

    public void addAspects(List<String> aspects) {
        for (String aspect : aspects) {
            nodeService.addAspect(nodeId, CCConstants.getValidGlobalName(aspect));
        }
    }

    public boolean getTemplateStatus() throws DAOException {
        Object value = this.getNativeProperties().getOrDefault(CCConstants.CCM_PROP_METADATA_PRESETTING_STATUS, false);
        if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }
        return (boolean) value;
    }

    public NodeDao getTemplateNode() throws DAOException {
        try {
            String template = nodeService.getTemplateNode(nodeId, false);
            if (template == null)
                return null;
            return NodeDao.getNode(repoDao, template, Filter.createShowAllFilter());
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public NodeDao changeTemplateProperties(Boolean enable, Map<String, String[]> properties) throws DAOException {
        try {
            nodeService.setTemplateStatus(nodeId, enable);
            if (enable) {
                nodeService.setTemplateProperties(nodeId, transformProperties(properties));
            }
            return getTemplateNode();
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public void setOwner(String username) {
        nodeService.setOwner(this.getId(), username);
    }

    public static void setProperty(RepositoryDao repoDao, String nodeId, String property, Serializable value, boolean keepModifiedDate) {
        NodeService nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
        if (keepModifiedDate) {
            nodeService.keepModifiedDate(
                    StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,
                    () -> setPropertyInternal(nodeService, nodeId, property, value)
            );
        } else {
            setPropertyInternal(nodeService, nodeId, property, value);
        }
    }

    private static void setPropertyInternal(NodeService nodeService, String nodeId, String property, Serializable value) {
        if (value == null) {
            nodeService.removeProperty(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, property);
        } else {
            nodeService.setProperty(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, property, value, false);
        }
    }

    public NodeDao createFork(String sourceId) throws DAOException {
        try {
            NodeDao sourceDao = NodeDao.getNode(repoDao, sourceId);
            String[] source = new String[]{sourceId};
            RunAsWork<NodeDao> work = () -> {
                try {
                    org.alfresco.service.cmr.repository.NodeRef newNode = nodeService.copyNode(source[0], nodeId, false);
                    permissionService.createNotifyObject(newNode.getId(), AuthenticationUtil.getFullyAuthenticatedUser(), CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD);
                    nodeService.addAspect(newNode.getId(), CCConstants.CCM_ASPECT_FORKED);
                    nodeService.setProperty(newNode.getStoreRef().getProtocol(), newNode.getStoreRef().getIdentifier(), newNode.getId(), CCConstants.CCM_PROP_FORKED_ORIGIN,
                            new org.alfresco.service.cmr.repository.NodeRef(storeProtocol, storeId, source[0]), false);
                    nodeService.setProperty(newNode.getStoreRef().getProtocol(), newNode.getStoreRef().getIdentifier(), newNode.getId(), CCConstants.CCM_PROP_FORKED_ORIGIN_VERSION,
                            nodeService.getProperty(storeProtocol, storeId, source[0], CCConstants.LOM_PROP_LIFECYCLE_VERSION), false);
                    AuthenticationUtil.runAsSystem(() -> {
                        permissionService.removeAllPermissions(newNode.getId());
                        // re-activate inherition
                        permissionService.setPermissions(newNode.getId(), null, true);
                        return null;
                    });
                    return new NodeDao(repoDao, newNode.getId());
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            };
            if (sourceDao.isCollectionReference()) {
                // validate ccm:restricted_access
                NodeServiceHelper.validatePermissionRestrictedAccess(
                        new org.alfresco.service.cmr.repository.NodeRef(new StoreRef(storeProtocol, storeId), sourceId),
                        CCConstants.PERMISSION_READ_ALL);
                source[0] = sourceDao.getReferenceOriginalId();
                return AuthenticationUtil.runAsSystem(work);
            } else {
                return work.doWork();
            }

        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public String storeXApiData(String xApi) throws DAOException {
        try {
            JSONObject json = new JSONObject(xApi);
            return XApiTool.sendToXApi(nodeId, json);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public static SearchResult<NodeDao> getFrontpageNodes(RepositoryDao repoDao) throws DAOException {
        try {
            SearchResult<NodeDao> sr = new SearchResult<>();
            sr.setNodes(NodeServiceFactory.getNodeService(repoDao.getId()).
                    getFrontpageNodes().stream().map((ref) -> {
                        try {
                            NodeDao dao = NodeDao.getNode(repoDao, ref);
                            // not required for the frontpage
                            dao.resolveUsernames = false;
                            return dao;
                        } catch (DAOException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList()));
            Pagination p = new Pagination();
            p.setFrom(0);
            p.setCount(sr.getNodes().size());
            p.setTotal(sr.getNodes().size());
            sr.setPagination(p);
            return sr;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    /**
     * If the NodeDao is a child, then all properties including inherited are returned
     * otherwise, the own properties are returned
     */
    public Map<String, Object> getInheritedPropertiesFromParent() throws Throwable {
        if (getAspectsNative().contains(CCConstants.CCM_ASPECT_IO_CHILDOBJECT)) {
            Map<String, Object> propsChild = getNativeProperties();
            String parentRef = NodeServiceFactory.getLocalService().getPrimaryParent(getRef().getId());
            Map<String, Object> propsParent =
                    NodeServiceHelper.getProperties(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentRef));
            // ignore some technical properties, like mimetypes etc. (configured via lightbend)
            for (String prop : LightbendConfigLoader.get().getStringList("repository.childobjects.ignoredInheritMetadata")) {
                propsParent.remove(prop);
            }
            // override it with the props from the child
            for (Map.Entry<String, Object> entry : propsChild.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                    propsParent.put(entry.getKey(), entry.getValue());
                }
            }
            return propsParent;
        } else {
            return getNativeProperties();
        }
    }

    public NodeDao publishCopy(HandleParam handleParam) throws DAOException {
        try {
            return NodeDao.getNode(repoDao, nodeService.publishCopy(nodeId, handleParam), Filter.createShowAllFilter());
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }

    }

    public List<NodeDao> getPublishedCopies() throws DAOException {
        try {
            return nodeService.getPublishedCopies(nodeId).stream().map(
                    (id) -> {
                        try {
                            return NodeDao.getNode(repoDao, id, Filter.createShowAllFilter());
                        } catch (DAOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            ).sorted((a, b) -> {
                try {
                    String[] va = ((String) a.getNativeProperties().get(CCConstants.LOM_PROP_LIFECYCLE_VERSION)).split("\\.");
                    String[] vb = ((String) b.getNativeProperties().get(CCConstants.LOM_PROP_LIFECYCLE_VERSION)).split("\\.");
                    for (int i = 0; i < va.length; i++) {
                        int c = Integer.compare(Integer.parseInt(va[i]), Integer.parseInt(vb[i]));
                        if (c != 0) {
                            return c;
                        }
                    }
                    return 0;
                } catch (Throwable ignored) {
                    return 0;
                }
            }).collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw DAOException.mapping(e.getCause());
        }
    }

    public static void setIsGlobalAdmin(Boolean isGlobalAdmin) {
        NodeDao.isGlobalAdmin.set(isGlobalAdmin);
    }

    /**
     * simply transfer an alfresco node to an rest-api compatible node object
     * This is a pretty simple call which is only supposed to be used for performance critical calls and will skip things like
     * - Current Access permission list
     * - Previews
     * - Content info
     * and ONLY include:
     * - aspects
     * - properties
     * Only use it if you don't need any advanced data or mappings
     */
    public static Node getAsNodeSimple(org.alfresco.service.cmr.repository.NodeRef nodeRef) throws DAOException {
        try {
            NodeDao dao = new NodeDao(nodeRef);
            Node node = new Node();
            dao.fillNodeObject(node, false, false);
            return node;
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }
}
