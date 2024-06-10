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
package org.edu_sharing.repository.server.tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.webservice.authentication.AuthenticationFault;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.login.v1.model.AuthenticationToken;
import org.edu_sharing.restservices.shared.UserProfileAppAuth;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.springframework.context.ApplicationContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class AuthenticatorRemoteRepository {
	
	private static Log logger = LogFactory.getLog(AuthenticatorRemoteRepository.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
	
	SSOAuthorityMapper ssoAuthorityMapper = (SSOAuthorityMapper)eduApplicationContext.getBean("ssoAuthorityMapper");
	
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	/**
	 * authenticates at remote app with actual local userdata, if fails an guest ticket and the exception message will be returned 
	 * @return AuthenticatorRemoteAppResult
	 */
	public AuthenticatorRemoteAppResult getAuthInfoForApp(String username, ApplicationInfo remoteAppInfo) throws Throwable{

		Map<String,String> resultAuthInfo = new HashMap<>();
		MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

		AuthenticationToken authToken = null;
		if(remoteAppInfo.getString("forced_user",null)!=null){
			logger.info("forced_user is set for remote, will authenticate as the specified user");
			try{
				authToken = remoteAuth(remoteAppInfo.getAppId(), remoteAppInfo.getString("forced_user",null));
			}catch(Exception e){
				logger.info("Remote repository "+remoteAppInfo.getAppId()+" auth failed (check the remote repo log for more details) "+e.getMessage());
				throw e;
			}
		} else {
			logger.info("getting userinfo for" + username);
			try {
				authToken = remoteAuth(remoteAppInfo.getAppId(), username);
			}catch(Exception e){
				logger.info("REMOTE REPOSITORY AUTH FAILED: "+e.getMessage());
				throw e;
			}
		}
		//TODO if exception repository unreachable -> special handling
		logger.info("REMOTE APPID:"+ remoteAppInfo.getAppId() +"REMOTE USERNAME:"+authToken.getUserId()+" REMOTETICKET:"+authToken.getTicket()) ;
		resultAuthInfo.put(CCConstants.AUTH_USERNAME,authToken.getUserId());
		resultAuthInfo.put(CCConstants.AUTH_TICKET,authToken.getTicket());
		AuthenticatorRemoteAppResult result = new AuthenticatorRemoteAppResult();
		result.setAuthenticationInfo(resultAuthInfo);
		logger.info("REMOTE USERNAME2:"+resultAuthInfo.get(CCConstants.AUTH_USERNAME)+" REMOTETICKET:"+resultAuthInfo.get(CCConstants.AUTH_TICKET)) ;
		return result;
	}

	private AuthenticationToken remoteAuth(String appId, String username) throws Exception{
		ApplicationInfo appInfoRemoteApp = ApplicationInfoList.getRepositoryInfoById(appId);

		String localAppId = ApplicationInfoList.getHomeRepository().getAppId();
		logger.info("startSession remoteApplicationId:"+appId +" localAppId:"+localAppId);


		Map<String,String> personMapping = new HashMap<>(ssoAuthorityMapper.getMappingConfig().getPersonMapping());
		String remoteUserid = ApplicationInfoList.getRepositoryInfoById(appId).getString(ApplicationInfo.REMOTE_USERID, null);
		if(remoteUserid!=null && !remoteUserid.isEmpty()){
			logger.info("remote_userid configured "+remoteUserid+", will change auth");
			personMapping.values().remove(CCConstants.CM_PROP_PERSON_USERNAME);
			personMapping.put(remoteUserid,CCConstants.CM_PROP_PERSON_USERNAME);
		}

		String esuid;
		Map<String, Serializable> personData;
		if(username.equals(ApplicationInfoList.getRepositoryInfoById(appId).getString(ApplicationInfo.FORCED_USER,null))){
			// do not escape the guest, send them as a "plain" user
			personData = new HashMap<>();
			personData.put(CCConstants.CM_PROP_PERSON_FIRSTNAME,ApplicationInfoList.getHomeRepository().getAppCaption());
			personData.put(CCConstants.CM_PROP_PERSON_LASTNAME,"");
			personData.put(CCConstants.CM_PROP_PERSON_EMAIL,"");
			esuid = username;
		} else {
			personData = AuthorityServiceFactory.getLocalService().getUserInfo(username);
			esuid = (String)personData.get(CCConstants.PROP_USER_ESUID);
			if(esuid == null || esuid.trim().equals("")){
				throw new Exception("missing esuid for user!!! (Note: Admin doesn't have a esuid!)");
			}
		}

		UserProfileAppAuth userProfile = new UserProfileAppAuth();
		userProfile.setPrimaryAffiliation((String)personData.get(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION));
		userProfile.setEmail((String)personData.get(CCConstants.CM_PROP_PERSON_EMAIL));
		userProfile.setLastName((String)personData.get(CCConstants.PROP_USER_LASTNAME));
		userProfile.setFirstName((String)personData.get(CCConstants.PROP_USER_FIRSTNAME));
		String remoteUsername = esuid + "@" + localAppId;

		/**
		 * add global groups
		 */
		String globalGroups = null;

		AuthorityService authorityService = serviceRegistry.getAuthorityService();
		Set<String> authoritiesForUser = authorityService.getAuthorities();
		for(String authority : authoritiesForUser){
			NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authority);
			//i.i. noderef for GROUP_EVERYONE is null
			if(authorityNodeRef == null) continue;
			String scopeType = (String)serviceRegistry.getNodeService().getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE));

			if(CCConstants.CCM_VALUE_SCOPETYPE_GLOBAL.equals(scopeType)){
				if(globalGroups == null){
					globalGroups = authority;
				}else{
					globalGroups += ";"+authority;
				}
			}
		}

		if(globalGroups != null){
			userProfile.getExtendedAttributes().put(CCConstants.EDU_SHARING_GLOBAL_GROUPS,new String[]{globalGroups});
		}

		/**
		 * auth
		 */
		Signing signing = new Signing();
		String timestamp = ""+System.currentTimeMillis();
		String signData = username + localAppId + timestamp;

		byte[] signature = signing.sign(signing.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM), signData, CCConstants.SECURITY_SIGN_ALGORITHM);
		signature = new Base64().encode(signature);

		java.util.logging.Logger jaxlogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		Client client = ClientBuilder.newClient(new ClientConfig().register(new LoggingFeature(jaxlogger)));

		WebTarget webTarget = client.target(appInfoRemoteApp.getClientBaseUrl() + "/rest/");
		WebTarget currentWebTarget = webTarget.path("authentication/v1/appauth").path(remoteUsername);

		Response response = currentWebTarget
				.request(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Edu-App-Id", localAppId)
				.header("X-Edu-App-Sig",new String(signature))
				.header("X-Edu-App-Signed",signData)
				.header("X-Edu-App-Ts",timestamp)
				.post(Entity.entity(userProfile, MediaType.APPLICATION_JSON));

		if(response.getStatus() == 200){
			AuthenticationToken token = response.readEntity(AuthenticationToken.class);
			return token;
		}else{
			String message = (response.getStatusInfo() != null)? response.getStatusInfo().toString() : null;
			logger.error("remote auth failed:" + response.getStatus()+" "+response.getStatusInfo());
			logger.error("url called: " + currentWebTarget.getUri().toString());
			RemoteAuthenticationException e = new RemoteAuthenticationException(response.getStatus(),message);
			throw e;
		}
	}

	public class RemoteAuthenticationException extends Exception{
		int httpStatus;
		public RemoteAuthenticationException(int httpStatus, String message){
			super(message);
			this.httpStatus = httpStatus;
		}

		public int getHttpStatus() {
			return httpStatus;
		}
	}
	
}
