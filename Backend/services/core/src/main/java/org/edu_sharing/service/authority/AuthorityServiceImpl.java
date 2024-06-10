package org.edu_sharing.service.authority;

import jakarta.transaction.UserTransaction;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.GuestCagePolicy;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.PropertyRequiredException;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.repository.server.tools.cache.UserCache;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class AuthorityServiceImpl implements AuthorityService {

	Logger logger = Logger.getLogger(AuthorityServiceImpl.class);

	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	UserCache userCache = alfApplicationContext.getBean(UserCache.class);
	org.alfresco.service.cmr.security.AuthorityService authorityService = serviceRegistry.getAuthorityService();
	NodeService nodeService = serviceRegistry.getNodeService();
	OwnableService ownableService = serviceRegistry.getOwnableService();
	PermissionService permissionService = serviceRegistry.getPermissionService();
    org.edu_sharing.alfresco.service.AuthorityService eduAuthorityService = (org.edu_sharing.alfresco.service.AuthorityService)alfApplicationContext.getBean("eduAuthorityService");



    /**
	 * Returns a property for a certain authority (it will fetch the coressponding node and load the property)
	 * @param authority
	 * @param property
	 * @return
	 */
	@Override
	public Object getAuthorityProperty(String authority,String property){
		NodeRef ref = authorityService.getAuthorityNodeRef(authority);
		if(ref==null)
			return null;
		return nodeService.getProperty(ref,QName.createQName(property));
	}
	/**
	 * returns the node id for the given authority (useful if you want to change metadata)
	 * @param authority
	 * @return
	 */
	@Override
	public NodeRef getAuthorityNodeRef(String authority){
		return authorityService.getAuthorityNodeRef(authority);
	}
	@Override
	public void setAuthorityProperty(String authority,String property,Serializable value){
		if(value==null){
			nodeService.removeProperty(authorityService.getAuthorityNodeRef(authority),QName.createQName(property));
		}else {
			nodeService.setProperty(authorityService.getAuthorityNodeRef(authority),QName.createQName(property),value);
		}
	}
	@Override
	public void addAuthorityAspect(String authority,String aspect){

		NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
		if(!nodeService.hasAspect(nodeRef, QName.createQName(aspect))) {
			nodeService.addAspect(nodeRef,QName.createQName(aspect),new HashMap<>());
		}
	}
	@Override
	public void deleteAuthority(String authorityName) {
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

                new RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                		String key =  authorityName;
                		String groupType = (String) getAuthorityProperty(key,CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
						if(authorityName.equals(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)) {
							throw new AccessDeniedException("Not allowed to delete group " + CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
						}
                		if(groupType!=null && groupType.equals(CCConstants.ADMINISTRATORS_GROUP_TYPE) && !new MCAlfrescoAPIClient().isAdmin(AuthenticationUtil.getFullyAuthenticatedUser()))
                			throw new AccessDeniedException("An admin group can not be deleted");
                		authorityService.deleteAuthority(key, true);

                		return null;
                    }
                }, false);
	}

	@Override
	public synchronized boolean hasModifyAccessToGroup(String groupName){
    	Set<String> memberships=serviceRegistry.getAuthorityService().getAuthorities();
    	if(AuthorityServiceHelper.isAdmin()) {
			return true;
		}
		if(!LightbendConfigLoader.get().getBoolean("repository.organizations.admins.canManage")) {
			return false;
		}
				// Detect the group prefix and decide
    	String[] split=groupName.split("_");
    	if(split.length<2)
    		return false;
    	String adminGroup=PermissionService.GROUP_PREFIX+split[0]+"_"+org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP;
    	if(memberships.contains(adminGroup)) {
			return isAdminGroup(adminGroup);
		}
    	return false;
	}

	private boolean isAdminGroup(String group) {
		return groupIsOfType(group, org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP_TYPE) ||
				groupIsOfType(group, org.edu_sharing.alfresco.service.AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE);
	}

	private boolean groupIsOfType(String adminGroup, String type) {
		String typeProp=(String) serviceRegistry.getNodeService().getProperty(serviceRegistry.getAuthorityService().getAuthorityNodeRef(adminGroup),QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
    	return typeProp.equals(type);
	}
	@Override
	public boolean isGuest() {
		try {
			return GuestCagePolicy.getGuestUsers().contains(AuthenticationUtil.getFullyAuthenticatedUser());
		} catch (Throwable e) {
		}
		return false;
	}
    private synchronized boolean hasAdminAccessToGroup(String groupName,String postfix){
		try {
			// strip the group prefix, if present
			if(groupName.startsWith(PermissionService.GROUP_PREFIX)){
				groupName = groupName.substring(PermissionService.GROUP_PREFIX.length());
			}
			if(AuthenticationUtil.isRunAsUserTheSystemUser()){
				return true;
			}
			Set<String> admins = serviceRegistry.getAuthorityService().getContainedAuthorities(AuthorityType.USER, CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS,false);
			if(admins.contains(AuthenticationUtil.getRunAsUser())){
				return true;
			}
			Set<String> memberships=serviceRegistry.getAuthorityService().getAuthorities();
			String group=PermissionService.GROUP_PREFIX+AuthorityService.getGroupName(postfix,groupName);
			if(memberships.contains(group))
				return isAdminGroup(group);

			// Detect the group prefix and decide
			String[] split=groupName.split("_");
			if(split.length<2)
				return false;
			group=PermissionService.GROUP_PREFIX+split[0]+"_"+postfix;
			if(memberships.contains(group))
				return isAdminGroup(group);

		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}
	@Override
	public boolean hasAdminAccessToGroup(String groupName){
		if(AuthenticationUtil.isRunAsUserTheSystemUser() || AuthorityServiceHelper.isAdmin()){
			return true;
		}
		return
				LightbendConfigLoader.get().getBoolean("repository.organizations.admins.canManage") &&
				hasAdminAccessToGroup(groupName,org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP);
	}
	@Override
	public boolean hasAdminAccessToMediacenter(String groupName){
		return hasAdminAccessToGroup(groupName,org.edu_sharing.alfresco.service.AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP);
	}
	public String getProperty(String authorityName, String property){
		return (String)nodeService.getProperty(authorityService.getAuthorityNodeRef(authorityName),QName.createQName(property));
	}
	@Override
	public synchronized boolean hasAdminAccessToOrganization(String orgName){
		try {
	    	Set<String> memberships=serviceRegistry.getAuthorityService().getAuthorities();
			if(AuthorityServiceHelper.isAdmin()) {
				return true;
			}
			if(!LightbendConfigLoader.get().getBoolean("repository.organizations.admins.canManage")) {
				return false;
			}


			String group=PermissionService.GROUP_PREFIX+AuthorityService.getGroupName(org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP,orgName);
			if(memberships.contains(group))
				return groupIsOfType(group,CCConstants.ADMINISTRATORS_GROUP_TYPE);

		} catch (Throwable t) {
			logger.error("Error while getting Admin access:" + orgName, t);
		}
		return false;
	}

	@Override
	public boolean isGlobalAdmin() {
		try{
			String user = AuthenticationUtil.getFullyAuthenticatedUser();
			if("admin".equals(user)){
				return true;
			}
			if(AuthenticationUtil.isRunAsUserTheSystemUser()){
				return true;
			}
			return getMemberships(user).contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
		}catch(Throwable t){
			return false;
		}
	}

	@Override
	public ArrayList<EduGroup> getAllEduGroups(String authority) {
		org.alfresco.service.cmr.security.AuthorityService alfAuthorityService = serviceRegistry.getAuthorityService();
		Set<String> authoritiesForUser = alfAuthorityService
				.getAuthoritiesForUser(authority);

		
		
		ArrayList<EduGroup> result = new ArrayList<>();

		for (String a : authoritiesForUser) {
			EduGroup eg = getEduGroup(a);
			if(eg != null)result.add(eg);
		}

		return result;
	}
	
@Override	
public EduGroup getEduGroup(String authority){
		if(!authority.startsWith(PermissionService.GROUP_PREFIX)){
			authority = PermissionService.GROUP_PREFIX + authority;
		}
		NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
		if(nodeRef == null){
			return null;
		}
		
		NodeRef nodeRefEduGroupHomeDir = (NodeRef) nodeService.getProperty(nodeRef,
				QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
		if (nodeRefEduGroupHomeDir != null) {
	
			Map<QName, Serializable> folderProps = nodeService.getProperties(nodeRefEduGroupHomeDir);
			EduGroup eduGroup = new EduGroup();
			eduGroup.setFolderId((String) folderProps.get(QName.createQName(CCConstants.SYS_PROP_NODE_UID)));
			eduGroup.setFolderName((String) folderProps.get(QName.createQName(CCConstants.CM_NAME)));
	
			Map<QName, Serializable> groupProps = nodeService.getProperties(nodeRef);
	
			eduGroup.setGroupId((String) groupProps.get(QName.createQName(CCConstants.SYS_PROP_NODE_UID)));
			eduGroup.setGroupname(
					(String) groupProps.get(QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME)));
			eduGroup.setGroupDisplayName(
					(String) groupProps.get(QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME)));
			eduGroup.setFolderPath(nodeService.getPath(nodeRefEduGroupHomeDir)
					.toPrefixString(serviceRegistry.getNamespaceService()));
			eduGroup.setScope((String) groupProps.get(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME)));
			
			return eduGroup;
		}
		
		return null;
	}

	@Override
	public ArrayList<EduGroup> getEduGroups() {
		String currentScope = NodeServiceInterceptor.getEduSharingScope();
		return getEduGroups(currentScope);
	}

	@Override
	public ArrayList<EduGroup> getEduGroups(String authority,String scope) {
		ArrayList<EduGroup> result = new ArrayList<>();

		for (EduGroup eduGroup : getAllEduGroups(authority)) {
			if ((eduGroup.getScope() == null && scope == null)
					|| (eduGroup.getScope() != null && eduGroup.getScope().equals(scope))) {
				result.add(eduGroup);
			}
		}
		return result;
	}
	@Override
	public Set<String> getMemberships(String username) {
		if(AuthenticationUtil.getFullyAuthenticatedUser() != null && AuthenticationUtil.getFullyAuthenticatedUser().equals(username)){
			return serviceRegistry.getAuthorityService().getAuthorities();
		}else {
			return serviceRegistry.getAuthorityService().getAuthoritiesForUser(username);
		}
	}
	@Override
	public String createGroup(String groupName, String displayName,String parentGroup) throws Exception {
		if(parentGroup!=null && parentGroup.isEmpty())
			parentGroup=null;

		if(parentGroup==null){
			if(!isGlobalAdmin())
				throw new AccessDeniedException("No permission to create global group");
		}
		else if(!hasAdminAccessToGroup(parentGroup)){
			throw new AccessDeniedException("No permission to create group in "+parentGroup);
		}
		final String parentGroupFinal=parentGroup;
		return AuthenticationUtil.runAsSystem(new RunAsWork<String>() {

			@Override
			public String doWork() throws Exception {
				return new MCAlfrescoAPIClient().createOrUpdateGroup(groupName, displayName,parentGroupFinal,true);
			}
		});
	}

	@Override
	public void createGroupWithType(String groupName, String displayName,String parentGroup,String groupType) throws Exception {
		String group = createGroup(groupName, displayName, parentGroup);
		addAuthorityAspect(PermissionService.GROUP_PREFIX + group,CCConstants.CCM_ASPECT_GROUPEXTENSION);
		setAuthorityProperty(PermissionService.GROUP_PREFIX + group,CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE, groupType);

	}

	public EduGroup getOrCreateEduGroup(EduGroup eduGroup, EduGroup unscopedEduGroup, String folderParentId) {

		AuthenticationUtil.RunAsWork<EduGroup> createEduGroupWorker = new AuthenticationUtil.RunAsWork<EduGroup>() {
			@Override
			public EduGroup doWork() throws Exception {
				ReentrantLock lock = new ReentrantLock();
				lock.lock();
				UserTransaction transaction = serviceRegistry.getTransactionService()
						.getNonPropagatingUserTransaction(false);

			
				if (!authorityService.getAllAuthorities(AuthorityType.GROUP).contains(eduGroup.getGroupname())) {
					try {
						transaction.begin();

						String authority = authorityService.createAuthority(AuthorityType.GROUP,
								eduGroup.getGroupname().replaceAll("GROUP_", ""));
						NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
						nodeService.setProperty(nodeRef,
								QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME),
								eduGroup.getGroupDisplayName());

						// scope
						Map<QName, Serializable> propsAspectEduScope = new HashMap<>();
						propsAspectEduScope.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
								eduGroup.getScope());
						nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE),
								propsAspectEduScope);

						String eduGroupHomeFolderId = eduGroup.getFolderId();
						if (eduGroupHomeFolderId == null) {
							String folderName = eduGroup.getFolderName();
							if (folderName == null) {
								folderName = eduGroup.getGroupname().replace(PermissionService.GROUP_PREFIX,"");
								if (eduGroup.getScope() != null) {
									folderName = folderName + "_" + eduGroup.getScope();
								}
							}
							folderName = NodeServiceHelper.cleanupCmName(folderName);
							Map<QName, Serializable> folderProps = new HashMap<>();
							folderProps.put(ContentModel.PROP_NAME, folderName);

							String assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + folderName;
							ChildAssociationRef newNode = nodeService.createNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, folderParentId),
									ContentModel.ASSOC_CONTAINS, QName.createQName(assocName),
									QName.createQName(CCConstants.CCM_TYPE_MAP), folderProps);

							nodeService.addAspect(newNode.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE),
									propsAspectEduScope);
							ownableService.setOwner(newNode.getChildRef(), AuthenticationUtil.getRunAsUser());
							serviceRegistry.getPermissionService().setPermission(newNode.getChildRef(), eduGroup.getGroupname(), PermissionService.READ, true);
							serviceRegistry.getPermissionService().setInheritParentPermissions(newNode.getChildRef(), false);
							eduGroupHomeFolderId = newNode.getChildRef().getId();
						}

						// edugroup aspect
						Map<QName, Serializable> propsAspectEduGroup = new HashMap<>();
						propsAspectEduGroup.put(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR),
								new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduGroupHomeFolderId));
						nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP),
								propsAspectEduGroup);


						//copy ORG_ADMINISTRATORS
						if(unscopedEduGroup != null && eduGroup.getScope() != null){
							Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, unscopedEduGroup.getGroupname(), true);
							for(String containedAuthority : containedAuthorities){
								NodeRef containedAuthorityNodeRef = authorityService.getAuthorityNodeRef(containedAuthority);
								String groupType = (String)nodeService.getProperty(containedAuthorityNodeRef,QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
								if(CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(groupType)){
									String groupname = (String)nodeService.getProperty(containedAuthorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
									groupname = groupname.replace(AuthorityType.GROUP.getPrefixString(), "");
									String groupDisplayName = (String)nodeService.getProperty(containedAuthorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
									String groupAdministrators = authorityService.createAuthority(AuthorityType.GROUP, groupname + "_" + eduGroup.getScope() , groupDisplayName + "_"  + eduGroup.getScope(), authorityService.getDefaultZones());
									NodeRef groupAdministratorsNodeRef = authorityService.getAuthorityNodeRef(groupAdministrators);
									nodeService.setProperty(groupAdministratorsNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.ADMINISTRATORS_GROUP_TYPE);
									//scope
									nodeService.addAspect(groupAdministratorsNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), propsAspectEduScope);

									authorityService.addAuthority(eduGroup.getGroupname(), groupAdministrators);
									permissionService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,eduGroupHomeFolderId), groupAdministrators, CCConstants.PERMISSION_ES_CHILD_MANAGER, true);
								}
							}
						}



					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
						try {
							transaction.rollback();
						} catch (Exception e2) {
							logger.error(e.getMessage(), e2);
						}
					}
					transaction.commit();
				}

				lock.unlock();

				return getEduGroup(eduGroup.getGroupname());
			}
		};

		return AuthenticationUtil.runAs(createEduGroupWorker, ApplicationInfoList.getHomeRepository().getUsername());

	}

	@Override
	public boolean authorityExists(String authority) {
		return authorityService.authorityExists(authority);
	}
	/**
	 * returns null when user not exists
	 */
	@Override
	public Map<String, Serializable> getUserInfo(String userName) throws Exception {
		User user = getUser(userName);
		if(user == null) return null;
		return user.getProperties();
	}

	@Override
	public User getUser(String userName) {
		return userCache.getUser(userName);
	}

	@Override
		public void createOrUpdateUser(Map<String, Serializable> userInfo) throws Exception {

			String currentUser = AuthenticationUtil.getRunAsUser();

			if(userInfo == null){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
			}

			String userName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_USERNAME);
			String firstName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
			String lastName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
			String email = (String) userInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);

			if(userName == null || userName.trim().equals("")){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
			}

			if(firstName == null || firstName.trim().equals("")){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_FIRSTNAME);
			}

			if(lastName == null || lastName.trim().equals("")){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_LASTNAME);
			}

			if(email == null || email.trim().equals("")){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_EMAIL);
			}

			if (!currentUser.equals(userName) && !AuthorityServiceFactory.getLocalService().isGlobalAdmin()) {
				throw new NotAnAdminException();
			}
			if(!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
				String personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
				//if configured initialize with active status
				if (personActiveStatus != null) {
					userInfo.put(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS, personActiveStatus);
				}
			}

			if(!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
				String personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
				//if configured initialize with active status
				if (personActiveStatus != null && !personActiveStatus.trim().isEmpty()) {
					userInfo.put(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS, personActiveStatus);
				}
			}

			PersonService personService = serviceRegistry.getPersonService();

			serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

		        new RetryingTransactionCallback<Void>()
		        {
		            public Void execute() throws Throwable
		            {
		        		Throwable runAs = AuthenticationUtil.runAs(

		    				new AuthenticationUtil.RunAsWork<Throwable>() {

		    					@Override
		    					public Throwable doWork() throws Exception {

		    						try {

		    	                    	if (personService.personExists(userName)) {

		    	                			personService.setPersonProperties(userName, transformQName(userInfo));

		    	                		} else {

		    	                			personService.createPerson(transformQName(userInfo));
		    	                		}
		    	                    	addUserExtensionAspect(userName);

		    						} catch (Throwable e) {
		    							logger.error(e.getMessage(), e);
		    							return e;
		    						}

		    						return null;
		    					}

		    				},
		    				ApplicationInfoList.getHomeRepository().getUsername());

		        		if (runAs != null) {
		        			throw runAs;
		        		}

		        		return null;
		            }

		        },
		        false);
		}
		private void addUserExtensionAspect(String userName) {
			PersonService personService = serviceRegistry.getPersonService();
			if(!nodeService.hasAspect(personService.getPerson(userName),QName.createQName(CCConstants.CCM_ASPECT_USER_EXTENSION)))
					nodeService.addAspect(personService.getPerson(userName),QName.createQName(CCConstants.CCM_ASPECT_USER_EXTENSION),null);
		}
		private Map<QName, Serializable> transformQName(Map<String, Serializable> data) {
			Map<QName, Serializable> transformed = new HashMap<>();
			for(String key : data.keySet()) {
				transformed.put(QName.createQName(key), data.get(key));
			}
			return transformed;
		}

	public String[] getMembershipsOfGroup(String groupName) {

		org.alfresco.service.cmr.security.AuthorityService authorityService = serviceRegistry.getAuthorityService();

		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

				new RetryingTransactionCallback<String[]>()
				{
					public String[] execute() throws Throwable
					{
						String key = groupName.startsWith(PermissionService.GROUP_PREFIX) ? groupName : PermissionService.GROUP_PREFIX + groupName;

						return authorityService.getContainedAuthorities(null, key, true).toArray(new String[0]);
					}
				}, true);

	}

    @Override
	public void addMemberships(String groupName, String[] members) {

		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

				new RetryingTransactionCallback<Void>()
				{
					public Void execute() throws Throwable
					{
						eduAuthorityService.addMemberships(groupName, members);
						return null;
					}
				}, false);

	}

	@Override
	public void removeMemberships(String groupName, String[] members) {

		org.alfresco.service.cmr.security.AuthorityService authorityService = serviceRegistry.getAuthorityService();

		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

				new RetryingTransactionCallback<Void>()
				{
					public Void execute() throws Throwable
					{
						String key = groupName.startsWith(PermissionService.GROUP_PREFIX) ? groupName : PermissionService.GROUP_PREFIX + groupName;

						for (String member : members) {

							if (member == null) {
								continue;
							}

							authorityService.removeAuthority(key, member);
						}

						return null;
					}
				}, false);

	}

	@Override
	public Map<String, Serializable> getProfileSettingsProperties(String userName, String profileSettingsProperty) {
		String user = userName;
		//check if userName exist, if not get login USER
		if (user == null)
			user = AuthenticationUtil.getFullyAuthenticatedUser();

		User userObj = this.getUser(user);
		Map<String, Serializable> profileSettings = userObj.getProfileSettings();
		if(profileSettings == null){
			profileSettings = new HashMap<>();
			NodeRef personRef = serviceRegistry.getPersonService().getPerson(user, false);
			for (String property : CCConstants.getAllPropertiesOfProfileSettings()) {
				profileSettings.put(property, serviceRegistry.getNodeService().getProperty(personRef, QName.createQName(property)));
			}
			userObj.setProfileSettings(profileSettings);
			userCache.put(user,userObj);
		}

		Map<String, Serializable> result = new HashMap<>();

		List<String> properties = new ArrayList<>();// ProfileSettings property to return
		// If profileSettingsProperty==null than  Get all Properties for ProfileSettings
		if (profileSettingsProperty == null)
			properties = CCConstants.getAllPropertiesOfProfileSettings();
		else
			properties.add(profileSettingsProperty);

		for(String property : properties){
			Serializable profileSetting = userObj.getProfileSettings().get(property);
			if(profileSetting != null){
				result.put(property,profileSetting);
			}
		}

		return result;
	}

	@Override
	public String getSubgroupByType(String parentGroup, String groupType){
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
			Optional<String> first = authorityService.getContainedAuthorities(AuthorityType.GROUP, parentGroup, true).stream().filter(
					(g) -> groupType.equals(nodeService.getProperty(getAuthorityNodeRef(g), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE))
					)
			).findFirst();
			return first.orElse(null);
		});
	}
	public void createProxyUser(){
		PersonService personService = serviceRegistry.getPersonService();

		if(personService.personExists(CCConstants.PROXY_USER)){
			return;
		}

		Map<QName, Serializable> properties = new HashMap<>();
		properties.put(ContentModel.PROP_USERNAME, CCConstants.PROXY_USER);
		personService.createPerson(properties);
		serviceRegistry.getAuthenticationService()
				.createAuthentication(CCConstants.PROXY_USER,new KeyTool().getRandomPassword().toCharArray());
	}


}
