package org.edu_sharing.webservices.alfresco.extension;

import java.util.HashMap;
import java.util.List;

import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.GetPreviewResult;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Share;

public class NativeAlfrescoWrapper{
    public java.util.HashMap getProperties(java.lang.String nodeId){
    	return null;
    }
    
    public java.util.HashMap getPropertiesExt(String storeProtocol, String storeId, String nodeId) {
    	return null;
    }
    
    public org.edu_sharing.webservices.alfresco.extension.SearchResult search(org.edu_sharing.webservices.alfresco.extension.SearchCriteria[] searchCriterias, java.lang.String metadatasetId, int start, int nrOfResults, java.lang.String[] facettes) {
    	return null;
    }
    
    public org.edu_sharing.webservices.alfresco.extension.SearchResult searchSolr(String query, int startIdx,int nrOfresults, String[] facettes, int facettesMinCount, int facettesLimit){
    	return null;
    }
    	
    
    
    public java.util.HashMap getChildren(java.lang.String parentID, java.lang.String type){
    	return null;
    }
    
    public Map<String, Map<String, Object>> getChildrenCheckPermissions(String parentID, String[] permissionsOnChild){
    	return null;
    }
    
    public java.lang.String createNode(java.lang.String parentID, java.lang.String nodeTypeString, java.lang.String childAssociation, java.util.HashMap props) {
    	return null;
    }
    
    public java.lang.String createNodeAtomicValues(java.lang.String parentID, java.lang.String nodeTypeString, java.lang.String childAssociation, java.util.HashMap props) {
    	return null;
    }
    
    public void updateNodeAtomicValues(java.lang.String nodeId, java.util.HashMap properties) {
    	
    }
    
    public void removeNode(java.lang.String nodeId, java.lang.String fromId) {
    	
    }
    public boolean isAdmin(java.lang.String username) {
    	return false;
    }
    public java.util.HashMap hasPermissions(java.lang.String userId, java.lang.String[] permissions, java.lang.String nodeId) {
    	return null;
    }
    
    public boolean hasPermissionsSimple(String nodeId, String[] permissions){
		return false;
	}
    
    public void updateNode(java.lang.String nodeId, java.util.HashMap properties) {
    	
    }
    public java.lang.String getCompanyHomeNodeId() {
    	return null;
    }
    public java.util.HashMap getPropertiesSimple(java.lang.String nodeId) {
    	return null;
    }
    public java.lang.String[] searchNodeIds(java.lang.String store, java.lang.String luceneQuery, java.lang.String permission) {
    	return null;
    	}
    public org.edu_sharing.webservices.alfresco.extension.RepositoryNode[] searchNodes(java.lang.String store, java.lang.String luceneQuery, java.lang.String permission, java.lang.String[] propertiesToReturn) {
    	return null;
    }
    public java.lang.String validateTicket(java.lang.String ticket) {
    	return null;
    }
    public org.edu_sharing.webservices.alfresco.extension.RepositoryNode[] getVersionHistory(java.lang.String nodeId) {
    	return null;
    }
    
    public String getType(String nodeId){
    	return null;
    }
    
    public void setProperty(String nodeId, String property, String value){
    	
    }
    
    public GetPreviewResult getPreviewUrl(String storeProtocol, String storeIdentifier, String nodeId){
    	return null;
    }
    
    public String getProperty(String storeProtocol, String storeIdentifier, String nodeId, String property){
    	return null;
    }
    
    public void copyNode(String nodeId, String toNodeId, boolean copyChildren){
    	
    }
    
    public void createShare(String nodeId, String[] emails, long expiryDate) throws Exception {
		// TODO Auto-generated method stub
		
	}
    
    public Share[] getShares(String nodeId) {
		// TODO Auto-generated method stub
		return null;
	}
    
    public boolean isOwner(String nodeId, String user) {
		// TODO Auto-generated method stub
		return false;
	}
    
    public String[] getMetadataSets(){
    	return null;
    }
    
    public SearchResult findGroups(
			String searchWord, String eduGroupNodeId, int from, int nrOfResults) {
		// TODO Auto-generated  method stub
		return null;
	}
	
	public SearchResult findUsers(KeyValue[] searchProps, String eduGroupNodeId, int from, int nrOfResults) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public KeyValue[] getEduGroupContextOfNode(String nodeId) {
		// TODO Auto-generated method stub
		return null;
	}
	

	public boolean hasToolPermission(String toolPermission) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void setOwner(String nodeId, String username) {
		// TODO Auto-generated method stub
		
	}
	
	public void setUserDefinedPreview(String nodeId, byte[] content, String fileName){
		
	}
	
	public void removeUserDefinedPreview(String nodeId){
		
	}
	
	public String guessMimetype(String filename) {
		return null;
	}
	
