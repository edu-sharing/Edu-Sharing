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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.repo.security.authentication.TicketComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;


public class EduAuthentication {
	EduAuthenticationComponent ccauthenticationComponent;
    TicketComponent ticketComponent;
    AuthenticationService authenticationService;
    private ServiceRegistry serviceRegistry;
    Logger logger = Logger.getLogger(EduAuthentication.class);
    
    public void init(){
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		
		this.serviceRegistry = (ServiceRegistry) applicationContext.getBean("ServiceRegistry");
		this.ticketComponent = (TicketComponent) applicationContext.getBean("ticketComponent");
		this.authenticationService = serviceRegistry.getAuthenticationService();
	}
    
    
	public void setCcauthenticationComponent(EduAuthenticationComponent ccauthenticationComponent) {
		this.ccauthenticationComponent = ccauthenticationComponent;
	}
	
	 /**
     * metaventis custom
     */
    public void authenticateByApp(String applicationId, String userName, String email, String ticket,String host, Boolean createUser) throws AuthenticationException{
    	
    	
    	Map<String,String> params = new HashMap<>();
    	params.put(AuthMethodTrustedRepository.PARAM_EMAIL, email);
    	params.put(AuthMethodTrustedRepository.PARAM_APPLICATIONID, applicationId);
    	params.put(AuthMethodTrustedRepository.PARAM_USERNAME, userName);
    	params.put(AuthMethodTrustedRepository.PARAM_TICKET, ticket);
    	params.put(AuthMethodTrustedRepository.PARAM_CREATEUSER, createUser.toString());
    	params.put(AuthMethodTrustedRepository.PARAM_CLIENTHOST, host);
    	
    	ccauthenticationComponent.authenticate(AuthMethodTrustedRepository.class.getName(), params);
    	
    }
    
    public void authenticateByTrustedApp(Map<String,String> params){
    	ccauthenticationComponent.authenticate(AuthMethodTrustedApplication.class.getName(), params); 	
    }
    
   
    
    
    
    /**
     * metaventis custom
     */
    public void authenticateByCAS(String userName, String proxyTicket){
    	
    	Map<String,String> params = new HashMap<>();
    	params.put(AuthMethodCAS.PARAM_USERNAME, userName);
    	params.put(AuthMethodCAS.PARAM_PROXYTICKET, proxyTicket);
    	ccauthenticationComponent.authenticate(AuthMethodCAS.class.getName(), params);
    }
    
    
    /**
     * auth type as extra method param cause "params" comes unfiltered from client and they could set the authtype param
     * 
     * @param authType
     * @param params
     */
    public void authenticateBySSO(String authType, Map<String,String> params){
    	params.put(SSOAuthorityMapper.PARAM_SSO_TYPE, authType);
    	ccauthenticationComponent.authenticate(AuthMethodSSO.class.getName(),params);
    }
    
    public String getCurrentTicket()
    {
    	//alfresco34e update it seems that when authenticationComponent.setCurrentUser is called, there isn't an ticket for that user ready
    	//so we can not use icketComponent.getCurrentTicket(authenticationService.getCurrentUserName(),false);
    	//authenticationService.getCurrentTicket() delivers a new ticket
    	
    	return authenticationService.getCurrentTicket();
    	//return ticketComponent.getCurrentTicket(authenticationService.getCurrentUserName(),false);
    }
    
    public String getCurrentUserName() throws AuthenticationException
    {
    	return authenticationService.getCurrentUserName();
    }
    
	public void validate(String ticket) throws AuthenticationException
    {
       authenticationService.validate(ticket); 
    }
	
	/**
	 * campuscontent method
	 * @param username
	 * @param ticket
	 * @return
	 */
	public boolean validateTicket(String username, final String ticket){
		logger.info("username:"+username+ " ticket:"+ticket);
		String usernameValidateTicket = null; 
		
		RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>() {
			public Object execute() throws Exception {
				try{
					String usernameValidateTicket = ticketComponent.validateTicket(ticket);
					logger.info("usernameValidateTicket:"+usernameValidateTicket);
					return usernameValidateTicket;
				}catch(Exception e){
					logger.error("validateTicket fails:",e);
					return null;
				}
			}
		};
		TransactionService transactionService = serviceRegistry.getTransactionService();
		usernameValidateTicket = (String) transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, true);
		if(usernameValidateTicket != null && usernameValidateTicket.equals(username)){
			return true;
		}else{
			return false;
		}
		
	}
	
	
	public void authenticate(String userName, char[] password) throws AuthenticationException
    {
		authenticationService.authenticate(userName, password);
    }
	
	
	
	public Map<String,String> getPersonProperties(String username){
		Map<String,String> result = new HashMap<>();
		/**
		 * Attention if you put an username that does not exist, alfresco creates a ne person object without an user object in user store
		 * watch out for Alfresco class PersonServiceImpl
		 */
		//NodeRef person = serviceRegistry.getPersonService().getPerson(username);
		
		//alfresco 3.4: with this version you can put an autoCreate param
		NodeRef person = serviceRegistry.getPersonService().getPerson(username,false);
		
		Map<QName,Serializable> props = serviceRegistry.getNodeService().getProperties(person);
		for(Map.Entry<QName,Serializable> entry:props.entrySet()){
			if(entry.getValue() instanceof String){
				result.put(entry.getKey().toString(),(String)entry.getValue());
				logger.info(entry.getKey().toString() + "  " + (String)entry.getValue());
			}else{
				logger.info("No String:"+entry.getKey().toString());
			}
		}
		return result;
	}
	
	public void invalidateTicket(String ticket){
		authenticationService.invalidateTicket(ticket);
	}
	
	
	
}
