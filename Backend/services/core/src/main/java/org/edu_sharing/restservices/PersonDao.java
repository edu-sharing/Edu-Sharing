package org.edu_sharing.restservices;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.edu_sharing.alfresco.lightbend.LightbendConfigCache;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.EduSharingLockHelper;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.cache.PersonCache;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.restservices.iam.v1.model.GroupEntries;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.restservices.iam.v1.model.ProfileSettings;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.servlet.http.HttpSession;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public class PersonDao {

	Logger logger = Logger.getLogger(PersonDao.class);
	public static final String ME = "-me-";

	public static PersonDao getPerson(RepositoryDao repoDao, String userName) throws DAOException {

		try {
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();

			if (ME.equals(userName)) {

				userName = currentUser;
			}
	
			/*
			if (   !currentUser.equals(userName)
				&& !repoDao.getBaseClient().isAdmin(currentUser)
				&& !AuthenticationUtil.isRunAsUserTheSystemUser()
					) {
								
				throw new AccessDeniedException(currentUser);
			}
			*/

			return new PersonDao(repoDao, userName);

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	private boolean isCurrentUserOrAdmin() {
		try {
		String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
		if (   !currentUser.equals(getUserName())
				&& !repoDao.getBaseClient().isAdmin(currentUser)
				&& !AuthenticationUtil.isRunAsUserTheSystemUser()
					) {

				return false;
			}
		}catch(Exception e) {
			return false;
		}
		return true;
	}
	public static PersonDao createPerson(RepositoryDao repoDao, String userName,String password, UserProfileEdit profile) throws DAOException {

		try {

			try {

				repoDao.getBaseClient().getUserInfo(userName);

				throw new DAOValidationException(
						new IllegalArgumentException("Username already exists."));

			} catch (NoSuchPersonException e) {

				Map<String, Serializable> userInfo = profileToMap(profile);
				userInfo.put(CCConstants.PROP_USERNAME, userName);

				AuthorityServiceFactory.getAuthorityService(repoDao.getId()).createOrUpdateUser(userInfo);
				PersonDao result=new PersonDao(repoDao, userName);
				if(password!=null)
					result.changePassword(null,password);
				return result;
			}

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	private final MCAlfrescoBaseClient baseClient;

	private final RepositoryDao repoDao;

	private final Map<String, Serializable> userInfo;
	private String homeFolderId;
	private List<String> sharedFolderIds = null;

	private NodeService nodeService;
	private SearchService searchService;

	private AuthorityService authorityService;


	public PersonDao(RepositoryDao repoDao, String userName) throws DAOException  {

		try {

			this.baseClient = repoDao.getBaseClient();
			this.nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
			this.searchService = SearchServiceFactory.getSearchService(repoDao.getId());
			this.authorityService = AuthorityServiceFactory.getAuthorityService(repoDao.getId());

			this.repoDao = repoDao;

			this.userInfo = authorityService.getUserInfo(userName);

			if(this.userInfo == null){
				throw new DAOMissingException(new Exception("user "+ userName + " not found"));
			}

			// may causes performance penalties!
			/* this.parentOrganizations = AuthenticationUtil.runAsSystem(() ->
					authorityService.getEduGroups(userName, NodeServiceInterceptor.getEduSharingScope())
			);*/

		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	private void initGroupFolders() {
		if(sharedFolderIds != null) {
			return;
		}
		try{
			sharedFolderIds = new ArrayList<>();
			boolean getGroupFolder = true;
			//don't run into access denied wrapped by Transaction commit failed
			if(!AuthenticationUtil.isRunAsUserTheSystemUser()
					&& !AuthenticationUtil.getRunAsUser().equals(ApplicationInfoList.getHomeRepository().getUsername())
					&& !AuthenticationUtil.getRunAsUser().equals(getUserName())) {
				getGroupFolder = false;
			}
			if(getGroupFolder && getUserName() !=null) {
				String groupFolderId = ((MCAlfrescoAPIClient)baseClient).getGroupFolderId(getUserName());
				if (groupFolderId != null) {

					Map<String, Map<String, Object>> children = baseClient.getChildren(groupFolderId);

					for (Object key : children.keySet()) {

						sharedFolderIds.add(key.toString());
					}
				}
			}
		}catch(Throwable e) {

        }
    }

	public void changeProfile(UserProfileEdit profile) throws DAOException {

		try {

			Map<String, Serializable> newUserInfo = profileToMap(profile);
			newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());
			authorityService.createOrUpdateUser(newUserInfo);
		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}

	}

	private static Map<String, Serializable> profileToMap(UserProfileEdit profile) {
		Map<String, Serializable> newUserInfo = new HashMap<>();
		newUserInfo.put(CCConstants.PROP_USER_FIRSTNAME, profile.getFirstName());
		newUserInfo.put(CCConstants.PROP_USER_LASTNAME, profile.getLastName());
		newUserInfo.put(CCConstants.PROP_USER_EMAIL, profile.getEmail());
        newUserInfo.put(CCConstants.CM_PROP_PERSON_ABOUT, profile.getAbout());
        newUserInfo.put(CCConstants.CM_PROP_PERSON_SKILLS, profile.getSkills());
        newUserInfo.put(CCConstants.CM_PROP_PERSON_VCARD, profile.getVCard());
		if(AuthorityServiceFactory.getLocalService().isGlobalAdmin()) {
			newUserInfo.put(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION, profile.getPrimaryAffiliation());
			if (profile.getSizeQuota() > 0) {
				newUserInfo.put(CCConstants.CM_PROP_PERSON_SIZE_QUOTA, "" + profile.getSizeQuota());
			} else {
				newUserInfo.put(CCConstants.CM_PROP_PERSON_SIZE_QUOTA, null);
			}
		}
		return newUserInfo;
	}

	public GroupEntries getMemberships(String pattern, int skipCount, int maxItems, SortDefinition sort) throws DAOException{
		if (!AuthenticationUtil.getFullyAuthenticatedUser().equals(getAuthorityName()) && !AuthorityServiceFactory.getLocalService().isGlobalAdmin()) {
			throw new NotAnAdminException();
		}
    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).searchPersonGroups(
    					getAuthorityName(),
    					pattern,
    					skipCount,
    					maxItems,
    					sort

    			);
		List<Group> result = new ArrayList<>();
    	for (String member: search.getData()) {
    		result.add(new GroupDao(repoDao,member).asGroup());
    	}
    	GroupEntries response = new GroupEntries();
    	response.setList(result);
    	response.setPagination(new Pagination(search));
    	return response;
	}

	public void changePassword(String oldPassword, String newPassword) throws DAOException {

		try {

			if (oldPassword == null) {

				((MCAlfrescoAPIClient)this.baseClient).setUserPassword(getUserName(), newPassword);

			} else {

				((MCAlfrescoAPIClient)this.baseClient).updateUserPassword(getUserName(), oldPassword, newPassword);

			}

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}

	}

	public void delete(boolean force) throws DAOException {
		try {
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
			if (currentUser.equals(getUserName())) {
				throw new DAOValidationException(
						new IllegalArgumentException("Session user can not be deleted."));
			}
			if(!force && !PersonLifecycleService.PersonStatus.todelete.equals(getStatus().getStatus())){
				throw new IllegalStateException("User status is not yet set " +
						PersonLifecycleService.PersonStatus.todelete + ", got " + getStatus().getStatus());
			}
			((MCAlfrescoAPIClient)this.baseClient).deleteUser(getUserName());

		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}

	private String getNodeId() {
		return (String) this.userInfo.get(CCConstants.SYS_PROP_NODE_UID);
	}
	public User asPerson() throws DAOException {

    	User data = new User();

    	data.setAuthorityName(getAuthorityName());
    	data.setAuthorityType(Authority.Type.USER);

    	data.setUserName(getUserName());

		data.setOrganizations(OrganizationDao.mapOrganizations(getParentOrganizations()));


		data.setProfile(getProfile());
    	data.setStatus(getStatus());

    	if(isCurrentUserOrAdmin()) {
			data.setProperties(getProperties());

			NodeRef homeDir = new NodeRef();
	    	homeDir.setRepo(repoDao.getId());
	    	homeDir.setId(getHomeFolder());
	    	data.setHomeFolder(homeDir);
            data.setQuota(getQuota());

			initGroupFolders();

			List<NodeRef> sharedFolderRefs = new ArrayList<>();
	    	for (String sharedFolderId : sharedFolderIds) {

	        	NodeRef sharedFolderRef = new NodeRef();
	        	sharedFolderRef.setRepo(repoDao.getId());
	        	sharedFolderRef.setId(sharedFolderId);

	        	sharedFolderRefs.add(sharedFolderRef);
	    	}
	    	data.setSharedFolders(sharedFolderRefs);
    	}
    	return data;
	}

	private List<EduGroup> getParentOrganizations() {
		// may causes performance penalties!
		return AuthenticationUtil.runAsSystem(() ->
				authorityService.getEduGroups(this.getUserName(), NodeServiceInterceptor.getEduSharingScope())
		);
	}

	private Map<String, String[]> getProperties() {
		Map<String, Serializable> properties = userInfo;
		if (!(getProfileSettings().getShowEmail() || isCurrentUserOrAdmin())) // email must be showed only if is admin, or if email ragards to user login
			properties.replace(CCConstants.CM_PROP_PERSON_EMAIL, null);

		return NodeServiceHelper.getPropertiesMultivalue(NodeServiceHelper.transformLongToShortProperties(properties));
	}

	private UserQuota getQuota() {
		UserQuota quota=new UserQuota();
		Long sizeQuota = (Long) userInfo.get(CCConstants.CM_PROP_PERSON_SIZE_QUOTA);
		if(sizeQuota==null || sizeQuota.equals(-1L)){
			quota.setEnabled(false);
			return quota;
		}
		Long sizeCurrent = (Long) userInfo.get(CCConstants.CM_PROP_PERSON_SIZE_CURRENT);
		quota.setEnabled(true);
		quota.setSizeQuota(sizeQuota);
		quota.setSizeCurrent(sizeCurrent);
		return quota;
	}

	private UserProfile getProfile() {
		UserProfile profile = new UserProfile();
    	profile.setFirstName(getFirstName());
    	profile.setLastName(getLastName());
		// Admin user can see all email even if they are not showed
		// hide only for non admin user and if showEmail is false
		if (getProfileSettings().getShowEmail() || isCurrentUserOrAdmin()) {
			profile.setEmail(getEmail());
		} else {
			profile.setEmail("");
		}
		profile.setPrimaryAffiliation(getPrimaryAffiliation());
    	profile.setAvatar(getAvatar());
    	profile.setAbout(getAbout());
    	profile.setSkills(getSkills());
    	profile.setVCard(getVCard());
    	profile.setType(getType());
    	return profile;
	}

	private String getVCard() {
		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_VCARD);
	}

	private UserStatus getStatus() {
		UserStatus status = new UserStatus();
		if(this.userInfo.get(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS)!=null) {
			try {
				status.setStatus(PersonLifecycleService.PersonStatus.valueOf((String) this.userInfo.get(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS)));
			} catch(IllegalArgumentException e) {
				logger.warn("Person " + getAuthorityName() +" has invalid lifecycle status: " + this.userInfo.get(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
			}
		}
		// cast to long for rest api
		Date date = (Date) this.userInfo.get(CCConstants.CM_PROP_PERSON_ESPERSONSTATUSDATE);
		if(date != null) {
			status.setDate(date.getTime());
		}
		return status;
	}
	public UserStats getStats() {
		if(!LightbendConfigCache.getBoolean("repository.statistics.byUser")){
			return null;
		}
		UserStats stats = new UserStats();
		// run as admin so solr counts all materials and collections
		return AuthenticationUtil.runAsSystem(new RunAsWork<UserStats>() {

			@Override
			public UserStats doWork() throws Exception {
				String luceneUser = "@cm\\:creator:\""+ QueryParser.escape(getAuthorityName())+"\"";
				luceneUser += " AND NOT ASPECT:"+QueryParser.escape(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE));
				luceneUser += " AND NOT ASPECT:"+QueryParser.escape(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT));
				SearchToken token=new SearchToken();
				token.setMaxResult(0);
				token.setLuceneString(luceneUser);
				token.setContentType(SearchService.ContentType.FILES);
		    	SearchResultNodeRef result = searchService.search(token);
		    	stats.setNodeCount(result.getNodeCount());

		    	token.setLuceneString(luceneUser+" AND @ccm\\:commonlicense_key:\"CC_*\"");
		    	result = searchService.search(token);
		    	stats.setNodeCountCC(result.getNodeCount());

		    	token.setLuceneString(luceneUser);
		    	token.setContentType(SearchService.ContentType.COLLECTIONS);
		    	result = searchService.search(token);
		    	stats.setCollectionCount(result.getNodeCount());
				return stats;
			}
		});
	}

	private org.alfresco.service.cmr.repository.NodeRef getAvatarNode() {
		List<ChildAssociationRef> refs = this.nodeService.getChildrenChildAssociationRef(getNodeId());
		for(ChildAssociationRef ref : refs) {
			if(ref.getTypeQName().equals(QName.createQName(CCConstants.ASSOC_USER_PREFERENCEIMAGE))){
				return ref.getChildRef();
			}
		}
		return null;
	}
	private String getAvatar() {
		org.alfresco.service.cmr.repository.NodeRef avatar=getAvatarNode();
		if(avatar==null)
			return null;
		return NodeServiceHelper.getPreview(avatar).getUrl();
	}
	public void removeAvatar() throws DAOException {
		try {
			org.alfresco.service.cmr.repository.NodeRef currentAvatar = getAvatarNode();
			if(currentAvatar!=null) {
				this.nodeService.removeNode(currentAvatar.getId(), getNodeId(), false);
			}
		}catch(Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	public void changeAvatar(InputStream is) throws DAOException {
		try {
		org.alfresco.service.cmr.repository.NodeRef currentAvatar = getAvatarNode();
		ImageTool.VerifyResult result = ImageTool.verifyAndPreprocessImage(is, ImageTool.MAX_THUMB_SIZE);
		String nodeId=null;
		if(currentAvatar==null) {
			nodeId = this.nodeService.createNode(getNodeId(), CCConstants.CCM_TYPE_IO, new HashMap<>(), CCConstants.ASSOC_USER_PREFERENCEIMAGE);
			this.baseClient.createAssociation(getNodeId(), nodeId, CCConstants.ASSOC_USER_AVATAR);
		}
		else {
			nodeId=currentAvatar.getId();
		}
		NodeServiceHelper.setCreateVersion(nodeId,false);
		nodeService.writeContent(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, result.getInputStream(), result.getMediaType().toString(), null, CCConstants.CM_PROP_CONTENT);
		this.nodeService.setPermissions(nodeId, CCConstants.AUTHORITY_GROUP_EVERYONE,new String[]{CCConstants.PERMISSION_CONSUMER},true);
		}catch(Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	public UserSimple asPersonSimple(boolean resolveOrganizations) {
		UserSimple data = new UserSimple();
		data.setAuthorityName(getAuthorityName());
		data.setAuthorityType(Authority.Type.USER);
		data.setUserName(getUserName());
		data.setProfile(getProfile());
		data.setStatus(getStatus());
		if (isCurrentUserOrAdmin()) {
			data.setProperties(getProperties());
		}
		if (resolveOrganizations) {
			data.setOrganizations(OrganizationDao.mapOrganizations(getParentOrganizations()));
		}
		if(isCurrentUserOrAdmin()) {
	    	NodeRef homeDir = new NodeRef();
	    	homeDir.setRepo(repoDao.getId());
	    	homeDir.setId(getHomeFolder());
    	}
    	return data;
	}
	public UserRender asPersonRender() {
		org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		UserRender data = new UserRender();
		data.setIsGuest(authorityService.isGuest());
		data.setAuthorityName(getAuthorityName());
		data.setAuthorityType(Authority.Type.USER);
		data.setUserName(getUserName());
		data.setProfile(getProfile());
		data.setPrimaryAffiliation((String) userInfo.get(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION));
		data.setRemoteRoles((List<String>) userInfo.get(CCConstants.PROP_USER_ESREMOTEROLES));
		return data;
	}
	public String getId() {
		return getNodeId();
	}

	public String getAuthorityName() {

		return getUserName();
	}

	public String getUserName() {
		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_USERNAME);
	}

	public String getFirstName() {

		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
	}

	public String[] getType() {
		return AuthenticationUtil.runAsSystem(new RunAsWork<String[]>() {
			@Override
			public String[] doWork() throws Exception {
				 PersonCache.get(getAuthorityName(),PersonCache.TYPE);
				if(PersonCache.contains(getAuthorityName(),PersonCache.TYPE)) {
					return (String[]) PersonCache.get(getAuthorityName(),PersonCache.TYPE);
				}
				Set<String> types=new HashSet<>();
				Set<String> groups = authorityService.getMemberships(getAuthorityName());
				for(String group : groups) {
					try {
						String type=GroupDao.getGroup(repoDao, group).getGroupType();
						if(type!=null)
							types.add(type);

					}catch(Throwable t) {}
				}
				String[] typesArray = types.toArray(new String[0]);
				PersonCache.put(getAuthorityName(),PersonCache.TYPE, typesArray);
				return typesArray;
			}
		});
	}

	public String getLastName() {

		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
	}

	public String getEmail() {

		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);
	}
	public String getAbout() {
		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_ABOUT);
	}

	public String getPrimaryAffiliation() {
		return (String) this.userInfo.get(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION);
	}

	public String[] getSkills() {
		return (String[])this.userInfo.get(CCConstants.CM_PROP_PERSON_SKILLS);
	}
	public String getHomeFolder() {
		if(this.homeFolderId == null) {
			try {
				this.homeFolderId = baseClient.getHomeFolderID(getUserName());
			}catch(Throwable t) {
				throw new RuntimeException(t);
			}
		}
		return this.homeFolderId;
	}

	public String getPreferences() {
		return (String)this.userInfo.get(CCConstants.CCM_PROP_PERSON_PREFERENCES);
	}
	public void setPreferences(String preferences) throws Exception{
		// validate json
		new JSONObject(preferences);
		Map<String, Object> newUserInfo = new HashMap<>();
		newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());
		newUserInfo.put(CCConstants.CCM_PROP_PERSON_PREFERENCES, preferences);
		((MCAlfrescoAPIClient)this.baseClient).updateUser(newUserInfo);
	}

	/**
	 * retrieve All property for ProfileSetting from alfresco db
	 *
	 * @return object of ProfileSettings
	 */
	public ProfileSettings getProfileSettings() {
		ProfileSettings profileSettings = new ProfileSettings();
		// fallback to true: because otherwise it will break previous behaviour
		profileSettings.setShowEmail(
				(!this.userInfo.containsKey(CCConstants.CCM_PROP_PERSON_SHOW_EMAIL) || (boolean) this.userInfo.get(CCConstants.CCM_PROP_PERSON_SHOW_EMAIL))
		);
		return profileSettings;
	}

	/**
	 * set value into alfresco database
	 * @param  profileSettings (Object)
	 */
	public void setProfileSettings(ProfileSettings profileSettings) throws Exception{
		Map<String, Object> newUserInfo = new HashMap<>();
		newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());
		newUserInfo.put(CCConstants.CCM_PROP_PERSON_SHOW_EMAIL, profileSettings.getShowEmail());
		((MCAlfrescoAPIClient)this.baseClient).updateUser(newUserInfo);
	}

	public void addNodeList(String list,String nodeId) throws Exception {
		EduSharingLockHelper.runSingleton(PersonDao.class, this.getAuthorityName() + "_nodeList", () -> {
			try {
		// Simply check if node is valid
				NodeDao node = NodeDao.getNode(this.repoDao, nodeId);
		if(node.isDirectory())
			throw new IllegalArgumentException("The node "+nodeId+" is a directory. Only files are allowed for this list");
				String data = getCurrentNodeListJson();
		JSONObject json=new JSONObject();
		if(data!=null)
			json=new JSONObject(data);

		JSONArray array=null;
		if(json.has(list))
			array=json.getJSONArray(list);
		List<JSONObject> nodes=new ArrayList<>();
		if(array!=null){
			for(int i=0;i<array.length();i++){
				if(array.getJSONObject(i).getString("id").equals(nodeId)) {
					throw new DAODuplicateNodeException(new Exception("Node is already in list"), nodeId);
				}
				nodes.add(array.getJSONObject(i));
			}
		}
		JSONObject object=new JSONObject();
		object.put("id",nodeId);
		object.put("dateAdded",System.currentTimeMillis());
		nodes.add(object);
		json.put(list,new JSONArray(nodes));
				updateNodeList(json);
			} catch(Exception e) {
				throw new RuntimeException(e);
	}
			return null;
		});
	}
	public List<NodeRef> getNodeList(String list) throws Exception {
		String data=getCurrentNodeListJson();
		if(data==null)
			return null;
		JSONObject json=new JSONObject(data);
		if(!json.has(list))
			return null;
		JSONArray array=json.getJSONArray(list);
		List<NodeRef> result=new ArrayList<>();
		for(int i=0;i<array.length();i++){
			String nodeId=array.getJSONObject(i).getString("id");
			try{
				// causes invalid nodes to fire throwable -> delete them
				NodeDao.getNode(this.repoDao, nodeId);
				result.add(new NodeRef(this.repoDao.getId(), nodeId));
			}
			catch(Throwable t){
				removeNodeList(list,nodeId);
			}
		}
		return result;
	}
	private String getCurrentNodeListJson() throws Exception {
		org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		String data;
		if(service.isGuest()){
			data=(String) session.getAttribute(CCConstants.CCM_PROP_PERSON_NODE_LISTS);
		}
		else{
			data=(String) AuthorityServiceFactory.getLocalService().getAuthorityProperty(this.getUserName(), CCConstants.CCM_PROP_PERSON_NODE_LISTS);
		}
		return data;
	}

	public void removeNodeList(String list, String nodeId) throws Exception {
		EduSharingLockHelper.runSingleton(PersonDao.class, this.getAuthorityName() + "_nodeList", () -> {
			try {
				String data = getCurrentNodeListJson();
		if(data==null)
			throw new IllegalArgumentException("Node list not found: "+list);
		JSONObject json=new JSONObject(data);
		if(!json.has(list))
			throw new IllegalArgumentException("Node list not found: "+list);
		JSONArray array=json.getJSONArray(list);
		boolean found=false;
		List<JSONObject> result=new ArrayList<>();
		for(int i=0;i<array.length();i++){
			if(array.getJSONObject(i).getString("id").equals(nodeId)){
				found=true;
			}
			else
				result.add(array.getJSONObject(i));
		}
		if(!found)
			throw new IllegalArgumentException("Node not found in list: "+nodeId);
		json.put(list, new JSONArray(result));
				updateNodeList(json);
			}catch(Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		});
}

	private void updateNodeList(JSONObject json) throws Exception {
		org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		if(service.isGuest()){
			session.setAttribute(CCConstants.CCM_PROP_PERSON_NODE_LISTS,json.toString());
		}
		else{
			AuthorityServiceFactory.getLocalService().setAuthorityProperty(this.getUserName(), CCConstants.CCM_PROP_PERSON_NODE_LISTS, json.toString());
		}
	}

	public void setStatus(PersonLifecycleService.PersonStatus status,boolean notifyMail) throws DAOValidationException {
		if(getAuthorityName().equals(ApplicationInfoList.getHomeRepository().getUsername())) {
			throw new DAOValidationException(
					new Exception("Method not allowed for the primary admin")
			);
		}
		String oldStatus= (String) userInfo.get(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS);
		NodeServiceFactory.getLocalService().setProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),getNodeId(),CCConstants.CM_PROP_PERSON_ESPERSONSTATUS,status.name(), false);
		NodeServiceFactory.getLocalService().setProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),getNodeId(),CCConstants.CM_PROP_PERSON_ESPERSONSTATUSDATE,new Date(), false);
		if(notifyMail){
			NotificationServiceFactoryUtility.getLocalService()
					.notifyPersonStatusChanged(
							(String) userInfo.get(CCConstants.CM_PROP_PERSON_EMAIL),
							getFirstName(),
							getLastName(),
							oldStatus,
							status.name());
		}
	}
}
