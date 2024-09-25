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
package org.edu_sharing.repository.server;

import org.edu_sharing.repository.client.rpc.*;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 
 * 
 * This interface is used for the repository Methods that are used only by Alfresco Repository
 */
public interface MCAlfrescoClient {
	enum ContextSearchMode{
		Default,
		UserAndGroups,
		Public
	}
	
	/**
	 * get all favorite folders for the current user
	 * 
	 * @return nested Map withe the nodeIds of the favorite folders and the corresponding propetries (as Map<String,Object>)
	 * @throws Throwable
	 */
	public Map<String, Map<String, Object>> getBaskets() throws Throwable;
	
	/**
	 * search for nodes with a lucene string, filtered by type 
	 * 
	 * @param luceneString a lucene String
	 * @param type the given type
	 * @return nested Map with nodeId's and nodes
	 * @throws Exception
	 */
	public Map<String,Map<String,Object>> search(String luceneString, String type) throws Exception;
	
	/**
	 * search for nodes with a lucene string
	 * 
	 * @param luceneString a lucene String
	 * @return nested Map with nodeId's and propeties
	 * @throws Throwable
	 */
	public Map<String, Map<String, Object>> search(String luceneString) throws Throwable;
	
	/**
	 * 
	 * search for nodes with a lucene string
	 * 
	 * @param luceneString
	 * @param eduGroupContext if true only the eduGroups of the current users will be send as authority to check read permissions
	 * @return
	 * @throws Throwable
	 */
	public Map<String, Map<String, Object>> search(String luceneString, ContextSearchMode mode) throws Throwable;
	
	/**
	 * search for nodeId's with a lucene string
	 * @param luceneString
	 * @return Array of nodeId's
	 * @throws Exception
	 */
	public String[] searchNodeIds(String luceneString) throws Exception;
	
	public String[] searchNodeIds(String luceneString, int limit) throws Exception;

	
	/**
	 * get all children of a given parentId
	 * 
	 * @param parentID
	 * @return nested Map with nodeIds and the corresponding properties Map<String,Map<String,Object>>
	 * @throws Throwable
	 */
	public Map<String, Map<String, Object>> getChildren(String parentID) throws Throwable;
	
	/**
	 * get all children with a given association
	 * 
	 * @param nodeId node Id of the parent object
	 * @param association the matching association
	 * @return nested Map with nodeIds and the corresponding properties
	 * @throws Exception
	 */
	public Map<String, Map<String, Object>> getChildrenByAssociation(String nodeId, String association) throws Exception;
	
	/**
	 * get all children with a given association in a given alfresco store
	 * 
	 * @param store the store 
	 * @param nodeId node Id of the parent object
	 * @param association the matching association
	 * @return nested Map with nodeIds and the corresponding properties
	 * @throws Exception
	 */
	public Map<String, Map<String, Object>> getChildrenByAssociation(String store, String nodeId, String association)  throws Exception;
	
	
	/**
	 * create a new favorite folder for the current user
	 * @param _basketName
	 * @return
	 * @throws Throwable
	 */
	public String newBasket(String _basketName) throws Throwable;
		
	/**
	 * 
	 * @param nodeId
	 * @param aspect
	 * @throws Throwable
	 */
	public void removeAspect(String nodeId, String aspect) throws Throwable;
	
	/**
	 * remove a favorite folder for the current user
	 * @param basketID the favorite folder id
	 * @return
	 * @throws Throwable
	 */
	public boolean removeBasket(String basketID) throws Throwable;
	
	/**
	 * use this to create links to nodes. a link has a secondary parent association to it's parent
	 * 
	 * @param folderId the folder id the node sholud to be linked to
	 * @param nodeId the nodeId of the node that should be linkes
	 * @return
	 * @throws Exception
	 */
	public boolean createChildAssociation(String folderId, String nodeId) throws Exception;
	
	/**
	 * get the favorite folder node id of the current user
	 * @return
	 * @throws Throwable
	 */
	public String getFavoritesFolder() throws Throwable;
	
	/**
	 * get the Group foulder id of the current user
	 * @return
	 * @throws Throwable
	 */
	public String getGroupFolderId() throws Throwable;

	/**
	 * get all folder ids that ar children of the group folder of the current user
	 * @return
	 * @throws Throwable
	 */
	public Map<String, Map<String, Object>> getGroupFolders() throws Throwable;

	/**
	 * get all parents of a given nodeId
	 * 
	 * @param nodeID
	 * @param primary
	 * @return
	 * @throws Throwable
	 */
	public Map<String, Map<String,Object>> getParents(String nodeID, boolean primary) throws Throwable;
	


