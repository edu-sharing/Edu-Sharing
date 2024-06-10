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

import com.typesafe.config.Config;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import java.util.Map;


/**
 * ensure that the authentication method of this class isn't provided through an webservice or something 
 * @author rudi
 *
 */
public class AuthMethodSSO implements AuthMethodInterface {


	Logger logger = Logger.getLogger(AuthMethodSSO.class);

	private UserDataService userdataService;

	private SSOAuthorityMapper ssoAuthorityMapper;

	public Boolean createUser = false;


	@Override
	public String authenticate(Map<String, String> params) throws AuthenticationException {
		//for security reasons: sso auth should be explicit activated in config
		//so that we don't get an open gate when someone forgets to remove the shibboleth servlet from web.xml and don't protects this url
		//(shibboleth header usage)

		String paramAuthType = params.get(SSOAuthorityMapper.PARAM_SSO_TYPE);

		if(paramAuthType == null){
			logger.error("missing paramAuthType");
			throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
		}

		Config config = LightbendConfigLoader.get();
		String springProfiles = config.hasPath("spring.profiles.active")
				? config.getString("spring.profiles.active")
				: null;
		if(SSOAuthorityMapper.SSO_TYPE_Shibboleth.equals(paramAuthType)
			&& springProfiles != null
			&& (springProfiles.contains("openidEnabled") || springProfiles.contains("samlEnabled"))) {
			return ssoAuthorityMapper.mapAuthority(params);
		}else if(SSOAuthorityMapper.SSO_TYPE_LTI.equals(paramAuthType) &&
				config.getBoolean("security.sso.lti.enabled")){
			return ssoAuthorityMapper.mapAuthority(params);
		}else if(SSOAuthorityMapper.SSO_TYPE_CAS.equals(paramAuthType) &&
				config.getBoolean("security.sso.cas.enabled")){
			return ssoAuthorityMapper.mapAuthority(params);
		}else {
			logger.error(AuthenticationExceptionMessages.INVALID_AUTHENTICATION_METHOD +" no SSO(shibboleth,cas) auth configured. authType:"+paramAuthType);
			throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_AUTHENTICATION_METHOD);
		}
	}


	public void setCreateUser(Boolean createUser) {
		this.createUser = createUser;
	}



	public UserDataService getUserdataService() {
		return userdataService;
	}

	public void setUserdataService(UserDataService userdataService) {
		this.userdataService = userdataService;
	}

	public void setSsoAuthorityMapper(SSOAuthorityMapper ssoAuthorityMapper) {
		this.ssoAuthorityMapper = ssoAuthorityMapper;
	}

}
