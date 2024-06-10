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

import java.util.Map;

public class AuthenticatorRemoteAppResult {
	Map<String,String> authenticationInfo = null;
	String exceptionMessage = null;
	public Map<String, String> getAuthenticationInfo() {
		return authenticationInfo;
	}
	public void setAuthenticationInfo(Map<String, String> authenticationInfo) {
		this.authenticationInfo = authenticationInfo;
	}
	public String getExceptionMessage() {
		return exceptionMessage;
	}
	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}
	
}
