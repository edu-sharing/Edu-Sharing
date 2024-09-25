package org.edu_sharing.service.authority;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public interface AuthorityService {
	Set<String> getMemberships(String username);
	boolean isGlobalAdmin();
	boolean hasAdminAccessToOrganization(String orgName);
	void deleteAuthority(String authorityName);
	void setAuthorityProperty(String authority, String property, Serializable value);
	Object getAuthorityProperty(String authority, String property);
	void addAuthorityAspect(String authority, String aspect);
	/**
	 * Returns true if the current user is allowed to admister (mamange members) to this group
	 */
	boolean hasAdminAccessToGroup(String groupName);
	/**
	 * returns if the current user is allowed to modify this group
	 * Internal node: the difference between admin access and modify is: Modifying of the main org group is not allowed for an org admin
	 * @param groupName
	 * @return
	 */
	boolean hasModifyAccessToGroup(String groupName);
	
	/**
	 * 
	 * @return ALL edugroups of current user
	 */
	default ArrayList<EduGroup> getAllEduGroups(){
		return getAllEduGroups(AuthenticationUtil.getFullyAuthenticatedUser());
	};

	public ArrayList<EduGroup> getAllEduGroups(String authority);

	/**
	 * 
	 * @return edugroups of current scope
	 */
	public ArrayList<EduGroup> getEduGroups();

	default ArrayList<EduGroup> getEduGroups(String scope){
		return getEduGroups(AuthenticationUtil.getFullyAuthenticatedUser(),scope);
	}
	public ArrayList<EduGroup> getEduGroups(String authority,String scope);
	
	/**
	 * creates an edugroup with groupadministrators group in a scoped area
	 * 
	 * @param eduGroup data of the new edugroup
	 * @param unscopedEduGroup if it is a scoped group then use the use unscopedEduGroup to find the ORG_ADMINISTRATORS displayName and groupname
	 * if it is null no ORG_ADMINISTRATORS will be created
	 * @param folderParentId
	 * @return
	 */
	public EduGroup getOrCreateEduGroup(EduGroup eduGroup, EduGroup unscopedEduGroup, String folderParentId);
	boolean isGuest();

    boolean hasAdminAccessToMediacenter(String groupName);

    public String getProperty(String authorityName, String ccmPropGroupextensionGrouptype);
	EduGroup getEduGroup(String authority);
	String createGroup(String groupName, String displayName, String parentGroup) throws Exception;
	static String getGroupName(String groupName,String parentGroup){
		return org.edu_sharing.alfresco.service.AuthorityService.getGroupName(groupName, parentGroup);
	}
	boolean authorityExists(String authority);
	Map<String, Serializable> getUserInfo(String userName) throws Exception;
	User getUser(String userName);
	void createOrUpdateUser(Map<String, Serializable> userInfo) throws Exception;

	NodeRef getAuthorityNodeRef(String authority);

    void addMemberships(String groupName, String[] members);

	void removeMemberships(String groupName, String[] members);

	String[] getMembershipsOfGroup(String groupName);

	void createGroupWithType(String groupName, String displayName, String parentGroup, String groupType) throws Exception;

	/**
	 * method to get all property of ProfileSettings for specfic User
	 *
	 * @param (String) userName  of Person, When is empty, it  get from current userLogin
	 * @param (String) property  of profileSettings, When is empty, it  get all the properties
	 * @return (Map<String, Serializable>) retun map with ONE or ALL properties for ProfileSettings
	 * @throws Exception
	 */
	Map<String, Serializable> getProfileSettingsProperties(String userName,String profileSettingsProperty) throws Exception;

	/**
	 * finds any matching subgroup of the given group type
	 * Node: there is no sorting so you need to make sure that exactly only ONE group of this type exists
	 */
	String getSubgroupByType(String parentGroup, String groupType);
}