	/**
	 * 
	 * 
	 * 
	 * @param parentID
	 * @param childID
	 * @param association
	 * @throws Exception
	 */
	public void removeChild(String parentID, String childID, String association) throws Exception;
	
	
	/**
	 * find out if an authority gots the specified permissions on a node
	 * 
	 * @param nodeId the id of the node
	 * @param authority the username or groupname
	 * @param permissions the permissions to check
	 * 
	 * @return Map with the permissions and the value true/false weather the permission is set or not
	 *  
	 * @throws Exception
	 */
	public Map<String, Boolean> hasAllPermissions(String nodeId, String authority, String[] permissions) throws Exception;
	
	/**
	 * 
	 * find out if the current user gots the specified permissions on a node
	 * 
	 * @param nodeId the id of the node
	 * @param permissions the permissions to check
	 * @return  Map with the permissions and the value true/false weather the permission is set or not
	 * @throws Exception
	 */
	public Map<String, Boolean> hasAllPermissions(String nodeId, String[] permissions) throws Exception;
	
	
	/**
	 * this method checks if the authority gots all permissions on a node
	 * 
	 * @param nodeId nodeId of a node
	 * @param authority the username or groupname
	 * @param permissions permissions that must be set
	 * @return
	 * @throws Exception
	 */
	public boolean hasPermissions(String nodeId, String authority, String[] permissions) throws Exception;
	
	
	/**
	 * use this for adding remote objects to favorite folder
	 * 
	 * params are 
	 *  CCConstants.NODEID: the id of the node in the remote repository
	 *  CCConstants.REPOSITORY_ID the id of the remote repository
	 *  CCConstants.REPOSITORY_TYPE the type of the remote repository
	 * 
	 * @param basketId the basketId
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public String dropToBasketRemoteNode(String basketId, Map<String, String> params) throws Exception;
	
	/**
	 * get all associated nodes
	 * 
	 * @param nodeID the nodeId of the source node
	 * @param association the association type
	 * @return nested Map with nodeIds and properties
	 * @throws Throwable
	 */
	public Map<String, Map<String, Object>> getAssocNode(String nodeID, String association) throws Throwable;
	
	/**
	 * remove relations for the current folder
	 * 
	 * @param parentID
	 * @throws Exception
	 */
	public void removeRelations(String parentID) throws Exception;
	
	
	/**
	 * remove all {http://www.campuscontent.de/model/1.0}maprelation objects and than remove the object itself
	 * @param nodeID
	 * @param fromID
	 * @throws Throwable
	 */
	public void removeNodeAndRelations(String nodeID, String fromID) throws Throwable;
	
	public void removeNodeAndRelations(String nodeID, String fromID, boolean recycle) throws Throwable;

	/**
	 * get the root folder of the current user. if admin then it's null else its the user home
	 * @return root nodeId
	 * @throws Exception
	 */
	public String getRootNodeId() throws Exception;
	
	
	/**
	 * move a node to another parent
	 * @param newParentId the new parent
	 * @param childAssocType the child assoc type that should be used
	 * @param nodeId the node id of the node to move
	 * @throws Exception
	 */
	public void moveNode(String newParentId, String childAssocType, String nodeId) throws Exception;
	

	
	/**
	 * create a shadow user in the remote repository if he is not already there
	 * 
	 * @param username
	 * @param email
	 * @param repId
	 * @return the person properties
	 * @throws Exception
	 */
	public Map<String, String> checkAndCreateShadowUser(String username, String email, String repId) throws Exception;
	
	
	/**
	 * get the versionhistory of a node
	 * 
	 * @param nodeId
	 * @return Map with id and properties. the id is the nodeId of the frozen state in the VersionStore
	 * 
	 * @throws Throwable
	 */
	public Map<String, Map<String,Object>> getVersionHistory(String nodeId) throws Throwable;
	
	/**
	 * revert a node to its state of a specified version
	 * 
	 * @param nodeId the nodeId in the SpacesStore
	 * @param verLbl the version label to revert to
	 * @throws Exception
	 */
	public void revertVersion(String nodeId, String verLbl) throws Exception;
	
	/**
	 * find out if a user got's the admin role
	 * 
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public boolean isAdmin(String username) throws Exception;
	
	/**
	 * take the current runAs alfresco user and check if it is an admin
	 * normaly runas = the fully authenticated user only when AuthenticationUtil.RunAsWork<Result>
	 * it differs
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean isAdmin() throws Exception;
	
	/**
	 * get the root node id of an repository
	 * @return
	 * @throws Exception
	 */
	public String getRepositoryRoot() throws Exception;
	
