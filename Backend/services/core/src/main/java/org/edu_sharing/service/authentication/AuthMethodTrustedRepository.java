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
package org.edu_sharing.service.authentication;

import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.mail2.core.EmailException;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.usage.AlfServicesWrapper;
import org.springframework.context.ApplicationContext;



public class AuthMethodTrustedRepository implements AuthMethodInterface {
	Logger logger = Logger.getLogger(AuthMethodTrustedRepository.class);

	private MutableAuthenticationDao authenticationDao;

	private PersonService personService;

	private NodeService nodeService;

	private SearchService searchService;

	private ServiceRegistry serviceRegistry;

	private AuthenticationService authenticationService;
	
	
	/**
	 * get Userdata from ldap or something
	 */
	private UserDataService userDataService;

	public static String PARAM_APPLICATIONID = "PARAM_APPLICATIONID";

	public static String PARAM_USERNAME = "PARAM_USERNAME";

	public static String PARAM_EMAIL = "PARAM_EMAIL";

	public static String PARAM_TICKET = "PARAM_TICKET";

	public static String PARAM_CREATEUSER = "PARAM_CREATEUSER";

	public static String PARAM_CLIENTHOST = "PARAM_CLIENTHOST";
	
	public static String test = null;
	
	
	public void init(){
		
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		
		this.authenticationDao = (MutableAuthenticationDao)applicationContext.getBean("authenticationDao");
	
		this.serviceRegistry = (ServiceRegistry) applicationContext.getBean("ServiceRegistry");
		
		this.personService = this.serviceRegistry.getPersonService();

		this.nodeService = this.serviceRegistry.getNodeService();

		this.searchService = this.serviceRegistry.getSearchService();

		this.authenticationService = this.serviceRegistry.getAuthenticationService();
		

	}

	public String authenticate(Map<String, String> params) throws AuthenticationException {
		throw new RuntimeException("old soap method should not be used");
	}

	private void sendActivationRequestMail(String receivermail, String applicationId, String username, String accesskey) {
		logger.info("start sending ActivationReauestMail...");
		
		try{
			new AlfServicesWrapper().sendActivationRequestMail(receivermail, applicationId, username, accesskey);
		}
		catch (EmailException e) {
			logger.error("Sending Activation Link failed", e);
			throw new AuthenticationException(AuthenticationExceptionMessages.SENDACTIVATIONLINK_FAILED);
		}
		
		logger.info("... return");
	}

	/**
	 * 
	 * @param personRef
	 * @param username
	 * @param applicationId
	 * @param accessAllowed
	 * @return NodeId of PersonAccessElement
	 */
	private String createNewPersonAccessElement(NodeRef personRef, String username, String applicationId, Boolean accessAllowed, String accesskey) {
		logger.info("starting...");
		ApplicationInfo homerepository = ApplicationInfoList.getHomeRepository();
		if (homerepository == null) {
			logger.error("no home repository found. check Repository configuration!");
			throw new AuthenticationException(AuthenticationExceptionMessages.REPOSITORY_FAULT);

		}
		
		
		AlfServicesWrapper alfServWrapper = new AlfServicesWrapper();
		alfServWrapper.authenticate(homerepository.getUsername(), homerepository.getPassword());
		String persAccEleId = alfServWrapper.createPersonAccessElement(personRef.getId(), username, applicationId, accessAllowed, accesskey);
		
		//make admin session invalid
		authenticationService.invalidateTicket(authenticationService.getCurrentTicket());
		
		if (persAccEleId == null) {
			logger.error("error by creating " + CCConstants.CM_TYPE_PERSONACCESSELEMENT + " ersAccEleId");
			throw new AuthenticationException(AuthenticationExceptionMessages.REPOSITORY_FAULT);
		}
		logger.info("...returning");
		return persAccEleId;

	}


	/**
	 * builds shadowUsername with pattern:
	 * {username}#{emailaddress}#{repositoryid}
	 * 
	 * @param username
	 * @param email
	 * @param repId
	 * @return
	 */
	private String getShadowUN(String username, String email, String repId) {
		return username + "#" + email + "#" + repId;
	}

	public void setAuthenticationDao(MutableAuthenticationDao authenticationDao) {
		this.authenticationDao = authenticationDao;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
}
