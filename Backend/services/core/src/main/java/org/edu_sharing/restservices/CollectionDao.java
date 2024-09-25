package org.edu_sharing.restservices;

import java.io.InputStream;
import java.util.*;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.collection.v1.model.*;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.node.v1.model.AbstractEntries;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.collection.CollectionProposalInfo;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

public class CollectionDao {

	public static Logger logger = Logger.getLogger(CollectionDao.class);
	public static final String ROOT = "-root-";

    public enum Scope {
		EDU_ALL,
		EDU_GROUPS,
		MY,
		CUSTOM,
		CUSTOM_PUBLIC
	}
	public enum SearchScope {
		EDU_ALL,
		EDU_GROUPS,
		TYPE_EDITORIAL,
		TYPE_MEDIA_CENTER,
		MY,
		RECENT
	}
	
	private final static String[] PERMISSIONS = new String[] {
			PermissionService.WRITE, PermissionService.DELETE };

	private static final int MAX = 0;


	private final CollectionService collectionClient;

	private final RepositoryDao repoDao;
	private final String collectionId;
	
	private final Collection collection;
	
	private final MCAlfrescoBaseClient baseClient;

	private List<String> access;

	private Preview preview;

	private NodeDao nodeDao;

	public static CollectionDao getCollection(RepositoryDao repoDao, String collectionId)
			throws DAOException {

		try {

			return new CollectionDao(repoDao, collectionId);

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	public static CollectionBaseEntries getCollectionsReferences(RepositoryDao repoDao, String parentId, Filter filter, SortDefinition sortDefinition, int skipCount, int maxItems)	throws DAOException {
		try {
			if(parentId==null || parentId.equals(ROOT)){
				throw new IllegalArgumentException("Invalid parameter for parentId");
			}

			return getCollectionsChildren(repoDao, parentId, null, false, filter, Arrays.asList(new String[]{"files"}), sortDefinition, skipCount, maxItems);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}

	public static CollectionProposalEntries getCollectionsContainingProposals(RepositoryDao repoDao, CCConstants.PROPOSAL_STATUS status, Boolean fetchCounts, Integer skipCount, Integer maxItems, SortDefinition sortDefinition) throws DAOException {
		try {
			List<NodeCollectionProposalCount> result = new ArrayList<>();

			CollectionProposalInfo children =
					repoDao.getCollectionClient().getCollectionsContainingProposals(
							status,
							skipCount, maxItems,
							sortDefinition
					);
			Pagination pagination = new Pagination();
			pagination.setTotal((int) children.getTotalHits());
			pagination.setFrom(skipCount);
			pagination.setCount(children.getData().size());

			for(CollectionProposalInfo.CollectionProposalData data : children.getData()) {
				NodeDao dao = NodeDao.getNode(repoDao, data.getNodeRef());
				NodeCollectionProposalCount node = new NodeCollectionProposalCount();
				dao.fetchCounts = fetchCounts;
				dao.fillNodeObject(node, true, true);
				node.setProposalCount(data.getProposalCount());
				result.add(node);
			}

			CollectionProposalEntries obj = new CollectionProposalEntries();
			obj.setCollections(result);
			obj.setPagination(pagination);
			return obj;
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public static CollectionBaseEntries getCollectionsSubcollections(RepositoryDao repoDao, String parentId, SearchScope scope, boolean fetchCounts, Filter filter, SortDefinition sortDefinition, int skipCount, int maxItems)	throws DAOException {
		try {
			return getCollectionsChildren(repoDao, parentId, scope, fetchCounts, filter, Arrays.asList(new String[]{"folders"}), sortDefinition, skipCount, maxItems);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	private static CollectionBaseEntries getCollectionsChildren(RepositoryDao repoDao, String parentId, SearchScope scope, boolean fetchCounts, Filter propFilter, List<String> filter, SortDefinition sortDefinition, int skipCount, int maxItems) throws DAOException {
		try {
			NodeDao parentNode;
			if(!ROOT.equals(parentId)) {
				parentNode = NodeDao.getNode(repoDao, parentId);
				parentNode.fetchCounts = false;
			}
			if(!ROOT.equals(parentId) || SearchScope.RECENT.equals(scope)) {
					List<Node> result = new ArrayList<>();
					List<org.edu_sharing.service.model.NodeRef> children;
					if(SearchScope.RECENT.equals(scope)) {
						children = repoDao.getCollectionClient().getRecentForCurrentUser();
					} else {
						children =
								repoDao.getCollectionClient().getChildren(
										ROOT.equals(parentId) ? null : parentId,
										scope != null ? scope.toString() : null, sortDefinition, filter);
					}

					//NodeDao.convertAlfrescoNodeRef(repoDao,children)
					NodeEntries sorted = NodeDao.convertToRest(repoDao,
							Filter.createShowAllFilter(),
							NodeDao.convertEduNodeRef(repoDao, children),
							skipCount,
							maxItems,
							(dao) -> {
								dao.fetchCounts = fetchCounts;
								return dao;
							});
					Pagination pagination = sorted.getPagination();
					for (Node child : sorted.getNodes()) {

						String nodeType = child.getType();

						if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP).equals(nodeType)) {

							// it's a collection
							result.add(child);

						} else if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO).equals(nodeType)) {

							// it's a reference
							try {
								result.add((CollectionReference) child);
							} catch (ClassCastException e) {
								logger.error("Collection " + parentId + " contains a non-ref object: " + child.getRef().getId() + ". Please clean up the collection", e);
							}
						}
					}
					CollectionBaseEntries obj = new CollectionBaseEntries();
					obj.setEntries(result);
					obj.setPagination(pagination);
					return obj;

			} else {
				SearchResultNodeRef searchResult = repoDao.getCollectionClient().getRoot(
						scope != null ? scope.toString() : null, sortDefinition, skipCount, maxItems
				);
				BoolQuery readPermissionsQuery = null;
				SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getId());
				if(searchService instanceof SearchServiceElastic) {
					// improve performance by caching the relatively expensive query
					readPermissionsQuery = ((SearchServiceElastic)searchService).getReadPermissionsQuery(new BoolQuery.Builder()).build();
				}
				NodeSearch transformed = NodeDao.transform(repoDao, searchResult, propFilter, null, readPermissionsQuery);
				CollectionBaseEntries obj = new CollectionBaseEntries();
				obj.setEntries(transformed.getNodes());
				obj.setPagination(new Pagination(searchResult));
				return obj;
			}
		}catch(Throwable e){
			throw DAOException.mapping(e);
		}
	}

	private String getOrderMode() {
		return this.collection.getOrderMode();
	}

	private CollectionDao(RepositoryDao repoDao, String collectionId) throws DAOException {

		try {
			
			this.collectionClient = repoDao.getCollectionClient();
	
			this.repoDao = repoDao;
			this.collectionId = collectionId;
			this.nodeDao=NodeDao.getNode(repoDao, collectionId);

			this.collection = unmarshalling(repoDao.getId(), collectionClient.get(nodeDao.getNodeRef(), nodeDao.fetchCounts, nodeDao.resolveUsernames, nodeDao.readPermissionsQuery));
			this.baseClient = repoDao.getBaseClient();

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	CollectionDao(RepositoryDao repoDao, String collectionId,NodeDao nodeDao,Node node) throws DAOException {

		try {
			
			this.collectionClient = repoDao.getCollectionClient();
	
			this.repoDao = repoDao;
			this.collectionId = collectionId;
			this.nodeDao=nodeDao;
			this.collection = unmarshalling(repoDao.getId(), collectionClient.get(nodeDao.getNodeRef(), nodeDao.fetchCounts, nodeDao.resolveUsernames, nodeDao.readPermissionsQuery));
			this.baseClient = repoDao.getBaseClient();
			this.access = node.getAccess();//baseClient.hasAllPermissions(collectionId, PERMISSIONS);
			this.preview= node.getPreview();

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	public String getScope() throws DAOException {
		return collection.getScope();
	}

	public static NodeDao createRoot(RepositoryDao repoDao, Node collection) throws DAOException {

		try {

			org.edu_sharing.service.collection.Collection child = 
					repoDao.getCollectionClient().createAndSetScope(null,marshalling(collection));
			return NodeDao.getNode(repoDao, child.getNodeId(), Filter.createShowAllFilter());

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
			
	}

	public NodeDao createChild(Node collection) throws DAOException {

		try {
			org.edu_sharing.service.collection.Collection child = collectionClient.createAndSetScope(collectionId, marshalling(collection));
			return NodeDao.getNode(repoDao, child.getNodeId(),Filter.createShowAllFilter());

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
			
	}

	public void update(Node collection) throws DAOException {

		try {
		
			collectionClient.updateAndSetScope(marshalling(collection));
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
		
	}
	
	public void delete() throws DAOException {

		try {
		
			collectionClient.remove(nodeDao.getRef().getId());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
			
	}


	public static AbstractEntries<NodeProposal> getCollectionsProposals(RepositoryDao repoDao, String parentId, CCConstants.PROPOSAL_STATUS filterStatus) throws DAOException {
		try {
			List<NodeProposal> proposals = new ArrayList<>();

			for(AssociationRef ref : CollectionServiceFactory.getCollectionService(repoDao.getApplicationInfo().getAppId()).
					getChildrenProposal(parentId)) {
				NodeProposal proposal = new NodeProposal();
				String status = NodeServiceHelper.getProperty(ref.getSourceRef(), CCConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS);
				CCConstants.PROPOSAL_STATUS enumStatus = CCConstants.PROPOSAL_STATUS.PENDING;
				if(status != null){
					enumStatus = CCConstants.PROPOSAL_STATUS.valueOf(status);
				}
				if(!Objects.equals(filterStatus, enumStatus)){
					continue;
				}
				try {
					NodeDao original = NodeDao.getNode(repoDao, ref.getTargetRef().getId());
					original.fillNodeObject(proposal, true, true);
					proposal.setAccessible(true);
				} catch(DAOSecurityException e){
					proposal = NodeDao.createEmptyDummy(NodeProposal.class,
							new org.edu_sharing.restservices.shared.NodeRef(repoDao.getId(),ref.getTargetRef().getId())
					);
					proposal.setName(NodeServiceHelper.getProperty(ref.getSourceRef(), CCConstants.CM_NAME));
					proposal.setAccessible(false);
				}
				proposal.setStatus(enumStatus);
				proposal.setProposal(NodeDao.getNode(repoDao, ref.getSourceRef().getId()).asNode());
				proposals.add(proposal);
			}
			AbstractEntries<NodeProposal> entries = new AbstractEntries<>();
			entries.setNodes(proposals);
			entries.setPagination(new Pagination(proposals));
			return entries;
		} catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public static synchronized void proposeForCollection(RepositoryDao repoDao, String collectionId, String nodeId, String sourceRepositoryId) throws DAOException {
		try {

			CollectionServiceFactory.getCollectionService(repoDao.getApplicationInfo().getAppId()).
					proposeForCollection(collectionId,nodeId,sourceRepositoryId);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public static NodeDao addToCollection(RepositoryDao repoDao, String collectionId, String nodeId, String sourceRepositoryId, boolean allowDuplicate) throws DAOException {
		try {

			String resultId=CollectionServiceFactory.getCollectionService(repoDao.getApplicationInfo().getAppId()).
					addToCollection(collectionId,nodeId,sourceRepositoryId,allowDuplicate);
			return NodeDao.getNode(repoDao,resultId,Filter.createShowAllFilter());
		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
	}
	
	public void removeFromCollection(NodeDao node) throws DAOException {
		
		try {
		
			collectionClient.removeFromCollection(nodeDao.getRef().getId(), node.getRef().getId());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
			
	}

	public Collection asCollection() throws DAOException {
		return this.collection;
	}

	private List<String> getAccess() {
		return access;
	}

	
	private static org.edu_sharing.service.collection.Collection marshalling(Node node) {
		
		if (node == null) {
			return null;
		}
		
		org.edu_sharing.service.collection.Collection result = new org.edu_sharing.service.collection.Collection();
		
		result.setColor(node.getCollection().getColor());
		result.setDescription(node.getCollection().getDescription());
		result.setLevel0(node.getCollection().isLevel0());
		
		if (node.getRef() != null) {
			
			result.setNodeId(node.getRef().getId());
		}
		
		result.setTitle(node.getTitle());
		result.setType(node.getCollection().getType());
		result.setViewtype(node.getCollection().getViewtype());
		result.setX(node.getCollection().getX());
		result.setY(node.getCollection().getY());
		result.setZ(node.getCollection().getZ());
		result.setPinned(node.getCollection().isPinned());
		result.setAuthorFreetext(node.getCollection().getAuthorFreetext());
		
		if (node.getRef() != null) {
			result.setNodeId(node.getRef().getId());
		}
		result.setScope(node.getCollection().getScope());
		
		return result;
	}
	
	private static Collection unmarshalling(String repoId, org.edu_sharing.service.collection.Collection collection) {
		
		if (collection == null) {
			return null;
		}
		
		Collection result = new Collection();
		
		result.setColor(collection.getColor());
		result.setDescription(collection.getDescription());
		result.setLevel0(collection.isLevel0());
		result.setFromUser(collection.isFromUser());
		
		result.setChildCollectionsCount(collection.getChildCollectionsCount());
		result.setChildReferencesCount(collection.getChildReferencesCount());
		
		result.setTitle(collection.getTitle());
		result.setType(collection.getType());
		result.setOrderMode(collection.getOrderMode());
		result.setOrderAscending(collection.getOrderAscending());
		result.setViewtype(collection.getViewtype());
		result.setX(collection.getX());
		result.setY(collection.getY());
		result.setZ(collection.getZ());
		result.setPinned(collection.isPinned());
		result.setAuthorFreetext(collection.getAuthorFreetext());

		result.setScope(collection.getScope());

		return result;
	}
	
	public void writePreviewImage(InputStream is, String mimeType) throws DAOException{
		try{
			collectionClient.writePreviewImage(collectionId,is,mimeType);
			//thumbnailService.createThumbnail(ref, QName.createQName(CCConstants.CCM_PROP_MAP_ICON), ,"collection");
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}

	public void removePreviewImage() throws DAOException{
		try{
			collectionClient.removePreviewImage(collectionId);
		}catch(Exception e){
			throw new DAOException(e,collectionId);
		}
	}

	public static void setPinned(RepositoryDao repoDao, String[] collections) {
		if(!ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING))
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING);
		AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				repoDao.getCollectionClient().setPinned(collections);
				return null;
			}
		});
	}

	public void setOrder(String[] nodes) {
		collectionClient.setOrder(collectionId,nodes);
	}

	public Node asNode() throws DAOException {
		return nodeDao.asNode();
	}
}