	/**
	 * switch remote object resoving on/off
	 * @param resolveRemoteObjects
	 */
	public void setResolveRemoteObjects(boolean resolveRemoteObjects);
	
	/**
	 * get the url to content of the node. the default alfresco content property is used
	 * @param nodeId
	 * @return
	 * @throws Exception
	 */
	public String getAlfrescoContentUrl( String nodeId) throws Exception;
	
	/**
	 * the nodeId of the systemfolder will be returned and it it not exists it will bhe created
	 * 
	 * @param foldername 
	 * @return the nodeId of the systemfolder
	 * @throws Throwable
	 */
	public String checkSystemFolderAndReturn(String foldername) throws Throwable;
	
	/**
	 * get children of the sepcified type and the specified properties
	 * 
	 * @param parentId
	 * @param type the type to match
	 * @param props the properties to match
	 * @return 
	 * @throws Throwable
	 */
	public abstract Map<String,Map<String,Object>> getChilden(String parentId, String type, Map<String, Object> props) throws Throwable;
	
	/**
	 * find a node that matches the given properties while navigatigating through all the children of the node with parentId
	 * 
	 * @param parentId the nodeId of the startnode
	 * @param type the type the child must have
	 * @param props the properties that must match
	 * @return the child prop if found else null
	 * @throws Throwable
	 */
	public Map<String,Object> getChildRecursive(String parentId, String type, Map<String, Object>  props)throws Throwable;
	
	/**
	 * fina all chidren taht match the given type and return i t in a flat Map structure
	 * @param parentId
	 * @param type
	 * @return
	 * @throws Throwable
	 */
	public Map<String,Map<String,Object>> getChildrenRecursive(String parentId, String type)throws Throwable;
	
	/**
	 * find out if the given type is subtype of parentType
	 * @param type
	 * @param parentType
	 * @return
	 * @throws Throwable
	 */
	public boolean isSubOf(String type, String parentType) throws Throwable;
	

	/**
	 * get all properties for a given user
	 * @param userName
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getUserInfo(String userName) throws Exception;
	
	

	/**
	 * get the company home node id
	 * @return
	 */
	public String getCompanyHomeNodeId();
	
	/**
	 * get the path to a node
	 * 
	 * @param nodeID
	 * @return
	 * @throws Exception
	 */
	public String getPath(String nodeID) throws Exception;
	
	public void setLocale(String localeStr);
	
	public String getNodeType(String nodeId);
	
	public void setProperty(String nodeId, String property, Serializable value);
	
	public String getProperty(String storeProtocol, String storeIdentifier, String nodeId, String property);
	
	public GetPreviewResult getPreviewUrl(String storeProtocol, String storeIdentifier, String nodeId);
	
	/**
	 * execute a action
	 * 
	 * @param nodeId the nodeId on which the action works
	 * @param actionName the action name
	 * @param actionId the action id
	 * @param parameters the parameters a action expects
	 * @param async
	 * @throws Exception
	 */
	public abstract void executeAction(String nodeId, String actionName, String actionId, Map<String, Object>  parameters, boolean async) throws Exception;

	public String copyNode(String nodeId, String toNodeId, boolean copyChildren) throws Exception;

	public void createShare(String nodeId, String[] emails, long expiryDate) throws Exception;
	
	public boolean isOwner(String nodeId, String user);

	public Share[] getShares(String nodeId);
	
	public void setOwner(String nodeId, String username);
	
	public void setUserDefinedPreview(String nodeId, byte[] content, String fileName);
	
	public void removeUserDefinedPreview(String nodeId);
	
	public SearchResult searchSolr(String query, int startIdx,int nrOfresults, List<String> facettes, int facettesMinCount, int facettesLimit) throws Throwable ;

	public String guessMimetype(String filename);
	
	public Map<String, Map<String, Object>> getChildren(String parentID, String[] permissionsOnChild) throws Throwable;

	public void removeGlobalAspectFromGroup(String groupNodeId) throws Exception;
	
	public ArrayList<EduGroup> getEduGroups() throws Throwable;
	public User getOwner(String storeId,String storeProtocol,String nodeId);
	
	public SearchResult search(String luceneString, String storeProtocol, String storeName, int from, int maxResult) throws Throwable;
	
	public void removeNode(String storeProtocol, String storeId, String nodeId);
	
	public Map<String, Object> getProperties(String storeId, String storeProtocol, String nodeId) throws Throwable;
	
	public Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId, String[] permissions);
	
	public String[] getAspects(String storeProtocol, String storeId, String nodeId);
	
	public void addAspect(String nodeId, String aspect) throws Exception;
	
} 
