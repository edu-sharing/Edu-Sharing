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

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;

public class AuthenticationToolAPI extends AuthenticationToolAbstract {
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	AuthenticationService authenticationService = serviceRegistry.getAuthenticationService();
	 
	PersonService personService = serviceRegistry.getPersonService();
	
	NodeService nodeService = serviceRegistry.getNodeService();
	
	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
	AuthenticationComponent authenticationComponent = (AuthenticationComponent) alfApplicationContext.getBean("authenticationComponent");
	 
	static Logger log = Logger.getLogger(AuthenticationToolAPI.class);
	
	public AuthenticationToolAPI() {
	}
	
	//for RepoFactory getAuthenticationToolInstance will ignore AppId cause it works only with homerepo
	public AuthenticationToolAPI(String appId) {
	}
	
	public Map<String,String> createNewSession(String userName, String password) throws Exception {
		authenticationService.authenticate(userName, password.toCharArray());
		
		Map<String,String> returnval = new HashMap<>();
		returnval.put(CCConstants.AUTH_USERNAME, authenticationService.getCurrentUserName());
		returnval.put(CCConstants.AUTH_TICKET, authenticationService.getCurrentTicket());
		
		addClientUserInfo(returnval);
		
		ToolPermissionService toolPermissionService = ToolPermissionServiceFactory.getInstance();
		// the context isn't set on startup, so we need to return null
		if(toolPermissionService != null) {
			toolPermissionService.invalidateSessionCache();
		}
		return returnval;
	};
	
	@Override
	public Map<String, String> getUserInfo(String userName, String ticket) throws Exception {
		serviceRegistry.getAuthenticationService().validate(ticket);
		Map<String,String> returnval = new HashMap<>();
		returnval.put(CCConstants.AUTH_USERNAME, authenticationService.getCurrentUserName());
		returnval.put(CCConstants.AUTH_TICKET, authenticationService.getCurrentTicket());
		
		addClientUserInfo(returnval);
		
		return returnval;
	}
	
	/**
	 * Gets the current scope of the session, or null for the default workspace
	 * @return
	 */
	public String getScope(){
		if(Context.getCurrentInstance() == null) return null;
		if(Context.getCurrentInstance().getRequest() == null) return null;
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		if (session==null) return null;
		return (String)session.getAttribute(CCConstants.AUTH_SCOPE);
	}
	
	private void addClientUserInfo(Map<String,String> authInfo) throws Exception{
		
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient(authInfo);
		Map<String, String> repositoryUseInfo = mcAlfrescoAPIClient.getUserInfo(authenticationService.getCurrentUserName());
		String userNameCaption = repositoryUseInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);
		
		if(userNameCaption == null || userNameCaption.trim().equals("")) userNameCaption = authInfo.get(CCConstants.AUTH_USERNAME);
		authInfo.put(CCConstants.AUTH_USERNAME_CAPTION, userNameCaption);
		
		String homeFolderId = mcAlfrescoAPIClient.getHomeFolderID(authInfo.get(CCConstants.AUTH_USERNAME));
		authInfo.put(CCConstants.AUTH_USER_HOMEDIR, homeFolderId);
		
		boolean isAdmin = mcAlfrescoAPIClient.isAdmin(authInfo.get(CCConstants.AUTH_USERNAME));
		authInfo.put(CCConstants.AUTH_USER_ISADMIN, new Boolean(isAdmin).toString());
	}

	public String setUser(String authorityName){
		if(!AuthorityServiceHelper.isAdmin()) {
			throw new NotAnAdminException();
		}
		if(authenticationComponent.setCurrentUser(authorityName) != null) {
			return authenticationService.getCurrentTicket();
		}
		throw new RuntimeException("Authentication failed for " + authorityName);
	}
	
	@Override
	public void logout(final String ticket) {
		try{
			serviceRegistry.getAuthenticationService().invalidateTicket(ticket);
			serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
		} catch (AuthenticationCredentialsNotFoundException e) {
			log.debug("it seems there is a logout call with a ticket without a security context:");
			log.debug(e.getMessage());
			logoutWithoutSecurityContext(ticket);
		}
	}
	
	public void logoutWithoutSecurityContext(final String ticket){
		
		RunAsWork<Void> ra = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
				ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
				AuthenticationService authenticationService = serviceRegistry.getAuthenticationService();
				authenticationService.invalidateTicket(ticket);
				authenticationService.clearCurrentSecurityContext();
				log.debug("none security context ticket invalidation done");
				return null;
			}
		};
		
		AuthenticationUtil.runAs(ra, "admin");
	}
	
	/**
	 * - tries to find a alfresco ticket in session 
	 * - if there is one it will be validated
	 * - when it's valid the corresponding user will be determined
	 * - ticket and user name will be returned
	 * 
	 * @param session
	 * @return null when no valid ticket was found else user name / ticket as Map<String,String>
	 */
	public Map<String,String> validateAuthentication(HttpSession session){
		Map<String,String> result = null;
		String currentTicket = (String)session.getAttribute(CCConstants.AUTH_TICKET);
		if(currentTicket != null){
			try{
				authenticationService.validate(currentTicket);
				result = new HashMap<>();
				result.put(CCConstants.AUTH_USERNAME, authenticationService.getCurrentUserName());
				result.put(CCConstants.AUTH_TICKET, currentTicket);
			}catch(AuthenticationException e){
				log.warn(e.getMessage());
			}
		} 
		return result;
	}
	
	@Override
	public boolean validateTicket(String ticket) {
		try{
			if(ticket == null){
				return false;
			}
			authenticationService.validate(ticket);

			log.info("User logged in: "+authenticationService.getCurrentUserName()+", ticket: "+ticket);
			return true;
		}catch(AuthenticationException e){
			log.info(e.getMessage()+", ticket: "+ticket);
		}
		return false;
	}
	
	/**
	 * ignores user name param and takes the one it gets from authentication service
	 */
	@Override
	public void storeAuthInfoInSession(String username, String ticket, String authType, HttpSession session) {

		authenticationService.validate(ticket);
		super.storeAuthInfoInSession(authenticationService.getCurrentUserName(), ticket, authType, session);
		
		//validate a second time cause super.storeAuthInfoInSession makes a logout when another tickets is in session
		//i.e jession with ticket + basic auth in ApiAuthenticationFilter
		authenticationService.validate(ticket);

		// prewarm tp session cache
		// not required since validateSession will do it anyway
		// otherwise, every status probe call will build unnecessary a tp cache
		//ToolPermissionServiceFactory.getInstance().getAllAvailableToolPermissions();

		try {
			Map<String, String> userInfo = getUserInfo(authenticationService.getCurrentUserName(), ticket);
			session.setAttribute(CCConstants.AUTH_USERNAME_CAPTION, userInfo.get(CCConstants.AUTH_USERNAME_CAPTION));
		}catch(Exception e) {
			
		}
		
		String locale = (String)session.getAttribute(CCConstants.AUTH_LOCALE);
		if(locale == null){
			Object localeObj = nodeService.getProperty(personService.getPerson(authenticationService.getCurrentUserName()), ContentModel.PROP_LOCALE);
			if(localeObj != null){
				session.setAttribute(CCConstants.AUTH_LOCALE, localeObj.toString());
			}
		}
	}
	
	public void authenticateUser(String username, HttpSession session) {
		authenticationComponent.setCurrentUser(username);
		storeAuthInfoInSession(username,authenticationService.getCurrentTicket(), CCConstants.AUTH_TYPE_DEFAULT, session);
	}
	
}
