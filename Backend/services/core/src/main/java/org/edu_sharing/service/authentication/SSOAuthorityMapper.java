package org.edu_sharing.service.authentication;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.service.authentication.sso.config.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.PatternSyntaxException;

/**
 * 
 * @author rudi
 * 
 *         this class does the job of creating and updating users and organizing
 *         group membership in an sso context
 * 
 *         the use of this class requires an preciding sso authentication
 *         process that delivers user data and group memberships i.e.: -
 *         shibboleth serviceprovider - cas - edu-sharing auth by app
 * 
 *         sso data will be mapped. mapping can be configured in
 *         edu-sharing-sso-context.xml
 * 
 */
public class SSOAuthorityMapper {

	MappingRoot mappingConfig;

	ServiceRegistry serviceRegistry;
	AuthorityService authorityService;
	PersonService personService;
	MutableAuthenticationService authenticationService;
	TransactionService transactionService;
	//private MutableAuthenticationDao authenticationDao;
	
	OrganisationService organisationService;
	
	NodeService nodeService;
	GuestService guestService;

	Logger logger = Logger.getLogger(SSOAuthorityMapper.class);
	
	String organisationParam;
	
	String globalGroupsParam;

	/**
	 * AuthByApp trusted IP
	 */
	public static final String PARAM_APP_IP = "APP_IP";

	/**
	 * can be shibboleth or application(i.e. lms) session
	 */
	public static final String PARAM_SESSION_ID = "APP_SESSION_ID";

	/**
	 * AuthByApp trusted AppId
	 */
	public static final String PARAM_APP_ID = "APP_ID";

	/**
	 * SSO type Shibboleth, CAS, AuthByApp
	 */
	public static final String PARAM_SSO_TYPE = "SSO_TYPE";

	public static final String SSO_TYPE_Shibboleth = "shibboleth";

	public static final String SSO_TYPE_CAS = "cas";

	public static final String SSO_TYPE_AuthByApp = "AuthByApp";

	public static final String SSO_TYPE_LTI = "lti";

	public static final String SSO_REFERER = "SSO_REFERER";
	
	boolean createUser = true;
	boolean updateUser = true;

	boolean createGroups = true;
	boolean hashUserName = false;
	boolean hashGroupNames = false;
	boolean updateMemberships = true;
	boolean debug = false;
	String mappingGroupBuilderClass;

	/**
	 * prefer value for req.getRemoteUser() as username
	 */
	boolean preferRemoteUser = true;

	boolean authByAppCheckClientIp = true;

	CustomGroupMapping customGroupMapping;

	List<String> additionalAttributes = new ArrayList<>();

	Map<String,String> requiredAttributes;

	public void init(){
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();

		this.serviceRegistry = (ServiceRegistry) applicationContext.getBean("ServiceRegistry");
		this.authorityService = serviceRegistry.getAuthorityService();
		this.personService = serviceRegistry.getPersonService();
		this.authenticationService = serviceRegistry.getAuthenticationService();
		this.transactionService = serviceRegistry.getTransactionService();
		//this.authenticationDao = (MutableAuthenticationDao)applicationContext.getBean("authenticationDao");
		this.organisationService = (OrganisationService)applicationContext.getBean("eduOrganisationService");
		this.nodeService = serviceRegistry.getNodeService();
		this.guestService = applicationContext.getBean(GuestService.class);
		this.requiredAttributes = eduApplicationContext.containsBean ("personDataRequired") ? (Map<String,String>)eduApplicationContext.getBean("personDataRequired") : null;
	}

	public static String mapAdminAuthority(String authority,String appid){
		// when coming from the native app, do not scope
		if(ApplicationInfoList.getHomeRepository().getAppId().equals(appid)){
			return authority;
		}
		return AuthenticationUtil.runAsSystem(()-> {
			ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
			ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean("ServiceRegistry");

			boolean scope;
			// a new person, does not need to be scoped
			if (!serviceRegistry.getPersonService().personExists(authority)) {
				scope = false;
			} // the main user (admin) has to be scoped
			else if (authority.trim().equals(ApplicationInfoList.getHomeRepository().getUsername())) {
				scope = true;
			} // the user has to be scoped if he/she is an admin
			else {
				Set<String> memberships = serviceRegistry.getAuthorityService().getAuthoritiesForUser(authority);
				scope = memberships != null && memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
			}

			if (scope) {
				return authority + "@" + appid;
			} else {
				return authority;
			}
		});
	}

