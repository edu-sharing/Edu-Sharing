package org.edu_sharing.repository.server.authentication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.service.authentication.EduAuthentication;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.springframework.context.ApplicationContext;

public class CASFilter  implements jakarta.servlet.Filter {

	Logger logger = Logger.getLogger(CASFilter.class);
	
	@Override
	public void init(FilterConfig paramFilterConfig) throws ServletException {
	}
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
			ServletException {
		
		HttpServletRequest httpReq = (HttpServletRequest) req;
		String remoteUser = httpReq.getRemoteUser();
		ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
			
		EduAuthentication authService =  (EduAuthentication)eduApplicationContext.getBean("authenticationService");
		
		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
		
		Map<String,String> validAuthInfo = authTool.validateAuthentication(httpReq.getSession());
		
		if (validAuthInfo != null ) {
			if(validAuthInfo.get(CCConstants.AUTH_USERNAME).equals(remoteUser)){
				
				chain.doFilter(req,res);
				return;
				
			}else{
				
				logger.error("INVALID ACCESS! sessionAlfrescoUser:"+validAuthInfo.get(CCConstants.AUTH_USERNAME) +" != remoteUser:"+remoteUser);
				return;
				
			}
		}
		
		try {
			
			logger.info("no valid authinfo found in session. doing the repository cas auth");
			
			logger.info("req.getCharacterEncoding():"+req.getCharacterEncoding());
			
			if (req.getCharacterEncoding() == null){
				req.setCharacterEncoding("UTF-8");
			}
			
			SSOAuthorityMapper ssoMapper = (SSOAuthorityMapper)eduApplicationContext.getBean("ssoAuthorityMapper");
			Map<String,String> ssoMap = new HashMap<>();
			ssoMap.put(ssoMapper.getSSOUsernameProp(), remoteUser);
			
			authService.authenticateBySSO(SSOAuthorityMapper.SSO_TYPE_CAS, ssoMap);
			String ticket = authService.getCurrentTicket();
			
			authTool.storeAuthInfoInSession(remoteUser, ticket,CCConstants.AUTH_TYPE_CAS, httpReq.getSession());
			chain.doFilter(req,res);
			return;
			
		} catch(org.alfresco.repo.security.authentication.AuthenticationException e) {
			return;
		}
		
	}
	
}
