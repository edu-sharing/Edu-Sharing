package org.edu_sharing.service.authentication;

import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class AuthMethodTrustedApplication implements AuthMethodInterface {

	Logger logger = Logger.getLogger(AuthMethodTrustedApplication.class);
	
	
	SSOAuthorityMapper ssoAuthorityMapper;
	
	public String authenticate(Map<String, String> params) throws AuthenticationException {
		
		String userName = params.get(ssoAuthorityMapper.getSSOUsernameProp());
		
		String applicationId = params.get(SSOAuthorityMapper.PARAM_APP_ID);
		String clientIp = params.get(SSOAuthorityMapper.PARAM_APP_IP);
		
		/**
		 * check params
		 */
		if(applicationId == null || applicationId.trim().length() == 0 || userName == null || userName.trim().length() == 0){
			logger.error(AuthenticationExceptionMessages.MISSING_PARAM);
			logger.error(" username:"+userName +" applicationId:"+applicationId +" ( clientIp:"+clientIp+")");
			throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
		}
		
		/**
		 * check applicationId
		 */
		final ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(applicationId);
		if (appInfo == null || appInfo.getTrustedclient() == null || !appInfo.getTrustedclient().equals("true")) {
			logger.info(AuthenticationExceptionMessages.INVALID_APPLICATION +" "+appInfo);
			throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_APPLICATION);
		}
		

		/**
		 * check host
		 */	
		if (ssoAuthorityMapper.isAuthByAppCheckClientIp() && (clientIp == null || !appInfo.isTrustedHost(clientIp))) {
			logger.error(AuthenticationExceptionMessages.INVALID_HOST + " clientHost:" + clientIp + " appInfo.trusted hosts:" + appInfo.getHost() +" "+ appInfo.getHostAliases() +" "+appInfo.getDomain() +" appInfo.getAppId():"+appInfo.getAppId() +" appfile:"+appInfo.getAppFile() +" param appid:"+applicationId);
			throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_HOST + ": " + clientIp);
		}
		
		
		
		params.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_AuthByApp);
		return ssoAuthorityMapper.mapAuthority(params);
	}
	
	public void setSsoAuthorityMapper(SSOAuthorityMapper ssoAuthorityMapper) {
		this.ssoAuthorityMapper = ssoAuthorityMapper;
	}
}