	/**
	 * @param ssoAttributes
	 * @return username: means the user exists(before or was created) null when
	 *         user does not exist and can not be created
	 */
	public String mapAuthority(final Map<String, String> ssoAttributes) {
		RunAsWork<String> runAs = new RunAsWork<String>() {
			@Override
			public String doWork() throws Exception {
				RetryingTransactionCallback<String> txnWork = new RetryingTransactionCallback<String>() {
					public String execute() throws Exception {
						return mapAuthorityInternal(ssoAttributes);
					}
				};
				return transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
			}
		};
		return AuthenticationUtil.runAsSystem(runAs);
	}
	
	
	private String mapAuthorityInternal(final Map<String, String> ssoAttributes) {
		
		if(isDebug()){
			for(Map.Entry<String,String> ssoAttribute : ssoAttributes.entrySet()){
				logger.info("sso attribute: " + ssoAttribute.getKey() + " value: " + ssoAttribute.getValue());
			}
		}

		String tmpUserName = ssoAttributes.get(getSSOUsernameProp());
		if (StringUtils.isBlank(tmpUserName)) {
			logErrorParams("userName", ssoAttributes);
			throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
		}

		//guest does not exsist in user store but exsist as a person, so user will not be found and trying to create person -> user already exsists
		if(guestService.isGuestUser(tmpUserName)){
			return tmpUserName;
		}

		String ssoType = ssoAttributes.get(PARAM_SSO_TYPE);
		if (ssoType == null) {
			logErrorParams(PARAM_SSO_TYPE, ssoAttributes);
			throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
		}


		if(requiredAttributes != null && requiredAttributes.size() > 0){

			requiredAttributes.keySet().stream().forEach(r -> {
				String value = ssoAttributes.get(r);
				if(value == null)
					throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
				try {
					if (!value.matches(requiredAttributes.get(r))) {
						logger.debug("required attribute " + r + " " + value +" not matches " +requiredAttributes.get(r)  );
						throw new AuthenticationException(AuthenticationExceptionMessages.SSO_REQ_ATT_NOT_MATCHES);
					}
				}catch (PatternSyntaxException e){
					logger.error("wrong required attribute pattern for:" + r + " pattern:"+requiredAttributes.get(r)+". " + e.getMessage());
					throw new AuthenticationException(AuthenticationExceptionMessages.SSO_REQ_ATT_PATTERN_SYNTAX);
				}
			});
		}

		String appId = ssoAttributes.get(PARAM_APP_ID);
		ApplicationInfo appInfo = (appId != null) ? ApplicationInfoList.getRepositoryInfoById(appId) : null;
		boolean whitelistedUser = false;
		if(SSO_TYPE_AuthByApp.equals(ssoType)) {
			String userWhiteList = appInfo.getAuthByAppUserWhitelist();
			if(userWhiteList != null && !userWhiteList.trim().equals("")){
				List<String> userList = Arrays.asList(userWhiteList.split(","));
				whitelistedUser = true;
				if(!userList.contains(tmpUserName)){
					throw new AuthenticationException(AuthenticationExceptionMessages.NOT_IN_WHITELIST);
				}
			}
		}

		if(SSO_TYPE_AuthByApp.equals(ssoType)) {
			tmpUserName = mapAdminAuthority(tmpUserName, ssoAttributes.get(PARAM_APP_ID));
		}
		

		
		final String originalUsername = tmpUserName;
		
		/**
		 * moodle hashes the username
		 */
		final String userName = (hashUserName && !ssoType.equals(SSO_TYPE_AuthByApp)) ? digest(tmpUserName) : tmpUserName;

		
		try {
			boolean createUser = isCreateUser();
			boolean updateUser = isUpdateUser();

			boolean createGroups = isCreateGroups();
			boolean hashGroupNames = isHashGroupNames();
			boolean updateMemberships = isUpdateMemberships();

			if (whitelistedUser) {
				createUser = false;
				updateUser = false;
				createGroups = false;
				hashGroupNames = false;
				updateMemberships = false;
			}

			/**
			 * SSO_TYPE_AuthByApp: need the Application type to
			 * decide if Crud Operations on user and group are
			 * allowed
			 */
			if (ssoType.equals(SSO_TYPE_AuthByApp)) {
				
				if (appId == null || appId.trim().equals("")) {
					logErrorParams(PARAM_APP_ID, ssoAttributes);
					throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
				}

				
				if (appInfo == null) {
					throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_APPLICATION);
				}
				
				if (ApplicationInfo.TYPE_RENDERSERVICE.equals(appInfo.getType())) {
					createUser = false;
					updateUser = false;
					createGroups = false;
					hashGroupNames = false;
					updateMemberships = false;
				}
			}
			
			boolean personExsists = false;
			
			personExsists = personService.personExists(userName);

			logger.debug("ut status:" + transactionService.getUserTransaction().getStatus());
			
			if (personExsists == false && !createUser) {
				logger.info("personExsists == null && !createUser -> returning null");
				return null;
			}

			// person
			Map<String, String> personMapping = mappingConfig.getPersonMapping();
			Map<QName, Serializable> personProperties = new HashMap<>();

			for (Map.Entry<String, String> ssoAttribute : ssoAttributes.entrySet()) {

				if (!personMapping.containsKey(ssoAttribute.getKey()) || personMapping.get(ssoAttribute.getKey()) == null
						|| personMapping.get(ssoAttribute.getKey()).trim().equals("")) {
					logger.debug("missing mapping entry for sso person attribute " + ssoAttribute.getKey());
					continue;
				}

				QName alfrescoProperty = QName.createQName(personMapping.get(ssoAttribute.getKey()));
				personProperties.put(alfrescoProperty, ssoAttribute.getValue());
			}

			if (personProperties.size() > 0) {
				
				if(mappingConfig.getPersonMappingCondition() != null && !mappingConfig.getPersonMappingCondition().isTrue(ssoAttributes)){
					logger.info("PersonMappingCondition is false for user:"+userName+". will not create.");
					return null;
				}

				//check active status
				if(personExsists){

					if(!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
						String personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
						NodeRef nodeRefPerson = personService.getPerson(userName);
						String personStatus = (String)nodeService.getProperty(nodeRefPerson,QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
						if(!personActiveStatus.equals(personStatus)){
							throw new AuthenticationException(AuthenticationExceptionMessages.USER_BLOCKED);
						}
					}

				}

				if (personExsists == false) {
					authenticationService.createAuthentication(userName, new KeyTool().getRandomPassword().toCharArray());
					//authenticationDao.createUser(userName, new KeyTool().getRandomPassword().toCharArray());

					// set username to the same we get from sso
					// context
					personProperties.put(ContentModel.PROP_USERNAME, userName);
					
					//so we can find out where the user comes from
					if(appInfo != null && ApplicationInfo.TYPE_REPOSITORY.equals(appInfo.getType())){
						personProperties.put(QName.createQName(CCConstants.PROP_USER_REPOSITORYID), appInfo.getAppId());
					}
					
					personProperties.put(QName.createQName(CCConstants.PROP_USER_ESSSOTYPE), ssoType);
					
					if(isHashUserName()) {
						personProperties.put(QName.createQName(CCConstants.CM_PROP_PERSON_ESORIGINALUID), originalUsername);
					}

					if(!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
						String personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
						//if configured initialize with active status
						if (personActiveStatus != null && !personActiveStatus.trim().isEmpty()) {
							personProperties.put(QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS), personActiveStatus);
						}
					}


					personService.createPerson(personProperties);
				} else if (updateUser) {

					//don't update the username (this lead to lowercase username when lowercase username comes with sso data
					personProperties.remove(ContentModel.PROP_USERNAME);
					personService.setPersonProperties(userName, personProperties);
				}

				
			} else {
				logger.warn("no personproperties delivered by sso context for user " + userName);
			}

			/**
			 * get the existent username, this can be different to the parameter username
			 * when this has another case(lower/upper) than the person object username
			 */
			NodeRef personNodeRef = personService.getPersonOrNull(userName);
			if(personNodeRef == null) {
				logger.error("person " + userName + " does not exist. can not create authentication object in userstore." );
				return null;
			}
			String existentUserName = (String)nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);

