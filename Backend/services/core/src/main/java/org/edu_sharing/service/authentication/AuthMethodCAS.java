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

import net.sf.acegisecurity.UserDetails;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.service.authentication.cas.PTValidation;
import org.springframework.context.ApplicationContext;




/**
 * 
 * 
 *
 */
public class AuthMethodCAS implements AuthMethodInterface {
	
	Logger logger = Logger.getLogger(AuthMethodCAS.class);
	
	private MutableAuthenticationDao authenticationDao;
	
	private ServiceRegistry serviceRegistry;
	
	public static String PARAM_USERNAME = "PARAM_PARAM_USERNAME";
	public static String PARAM_PROXYTICKET = "PARAM_PROXYTICKET";
	
	public void init(){
		
		
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		
		this.authenticationDao = (MutableAuthenticationDao)applicationContext.getBean("authenticationDao");
		this.serviceRegistry = (ServiceRegistry) applicationContext.getBean("ServiceRegistry");
	}
	
	public String authenticate(Map<String, String> params) throws AuthenticationException{
		String result = null;
		String proxyValidate = null;
		String proxyservice = null;
		String proxycallback = null;
		try{
			proxyValidate = PropertiesHelper.getProperty("proxyvalidate", "ccsearch-auth.properties.xml", PropertiesHelper.XML);
			proxyservice = PropertiesHelper.getProperty("proxyservice", "ccsearch-auth.properties.xml", PropertiesHelper.XML);
			proxycallback = PropertiesHelper.getProperty("proxycallback", "ccsearch-auth.properties.xml", PropertiesHelper.XML);
		}catch(Exception e){
			throw new AuthenticationException("Exception when getting property of file ccsearch-auth.properties.xml:"+e.getMessage());
		}
		
		
		PTValidation ptValidation = new PTValidation(proxyValidate, proxyservice, proxycallback);
		
		
		Map<String,String> resultMap = ptValidation.getCredentials((String)params.get(PARAM_PROXYTICKET));
	    
		if(resultMap != null){
	    	String resultUser = resultMap.get(PTValidation.USER);
	    	String resultCallback = resultMap.get(PTValidation.CALLBACKURL);
	    	//TODO test if resultUser is an valid alfresco user
	    	if(resultUser != null && resultCallback != null){
	    		//setCurrentUser(userName);
	    		result = resultUser;
	    		checkAndCreateUser(result);
	    	}else{
	    		logger.info("resultUser is null or resultCallback is null Authentication failed");
	    		throw new AuthenticationException("CAS Authentication failed!!! resultUser is null or resultCallback is null");
	    	}
	    }else{
	    	logger.info("resultMap is null. Proxy Ticket seems to be invalid!!!");
	    	throw new AuthenticationException("CAS Authentication failed!!! resultMap is null. Proxy Ticket seems to be invalid!!!");
		}
		return result;
	}
	
	
	/**
	 * cretes user with userName when not exists
	 * @param userName
	 * @return
	 */
	private NodeRef checkAndCreateUser(final String userName){
		UserDetails userDetails = null;
		try {
			RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>() {
				public Object execute() throws Exception {
					UserDetails userDetails = authenticationDao.loadUserByUsername(userName);
					return userDetails;
				}
			};
			
			TransactionService transactionService = serviceRegistry.getTransactionService();
			userDetails = (UserDetails) transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, true);
		} catch (net.sf.acegisecurity.providers.dao.UsernameNotFoundException e) {
			e.printStackTrace();
		}
		
		if(userDetails == null){
			logger.info("cas user exists not alfresco creating...");
			final AuthenticationService authenticationService = serviceRegistry.getAuthenticationService();
			final PersonService personService = serviceRegistry.getPersonService();
			final ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();

			if (homeRepository == null) {
				logger.error("missing homerepository config!!!");
				throw new AuthenticationException(AuthenticationExceptionMessages.REPOSITORY_FAULT);
			}

			RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>() {
				public Object execute() throws Exception {
					authenticationService.authenticate(homeRepository.getUsername(), homeRepository.getPassword().toCharArray());

					authenticationDao.createUser(userName, "liferay".toCharArray());
					Map<QName, Serializable> persProps = new HashMap<>();
					persProps.put(QName.createQName(CCConstants.PROP_USERNAME), userName);
					persProps.put(QName.createQName(CCConstants.PROP_USER_EMAIL), userName);
					personService.setPersonProperties(userName, persProps);
					NodeRef newPersNodeRef = personService.getPerson(userName);
					
					// personService.
					logger.info("user created");

					return newPersNodeRef;
				}
			};
			TransactionService transactionService = serviceRegistry.getTransactionService();
			NodeRef newPersNodeRef = (NodeRef) transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
			logger.info("new alfresco user created username:"+userName+" id:"+newPersNodeRef.getId());
			return newPersNodeRef;
		}
		logger.info("user"+ userName+ "already exists in alfresco");
		return null;
	}


	
}
