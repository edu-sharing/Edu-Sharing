package org.edu_sharing.restservices.login.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.TrackingApplicationInfo;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.RequestHelper;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions.SessionInfo;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.login.v1.model.AuthenticationToken;
import org.edu_sharing.restservices.login.v1.model.Login;
import org.edu_sharing.restservices.login.v1.model.LoginCredentials;
import org.edu_sharing.restservices.login.v1.model.ScopeAccess;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.UserProfileAppAuth;
import org.edu_sharing.service.authentication.*;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.spring.security.openid.SilentLoginModeRedirect;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/authentication/v1")
@Tag(name="AUTHENTICATION v1")
@ApiService(value="AUTHENTICATION", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class LoginApi {

	Logger logger = Logger.getLogger(LoginApi.class);

	@GET       
	@Path("/validateSession")
    @Operation(summary = "Validates the Basic Auth Credentials and check if the session is a logged in user", description = "Use the Basic auth header field to transfer the credentials")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Login.class))),        
    	})

    public Response login(@Context HttpServletRequest req) {
		
		
    	AuthenticationToolAPI authTool = new AuthenticationToolAPI();
    	boolean authenticated = (authTool.validateAuthentication(req.getSession()) == null) ? false : true;
		String personActiveStatus = null;
		if(!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
			personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
		}
		String status = null;
		if(authenticated && personActiveStatus != null && !personActiveStatus.trim().equals("")) {
			String username = (String)req.getSession().getAttribute(CCConstants.AUTH_USERNAME);
			NodeRef authorityNodeRef = AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(username);
			
			String personStatus = NodeServiceFactory.getLocalService().getProperty(authorityNodeRef.getStoreRef().getProtocol(), 
					authorityNodeRef.getStoreRef().getIdentifier(), 
					authorityNodeRef.getId(), CCConstants.CM_PROP_PERSON_ESPERSONSTATUS);
			// ignore the active status for admins to prevent a "lock out" from the system
			boolean allowAdminAccess = AuthorityServiceFactory.getLocalService().isGlobalAdmin() && personStatus == null;
			if(!personActiveStatus.equals(personStatus) && !allowAdminAccess) {
				authenticated = false;
				authTool.logoutWithoutSecurityContext(authTool.getTicketFromSession(req.getSession()));
				status = Login.STATUS_CODE_PERSON_BLOCKED;
				req.getSession().invalidate();
			}
		}

		if(status != null){
			return Response.ok(new Login(authenticated,authTool.getScope(),null, req.getSession(), status)).build();
		}else{
			return Response.ok(new Login(authenticated,authTool.getScope(),req.getSession())).build();
		}
    }

	@GET
	@Path("/validateSSOSession")
	@Operation(summary = "Validates if an provider (idp) session exists.", description = "If no provider session exists an 401 with 'login required' message is delivered. If true the current Login Object is shown.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Login.class))),
			})

	public Response validateSSOSession(@Context HttpServletRequest req, @Context HttpServletResponse resp) {

        try {
            if(SilentLoginModeRedirect.processSuccess(req,resp)){
                return Response.ok().build();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return this.login(req);
	}
    
    
    @POST      
   	@Path("/loginToScope")
       @Operation(summary = "Validates the Basic Auth Credentials and check if the session is a logged in user", description = "Use the Basic auth header field to transfer the credentials")
       
       @ApiResponses(
       	value = { 
       		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Login.class))),        
       	})

       public Response loginToScope(@Parameter(description = "credentials, example: test,test" , required=true ) LoginCredentials credentials,
    		   @Context HttpServletRequest req) {
       		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
       		ScopeAuthenticationService service = ScopeAuthenticationServiceFactory.getScopeAuthenticationService();
       	
       		Map<String,String> auth =  authTool.validateAuthentication(req.getSession());
       		if(auth == null){
       			return Response.ok(new Login(false,null,  null,req.getSession(),Login.STATUS_CODE_PREVIOUS_SESSION_REQUIRED)).build();
       		}
       		
       		if(!credentials.getUserName().equals(auth.get(CCConstants.AUTH_USERNAME))){
       			return Response.ok(new Login(false,null,  null,req.getSession(),Login.STATUS_CODE_PREVIOUS_USER_WRONG)).build();
       		}
       		
       		/**
       		 * String shibbolethSessionId = getShibValue("Shib-Session-ID", req);
			
			if(shibbolethSessionId != null && !shibbolethSessionId.trim().equals("")){
				ShibbolethSessions.put(shibbolethSessionId, new SessionInfo(ticket, req.getSession()));
				req.getSession().setAttribute(CCConstants.AUTH_SSO_SESSIONID, shibbolethSessionId);
			}
       		 */
       		
       		/**
       		 * remember shibboleth session id to kill safe scope session by LogoutNotiFication 
       		 * 
       		 */
       		String shibbolethSessionId = (String)req.getSession().getAttribute(CCConstants.AUTH_SSO_SESSIONID);
       		
       		
       		String loginStatus = service.authenticate(
       					credentials.getUserName(), 
       					credentials.getPassword(), 
       					credentials.getScope());
       		
       		String userHome = null;
       		
       		String statusCode = Login.STATUS_CODE_OK;
       		if(Login.STATUS_CODE_OK.equals(loginStatus)){
       			
       			NodeRef ref  = ScopeUserHomeServiceFactory.getScopeUserHomeService().getUserHome(credentials.getUserName(), credentials.getScope(), true);
       			userHome = ref.getId();
               	req.getSession().setMaxInactiveInterval(service.getSessionTimeout());
               	
               	if(shibbolethSessionId != null && !shibbolethSessionId.trim().equals("")){
    				ShibbolethSessions.put(shibbolethSessionId, new SessionInfo((String)req.getSession().getAttribute(CCConstants.AUTH_TICKET), req.getSession()));
    				req.getSession().setAttribute(CCConstants.AUTH_SSO_SESSIONID, shibbolethSessionId);
    			}
               	
       		}else{
       			statusCode = loginStatus;
       		}
       		return Response.ok(new Login(Login.STATUS_CODE_OK.equals(loginStatus) ? true : false,authTool.getScope(),userHome,req.getSession(),statusCode)).build();
       }
    
    @GET       
   	@Path("/hasAccessToScope")
       @Operation(summary = "Returns true if the current user has access to the given scope", description = "")
       
       @ApiResponses(
       	value = { 
           		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
           		@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = Void.class))),        
       	})

       public Response hasAccessToScope(
    		   @Parameter(description = "scope" , required=true ) @QueryParam("scope") String scope,
    		   @Context HttpServletRequest req) {
    	try{
       		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
       		ScopeAuthenticationService service = ScopeAuthenticationServiceFactory.getScopeAuthenticationService();
       		boolean access=service.checkScope(authTool.getCurrentUser(), scope);
    		return Response.ok(new ScopeAccess(access)).build();
    	}catch(Throwable t){
    		return ErrorResponse.createResponse(t);
    	}
       }
    @GET       
   	@Path("/destroySession")
       @Operation(summary = "Destroys the current session and logout the user", description = "")
       
       @ApiResponses(
       	value = { 
           		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
           		@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = Void.class))),        
       	})

       public Response logout(@Context HttpServletRequest req) {
    	try{
    		new AuthenticationToolAPI().logout((String)req.getSession().getAttribute(CCConstants.AUTH_TICKET));
    		req.getSession().invalidate();
    		return Response.ok().build();
    	}catch(Throwable t){
    		// this may fail when the user is not logged in, so he is logged out anyway 
    		//return ErrorResponse.createResponse(t);
    		return Response.ok().build();
    	}
       }
       
    @OPTIONS        
    @Hidden
    public Response options() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }

    static List<String> disallowedProps = Arrays.asList(new String[]{CCConstants.CM_PROP_PERSON_SIZE_QUOTA});


	@POST
	@Path("/appauth/{userId}")
	@Operation(summary = "authenticate user of an registered application.", description = "headers must be set: X-Edu-App-Id, X-Edu-App-Sig, X-Edu-App-Signed, X-Edu-App-Ts")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = AuthenticationToken.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})
	public Response authenticate(@Parameter(description = "User Id", required=true ) @PathParam("userId") String userId,
								 @Parameter(description = "User Profile", required=false) UserProfileAppAuth userProfile,
								 @Context HttpServletRequest req){

		try {
			ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
			SSOAuthorityMapper ssoMapper = (SSOAuthorityMapper) eduApplicationContext.getBean("ssoAuthorityMapper");

			TrackingApplicationInfo verifiedApp = ContextManagementFilter.accessTool.get();
			if(verifiedApp == null){
				String msg = "no app was verified. check if AppSignatureFilter is configured";
				logger.error(msg);
				return Response.status(Response.Status.PRECONDITION_FAILED).entity(msg).build();
			}

			Map<String, String> ssoDataMap = new HashMap<>();
			ssoDataMap.put(ssoMapper.getSSOUsernameProp(), userId);

			//add authByAppData
			ssoDataMap.put(SSOAuthorityMapper.PARAM_APP_ID, verifiedApp.getApplicationInfo().getAppId());
			ssoDataMap.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_AuthByApp);
			/**
			 * @TODO check if host validation still needed
			 * @org.edu_sharing.service.authentication.AuthMethodTrustedApplication.authenticate
			 */
			ssoDataMap.put(SSOAuthorityMapper.PARAM_APP_IP, new RequestHelper(req).getRemoteAddr());

			if(userProfile != null){
				String firstNameProp = ssoMapper.getUserAttribute(CCConstants.PROP_USER_FIRSTNAME);
				if(firstNameProp != null && userProfile.getFirstName() != null){
					ssoDataMap.put(firstNameProp,userProfile.getFirstName());
				}
				String lastNameProp = ssoMapper.getUserAttribute(CCConstants.PROP_USER_LASTNAME);
				if(lastNameProp != null && userProfile.getLastName() != null){
					ssoDataMap.put(lastNameProp,userProfile.getLastName());
				}
				String mailProp = ssoMapper.getUserAttribute(CCConstants.PROP_USER_EMAIL);
				if(mailProp != null && userProfile.getEmail() != null){
					ssoDataMap.put(mailProp,userProfile.getEmail());
				}
				String affiliationProp = ssoMapper.getUserAttribute(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION);
				if(affiliationProp != null && userProfile.getPrimaryAffiliation() != null){
					ssoDataMap.put(affiliationProp, userProfile.getPrimaryAffiliation());
				}

				if(userProfile.getExtendedAttributes() != null){
					for(Map.Entry<String,String[]> extAtt : userProfile.getExtendedAttributes().entrySet()){
						String val = (extAtt.getValue() != null) ? String.join(",",extAtt.getValue()) : null;
						if(val != null){
							ssoDataMap.put(extAtt.getKey(),val);
						}
					}
				}
			}

			EduAuthentication authService = (EduAuthentication) eduApplicationContext.getBean("authenticationService");
			authService.authenticateByTrustedApp(ssoDataMap);

			AuthenticationToken result = new AuthenticationToken();
			result.setTicket(authService.getCurrentTicket());
			result.setUserId(authService.getCurrentUserName());
			return Response.status(Response.Status.OK).entity(result).build();
		}catch (Throwable e){
			return ErrorResponse.createResponse(e);
		}
	}
}