			if(!authenticationService.authenticationExists(existentUserName)){
				logger.info("no authentication object for " +userName +" trying to create!");
				authenticationService.createAuthentication(existentUserName, new KeyTool().getRandomPassword().toCharArray());
			}
			// group memberships
			List<MappingGroup> mappingGroups = new ArrayList<>(mappingConfig.getGroupMapping());
			
			/**
			 * add moodle global groups
			 */
			String lmsGlobalGroups = (globalGroupsParam != null) ? ssoAttributes.get(globalGroupsParam) : null;
			
			//only if organisationparam is configured
			String organisationName = (organisationParam  != null) ? ssoAttributes.get(organisationParam) : null;
			String organisationDisplayName = null;
			
			String existingOrganisationName = null;

			MappingGroupBuilder mappingGroupBuilder = null;
			if(mappingGroupBuilderClass != null && !mappingGroupBuilderClass.trim().equals("")) {
				mappingGroupBuilder = MappingGroupBuilderFactory.instance(ssoAttributes, mappingGroupBuilderClass);
				if(mappingGroupBuilder.getOrganisation() != null) {
					organisationName = mappingGroupBuilder.getOrganisation().getMapTo();
					if(organisationName != null) {
						organisationDisplayName = mappingGroupBuilder.getOrganisation().getMapToDisplayName();
						mappingGroups.addAll(mappingGroupBuilder.getMapTo());
					}
				}
			}

