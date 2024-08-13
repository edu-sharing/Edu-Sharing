package org.edu_sharing.repository.server.authentication;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.NgServlet;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LocaleValidator;
import org.edu_sharing.service.authentication.oauth2.TokenService;
import org.edu_sharing.service.authentication.oauth2.TokenService.Token;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.nodeservice.CallSourceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthenticationFilter implements jakarta.servlet.Filter {
	
	public static final String LOGIN_SUCCESS_REDIRECT_URL = "LOGIN_SUCCESS_REDIRECT_URL";
	
	public static final String PATH_LOGIN_ANGULAR = "/components/login";
	
	public static final String PATH_INDEX_JSP = "/index.html";
	
	public static final String PATH_SHIBBOLETH = "/shibboleth";
	 
	Logger log = Logger.getLogger(AuthenticationFilter.class);
	
	TokenService tokenService;

	List<String> unprotectedPaths = Arrays.asList("components/sharing");
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		ApplicationContext eduApplicationContext = 
				org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();

		tokenService = (TokenService) eduApplicationContext.getBean("oauthTokenService");
		
	}
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest) req;
	    HttpServletResponse httpRes = (HttpServletResponse) res;
	        
	    //alfresco WEBDAV fix (was done in index.jsp before but this is protected by this filter)
	    if (httpReq.getMethod().equalsIgnoreCase("PROPFIND") ||
	    		httpReq.getMethod().equalsIgnoreCase("OPTIONS"))
	    {
	    	log.info("webdav auth redirecting to /webdav/");
	    	httpRes.sendRedirect(httpReq.getContextPath() + "/webdav/");
	    	return;
	    }

		AuthenticationFilter.handleLocale(false, httpReq.getParameter("locale"), httpReq, httpRes);
		// when on root entry point
		if (redirectToDefaultLocation(httpReq, httpRes)) return;
		//find out if we have to do the guest login
	  	String user = httpReq.getParameter("user");
	  	if(user != null && user.equals("guest")){
	  		boolean guestAuthenticated = authenticateByGuest(httpReq);
	  		if(guestAuthenticated){
	  			chain.doFilter(req,res);
	  			return;
	  		}
	  	}
	    
	  	AuthenticationTool authTool = null;
	  	try{
	  		authTool = (AuthenticationTool)RepoFactory.getAuthenticationToolInstance(ApplicationInfoList.getHomeRepository().getAppId());
	  	}catch(Throwable e){
	  		log.error(e.getMessage(), e);
	  		httpRes.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	  	}
	  	//find out if we have to do the ticket login
	  	try{
			String ticket =  httpReq.getParameter("ticket");
  		  		if(ticket != null && !ticket.equals("")){
  				
  				if(authTool.validateTicket(ticket)){
  					//if its APIClient user name is ignored and is figured out with authentication service

					// Force a renew of all toolpermissions since they might have now changed!
					ToolPermissionServiceFactory.getInstance().getAllAvailableToolPermissions(true);
  					/**
  					 * auth type ticket means no profile editing allowed, 
  					 * authenticated by an lms or other connected system that
  					 * manages the users
  					 */
  					authTool.storeAuthInfoInSession(user, ticket, CCConstants.AUTH_TYPE_TICKET, httpReq.getSession());
  					chain.doFilter(req,res);
  					return;
  				}
	  		}
		}catch(Throwable e){
				e.printStackTrace();
		}
	  	
	  	
	  	String accessToken = req.getParameter(CCConstants.REQUEST_PARAM_ACCESSTOKEN);
	  	if(accessToken != null && !accessToken.trim().equals("")){
	  		
			//oAuth
			try {
				
				Token token = tokenService.getToken(accessToken);

				if (token != null) {
					
					//validate and set current user
					authTool.storeAuthInfoInSession(
							token.getUsername(), 
							token.getTicket(),
							CCConstants.AUTH_TYPE_OAUTH,
							httpReq.getSession());						
				}										
			} catch (Exception ex) {
				ex.printStackTrace();
				httpRes.sendError(HttpServletResponse.SC_FORBIDDEN,ex.getMessage());
				return;
			}
	  	}
  		
	    
	    //find out if the user is already authenticated
		boolean isValidated = false;
	    try{
			
			String ticket = authTool.getTicketFromSession( httpReq.getSession());
			log.debug("ticket from session:"+ticket);
			isValidated = authTool.validateTicket(ticket);
			
		}catch(Throwable e){
			e.printStackTrace();
		}

		if(isValidated){

			//default stuff
			chain.doFilter(req,res);
			return;
		}
		
		//for async GWT calls
		if(httpReq.getServletPath().contains(CCConstants.EDU_SHARING_SERVLET_PATH_GWT_RPC)){
			log.info("unauthorized call to gwt servlet");
			httpRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String requestUri = httpReq.getRequestURI();
		if(unprotectedPaths.stream().anyMatch(s -> requestUri.contains(s))){
			//default stuff
			chain.doFilter(req,res);
			return;
		}

		//redirect to login page
		this.redirectToLoginpage(httpReq, httpRes, chain);
	}

	public static void handleLocale(boolean throwOnError, String locale,  HttpServletRequest httpReq, HttpServletResponse httpRes) throws IOException {
		//set the locale
		try {
			if (locale == null && httpReq.getSession().getAttribute(CCConstants.AUTH_LOCALE) == null && httpReq.getLocale() != null) {
				locale = httpReq.getLocale().toString();
				throwOnError = false;
			}
			if(LocaleValidator.validate(locale, throwOnError)) {
				httpReq.getSession().setAttribute(CCConstants.AUTH_LOCALE, locale);
			}
		}catch(Throwable t){
			httpRes.sendError(HttpServletResponse.SC_BAD_REQUEST, t.getMessage());
		}
	}

	private void redirectToLoginpage(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException,ServletException{
		
		//remember the URL the user wants to get
		String loginSuccessRedirectUrl = req.getRequestURI() +( req.getQueryString() != null ? "?"+req.getQueryString() : "");
		
		if(loginSuccessRedirectUrl.contains(CCConstants.REQUEST_PARAM_DISABLE_GUESTFILTER)){
			loginSuccessRedirectUrl = UrlTool.removeParam(loginSuccessRedirectUrl, CCConstants.REQUEST_PARAM_DISABLE_GUESTFILTER);
		}
		URL url = new URL(req.getRequestURL().toString()+(StringUtils.isBlank(req.getQueryString()) ? "" : "?"+req.getQueryString()));
		if(!url.getPath().contains(NgServlet.COMPONENTS_ERROR)) {
			log.info(LOGIN_SUCCESS_REDIRECT_URL + ":" + loginSuccessRedirectUrl);
			req.getSession().setAttribute(LOGIN_SUCCESS_REDIRECT_URL, loginSuccessRedirectUrl);
		}

		// changed in 5.1: We always allow the webapp to load for mobile apps & angular routing
		boolean allowSSO = false;
		// if true, redirect to shib
		/*
		boolean allowSSO = true;
		try {
			Config config=ConfigServiceFactory.getCurrentConfig(req);
			// local login requested -> do not redirect on server-side
			if(req.getParameter("local")!=null && Boolean.parseBoolean(req.getParameter("local"))){
				allowSSO = false;
			}
			if(!(config.values.loginUrl != null && 
					config.values.loginUrl.contains("edu-sharing/shibboleth"))){
				// client based redirect, disable server-side redirect (guest support)
				allowSSO = false;
			}
			
			if(config.values.loginAllowLocal != null && config.values.loginAllowLocal) {
				allowSSO = false;
			}
			

		}catch(Throwable e) {
			log.error(e.getMessage());
		}

		 */

        // detect if the error component was requested -> then go ahead
        // otherwise, go to the angular login page
        URL requestUrl = new URL(req.getRequestURL().toString());
        if(requestUrl.getPath().contains(NgServlet.COMPONENTS_ERROR)){
            addErrorCode(resp, url);
            // go to the error component
            chain.doFilter(req, resp);
        }
        else {
            if(requestUrl.getPath().contains("eduservlet/")){
                resp.sendRedirect("/edu-sharing/shibboleth");
            } else {
                // go to angular login
                RequestDispatcher rp = req.getRequestDispatcher(AuthenticationFilter.PATH_LOGIN_ANGULAR);
                rp.forward(req, resp);
            }
        }

    }

	public static boolean redirectToDefaultLocation(HttpServletRequest req, HttpServletResponse resp) throws MalformedURLException {
		URL url = new URL(req.getRequestURL().toString());
		if(url.getPath().equals(CallSourceHelper.WEBAPP_BASE_PATH + "/")) {
			// navigate to default location
            try {
				String defaultLocation = ConfigServiceFactory.getCurrentConfig(req).getValue("defaultLocation", "login");
				if (defaultLocation.contains("://")) {
					resp.sendRedirect(defaultLocation);
				} else {
					resp.sendRedirect(CallSourceHelper.WEBAPP_BASE_PATH + "/components/" + defaultLocation);
				}
				return true;
            } catch (Exception e) {
                Logger.getLogger(AuthenticationFilter.class).error(e);
                try {
                    resp.sendRedirect(CallSourceHelper.WEBAPP_BASE_PATH + "/components/login");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

            }

		}
		return false;
	}

	private void addErrorCode(HttpServletResponse resp, URL url) {
		String error=url.getPath().substring(url.getPath().indexOf(NgServlet.COMPONENTS_ERROR) + NgServlet.COMPONENTS_ERROR.length() + 1);
		try {
			resp.setStatus(Integer.parseInt(error));
		}catch(Throwable t){
			log.info("Can not parse the reported error code "+error);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private boolean authenticateByGuest(HttpServletRequest req) {
		ApplicationInfo repHomeInfo = ApplicationInfoList.getRepositoryInfo(CCConstants.REPOSITORY_FILE_HOME);
		String guestUn = repHomeInfo.getGuest_username();
		String guestPw = repHomeInfo.getGuest_password();
		if(guestUn == null || guestPw == null){
			log.info("guest login not allowed");
			return false;
		}
		try{
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(null);
			Map<String,String> result = authTool.createNewSession(guestUn, guestPw);
			
			//save ticket in session
			HttpSession session = req.getSession();
			authTool.storeAuthInfoInSession(guestUn, result.get(CCConstants.AUTH_TICKET), CCConstants.AUTH_TYPE_DEFAULT,session);
			return true;
		}catch(Throwable e){
			log.error(e.getMessage(), e);
			return false;
		}
	}
	
}
