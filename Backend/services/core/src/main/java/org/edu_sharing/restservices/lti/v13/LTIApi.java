package org.edu_sharing.restservices.lti.v13;

import com.google.gson.Gson;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import edu.uoc.elc.lti.tool.Tool;
import edu.uoc.elc.lti.tool.oidc.LoginRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.lti.v13.model.JWKResult;
import org.edu_sharing.restservices.lti.v13.model.JWKSResult;
import org.edu_sharing.restservices.lti.v13.model.RegistrationUrl;
import org.edu_sharing.restservices.ltiplatform.v13.LTIPlatformConstants;
import org.edu_sharing.restservices.ltiplatform.v13.model.ValidationException;
import org.edu_sharing.restservices.rendering.v1.RenderingApi;
import org.edu_sharing.restservices.rendering.v1.model.RenderingDetailsEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeLTIDeepLink;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.lti13.LTIConstants;
import org.edu_sharing.service.lti13.LTIJWTUtil;
import org.edu_sharing.service.lti13.RepoTools;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.lti13.registration.DynamicRegistrationToken;
import org.edu_sharing.service.lti13.registration.DynamicRegistrationTokens;
import org.edu_sharing.service.lti13.registration.RegistrationService;
import org.edu_sharing.service.lti13.uoc.Config;
import org.edu_sharing.service.usage.Usage2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.net.*;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

@Path("/lti/v13")
@Consumes({ "text/html" })
@Produces({"text/html"})
@Tag(name="LTI v13")
public class LTIApi {

    Logger logger = Logger.getLogger(LTIApi.class);

    Usage2Service usageService = new Usage2Service();

    @Autowired
    AuthenticationComponent authenticationComponent;

    @Autowired
    RegistrationService registrationService;

