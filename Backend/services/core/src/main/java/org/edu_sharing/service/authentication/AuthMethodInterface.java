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

public interface AuthMethodInterface {
	
	String authenticate(Map<String, String> params) throws AuthenticationException;
}
