package org.edu_sharing.service.authentication.sso.config;

import java.util.Map;

import org.edu_sharing.service.authentication.SSOAuthorityMapper;

public interface CustomGroupMapping {

	
	public void map(Map<String, String> ssoAttributes);
	
	
	public void setSSOAuthorityMapper(SSOAuthorityMapper ssoAuthorityMapper);
}