    @POST
    @Path("/oidc/login_initiations")
    @Operation(summary = "lti authentication process preparation.", description = "preflight phase. prepares lti authentication process. checks it issuer is valid")
    @Consumes({"application/x-www-form-urlencoded"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response loginInitiations(@Parameter(description = "Issuer of the request, will be validated",required=true) @FormParam(LTIConstants.LTI_PARAM_ISS) String iss,
                                 @Parameter(description = "target url of platform at the end of the flow",required=true) @FormParam(LTIConstants.LTI_PARAM_TARGET_LINK_URI) String targetLinkUrl,
                                 @Parameter(description = "Id of the issuer",required=false) @FormParam(LTIConstants.LTI_PARAM_CLIENT_ID) String clientId,
                                 @Parameter(description = "context information of the platform",required=false) @FormParam(LTIConstants.LTI_PARAM_LOGIN_HINT) String loginHint,
                                 @Parameter(description = "additional context information of the platform",required=false) @FormParam(LTIConstants.LTI_PARAM_MESSAGE_HINT) String ltiMessageHint,
                                 @Parameter(description = "A can have multiple deployments in a platform",required=false) @FormParam(LTIConstants.LTI_PARAM_DEPLOYMENT_ID) String ltiDeploymentId,
                                 @Context HttpServletRequest req
                                 ){
        /**
         * @TODO check if multiple deployments got an own key pair
         */
        
        try {
            return loginInitiationsCore(iss, clientId, ltiDeploymentId, req);

        } catch (Throwable e) {
            logger.error(e.getMessage(),e);
            return ApiTool.processError(req,e,"LTI_ERROR");
        }
    }

    @GET
    @Path("/oidc/login_initiations")
    @Operation(summary = "lti authentication process preparation.", description = "preflight phase. prepares lti authentication process. checks it issuer is valid")
    @Consumes({"application/x-www-form-urlencoded"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response loginInitiationsGet(@Parameter(description = "Issuer of the request, will be validated",required=true) @QueryParam(LTIConstants.LTI_PARAM_ISS) String iss,
                                     @Parameter(description = "target url of platform at the end of the flow",required=true) @QueryParam(LTIConstants.LTI_PARAM_TARGET_LINK_URI) String targetLinkUrl,
                                     @Parameter(description = "Id of the issuer",required=false) @QueryParam(LTIConstants.LTI_PARAM_CLIENT_ID) String clientId,
                                     @Parameter(description = "context information of the platform",required=false) @QueryParam(LTIConstants.LTI_PARAM_LOGIN_HINT) String loginHint,
                                     @Parameter(description = "additional context information of the platform",required=false) @QueryParam(LTIConstants.LTI_PARAM_MESSAGE_HINT) String ltiMessageHint,
                                     @Parameter(description = "A can have multiple deployments in a platform",required=false) @QueryParam(LTIConstants.LTI_PARAM_DEPLOYMENT_ID) String ltiDeploymentId,
                                     @Context HttpServletRequest req
    ){
        /**
         * @TODO check if multiple deployments got an own key pair
         */

        try {
            return loginInitiationsCore(iss, clientId, ltiDeploymentId, req);

        } catch (Throwable e) {
            logger.error(e.getMessage(),e);
            return ApiTool.processError(req,e,"LTI_ERROR");
        }
    }

    private Response loginInitiationsCore(String iss, String clientId, String ltiDeploymentId, HttpServletRequest req) throws Exception {
        RepoTools repoTools = new RepoTools();
        ApplicationInfo platform = repoTools.getApplicationInfo(iss, clientId, ltiDeploymentId);
        Tool tool = Config.getTool(platform, req,true);
        /**
         * @TODO
         *  jakarta/javax lib problem
         *  justed fixed compile problems
         */
        // get data from request
        final LoginRequest loginRequest = null;//LoginRequestFactory.from(req);
        if (this.logger.isInfoEnabled()) {
            this.logger.info("OIDC launch received with " + loginRequest.toString());
        }

        String targetLinkUri = loginRequest.getTarget_link_uri();
        if(targetLinkUri == null){
            throw new Exception("Bad request targetLinkUri is null. check tool config on platform side.");
        }

        final URI uri = new URI(targetLinkUri);
        /* commented in local because localhost resolves to 0:0:0:0*/
        String host = uri.getHost();
        String remoteHost = new URI(req.getRequestURL().toString()).getHost();

        logger.info("host:" + host + " remoteHost:"+remoteHost);
        if (!host.equals(remoteHost)) {
            throw new Exception("Bad request target uri host:" + host +" remoteHost:"+remoteHost);
        }


        // do the redirection
        String authRequest = tool.getOidcAuthUrl(loginRequest);
        /**
         *  fix param encoding of params that are not properly url encoded by lti library class
         *  edu.uoc.elc.lti.tool.oidc.AuthRequestUrlBuilder
         */
        if(loginRequest.getLti_message_hint() != null) {
            authRequest = UrlTool.replaceParam(authRequest, "lti_message_hint",
                    URLEncoder.encode(loginRequest.getLti_message_hint(),"UTF-8"));
        }
        /**
         * fix: when it's an LtiResourceLinkRequest moodle sends rendering url (/edu-sharing/components/render)
         * as targetUrl. edu.uoc.elc.lti.tool.Tool take this url for redirect_url which is wrong.
         * moodle validates redirect url against config redirecturl which would fail with nodeId
         *
         * we can not detect if it will be an ResourceLink or Deeplink call here.
         * This fix is only for ResourceLink calls. Deeplinks use the same redirect url so this is ok here.
         */
        authRequest = UrlTool.replaceParam(authRequest,"redirect_uri",ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/rest/lti/v13/" + LTIConstants.LTI_TOOL_REDIRECTURL_PATH);


        //response.sendRedirect(authRequest);
        return Response.status(302).location(new URI(authRequest)).build();
    }

    /**
     * store multiple for allowing multiple deployments of edu-sharing(ltitool) in lms(lti platform)
     * @param session
     * @param params
     */
    private void storeInSession(HttpSession session, Map<String,String> params){
        for(Map.Entry<String,String> entry : params.entrySet()){
            List<String> list = (List<String>)session.getAttribute(entry.getKey());
            if(list == null){
               list = new ArrayList<>();
            }
            list.add(entry.getValue());
            session.setAttribute(entry.getKey(), list);
        }
    }




    @POST
    @Path("/" + LTIConstants.LTI_TOOL_REDIRECTURL_PATH  )
    @Operation(summary = "lti tool redirect.", description = "lti tool redirect")

    @Consumes({ "application/x-www-form-urlencoded" })
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response lti(@Parameter(description = "Issuer of the request, will be validated",required=true) @FormParam("id_token") String idToken,
                        @Parameter(description = "Issuer of the request, will be validated",required=true) @FormParam("state") String state,
                        @Context HttpServletRequest req){
        logger.info("id_token:"+idToken +" state:"+state);
        try{
            return ltiLaunch(idToken, state, req, null);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            return ApiTool.processError(req,e,"LTI_ERROR");
        }
    }

    @POST
    @Path("/" + LTIConstants.LTI_TOOL_REDIRECTURL_PATH +"/{nodeId}"  )
    @Operation(summary = "lti tool resource link target.", description = "used by some platforms for direct (without oidc login_init) launch requests")

    @Consumes({ "application/x-www-form-urlencoded" })
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response ltiTarget(@Parameter(description = "Issuer of the request, will be validated",required=true) @FormParam("id_token") String idToken,
                        @Parameter(description = "Issuer of the request, will be validated",required=true) @FormParam("state") String state,
                        @Parameter(description = "edu-sharing node id",required = true) @PathParam("nodeId") String nodeId,
                        @Context HttpServletRequest req){
        logger.info("id_token:"+idToken +" state:"+state +" nodeId:" + nodeId);
        try{
            return ltiLaunch(idToken, state, req, nodeId);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            return ApiTool.processError(req,e,"LTI_ERROR");
        }
    }

    private Response ltiLaunch(String idToken, String state, HttpServletRequest req, String nodeId) throws Exception {
        if(state == null) {
            throw new IllegalStateException("no state param provided");
        }

        if (idToken == null || !StringUtils.hasText(idToken)) {
            String message = "The request is not a LTI request, so no credentials at all. Returning current credentials";
            this.logger.error(message);
            throw new IllegalStateException(message);
        }

        /**
         * get claims cause we need clientId,deploymentId,iss for applicationinfo of plattform to instance tool
         * token will be validated with public key of the platform app
         * @TODO use keyset url
         */
        LTIJWTUtil ltijwtUtil = new LTIJWTUtil();
        Jws<Claims> jws = ltijwtUtil.validateJWT(idToken);

        if (jws == null) {
            throw new IllegalStateException("jws is null");
        }

        /**
         * validate nonce
         */
        String nonce = jws.getBody().get("nonce", String.class);
        /**
         * @TODO
         *  jakarta/javax lib problem
         *  justed fixed compile problems
         */
        String sessionNonce = null;//new HttpSessionOIDCLaunchSession(req).getNonce();
        if(!nonce.equals(sessionNonce)){
            logger.error("nonce:"+nonce+ " sessionNonce:"+sessionNonce +". maybe jsessionid is not the same for login_initiation and launch url. ");
            throw new IllegalStateException("nonce is invalid");
        }

        Tool tool = Config.getTool(ltijwtUtil.getApplicationInfo(), req,false);

        /**
         * Launch validation: validates authentication response, and specific message(deeplink,....) validation
         * https://www.imsglobal.org/spec/security/v1p0/#authentication-response-validation
         */
        logger.info("idToken:"+idToken+" state:"+state);
        tool.validate(idToken, state);
        if (!tool.isValid()) {
            logger.error(tool.getReason());
            throw new IllegalStateException(tool.getReason());
        }

        //check version
        String ltiVersion = jws.getBody().get(LTIConstants.LTI_VERSION, String.class);
        if(!LTIConstants.LTI_VERSION_3.equals(ltiVersion)){
            throw new Exception("lti version:" +ltiVersion +" not allowed");
        }


            /*List<String> sessionStates = (List<String>)req.getSession().getAttribute(LTIConstants.LTI_TOOL_SESS_ATT_STATE);
            if(sessionStates == null){
                throw new IllegalStateException("no states initiated for this session");
            }

            if(!sessionStates.contains(state)){
                throw new IllegalStateException("LTI request doesn't contains the expected state");
            }*/

        //Now we validate the JWT token


        /**
         * safe to session for later usage
         */
        String ltiMessageType = jws.getBody().get(LTIConstants.LTI_MESSAGE_TYPE,String.class);
        LTISessionObject ltiSessionObject = new LTISessionObject();
        ltiSessionObject.setDeploymentId(jws.getBody().get(LTIConstants.LTI_DEPLOYMENT_ID,String.class));
        ltiSessionObject.setIss(jws.getBody().get(LTIConstants.LTI_PARAM_ISS,String.class));
        ltiSessionObject.setNonce(jws.getBody().get(LTIConstants.LTI_NONCE,String.class));
        ltiSessionObject.setMessageType(ltiMessageType);
        ltiSessionObject.setEduSharingAppId(new RepoTools().getAppId(ltiSessionObject.getIss(),
                jws.getBody().getAudience(),
                ltiSessionObject.getDeploymentId()));

        Map<String,Object> context = jws.getBody().get(LTIConstants.CONTEXT, Map.class);
        if(context != null){
            String courseId = (String)context.get("id");
            if (courseId != null) {
                ltiSessionObject.setContextId(courseId);
            }
        }


        /**
         * edu-sharing authentication
         */
        if(!ltiMessageType.equals(LTIConstants.LTI_MESSAGE_TYPE_DEEP_LINKING) &&
                !ApplicationInfoList.getRepositoryInfoById(ltiSessionObject.getEduSharingAppId()).isLtiSyncReaders()){
            //authenticationComponent.setCurrentUser(AuthorityServiceImpl.PROXY_USER);
            RepoTools.authenticate(req,
                    RepoTools.mapToSSOMap(CCConstants.PROXY_USER, null, null, null));
        }else{
            String user = jws.getBody().getSubject();
            Map<String,String> ext = ( Map<String,String>)jws.getBody().get("https://purl.imsglobal.org/spec/lti/claim/ext",Map.class);
            if(ext != null){
                if(ext.containsKey("user_username")){
                    String tmpUser = ext.get("user_username");
                    if(tmpUser != null && !tmpUser.trim().isEmpty()){
                        user = tmpUser;
                    }
                }
            }
            if(ltijwtUtil.getApplicationInfo().isLtiScopeUsername()){
                String scope = jws.getBody().getIssuer();
                try {
                    URL url = new URL(scope);
                    url.toURI();
                    scope = url.getHost();
                } catch (MalformedURLException | URISyntaxException e) {}

                user = user+"@"+scope;
            }else{
                /**
                 * prevent an lti platform can becomes an alfresco admin
                 */
                user = SSOAuthorityMapper.mapAdminAuthority(user,ltijwtUtil.getApplicationInfo().getAppId());
            }

            String name = jws.getBody().get(LTIConstants.LTI_NAME, String.class);
            String familyName = jws.getBody().get(LTIConstants.LTI_FAMILY_NAME, String.class);
            String givenName = jws.getBody().get(LTIConstants.LTI_GIVEN_NAME, String.class);
            String email = jws.getBody().get(LTIConstants.LTI_EMAIL, String.class);

            String authenticatedUsername = RepoTools.authenticate(req,
                    RepoTools.mapToSSOMap(user, givenName, familyName, email));
        }

        URI toRedirectTo = null;

        if(ltiMessageType.equals(LTIConstants.LTI_MESSAGE_TYPE_DEEP_LINKING)){
            if(jws.getBody().containsKey(LTIConstants.DEEP_LINKING_SETTINGS)){
                Map deepLinkingSettings = jws.getBody().get(LTIConstants.DEEP_LINKING_SETTINGS, Map.class);
                ltiSessionObject.setDeepLinkingSettings(deepLinkingSettings);
            }
            /**
             * @TODO check if this kind of redirect works
             */

            //return Response.status(302).location(new URI(ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/edu-sharing/components/search")).build();
            toRedirectTo = new URI(ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/components/search");
            //return Response.temporaryRedirect(new URI("/edu-sharing/components/search")).build();
        }else if(ltiMessageType.equals(LTIConstants.LTI_MESSAGE_TYPE_RESOURCE_LINK)){
            //rendering stuff
            /**
             * @TODO check for launch_presentation
             * "https://purl.imsglobal.org/spec/lti/claim/launch_presentation": {
             *     "locale": "en",
             *     "document_target": "iframe",
             *     "return_url": "http://localhost/moodle/mod/lti/return.php?course=2&launch_container=3&instanceid=1&sesskey=q6noraEPlA"
             *   }
             */

            /**
             * moodle uses redirect url which does not contain a nodeid
             */
            if(nodeId == null){
                String ltiTargetLink = jws.getBody().get(LTIConstants.LTI_TARGET_LINK_URI, String.class);
                String[] splitted = ltiTargetLink.split("/");
                nodeId = splitted[splitted.length -1].split("\\?")[0];
            }
            String targetLink = ApplicationInfoList.getHomeRepository().getClientBaseUrl() + "/components/render/"+ nodeId +"?closeOnBack=true";

            Map<String,String> lauchPresentation = jws.getBody().get(LTIConstants.LTI_LAUNCH_PRESENTATION,Map.class);
            if(lauchPresentation != null && lauchPresentation.containsKey("document_target")){
                String documentTarget = lauchPresentation.get("document_target");
                if(documentTarget != null && (documentTarget.equals("iframe") || documentTarget.equals("frame")) ){
                    //@TODO version???
                    targetLink = ApplicationInfoList.getHomeRepository().getClientBaseUrl() +"/eduservlet/render?node_id="+nodeId+ "&version=-1";
                }
            }

            ApiTool.handleUsagePermissions(nodeId, req.getSession(), ltiSessionObject.getEduSharingAppId(), ltiSessionObject.getContextId(), usageService);

            toRedirectTo = new URI(targetLink);
            //return Response.temporaryRedirect(new URI(targetLink)).build();
        }else{
            String message = "can not handle message type:" + ltiMessageType;
            logger.error(message);
            throw new Exception(message);
        }

        /**
         * @TODO: what happens when user is using the sames session within two browser windows
         * maybe use list of LTISessionObject's
         */
        req.getSession().setAttribute(LTISessionObject.class.getName(),ltiSessionObject);
        return Response.seeOther(toRedirectTo).build();

    }


    @GET
    @Path("/generateDeepLinkingResponse")
    @Operation(summary = "generate DeepLinkingResponse")
    @Consumes({ "application/json" })
    @Produces({ "application/json"})
    @ApiResponses(value = {
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeLTIDeepLink.class))),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response generateDeepLinkingResponse(@Parameter(description = "selected node id's",required=true)  @QueryParam("nodeIds")  List<String> nodeIds,
                                                @Context HttpServletRequest req){
        LTISessionObject ltiSessionObject = (LTISessionObject)req
                .getSession()
                .getAttribute(LTISessionObject.class.getName());
        try {
            if (ltiSessionObject != null) {
                RepositoryDao repoDao = RepositoryDao.getHomeRepository();
                List<Node> nodes = new ArrayList<>();
                for(String nodeId : nodeIds){
                    Node n = NodeDao.getNode(repoDao, nodeId).asNode();
                    nodes.add(n);
                }

                NodeLTIDeepLink dl = new NodeLTIDeepLink((String)ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_RETURN_URL),
                        new LTIJWTUtil().getDeepLinkingResponseJwt(ltiSessionObject, nodes.toArray(new Node[]{})));

                if(ApplicationInfoList.getRepositoryInfoById(ltiSessionObject.getEduSharingAppId()).isLtiUsagesEnabled()){
                    String user = AuthenticationUtil.getFullyAuthenticatedUser();
                    for(String nodeId : nodeIds) {
                        usageService.setUsage(ApplicationInfoList.getHomeRepository().getAppId(),
                                user,
                                ltiSessionObject.getEduSharingAppId(),
                                ltiSessionObject.getContextId(),
                                nodeId,
                                (String) AuthorityServiceFactory.getLocalService().getUserInfo(user).get(CCConstants.PROP_USER_EMAIL),
                                null,null,-1,null,
                                null, //TODO moodle does not deliver such information
                                null);
                    }
                }
                req.getSession().removeAttribute(LTISessionObject.class.getName());
                return Response.ok(dl).build();
            }else{
                throw new Exception("no active lti session");
            }
        }catch (Throwable t){
            return ErrorResponse.createResponse(t);
        }
    }

    /**
     * jsonResponse.put("jwks_uri",homeApp.getClientBaseUrl()+"/rest/lti/v13/jwks");
     *
     */
    @GET
    @Path("/jwks")
    @Operation(summary = "LTI - returns repository JSON Web Key Sets")
    @Consumes({ "application/json" })
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = RegistrationUrl.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response jwksUri(){
        try {

            ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();
            Signing signing = new Signing();
            PublicKey pemPublicKey = signing.getPemPublicKey(homeApp.getPublicKey(), CCConstants.SECURITY_KEY_ALGORITHM);
            RSAPublicKey pub = (RSAPublicKey)pemPublicKey;
            JWKSResult rs = new JWKSResult();

            String kid = homeApp.getLtiKid();

            JWKResult JWKResult = new Gson().fromJson(new RSAKey.Builder(pub)
                    .keyUse(KeyUse.SIGNATURE)
                    //.privateKey((RSAPrivateKey)privKey)
                    .keyID(kid)
                    .build().toPublicJWK().toJSONString(), JWKResult.class);
            JWKResult.setAlg(SignatureAlgorithm.RS256.getValue());

            rs.setKeys(Arrays.asList(new JWKResult[]{JWKResult}));
            return Response.status(Response.Status.OK).entity(rs).build();
        }catch(Throwable e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(e)).build();
        }
    }

    @GET
    @Path("/registration/dynamic/{token}")
    @Operation(summary = "LTI Dynamic Registration - Initiate registration")
    @Consumes({ "text/html" })
    @Produces({"text/html"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response ltiRegistrationDynamic(@Parameter(description = "the endpoint to the open id configuration to be used for this registration",required=true) @QueryParam("openid_configuration") String openidConfiguration,
                                           @Parameter(description = "the registration access token. If present, it must be used as the access token by the tool when making the registration request to the registration endpoint exposed in the openid configuration.",required=false) @QueryParam("registration_token") String registrationToken,
                                           @Parameter(description = "one time usage token which is autogenerated with the url in edu-sharing admin gui.", required = true) @PathParam("token") String eduSharingRegistrationToken,
                                           @Context HttpServletRequest req){

       try{
            Throwable throwable = AuthenticationUtil.runAsSystem(() -> {
                try {
                    registrationService.ltiDynamicRegistration(openidConfiguration, registrationToken, eduSharingRegistrationToken);
                } catch (Throwable ex) {
                    return ex;
                }
                return null;
            });
            if(throwable != null) throw throwable;

           String serverPort = "";
           if(!("443".equals(Integer.toString(req.getServerPort()))|| "80".equals(Integer.toString(req.getServerPort())))){
               serverPort = ":" + Integer.toString(req.getServerPort());
           }

           return Response.seeOther(new URI(req.getScheme() +"://"
                           + req.getServerName()
                           + serverPort
                           + "/edu-sharing/components/lti"))
                   .build();
        }catch(Throwable e){
           logger.error(e.getMessage(),e);
           return ApiTool.processError(req,e,"LTI_REG_ERROR");
       }

    }


    @GET
    @Path("/registration/url")
    @Operation(summary = "LTI Dynamic Registration - generates url for platform")
    @Consumes({ "application/json" })
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = DynamicRegistrationTokens.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response ltiRegistrationUrl(@Parameter(description = "if to add a ne url to the list",required=true, schema = @Schema(defaultValue="false" ) ) @QueryParam("generate") boolean generate,
                                       @Context HttpServletRequest req){

        try {
            if(generate){
                registrationService.generate();
            }
            return Response.status(Response.Status.OK).entity(registrationService.get()).build();
        }catch(Throwable e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(e)).build();
        }
    }

    @DELETE
    @Path("/registration/url/{token}")
    @Operation(summary = "LTI Dynamic Regitration - delete url")
    @Consumes({ "application/json" })
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = DynamicRegistrationTokens.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response removeLtiRegistrationUrl(@Parameter(description = "the token of the link you have to remove", required = true) @PathParam("token") String token,
                                                 @Context HttpServletRequest req){

        try {
            DynamicRegistrationToken dynamicRegistrationToken = new DynamicRegistrationToken();
            dynamicRegistrationToken.setToken(token);
            registrationService.remove(dynamicRegistrationToken);
            return Response.status(Response.Status.OK).entity(registrationService.get()).build();
        }catch(Throwable e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(e)).build();
        }
    }



    @POST
    @Path("/registration/static")
    @Operation(summary = "register LTI platform")
    @Consumes({ "application/json" })
    @Produces({ "application/json"})
    @ApiResponses(value = {
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response registerTest(@Parameter(description = "the issuer",required=true) @QueryParam("platformId") String platformId,
                                 @Parameter(description = "client id",required=true) @QueryParam("client_id") String clientId,
                                 @Parameter(description = "deployment id",required=true)  @QueryParam("deployment_id") String deploymentId,
                                 @Parameter(description = "oidc endpoint, authentication request url",required=true) @QueryParam("authentication_request_url") String authenticationRequestUrl,
                                 @Parameter(description = "jwks endpoint, keyset url",required=true) @QueryParam("keyset_url") String keysetUrl,
                                 @Parameter(description = "jwks key id",required=false) @QueryParam("key_id") String keyId,
                                 @Parameter(description = "auth token url",required=true) @QueryParam("auth_token_url") String authTokenUrl,
                                 @Context HttpServletRequest req
    ){
        try {
            registrationService.registerPlatform(platformId,clientId,deploymentId,authenticationRequestUrl,keysetUrl,keyId,authTokenUrl);
            return Response.ok().build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    public static enum LTI_Plattforms {moodle};

    @POST
    @Path("/registration/{type}")
    @Operation(summary = "register LTI platform")
    @Consumes({ "application/json" })
    @Produces({ "application/json"})
    @ApiResponses(value = {
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response registerByType(@Parameter(description = "lti platform typ i.e. moodle", required=true) @PathParam("type") LTI_Plattforms type,
                                   @Parameter(description = "base url i.e. http://localhost/moodle used as platformId",required=true) @QueryParam("baseUrl") String baseUrl,
                                   @Parameter(description = "client id",required=false) @QueryParam("client_id") String clientId,
                                   @Parameter(description = "deployment id",required=false) @QueryParam("deployment_id") String deploymentId,
                                   @Context HttpServletRequest req
    ){
        try {
            registrationService.registerPlatform(baseUrl,clientId,deploymentId,
                    baseUrl + LTIConstants.MOODLE_AUTHENTICATION_REQUEST_URL_PATH,
                    baseUrl + LTIConstants.MOODLE_KEYSET_URL_PATH,
                    null,
                    baseUrl+LTIConstants.MOODLE_AUTH_TOKEN_URL_PATH);
            return Response.ok().build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @GET
    @Path("/details/{repository}/{node}")
    @Consumes({ "application/json" })
    @Produces({ "application/json"})

    @Operation(summary = "get a html snippet containing a rendered version of a node. this method can be called from a platform as a xhr request instead of doing the resource link flow", description = "get rendered html snippet for a node.")

    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = RenderingDetailsEntry.class))),
                    @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })

    public Response getDetailsSnippet(
            @Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
            @Parameter(description = "ID of node",required=true ) @PathParam("node") String node,
            @Parameter(description = "version of node",required=false) @QueryParam("version") String nodeVersion,
            @Parameter(description = "Rendering displayMode", required=false) @QueryParam("displayMode") String displayMode,
            @Parameter(description = "jwt containing the claims aud (clientId of platform), deploymentId and a token. must be signed by platform", required=true ) @QueryParam("jwt")  String jwt,
            @Context HttpServletRequest req){

        try{
            Jws<Claims> claims = new LTIJWTUtil().validateForInitialToolSession(jwt);
            String token = claims.getBody().get(LTIPlatformConstants.CUSTOM_CLAIM_TOKEN, String.class);
            Map<String, String> tokenData = (Map<String,String>)new Gson().fromJson(ApiTool.decrpt(token), Map.class);
            String user = tokenData.get(LTIPlatformConstants.CUSTOM_CLAIM_USER);
            //context is the embedding node
            String contextId = tokenData.get(LTIPlatformConstants.CUSTOM_CLAIM_NODEID);
            if(contextId == null){
                throw new ValidationException("missing " +LTIConstants.CONTEXT);
            }
            //don't use this: it is the appId of the tool
            //String appId = tokenData.get(LTIPlatformConstants.CUSTOM_CLAIM_APP_ID);
            String deploymentId = claims.getBody().get(LTIConstants.LTI_DEPLOYMENT_ID,String.class);
            String iss = claims.getBody().getIssuer();
            String clientId = claims.getBody().getAudience();
            String appId = new RepoTools().getAppId(iss,clientId,deploymentId);

            return AuthenticationUtil.runAs(() -> {

                ApiTool.handleUsagePermissions(node, req.getSession(), appId, contextId, usageService);

                return new RenderingApi().getDetailsSnippet(repository,node,nodeVersion,displayMode,req);
            },user);
        }catch(ValidationException e){
            logger.warn(e.getMessage(),e);
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        }catch (Throwable t) {

            logger.error(t.getMessage(), t);
            return ErrorResponse.createResponse(t);
        }
        //TODO maybe destroy session
    }

}