	public String[] searchNodeIdsLimit(String luceneString, int limit){
		return null;
	}
	
	
	public void removeAspect(String nodeId, String aspect){
		
	}
	
	public void removeGlobalAspectFromGroup(String groupNodeId){
		
	}
	
	public Notify[] getNotifyList(String nodeId) {
		return null;
	}
	
	public void setPermissions(String nodeId, ACE[] aces){
		
	}
	
	public void revertVersion(String nodeId, String verLbl){
		
	}
	
	public void createVersion(String nodeId, HashMap properties){
		
	}
	
	
	public Map<String, Boolean> hasAllPermissions(String nodeId, String[] permissions) {
		return null;
	}
	
	public Map<String, Boolean> hasAllPermissionsExt(String storeProtocol, String storeId, String nodeId,
			String[] permissions){
		return null;
	}
	
	
	public String getHomeFolderID(String username){
		return null;
	}
	
	public ACL getPermissions(String nodeId){
		return null;
	}

	public void addPermissionACEs(String nodeId, ACE[] aces){
	}
	
	public void removePermissionACEs(String nodeId, ACE[] aces){
	}
	
	public void setPermissionsBasic(String nodeId, String _authority, String[] permissions, boolean changeInherit, boolean inheritPermission){
		
	}
	
	public void removePermissions(String nodeId, String _authority, String[] _permissions){
		
	}
	
	public void executeAction(String nodeId, String actionName, String actionId, HashMap parameters, boolean async) {
		
	}
	
	public void createAssociation(String fromID, String toID, String association) {
		
	}
	
	public void createChildAssociation(String from, String to, String assocType, String assocName) {
		
	}
	
	public void moveNode(String newParentId, String childAssocType, String nodeId) throws Exception {
		
	}
	
	public void removeAssociation(String fromID, String toID, String association) {
		
	}
	
	public void removeChild(String parentID, String childID, String association) {
		
	}
	
	public void removeRelationsForNode(String nodeId, String nodeParentId){
		
	}
	
	public void removeRelations(String parentID){
		
	}
	
	public void addAspect(String nodeId, String aspect){
		
	}
	
	public String getGroupFolderId() {
		return null;
	}
	
	public String getRepositoryRoot() {
		return null;
	}
	
	public Map<String, Object> getChild(String parentId, String type, String property, String value){
		return null;
	}
	
	public Map<String,Map<String,Object>> getChildenByProps(String parentId, String type, HashMap props){
		return null;
	}
	
	public Map<String, Map<String, Object>> getChildrenByType(String nodeId, String type) {
		return null;
	}

	
	public Map<String, Map<String, Object>> getChildrenByAssociation(String storeString, String nodeId, String association){
		return null;
	}
	
	public Map<String, Map<String, Object>> getParents(String nodeID, boolean primary){
		return null;
	}
	
	public Map<String, Map<String, Object>> getAssocNode(String nodeID, String association) {
		return null;
	}
	
	public Map<String, Object> getChildRecursive(String parentId, String type, HashMap props){
		return null;
	}
	
	public Map<String, Map<String,Object>> getChildrenRecursive(String parentId, String type){
		return null;
	}
	
	public String[] getAssociationNodeIds(String nodeID, String association) {
		return null;
	}
	
	public Map<String, String> getUserInfo(String userName){
		return null;
	}
	
	public String[] getUserNames() {
		return null;
	}
	
	public UserDetails[] getUserDetails(String[] userNames) {
		return null;
	}
	
	public void setUserDetails(UserDetails[] userDetails) {
	}
	
	public void deleteUser(String[] userNames) {
	}

	public String[] getGroupNames() {
		return null;
	}
	
	public GroupDetails[] getGroupDetails(String[] groupNames) {
		return null;
	}
	
	public GroupDetails setGroupDetails(GroupDetails groupDetails) {
		return null;
	}
	
	public void deleteGroup(String[] groupNames) {
	}

	public String[] getMemberships(String groupName) {
		return null;
	}
	
	public void addMemberships(String groupName, String[] members) {
	}
	
	public void removeMemberships(String groupName, String[] members) {
	}

	public void removeAllMemberships(String[] groupNames) {		
	}
	
	public boolean isSubOf(String type, String parentType){
		return false;
	}
	
	public String getPath(String nodeID) {
		return null;
	}
	
	public boolean hasContent(String nodeId, String contentProp){
		return false;
	}
	
	public void writeContent(String nodeID, byte[] content, String mimetype, String encoding, String property){
		
	}
	
	public void setUserPassword(String userName, String password){
		
	}
	
	public void invalideTicket(String ticket){
		
	}
	
	public void bindEduGroupFolder(String groupName, String folderId) {
		
	}
	
	public String findNodeByPath(String path) {
		return null;
	}
	
	public String[] getAspects(String storeProtocol, String storeId, String nodeId){
		return null;
	}
}