			if(customGroupMapping != null) {
				customGroupMapping.setSSOAuthorityMapper(SSOAuthorityMapper.this);
				customGroupMapping.map(ssoAttributes);
			}

			/**
			 * create eduGroup for affiliation
			 */
			if(organisationName != null && !organisationName.trim().equals("")) {
				
				if(organisationDisplayName == null) {
					organisationDisplayName = ssoAttributes.get(organisationParam + "name");
				}

				if(organisationDisplayName == null) {
					organisationDisplayName = organisationName;
				}
				
				Map<QName, Serializable> orgProps = organisationService.getOrganisation(organisationName);
				if(orgProps != null) {
					existingOrganisationName = (String)orgProps.get(ContentModel.PROP_AUTHORITY_NAME);
				}
				
				if(existingOrganisationName == null) {

					String metadataSetId = ssoType.equals(SSO_TYPE_Shibboleth) ? HttpContext.getCurrentMetadataSet() : null;

					existingOrganisationName = organisationService.createOrganization(organisationName, organisationDisplayName, metadataSetId,null);
					existingOrganisationName = AuthorityType.GROUP.getPrefixString() + existingOrganisationName;
				}else{
					String currentDisplayname = (String)orgProps.get(ContentModel.PROP_AUTHORITY_DISPLAY_NAME);
					if(currentDisplayname == null || !currentDisplayname.equals(organisationDisplayName)){
						authorityService.setAuthorityDisplayName((String)orgProps.get(ContentModel.PROP_AUTHORITY_NAME),organisationDisplayName);
					}
				}
				
				if (updateMemberships) {
					Set<String> userAuthorities = authorityService.getAuthoritiesForUser(userName);
					if(!userAuthorities.contains(existingOrganisationName)) {
						authorityService.addAuthority(existingOrganisationName, userName);
					}
				}
				
			}
			
