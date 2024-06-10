/**
 * AuthbyappSoapBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authbyapp;

import java.rmi.RemoteException;
import java.util.HashMap;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authentication.EduAuthentication;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.service.authentication.oauth2.TokenService;
import org.edu_sharing.service.authentication.oauth2.TokenService.Token;
import org.edu_sharing.webservices.authentication.AuthenticationException;
import org.edu_sharing.webservices.authentication.AuthenticationResult;
import org.edu_sharing.webservices.types.KeyValue;
import org.springframework.context.ApplicationContext;

public class AuthbyappSoapBindingImpl implements org.edu_sharing.webservices.authbyapp.AuthByApp{
   
	org.edu_sharing.service.authentication.EduAuthentication eduAuthenticationService;
	
	Logger log = Logger.getLogger(AuthbyappSoapBindingImpl.class);	
	
	public AuthbyappSoapBindingImpl() {
		ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
		eduAuthenticationService = (EduAuthentication)eduApplicationContext.getBean("authenticationService");	
 	}
	
	public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByTrustedApp(java.lang.String applicationId, org.edu_sharing.webservices.types.KeyValue[] ssoData) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException {
		log.info("applicationId:"+applicationId );
		
		for(KeyValue kv : ssoData){
			log.info("param "+kv.getKey()+": "+kv.getValue());
		}
		
    	
		MessageContext messageContext = MessageContext.getCurrentContext();
		String ipAddress = messageContext.getStrProp(Constants.MC_REMOTE_ADDR);
		
		log.info("Client ipAddress:"+ipAddress);
		log.info("AuthClass:"+eduAuthenticationService.getClass().getName());
		
		Map<String,String> ssoDataMap = new HashMap<>();
		try{
			
			
			
			//add sso data
			for(KeyValue kv : ssoData){
				ssoDataMap.put(kv.getKey(), kv.getValue());
			}
			
			//add authByAppData
			ssoDataMap.put(SSOAuthorityMapper.PARAM_APP_ID, applicationId);
			ssoDataMap.put(SSOAuthorityMapper.PARAM_APP_IP, ipAddress);
			ssoDataMap.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_AuthByApp);
			
			eduAuthenticationService.authenticateByTrustedApp(ssoDataMap);
			
		}catch(org.alfresco.repo.security.authentication.AuthenticationException e){
			log.error("Exception authenticateByApp message:"+e.getMessage(),e);
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
		try {
			
			
			AuthenticationResult authResult = new AuthenticationResult();
			authResult.setCourseId(localUserNodeId);
			authResult.setEmail(localemail);
			authResult.setGivenname(localFirstname);
			authResult.setSurname(localLastname);
			authResult.setSessionid(null);
			authResult.setTicket(eduAuthenticationService.getCurrentTicket());
			authResult.setUsername(eduAuthenticationService.getCurrentUserName());

			return authResult;
		}
		catch (Throwable e) {
			e.printStackTrace();
			throw new AuthenticationException(e,e.getMessage());
		}
		//@TODO ATTENTION COURSEID is used as userNodeId Container
    }
	
	public boolean checkTicket(String ticket) throws AuthenticationException {
		try{
			eduAuthenticationService.validate(ticket);
			return true;
		}catch(org.alfresco.repo.security.authentication.AuthenticationException e){
			return false;
		}
	}
	

	
}
