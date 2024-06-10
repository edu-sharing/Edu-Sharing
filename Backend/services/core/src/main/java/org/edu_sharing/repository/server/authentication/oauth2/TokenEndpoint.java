package org.edu_sharing.repository.server.authentication.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.service.authentication.EduAuthentication;
import org.edu_sharing.service.authentication.oauth2.TokenService;
import org.edu_sharing.service.authentication.oauth2.TokenService.Token;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TokenEndpoint extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private transient TokenService tokenService;
	private transient AuthenticationComponent authenticationComponent;
	private transient EduAuthentication eduAuthenticationService;
	
	@Override
	public void init() throws ServletException {
		
		ApplicationContext eduApplicationContext = 
				org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
		
		tokenService = (TokenService) eduApplicationContext.getBean("oauthTokenService");
		
		ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
		authenticationComponent = (AuthenticationComponent) alfApplicationContext.getBean("authenticationComponent");
		
		eduAuthenticationService = (EduAuthentication) eduApplicationContext.getBean("authenticationService");
		
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try {

			try {

				EduOAuthTokenRequest oauthRequest = new EduOAuthTokenRequest(request);
				OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());

				String clientId = oauthRequest.getClientId();
	
				tokenService.validateClient(clientId, oauthRequest.getClientSecret());
				
				String accessToken = oauthIssuerImpl.accessToken();
				String refreshToken = oauthIssuerImpl.refreshToken();
				
				String grantType = oauthRequest.getGrantType();				
				
				if (GrantType.PASSWORD.toString().equals(grantType)) {
					
					// Resource Owner Password Credentials Flow
					
					try {
						
						String username = oauthRequest.getUsername();
						
						// check
						Map<String, String> authInfo = RepoFactory.getAuthenticationToolInstance(null)
							.createNewSession(username, oauthRequest.getPassword());

						tokenService.createToken(username, accessToken, refreshToken, clientId,authInfo.get(CCConstants.AUTH_TICKET));
                        TrackingServiceFactory.getTrackingService().trackActivityOnUser(username,TrackingService.EventType.LOGIN_USER_OAUTH_PASSWORD);
						
					} catch (Throwable e) {
						
						throw OAuthProblemException.error(e.getMessage());
					}
					
				} else if (GrantType.REFRESH_TOKEN.toString().equals(grantType)) {
				
					// Token Refresh Flow
					
					try {
						AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(null);
						Token oldToken = tokenService.getRefreshToken(oauthRequest.getRefreshToken());
						
						String ticket = null;
						if(authTool.validateTicket(oldToken.getTicket())){
							ticket = oldToken.getTicket();
						}else{
							authenticationComponent.setCurrentUser(oldToken.getUsername());
							ticket = eduAuthenticationService.getCurrentTicket();
						}
						
						Token newToken=tokenService.refreshToken(oauthRequest.getRefreshToken(), accessToken, refreshToken, clientId, ticket);
                        TrackingServiceFactory.getTrackingService().trackActivityOnUser(newToken.getUsername(),TrackingService.EventType.LOGIN_USER_OAUTH_REFRESH_TOKEN);

                    } catch (Throwable e) {
						Logger.getLogger(TokenEndpoint.class).warn(e.toString());
						throw OAuthProblemException.error(e.getMessage());
					}
				}
				else if (GrantType.CLIENT_CREDENTIALS.toString().equals(grantType)) {
					try {

						Map<String, String> authInfo = RepoFactory.getAuthenticationToolInstance(null)
							.validateAuthentication(request.getSession());
						String userName=authInfo.get("UserName");
						if(authInfo==null || userName==null)
							throw OAuthProblemException.error("Invalid session");
						if(request.getSession().getAttribute(CCConstants.AUTH_SCOPE)!=null)
							throw OAuthProblemException.error("OAuth not allowed for scoped session");
						Logger.getLogger(TokenEndpoint.class).info("auth via session for "+userName);
						tokenService.createToken(userName, accessToken, refreshToken, clientId,authInfo.get(CCConstants.AUTH_TICKET));
						
					} catch (Throwable e) {
						
						throw OAuthProblemException.error(e.getMessage());
					}
				}
				OAuthResponse r = OAuthASResponse
						.tokenResponse(HttpServletResponse.SC_OK)
						.setAccessToken(accessToken).setExpiresIn(Long.toString(tokenService.getExpiresIn()))
						.setRefreshToken(refreshToken)
						.buildJSONMessage();
	
				response.setStatus(r.getResponseStatus());
				PrintWriter pw = response.getWriter();
				pw.print(r.getBody());
				pw.flush();
				pw.close();
	
			} catch (OAuthProblemException ex) {
	
				OAuthResponse r = 
						OAuthResponse
						.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
						.error(ex)
						.buildJSONMessage();
	
				response.setStatus(r.getResponseStatus());
	
				PrintWriter pw = response.getWriter();
				pw.print(r.getBody());
				pw.flush();
				pw.close();
			}
			
		} catch (OAuthSystemException e) {

			throw new ServletException(e);
		}

	}
}
