package org.edu_sharing.repository.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.rpc.SearchToken;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.rpc.User;

public class MCAlfrescoClientAdapter extends MCAlfrescoBaseClient {

	@Override
	public ACL getPermissions(String nodeId) throws Exception {
		return null;
	}

	@Override
	public Map<String,Map<String,Object>> getBaskets() throws Throwable {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> search(String luceneString, String type) throws Exception {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> search(String luceneString) throws Throwable {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> search(String luceneString, ContextSearchMode mode)
			throws Throwable {
		return null;
	}

	@Override
	public String[] searchNodeIds(String luceneString) throws Exception {
		return null;
	}

	@Override
	public String[] searchNodeIds(String luceneString, int limit) throws Exception {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getChildren(String parentID) throws Throwable {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getChildrenByAssociation(String nodeId, String association)
			throws Exception {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getChildrenByAssociation(String store, String nodeId,
			String association) throws Exception {
		return null;
	}

	@Override
	public String newBasket(String _basketName) throws Throwable {
		return null;
	}

	@Override
	public void removeAspect(String nodeId, String aspect) throws Throwable {		
	}

	@Override
	public boolean removeBasket(String basketID) throws Throwable {
		return false;
	}

	@Override
	public boolean createChildAssociation(String folderId, String nodeId) throws Exception {
		return false;
	}

	@Override
	public String getFavoritesFolder() throws Throwable {
		return null;
	}

	@Override
	public String getGroupFolderId() throws Throwable {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getGroupFolders() throws Throwable {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getParents(String nodeID, boolean primary) throws Throwable {
		return null;
	}

	@Override
	public void removeChild(String parentID, String childID, String association) throws Exception {
	}

	@Override
	public Map<String, Boolean> hasAllPermissions(String nodeId, String authority, String[] permissions)
			throws Exception {
		return null;
	}

	@Override
	public Map<String, Boolean> hasAllPermissions(String nodeId, String[] permissions) throws Exception {
		return null;
	}

	@Override
	public boolean hasPermissions(String nodeId, String authority, String[] permissions) throws Exception {
		return false;
	}

	@Override
	public String dropToBasketRemoteNode(String basketId, Map<String, String> params) throws Exception {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getAssocNode(String nodeID, String association) throws Throwable {
		return null;
	}

	@Override
	public void removeRelations(String parentID) throws Exception {
	}
	
	@Override
	public void removeNodeAndRelations(String nodeID, String fromID, boolean recycle) throws Throwable {
	}

	@Override
	public void removeNodeAndRelations(String nodeID, String fromID) throws Throwable {
	}

	@Override
	public String getRootNodeId() throws Exception {
		return null;
	}

	@Override
	public void moveNode(String newParentId, String childAssocType, String nodeId) throws Exception {
	}

	@Override
	public Group getEduGroupContextOfNode(String nodeId) {
		return null;
	}

	@Override
	public Map<String, String> checkAndCreateShadowUser(String username, String email, String repId)
			throws Exception {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getVersionHistory(String nodeId) throws Throwable {
		return null;
	}

	@Override
	public void revertVersion(String nodeId, String verLbl) throws Exception {
	}

	@Override
	public boolean isAdmin(String username) throws Exception {
		return false;
	}

	@Override
	public boolean isAdmin() throws Exception {
		return false;
	}

	@Override
	public String getRepositoryRoot() throws Exception {
		return null;
	}

	@Override
	public void setResolveRemoteObjects(boolean resolveRemoteObjects) {	
	}

	@Override
	public String getAlfrescoContentUrl(String nodeId) throws Exception {
		return null;
	}

	@Override
	public String checkSystemFolderAndReturn(String foldername) throws Throwable {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getChilden(String parentId, String type, Map<String, Object> props)
			throws Throwable {
		return null;
	}

	@Override
	public Map<String, Object> getChildRecursive(String parentId, String type, Map<String, Object> props) throws Throwable {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getChildrenRecursive(String parentId, String type)
			throws Throwable {
		return null;
	}

	@Override
	public boolean isSubOf(String type, String parentType) throws Throwable {
		return false;
	}

	@Override
	public String getCompanyHomeNodeId() {
		return null;
	}

	@Override
	public String getPath(String nodeID) throws Exception {
		return null;
	}

	@Override
	public void setLocale(String localeStr) {
	}

	@Override
	public String getNodeType(String nodeId) {
		return null;
	}


	@Override
	public void setProperty(String nodeId, String property, Serializable value) {
	}

	@Override
	public String getProperty(String storeProtocol, String storeIdentifier, String nodeId, String property) {
		return null;
	}

	@Override
	public GetPreviewResult getPreviewUrl(String storeProtocol, String storeIdentifier, String nodeId) {
		return null;
	}

	@Override
	public void executeAction(String nodeId, String actionName, String actionId, Map<String, Object> parameters, boolean async)
			throws Exception {	
	}

	@Override
	public String copyNode(String nodeId, String toNodeId, boolean copyChildren) throws Exception {
		return null;
	}

	@Override
	public void createShare(String nodeId, String[] emails, long expiryDate) throws Exception {
	}

	@Override
	public boolean isOwner(String nodeId, String user) {
		return false;
	}

	@Override
	public Share[] getShares(String nodeId) {
		return null;
	}

	@Override
	public void setOwner(String nodeId, String username) {
	}

	@Override
	public void setUserDefinedPreview(String nodeId, byte[] content, String fileName) {
	}

	@Override
	public void removeUserDefinedPreview(String nodeId) {
	}

	@Override
	public SearchResult searchSolr(String query, int startIdx, int nrOfresults, List<String> facets,
			int facetsMinCount, int facetsLimit) throws Throwable {
		return null;
	}

	@Override
	public String guessMimetype(String filename) {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getChildren(String parentID, String[] permissionsOnChild)
			throws Throwable {
		return null;
	}

	@Override
	public void removeGlobalAspectFromGroup(String groupNodeId) throws Exception {
	}

	@Override
	public ArrayList<EduGroup> getEduGroups() throws Throwable {
		return null;
	}

	@Override
	public User getOwner(String storeId, String storeProtocol, String nodeId) {
		return null;
	}

	@Override
	public SearchResult search(String luceneString, String storeProtocol, String storeName, int from, int maxResult)
			throws Throwable {
		return null;
	}

	@Override
	public void removeNode(String storeProtocol, String storeId, String nodeId) {
	}

	@Override
	public Map<String, Object> getProperties(String storeId, String storeProtocol, String nodeId) throws Throwable {
		return null;
	}

	@Override
	public Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId,
			String[] permissions) {
		return null;
	}

	@Override
	public String[] getAspects(String storeId, String storeProtocol, String nodeId) {
		return null;
	}

	@Override
	public String createNode(String parentID, String nodeTypeString, String childAssociation,
			Map<String, Object> _props) throws Exception {
		return null;
	}

	@Override
	public String createNode(String parentID, String nodeTypeString, Map<String, Object> _props) throws Exception {
		return null;
	}

	@Override
	public void addAspect(String nodeId, String aspect) {
	}

	@Override
	public void updateNode(String nodeId, Map<String, Object> _props) throws Exception {
	}

	@Override
	public void createAssociation(String fromID, String toID, String association) {
	}

	@Override
	public void writeContent(String nodeID, byte[] content, String mimetype, String encoding, String property)
			throws Exception {
	}

	@Override
	public Map<String, Object> getProperties(String nodeId) throws Throwable {
		return null;
	}

	@Override
	public Map<String, Object> getChild(String parentId, String type, String property, String value)
			throws Throwable {
		return null;
	}

	@Override
	public String getHomeFolderID(String username) throws Exception {
		return null;
	}

	@Override
	public void createVersion(String nodeId) throws Exception {
	}

	@Override
	public List<String> getAssociationNodeIds(String nodeID, String association) throws Exception {
		return null;
	}

	@Override
	public void removeAssociation(String fromID, String toID, String association) throws Exception {
	}

	@Override
	public Map<String, Map<String, Object>> getChildrenByType(String nodeId, String type) throws Exception {
		return null;
	}

	@Override
	public Map<String, String> getUserInfo(String userName) throws Exception {
		return null;
	}

	@Override
	public void removeNode(String nodeID, String fromID) throws Exception {
	}

	@Override
	public boolean hasContent(String nodeId, String contentProp) throws Exception {
		return false;
	}

	@Override
	public String getDetailsHtmlSnippet(String nodeId) throws Exception {
		return null;
	}

	@Override
	public boolean hasPermissions(String nodeId, String[] permissions) throws Exception {
		return false;
	}

	@Override
	public MCBaseClient getInstance(Map<String, String> _authenticationInfo) {
		return null;
	}

	@Override
	public MCBaseClient getInstance(String _repositoryFile, Map<String, String> _authenticationInfo) {
		return null;
	}
	
	@Override
	public void removeNode(String nodeID, String fromID, boolean recycle) throws Exception {
	}

}