			/**
			 * create LMS globalGroups
			 */
			organisationName = (organisationName == null) ? "" : organisationName;
			if(lmsGlobalGroups != null && !organisationName.trim().equals("")){
				JSONArray globalGroupsJA = new JSONArray(lmsGlobalGroups);
				
				Map<String,String> alfrescoNameLmsIdMap = new HashMap<>();
				
				List<MappingGroup> lmsGlobalGroupsList = new ArrayList<>();
				
				for(int i = 0; i < globalGroupsJA.length(); i++){
					JSONObject globalGroupJO = (JSONObject)globalGroupsJA.get(i);
					String id = globalGroupJO.getString("id");
					
					String name = globalGroupJO.getString("name");
					
					String groupName = organisationName + "_" + name;
					String groupDisplayName = name + " (" + organisationDisplayName + ")";
					
					alfrescoNameLmsIdMap.put(groupName,id);
					
					MappingGroup mappingGroup = new MappingGroup();
					mappingGroup.setCondition(new Condition() {
						@Override
						public boolean isTrue(Map<String, String> ssoAttributes) {
							return true;
						}
					});
					
					mappingGroup.setMapTo(groupName);
					mappingGroup.setMapToDisplayName(groupDisplayName);
					
					lmsGlobalGroupsList.add(mappingGroup);
				}
				

				mappingGroups.addAll(lmsGlobalGroupsList);
			}
			
			
			if (mappingGroups != null) {

				List<String> currentGroupsForUser = new ArrayList<>();
				for (MappingGroup mappingGroup : mappingGroups) {
					
					String groupName = mappingGroup.getMapTo();
					String alfrescoGroupName = AuthorityType.GROUP.getPrefixString() + groupName;
					if (hashGroupNames) {
						groupName = digest(groupName);
					}

					if (groupName == null || groupName.trim().equals("")) {
						logger.error("alfresco groupName is null or length 0");
						continue;
					}
					
					if (mappingGroup.getCondition().isTrue(ssoAttributes)) {

						currentGroupsForUser.add(alfrescoGroupName);

						if (createGroups && !authorityService.authorityExists(AuthorityType.GROUP.getPrefixString() + groupName)) {
							String newGroupName = authorityService.createAuthority(AuthorityType.GROUP, mappingGroup.getMapTo(), mappingGroup.getMapToDisplayName(), authorityService.getDefaultZones());
							NodeRef nodeRef = authorityService.getAuthorityNodeRef(newGroupName);
							
							Map<QName,Serializable> aspectProperties = new HashMap<>();
							aspectProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPSOURCE), appId);
							nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_GROUPEXTENSION), aspectProperties);
						}

						if (updateMemberships) {
							
							if (authorityService.authorityExists(alfrescoGroupName)) {

								Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
								if (!authoritiesForUser.contains(alfrescoGroupName)) {
									logger.debug("will add "+userName +" in "+alfrescoGroupName);
									authorityService.addAuthority(alfrescoGroupName, userName);
								}
							} else {
								logger.error("Authority " + groupName + " does not exist!");
							}
						}

					}  else {
						
						/**
						 * remove memberships for group mapping defined in edu-sharing-sso-context.xml
						 */
						if (updateMemberships) {
							logger.debug("condition for alfresco group " + mappingGroup.getMapTo() + " does not match will remove membership if exists");
							Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
							if (authoritiesForUser.contains(alfrescoGroupName)) {
								logger.debug("will remove "+userName +" from "+alfrescoGroupName);
								authorityService.removeAuthority(alfrescoGroupName, userName);
							}
						}
					}
				}
				
				/**
				 * removeuser from groups that came from the lms or other application but the user is no longer in
				 */
				if(updateMemberships) {
					Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
					for(String authorityForUser : authoritiesForUser) {
						NodeRef groupNodeRef = authorityService.getAuthorityNodeRef(authorityForUser);
						if(appId == null || groupNodeRef == null) {
							continue;
						}
						String groupSource = (String)nodeService.getProperty(groupNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPSOURCE));
						
						if(groupSource != null && !groupSource.trim().isEmpty() && appId.equals(groupSource)) {
							if(!currentGroupsForUser.contains(authorityForUser)) {
								authorityService.removeAuthority(authorityForUser, userName);
							}
						}
						
					}
				}
				
				//handle parent group
				for (MappingGroup mappingGroup : mappingGroups) {
					if(mappingGroup.getCondition().isTrue(ssoAttributes)) {
						if(mappingGroup.getParentGroup() != null && !mappingGroup.getParentGroup().trim().equals("")){
							logger.debug("checking if " + mappingGroup.getMapTo() +  " is in " + mappingGroup.getParentGroup());
							Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, AuthorityType.GROUP.getPrefixString() + mappingGroup.getParentGroup() , false);
							if(!containedAuthorities.contains(AuthorityType.GROUP.getPrefixString() + mappingGroup.getMapTo())){
								logger.info("adding:" + mappingGroup.getMapTo()  + " to:" +   mappingGroup.getParentGroup());
								authorityService.addAuthority(AuthorityType.GROUP.getPrefixString() + mappingGroup.getParentGroup(), AuthorityType.GROUP.getPrefixString() + mappingGroup.getMapTo());
							}
						}else {
							/**
							 * add to organization
							 */
							if(existingOrganisationName != null) {
								logger.debug("checking if: " + mappingGroup.getMapTo() +  " is in org: " + existingOrganisationName);
								Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, existingOrganisationName , false);
								if(containedAuthorities != null && !containedAuthorities.contains(AuthorityType.GROUP.getPrefixString() + mappingGroup.getMapTo())) {
									logger.info("adding:" + mappingGroup.getMapTo() + " to:" + existingOrganisationName );
									authorityService.addAuthority(existingOrganisationName, AuthorityType.GROUP.getPrefixString() +  mappingGroup.getMapTo());
								}
							}
						}
					}
				}

			}
			
			if (ssoType.equals(SSO_TYPE_AuthByApp) 
					&& appInfo.getType().equals(ApplicationInfo.TYPE_REPOSITORY) 
					&& !ApplicationInfoList.getHomeRepository().getAppId().equals(appInfo.getAppId())) {
			
				//edu-sharing federated global groups
				String gg = ssoAttributes.get(CCConstants.EDU_SHARING_GLOBAL_GROUPS);
				List<String> globalGroupsMembership = (gg != null && !gg.trim().equals("")) ? Arrays.asList(gg.split(";")) : new ArrayList<>();
				
				//remove user from global group
				Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
				for (String authority : authoritiesForUser) {
					NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authority);
					//i.e. EVERYONE is null
					if(authorityNodeRef == null) continue;
					String scopeType = (String)nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE));
				
					if(CCConstants.CCM_VALUE_SCOPETYPE_GLOBAL.equals(scopeType) && !globalGroupsMembership.contains(authority)){
						authorityService.removeAuthority(authority, userName);
					}
				}
				
				//add user to edu-sharing global groups
				for (String globalGroup : globalGroupsMembership) {
					if(authorityService.authorityExists(globalGroup)){
						NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(globalGroup);
						if(authorityNodeRef == null) continue;
						String scopeType = (String)nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE));
				
						if(CCConstants.CCM_VALUE_SCOPETYPE_GLOBAL.equals(scopeType) && !authoritiesForUser.contains(globalGroup)){
							authorityService.addAuthority(globalGroup, userName);
						}
					}
				}
				
			}
			return userName;

		}catch(AuthenticationException e){
			throw e;
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	public void setMappingConfig(MappingRoot mappingConfig) {
		this.mappingConfig = mappingConfig;
	}

	public String digest(String str) {

		String hashtext = null;

		try {

			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(str.getBytes());
			byte[] digest = m.digest();

			hashtext = new BigInteger(1, digest).toString(16);

			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return hashtext;
	}

	public String getSSOUsernameProp() {
		return getUserAttribute(CCConstants.CM_PROP_PERSON_USERNAME);
	}

	public String getUserAttribute(String alfrescoUserAtt){
		String result = null;
		for (Map.Entry<String, String> entry : mappingConfig.getPersonMapping().entrySet()) {
			if (entry.getValue().equals(alfrescoUserAtt)) {
				result = entry.getKey();
			}
		}
		return result;
	}

	private void logErrorParams(String missing, Map<String, String> ssoAttributes) {
		String logString = "";
		for (Map.Entry<String, String> entry : ssoAttributes.entrySet()) {
			logString += entry.getKey() + ": " + entry.getValue() + "; ";
		}
		logger.error("missing:" + missing + " got:(" + logString + ")");
	}

	public MappingRoot getMappingConfig() {
		return mappingConfig;
	}
	
	public boolean isCreateUser() {
		return createUser;
	}

	public void setCreateUser(boolean createUser) {
		this.createUser = createUser;
	}

	public boolean isUpdateUser() {
		return updateUser;
	}

	public void setUpdateUser(boolean updateUser) {
		this.updateUser = updateUser;
	}

	public boolean isCreateGroups() {
		return createGroups;
	}

	public void setCreateGroups(boolean createGroups) {
		this.createGroups = createGroups;
	}

	public boolean isHashGroupNames() {
		return hashGroupNames;
	}

	public void setHashGroupNames(boolean hashGroupNames) {
		this.hashGroupNames = hashGroupNames;
	}

	public boolean isHashUserName() {
		return hashUserName;
	}

	public void setHashUserName(boolean hashUserName) {
		this.hashUserName = hashUserName;
	}

	public boolean isUpdateMemberships() {
		return updateMemberships;
	}

	public void setUpdateMemberships(boolean updateMemberships) {
		this.updateMemberships = updateMemberships;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public boolean isDebug() {
		return debug;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setOrganisationParam(String organisationParam) {
		this.organisationParam = organisationParam;
	}

	public void setGlobalGroupsParam(String globalGroupsParam) {
		this.globalGroupsParam = globalGroupsParam;
	}

	public void setMappingGroupBuilderClass(String mappingGroupBuilderClass) {
		this.mappingGroupBuilderClass = mappingGroupBuilderClass;
	}

	public String getMappingGroupBuilderClass() {
		return mappingGroupBuilderClass;
	}

	public void setCustomGroupMapping(CustomGroupMapping customGroupMapping) {
		this.customGroupMapping = customGroupMapping;
	}

	public boolean isPreferRemoteUser() {
		return preferRemoteUser;
	}

	public void setPreferRemoteUser(boolean preferRemoteUser) {
		this.preferRemoteUser = preferRemoteUser;
	}

	public void setAuthByAppCheckClientIp(boolean authByAppCheckClientIp) {
		this.authByAppCheckClientIp = authByAppCheckClientIp;
	}

	public boolean isAuthByAppCheckClientIp() {
		return authByAppCheckClientIp;
	}
}
