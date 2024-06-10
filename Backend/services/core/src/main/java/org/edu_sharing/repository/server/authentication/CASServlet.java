package org.edu_sharing.repository.server.authentication;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.service.authentication.EduAuthentication;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.springframework.context.ApplicationContext;

public class CASServlet extends HttpServlet {

	private static Logger logger = Logger.getLogger(CASServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {


		String remoteUser = req.getRemoteUser();
		ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
		
		EduAuthentication authService =  (EduAuthentication)eduApplicationContext.getBean("authenticationService");
		
		
		
		
		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
		
		Map<String,String> validAuthInfo = authTool.validateAuthentication(req.getSession());
		
		if(validAuthInfo != null ){
			if(validAuthInfo.get(CCConstants.AUTH_USERNAME).equals(remoteUser)){
				
				redirect(resp, req);
				return;
			}else{
				logger.error("INVALID ACCESS! sessionAlfrescoUser:"+validAuthInfo.get(CCConstants.AUTH_USERNAME) +" != remoteUser:"+remoteUser);
				resp.getOutputStream().println("INVALID ACCESS!");
				return;
			}
		}
		try{
			logger.info("no valid authinfo found in session. doing the repository cas auth");
			
			logger.info("req.getCharacterEncoding():"+req.getCharacterEncoding());
			
			if(req.getCharacterEncoding() == null){
				req.setCharacterEncoding("UTF-8");
			}
			
			
			
			
			SSOAuthorityMapper ssoMapper = (SSOAuthorityMapper)eduApplicationContext.getBean("ssoAuthorityMapper");
			
			Map<String,String> ssoMap = new HashMap<>();
			ssoMap.put(ssoMapper.getSSOUsernameProp(), remoteUser);
			
			authService.authenticateBySSO(SSOAuthorityMapper.SSO_TYPE_CAS, ssoMap);
			String ticket = authService.getCurrentTicket();
			
			authTool.storeAuthInfoInSession(remoteUser, ticket, CCConstants.AUTH_TYPE_CAS, req.getSession());
			redirect(resp,req);
			
		
		}catch(org.alfresco.repo.security.authentication.AuthenticationException e){
			logger.error("INVALID ACCESS!",e);
			resp.getOutputStream().println("INVALID ACCESS! "+e.getMessage());
			return;
		}
	}
	
	private void redirect(HttpServletResponse resp, HttpServletRequest req) throws IOException{
		String redirectUrl = req.getContextPath();
		
		
		Enumeration paramNames =  req.getParameterNames();
		while(paramNames.hasMoreElements()){
			String paramName = (String)paramNames.nextElement();
			String paramVal = req.getParameter(paramName);
			redirectUrl = UrlTool.setParam(redirectUrl, paramName , paramVal);
		}
		resp.sendRedirect(redirectUrl);
	}
}
