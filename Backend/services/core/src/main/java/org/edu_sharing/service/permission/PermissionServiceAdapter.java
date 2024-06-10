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
import org.edu_sharing.service.InsufficientPermissionException;

public class PermissionServiceAdapter implements PermissionService {
	
	protected List<String> ALLOWED_PERMISSIONS=new ArrayList<>();
	
	@Override
	public void addPermissions(String _nodeId, Map<String, String[]> _authPerm, Boolean _inheritPermissions,
							   String _mailText, Boolean _sendMail, Boolean _sendCopy) throws Throwable {
	}

	@Override
	public List<Notify> getNotifyList(String nodeId) throws Throwable {
		return null;
	}

	@Override
	public void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermissions, String mailText,
							   Boolean sendMail, Boolean sendCopy) throws Throwable {

	}

	@Override
	public void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermission) throws Exception {

	}

	@Override
	public void setPermissions(String nodeId, List<ACE> aces) throws Exception {

	}

	@Override
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission)
			throws Exception {
	}

	@Override
	public void setPermissionInherit(String nodeId, boolean inheritPermission) throws Exception {

	}

	@Override
	public void addPermissions(String nodeId, ACE[] aces) throws Exception {
	}

	@Override
	public void removePermissions(String nodeId, ACE[] aces) throws Exception {	
	}

	@Override
	public void removeAllPermissions(String nodeId) throws Exception {

	}

	@Override
	public void removePermissions(String nodeId, String authority, String[] _permissions) throws Exception {
	}

	@Override
	public StringBuffer getFindGroupsSearchString(String searchWord, boolean globalContext, boolean skipTpCheck) {
		return null;
	}

	@Override
	public Result<List<User>> findUsers(String query, Map<String, Double> searchFields, boolean globalContext, int from, int nrOfResults) {
		return null;
	}

	@Override
	public StringBuffer getFindUsersSearchString(String query, Map<String, Double> searchFields, boolean globalContext) {
		return null;
	}

	@Override
	public Result<List<Authority>> findAuthorities(String searchWord, boolean globalContext, int from,
			int nrOfResults) {
		return null;
	}

	@Override
	public Result<List<Group>> findGroups(String searchWord, boolean globalContext, int from, int nrOfResults) {
		return null;
	}

	@Override
	public void addUserToSharedList(String user, NodeRef nodeRef) {

	}

	@Override
	public void cleanUpSharedList(NodeRef nodeRef) {

	}

	@Override
	public void createNotifyObject(String nodeId, String user, String action) {
	}

	@Override
	public void addToRecentProperty(String property, NodeRef elementAdd) {

	}

	@Override
	public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String permission) {
		return 	ALLOWED_PERMISSIONS.contains(permission);
	}

	@Override
	public Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId,
			String[] permissions) {
		Map<String, Boolean> map = new HashMap<>();
		for(String permission : permissions){
			if(ALLOWED_PERMISSIONS.contains(permission))
				map.put(permission, true);
			else
				map.put(permission, false);
		}
		return map;
	}
	
	@Override
	public ACL getPermissions(String nodeId) throws Exception {
		return null;
	}

	@Override
	public List<String> getPermissionsForAuthority(String nodeId, String authorityId, Collection<String> permissions) throws InsufficientPermissionException {
		return null;
	}

	@Override
	public void setPermission(String nodeId, String authority, String permission) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String authority, String permission) {
		return false;
	}

	@Override
	public Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId, String authority, String[] permissions) {
		return null;
	}
	@Override
	public List<String> getExplicitPermissionsForAuthority(String nodeId, String authorityId) throws InsufficientPermissionException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<String> getRecentlyInvited() {
		return null;
	}

	@Override
	public ArrayList<NodeRef> getRecentProperty(String property) {
		return null;
	}
}
