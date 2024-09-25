package org.edu_sharing.service.collection;

import java.io.InputStream;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.NotImplementedException;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.search.model.SortDefinition;

public interface CollectionService {


    public Collection create(String collectionId, Collection collection) throws Throwable;
	
	public void update(Collection collection);
	
	public void remove(String collectionId);


	/**
	 * Get the pending proposals for a given collection
	 * @param parentId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws Exception
	 */
    List<AssociationRef> getChildrenProposal(String parentId) throws InsufficientPermissionException, Exception;

    void proposeForCollection(String collectionId, String originalNodeId, String sourceRepositoryId)
            throws DuplicateNodeException, Throwable;

    /**
	 * /**
	 * creates an refObject
	 * adds Usage to Original IO (lms=repo,course=sammlungsid,resourceId=refIoId, nodeId=originalIOId
	 * @param repoDao
	 *
	 *
     * @param collectionId
     * @param allowDuplicate
     * @param originalNodeId
     * @return nodeId of the refObject
	 * @throws DuplicateNodeException 
	 * @throws Throwable 
	 */
	public String addToCollection(String collectionId, String originalNodeId, String sourceRepositoryId, boolean allowDuplicate) throws DuplicateNodeException, Throwable;
	
	/**
	 * removes refobject or collection
	 * removesUsage
	 * 
	 * @param collectionId
	 * @param nodeId
	 */
	public void removeFromCollection(String collectionId, String nodeId);
	
	public void move(String toCollection, String nodeId);
	

	public List<org.edu_sharing.service.model.NodeRef> getChildren(String parentId, String scope, SortDefinition sortDefinition, List<String> filter);

	List<org.edu_sharing.service.model.NodeRef> getRecentForCurrentUser() throws Throwable;

	public SearchResultNodeRef getRoot(String scope, SortDefinition sortDefinition, int skipCount, int maxItems) throws Throwable;

	public Collection get(org.edu_sharing.service.model.NodeRef collection, boolean fetchCounts, boolean resolveUsernames, BoolQuery readPermissionsQuery);

	void removePreviewImage(String collectionId) throws Exception;

	/**
	 * 
	 * @param parentId collection to set the order of the children
	 * @param nodes Children in the order. Use null to reset the order
	 */
	public void setOrder(String parentId, String[] nodes);

	void updateAndSetScope(Collection collection) throws Exception;

    String getCollectionHomeParent();
    String getHomePath();

    Collection createAndSetScope(String parentId, Collection collection) throws Throwable;

    void updateScope(NodeRef ref, List<ACE> permissions);

    public void setPinned(String[] collections);

	void writePreviewImage(String collectionId, InputStream is, String mimeType) throws Exception;

    List<org.edu_sharing.service.model.NodeRef> getReferenceObjects(String nodeId);

	List<NodeRef> getReferenceObjectsSync(String nodeId);

	default CollectionProposalInfo getCollectionsContainingProposals(CCConstants.PROPOSAL_STATUS status, Integer skipCount, Integer maxItems, SortDefinition sortDefinition) throws Throwable {
		throw new NotImplementedException("collections proposal feature is not supported without elasticsearch");
	}

	/**
	 * fetch the collections which have this node as a pending proposal
	 * Will only provide collections with appropriate permissions
	 * @param nodeId The node id to check for
	 * @param status The status of the proposals to find
	 */
	List<NodeRef> getCollectionProposals(String nodeId, CCConstants.PROPOSAL_STATUS status);
}
