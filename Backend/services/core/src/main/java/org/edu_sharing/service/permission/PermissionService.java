package org.edu_sharing.service.permission;

import java.util.*;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.Authority;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.InsufficientPermissionException;

public interface PermissionService {
	public static final String[] GUEST_PERMISSIONS = new String[]{ org.alfresco.service.cmr.security.PermissionService.READ,CCConstants.PERMISSION_READ_PREVIEW,CCConstants.PERMISSION_READ_ALL, CCConstants.PERMISSION_FEEDBACK};
	/**
	 * adds permissions to the current ACL
	 * @param _nodeId
	 * @param _authPerm
	 * @param _inheritPermissions
	 * @param _mailText
	 * @param _sendMail
	 * @param _sendCopy
	 * @throws Throwable
	 */
	 void addPermissions(String _nodeId, Map<String,String[]> _authPerm,
			Boolean _inheritPermissions, String _mailText, Boolean _sendMail, 
			Boolean _sendCopy) throws Throwable;
	
	
	/**
	 * adds new , updates and removes
	 * 
	 * @param nodeId
	 * @param aces: the the new ace list, inherited will be ignored
	 * @param inheritPermissions
	 * @param mailText
	 * @param sendMail
	 * @param sendCopy
	 * @throws Throwable
	 */
	 void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermissions,
							   String mailText, Boolean sendMail, Boolean sendCopy) throws Throwable;


	 void createNotifyObject(final String nodeId, final String user, final String action);

	void addToRecentProperty(String property, NodeRef elementAdd);

	List<String> getRecentlyInvited();

    ArrayList<NodeRef> getRecentProperty(String property);

    List<Notify> getNotifyList(String nodeId) throws Throwable;
		
	
	 void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermission) throws Exception;
	
	 void setPermissions(String nodeId, List<ACE> aces) throws Exception;
	
	 void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission) throws Exception;

	void setPermissionInherit(String nodeId, boolean inheritPermission) throws Exception;

	 void addPermissions(String nodeId, ACE[] aces) throws Exception;
	
	 void removePermissions(String nodeId, ACE[] aces) throws Exception;

    void removeAllPermissions(String nodeId) throws Exception;

    StringBuffer getFindGroupsSearchString(String searchWord, boolean globalContext, boolean skipTpCheck);

     Result<List<User>> findUsers(String query, Map<String, Double> searchFields, boolean globalContext, int from, int nrOfResults);

	StringBuffer getFindUsersSearchString(String query, Map<String, Double> searchFields, boolean globalContext);

	 Result<List<Authority>> findAuthorities(String searchWord, boolean globalContext, int from, int nrOfResults);

	 Result<List<Group>> findGroups(String searchWord, boolean globalContext, int from, int nrOfResults);

    void addUserToSharedList(String user, NodeRef nodeRef);

	void cleanUpSharedList(NodeRef nodeRef);

	 boolean hasPermission(String storeProtocol, String storeId, String nodeId, String permission);

	 boolean hasPermission(String storeProtocol, String storeId, String nodeId, String authority, String permission);

	Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId, String authority,
											   String[] permissions);

	 Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId, String[] permissions);
	
	 ACL getPermissions(String nodeId) throws Exception;
	 List<String> getPermissionsForAuthority(String nodeId,String authorityId, Collection<String> permissions) throws InsufficientPermissionException;

	default List<String> getPermissionsForAuthority(String nodeId,String authorityId) throws InsufficientPermissionException {
		return getPermissionsForAuthority(nodeId, authorityId, CCConstants.getPermissionList());
	}


	void setPermission(String nodeId, String authority, String permission);


	List<String> getExplicitPermissionsForAuthority(String nodeId, String authorityId) throws InsufficientPermissionException;
}
