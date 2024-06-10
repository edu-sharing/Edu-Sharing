package org.edu_sharing.repository.server.authentication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class LoginServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	public static final String PARAM_USERNAME = "username";
	
	public static final String PARAM_PASSWORD = "password";
	
	public static final String PARAM_REDIRECT = "redirect";
	
	public static final String ERR_AUTH_FAILED = "ERR_AUTH_FAILED";
	
	public static final String AUTH_RESULT_ERROR = "AUTH_RESULT_ERROR";

	private static Logger log = Logger.getLogger(LoginServlet.class);

	private void loginProcess(HttpServletRequest req, HttpServletResponse resp, String userName, String password, String redirectSuccessUrl) throws ServletException, IOException {
		try{
			if (redirectSuccessUrl==null){
				log.warn("missing redirect url");
				ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
				redirectSuccessUrl = appInfo.getClientBaseUrl();
			}
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(ApplicationInfoList.getHomeRepository().getAppId());
			Map<String,String> authInfo = authTool.createNewSession(userName, password);
			authTool.storeAuthInfoInSession(userName, authInfo.get(CCConstants.AUTH_TICKET), CCConstants.AUTH_TYPE_DEFAULT, req.getSession());
			if (redirectSuccessUrl.length()<=1) {
				try {Thread.sleep(1000);} catch(Exception ex) {}
				resp.getWriter().print("OK");
			} else {
				resp.sendRedirect(resp.encodeURL(redirectSuccessUrl));
			}
		} catch(Throwable e) {
			log.error(e.getMessage(),e);
			req.setAttribute(AUTH_RESULT_ERROR, ERR_AUTH_FAILED);
			if ((redirectSuccessUrl!=null) && (redirectSuccessUrl.length()<=1)) {
				try {Thread.sleep(1000);} catch(Exception ex) {}
				resp.getWriter().print("FAIL");
			} else {
				RequestDispatcher rp = req.getRequestDispatcher(AuthenticationFilter.PATH_LOGIN_ANGULAR);
			    rp.forward(req,resp); 
			}
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// get USERNAME and PASSWORD from POST body
		String username = req.getParameter(PARAM_USERNAME);
		String password = req.getParameter(PARAM_PASSWORD);
		
		// get REDIRECT-URL from HTTP session
		String redirectSuccessUrl = null;
		HttpSession httpSession = req.getSession();
		if (httpSession!=null) redirectSuccessUrl = (String) httpSession.getAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL);
		
		// security check
		if (!req.isSecure()) log.error("Use of POST-Login without HTTPS is a security risk! Make sure this is not the default on production.");
		
		// do login process
		loginProcess(req, resp, username, password, redirectSuccessUrl);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// get USERNAME, PASSWORD and REDIRECT-URL from GET url
		String username = req.getParameter(PARAM_USERNAME);
		String password = req.getParameter(PARAM_PASSWORD);
		String redirectSuccessUrl = req.getParameter(PARAM_REDIRECT);
		
		// forward to login page if any of the parameters above is missing
		if ((username==null) || (password==null) || (redirectSuccessUrl==null)) {
			log.warn("missing parameters on GET login-servlet ... forwarding to login page");
			RequestDispatcher rp = req.getRequestDispatcher(AuthenticationFilter.PATH_LOGIN_ANGULAR);
		    rp.forward(req,resp); 
		    return;
		}
		
		// security check
		if (!req.isSecure()) log.error("Use of GET-Login without HTTPS is a BIG security risk! Make sure this is not the default on production.");
		
		// do login process
		resp.addHeader("Access-Control-Allow-Origin", "*");
		loginProcess(req, resp, username, password, redirectSuccessUrl);
		
	}
	
}
