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
package org.edu_sharing.repository.client.exception;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CCException extends Exception implements Serializable {

	private static final long serialVersionUID = 1L;

	public static String REPOSITORYISNOTREACHABLE = "REPOSITORYISNOTREACHABLE";
	
	public static String SESSIONEXPIREDEXCEPTION = "SESSIONEXPIREDEXCEPTION";
	
	public static String AUTHENTIFICATIONFAILED = "AUTHENTIFICATIONFAILED";
	
	public static String AUTHENTIFICATIONEXCEPTION = "AUTHENTIFICATIONEXCEPTION";
	
	public static String REMOTEEXCEPTION = "REMOTEEXCEPTION";
	
	public static String UNKNOWNEXCEPTION = "UNKNOWNEXCEPTION";
	
	public static String CYCLE_CHILDRELATION = "CYCLE_CHILDRELATION";
	
	public static String DUPLICATE_CHILD = "DUPLICATE_CHILD";
	
	public static String INVITATIONFAILED = "INVITATIONFAILED";
	
	public static String REMOVE_FROM_FAVORITES_NOT_ALLOWED = "REMOVE_FROM_FAVORITES_NOT_ALLOWED";
	
	public static String SENDACTIVATIONLINK_SUCCESS = "SENDACTIVATIONLINK_SUCCESS";
	
	public static String APPLICATIONACCESS_NOT_ACTIVATED_BY_USER = "APPLICATIONACCESS_NOT_ACTIVATED_BY_USER";
	
	public static String LUCENE_TO_MANY_CLAUSES = "LUCENE_TO_MANY_CLAUSES";
	
	public static String REMOVE_PERMISSION_INHERIT = "REMOVE_PERMISSION_INHERIT";
	
	public static String ACCESS_DENIED_EXCEPTION = "ACCESS_DENIED_EXCEPTION";
	
	public static String NO_PERMISSIONS_TO_DELETE = "NO_PERMISSIONS_TO_DELETE";
	
	/**
	 * direct share
	 */
	public static String SHARE_SERVICE_EMAILSENDFAILED = "SHARE_SERVICE_EMAILSENDFAILED";
	public static String SHARE_SERVICE_EMAILVALIDATIONFAILED = "SHARE_SERVICE_EMAILVALIDATIONFAILED";
	public static String SHARE_SERVICE_NODEDOESNOTEXSIST = "SHARE_SERVICE_NODEDOESNOTEXSIST";
	public static String SHARE_SERVICE_NOPERMISSIONS = "SHARE_SERVICE_NOPERMISSIONS";
	public static String SHARE_SERVICE_EXPIRYDATETOOLD = "SHARE_SERVICE_EXPIRYDATETOOLD";
	
	Map<String,String> messageParams = new HashMap<>();
	
	String id = null;
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * this constructor is mandatory cause this Object is serializeable
	 */
	public CCException(){
		super();
	}
	
	public CCException(String id){
		super();
		this.id = id;
		
	}
	
	public CCException(String id, String message){
		super(message);
		this.id = id;
		
	}
	
	public void setMessageParam(String key,String val){
		messageParams.put(key, val);
	}
	
	public Map<String, String> getMessageParams() {
		return messageParams;
	}
}
