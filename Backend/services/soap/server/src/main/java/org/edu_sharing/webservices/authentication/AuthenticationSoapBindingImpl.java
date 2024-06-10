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

package org.edu_sharing.webservices.authentication;

import java.rmi.RemoteException;
import java.util.HashMap;

import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authentication.EduAuthentication;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.webservices.types.KeyValue;
import org.springframework.context.ApplicationContext;


public class AuthenticationSoapBindingImpl implements org.edu_sharing.webservices.authentication.Authentication{
   
	org.edu_sharing.service.authentication.EduAuthentication eduAuthenticationService;
	
	Logger log = Logger.getLogger(AuthenticationSoapBindingImpl.class);
	
	public AuthenticationSoapBindingImpl() {
		
		ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
		eduAuthenticationService = (EduAuthentication)eduApplicationContext.getBean("authenticationService");
	}
	
	public AuthenticationResult authenticateByApp(java.lang.String applicationId, java.lang.String username, java.lang.String email, java.lang.String ticket, boolean createUser) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException {
       
		log.info(applicationId + " " +username+" "+ email+" "+" "+ ticket);
    	
		MessageContext messageContext = MessageContext.getCurrentContext();
		String ipAddress = messageContext.getStrProp(Constants.MC_REMOTE_ADDR);
		
		log.info("Client ipAddress:"+ipAddress);
		log.info("AuthClass:"+eduAuthenticationService.getClass().getName());
		
	
		try{
			eduAuthenticationService.authenticateByApp(applicationId, username, email, ticket,ipAddress,createUser);
		}catch(org.alfresco.repo.security.authentication.AuthenticationException e){
			log.info("Exception authenticateByApp message:"+e.getMessage());
			throw new AuthenticationException(null,e.getMessage());
		}
		
		log.info("Result CurrentTicket:"+eduAuthenticationService.getCurrentTicket() +" currentUserName:"+eduAuthenticationService.getCurrentUserName());
		/**
		 * Attention if you put an username that does not exist, alfresco creates a ne person object without an user object in user store
		 * watch out for Alfresco class PersonServiceImpl
		 */
		Map<String,String> userProps = eduAuthenticationService.getPersonProperties(eduAuthenticationService.getCurrentUserName());
		String localemail = userProps.get(CCConstants.CM_PROP_PERSON_EMAIL);
		String localFirstname = userProps.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
		String localLastname = userProps.get(CCConstants.CM_PROP_PERSON_LASTNAME);
		String localUserNodeId = userProps.get(CCConstants.SYS_PROP_NODE_UID);
			
		//@TODO ATTENTION COURSEID wirdd missbraucht als userNodeId Container
		AuthenticationResult authResult = new AuthenticationResult();
		authResult.setCourseId(localUserNodeId);
		authResult.setEmail(localemail);
		authResult.setGivenname(localFirstname);
		authResult.setSessionid(null);
		authResult.setSurname(localLastname);
		authResult.setTicket(eduAuthenticationService.getCurrentTicket());
		authResult.setUsername(eduAuthenticationService.getCurrentUserName());
		return authResult;
    }
	
	@Override
	public AuthenticationResult authenticateByTrustedApp(String applicationId, String ticket, KeyValue[] ssoData)
			throws RemoteException, AuthenticationException {
		
		if(true){
			throw new RemoteException("use signed version!!!");
		}
	

		log.info("applicationId:"+applicationId + " ticket:"+ticket);
		
		for(KeyValue kv : ssoData){
			log.debug("param "+kv.getKey()+": "+kv.getValue());
		}
		
    	
		MessageContext messageContext = MessageContext.getCurrentContext();
		String ipAddress = messageContext.getStrProp(Constants.MC_REMOTE_ADDR);
		
		log.info("Client ipAddress:"+ipAddress);
		log.info("AuthClass:"+eduAuthenticationService.getClass().getName());
		
	
		try{
			
			Map<String,String> ssoDataMap = new HashMap<>();
			
			//add sso data
			for(KeyValue kv : ssoData){
				ssoDataMap.put(kv.getKey(), kv.getValue());
			}
			
			//add authByAppData
			ssoDataMap.put(SSOAuthorityMapper.PARAM_APP_ID, applicationId);
			ssoDataMap.put(SSOAuthorityMapper.PARAM_SESSION_ID, ticket);
			ssoDataMap.put(SSOAuthorityMapper.PARAM_APP_IP, ipAddress);
			ssoDataMap.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_AuthByApp);
			
			eduAuthenticationService.authenticateByTrustedApp(ssoDataMap);
			
		}catch(org.alfresco.repo.security.authentication.AuthenticationException e){
			log.info("Exception authenticateByApp message:"+e.getMessage());
			throw new AuthenticationException(null,e.getMessage());
		}
		
		log.info("Result CurrentTicket:"+eduAuthenticationService.getCurrentTicket() +" currentUserName:"+eduAuthenticationService.getCurrentUserName());
		/**
		 * Attention if you put an username that does not exist, alfresco creates a ne person object without an user object in user store
		 * watch out for Alfresco class PersonServiceImpl
		 */
		Map<String,String> userProps = eduAuthenticationService.getPersonProperties(eduAuthenticationService.getCurrentUserName());
		String localemail = userProps.get(CCConstants.CM_PROP_PERSON_EMAIL);
		String localFirstname = userProps.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
		String localLastname = userProps.get(CCConstants.CM_PROP_PERSON_LASTNAME);
		String localUserNodeId = userProps.get(CCConstants.SYS_PROP_NODE_UID);
			
		//@TODO ATTENTION COURSEID wirdd missbraucht als userNodeId Container
		//AuthenticationResult authResult = new AuthenticationResult(localUserNodeId,localemail,localFirstname,null,localLastname,eduAuthenticationService.getCurrentTicket(),eduAuthenticationService.getCurrentUserName());
		AuthenticationResult authResult = new AuthenticationResult();
		authResult.setCourseId(localUserNodeId);
		authResult.setEmail(localemail);
		authResult.setGivenname(localemail);
		authResult.setSurname(localLastname);
		authResult.setTicket(eduAuthenticationService.getCurrentTicket());
		authResult.setUsername(eduAuthenticationService.getCurrentUserName());
		return authResult;
	}

    public AuthenticationResult authenticateByCAS(java.lang.String username, java.lang.String proxyTicket) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException {
    	log.info("username:"+ username+"proxyTicket:"+ proxyTicket);
    	
    	eduAuthenticationService.authenticateByCAS(username, proxyTicket);
    	AuthenticationResult authResult = new AuthenticationResult();
    	authResult.setTicket(eduAuthenticationService.getCurrentTicket());
    	authResult.setUsername(eduAuthenticationService.getCurrentUserName());
 
    	return authResult;
    }

    public boolean checkTicket(java.lang.String username, java.lang.String ticket) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException {
    	log.info("ticket:"+ticket +" un:"+username);
    	boolean result = false;     
    	
    	result = eduAuthenticationService.validateTicket(username, ticket);
    	
    	return result;
    }

    public AuthenticationResult authenticate(java.lang.String username, java.lang.String password) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException {
    	 eduAuthenticationService.authenticate(username, password.toCharArray());
         
    	AuthenticationResult authResult = new AuthenticationResult();
     	authResult.setTicket(eduAuthenticationService.getCurrentTicket());
     	authResult.setUsername(eduAuthenticationService.getCurrentUserName());
       	return authResult;
    }
    

}
