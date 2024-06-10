package org.edu_sharing.service.lti13;

import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpSession;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.AllSessions;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.lti.v13.ApiTool;
import org.edu_sharing.restservices.ltiplatform.v13.LTIPlatformConstants;
import org.edu_sharing.restservices.ltiplatform.v13.model.LoginInitiationSessionObject;
import org.edu_sharing.restservices.ltiplatform.v13.model.ValidationException;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.lti13.model.LoginInitiationDTO;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.*;

public class LTIJWTUtil {

    Logger logger = Logger.getLogger(LTIJWTUtil.class);

    ApplicationInfo platform = null;

    public String generateState(ApplicationInfo platformDeployment, Map<String, String> authRequestMap, LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue) throws GeneralSecurityException, IOException {

        Date date = new Date();
        Key issPrivateKey = new Signing().getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);
        String state = Jwts.builder()
                .setHeaderParam("kid", ApplicationInfoList.getHomeRepository().getAppId())  // The key id used to sign this
                .setHeaderParam("typ", "JWT") // The type
                .setIssuer("ltiStarter")  //This is our own identifier, to know that we are the issuer.
                .setSubject(platformDeployment.getLtiIss()) // We store here the platform issuer to check that matches with the issuer received later
                .setAudience(platformDeployment.getLtiClientId())  //We send here the clientId to check it later.
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .setId(authRequestMap.get(LTIConstants.LTI_NONCE)) //just a nonce... we don't use it by the moment, but it could be good if we store information about the requests in DB.
                .claim("original_iss", loginInitiationDTO.getIss())  //All this claims are the information received in the OIDC initiation and some other useful things.
                .claim("loginHint", loginInitiationDTO.getLoginHint())
                .claim("ltiMessageHint", loginInitiationDTO.getLtiMessageHint())
                .claim("targetLinkUri", loginInitiationDTO.getTargetLinkUri())
                .claim("clientId", clientIdValue)
                .claim("ltiDeploymentId", deploymentIdValue)
                .claim("controller", "/oidc/login_initiations")
                .signWith(SignatureAlgorithm.RS256, issPrivateKey)  //We sign it
                .compact();
        logger.info("State:" + state);
        return state;
    }

    public Jws<Claims> validateState(String state) {
        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {
            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                PublicKey toolPublicKey;
                try {
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    toolPublicKey = new Signing().getPemPublicKey(
                            ApplicationInfoList.getHomeRepository().getPublicKey(),
                            CCConstants.SECURITY_KEY_ALGORITHM);
                } catch (GeneralSecurityException ex) {
                    logger.error("Error validating the state. Error generating the tool public key", ex);
                    return null;
                }
                return toolPublicKey;
            }
        }).parseClaimsJws(state);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }

    public Jws<Claims> validateJWT(String jwt){
        return validateJWT(jwt,null,null);
    }

    public Jws<Claims> validateJWT(String jwt, String clientId, String deploymentId) {

        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {

            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {

                String tmpClientId = clientId;
                String tmpDeploymentId = deploymentId;
                try {

                    /**
                     * @TODO: why this fallback is used
                     * clientId vs audience (" audience of a token is the intended recipient of the token.")
                     */
                    if(tmpClientId == null){
                        tmpClientId = claims.getAudience();
                        //aud is a list, there is a bug in DefaultClaims implementation which returns the list as a string with surrounding []
                        if(tmpClientId.startsWith("[") && tmpClientId.endsWith("]")){
                            tmpClientId = tmpClientId.replace("[","").replace("]","");
                            tmpClientId = tmpClientId.split(",")[0];
                        }
                    }
                    if(tmpDeploymentId == null) tmpDeploymentId = claims.get(LTIConstants.LTI_DEPLOYMENT_ID, String.class);
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    platform = new RepoTools().getApplicationInfo(claims.getIssuer(),tmpClientId,tmpDeploymentId);
                } catch (LTIException ex) {
                    logger.error("no platform with " + claims.getIssuer() +" " + clientId + " registered", ex);
                    return null;
                }
                // If the platform has a JWK Set endpoint... we try that.
                if (StringUtils.isNoneEmpty(platform.getLtiKeysetUrl())) {
                    try {
                        JWKSet publicKeys = JWKSet.load(new URL(platform.getLtiKeysetUrl()));
                        JWK jwk = publicKeys.getKeyByKeyId(header.getKeyId());
                        return ((AsymmetricJWK) jwk).toPublicKey();
                        /*return new Signing().getPemPublicKey(
                                platform.getPublicKey(),
                                CCConstants.SECURITY_KEY_ALGORITHM); */
                    } catch (Exception e) {
                        logger.error("Error getting the iss public key", e);
                        return null;
                    }
                } else { // If not, we get the key stored in our configuration
                    logger.error("The platform configuration must contain a keyset url");
                    return null;
                }

            }
        }).parseClaimsJws(jwt);
    }

    public String getDeepLinkingResponseJwt(LTISessionObject ltiSessionObject, String nodeId, String title) throws GeneralSecurityException{
        Node n = new Node();
        n.setRef(new NodeRef(ApplicationInfoList.getHomeRepository().getAppId(),nodeId));
        n.setTitle(title);
        return getDeepLinkingResponseJwt(ltiSessionObject,new Node[]{n});
    }

    /**
     * generates an jwt signed by homeApp with a claim nodeId that expires after "expireInSeconds"
     * @param nodeId
     * @return
     * @throws GeneralSecurityException
     */
    public static String getShortAccessJwt(String nodeId, int expireInSeconds) throws GeneralSecurityException{
        ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();
        Key toolPrivateKey = new Signing().getPemPrivateKey(homeApp.getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);
        Date date = new Date();
        String jwt = Jwts.builder()
                .setHeaderParam(LTIConstants.TYP, LTIConstants.JWT)
                .setHeaderParam(LTIConstants.KID, homeApp.getLtiKid())
                .setHeaderParam(LTIConstants.ALG, LTIConstants.RS256)
                .setExpiration(DateUtils.addSeconds(date, expireInSeconds))
                .setIssuedAt(date)
                .claim(CCConstants.NODEID, nodeId).signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();
        return jwt;
    }


    public String getDeepLinkingResponseJwt(LTISessionObject ltiSessionObject, Node[] nodes) throws GeneralSecurityException{
        String appId = ltiSessionObject.getEduSharingAppId();
        ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();
        Key toolPrivateKey = new Signing().getPemPrivateKey(homeApp.getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);

        ApplicationInfo appInfo = ApplicationInfoList.getApplicationInfos().get(appId);
        if(appInfo == null){
            logger.error("no appinfo found for appId:" + appId);
            return null;
        }

        Date date = new Date();
        String jwt = Jwts.builder()
                .setHeaderParam(LTIConstants.TYP, LTIConstants.JWT)

                .setHeaderParam(LTIConstants.KID, homeApp.getLtiKid())
                .setHeaderParam(LTIConstants.ALG, LTIConstants.RS256)
                .setIssuer(appInfo.getLtiClientId())  //Client ID
                .setAudience(ltiSessionObject.getIss())
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim(LTIConstants.LTI_NONCE, ltiSessionObject.getNonce())
                .claim(LTIConstants.LTI_AZP, ltiSessionObject.getIss())
                .claim(LTIConstants.LTI_DEPLOYMENT_ID, ltiSessionObject.getDeploymentId())
                    .claim(LTIConstants.LTI_MESSAGE_TYPE, LTIConstants.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                .claim(LTIConstants.LTI_VERSION, LTIConstants.LTI_VERSION_3)
                .claim(LTIConstants.LTI_DATA, ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_DATA))
                .claim(LTIConstants.LTI_CONTENT_ITEMS, generateContentItems(nodes))
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();
        return jwt;
    }

    public static String sign(String string, ApplicationInfo appInfo){
        Key privateKey = null;
        try {
            privateKey = new Signing().getPemPrivateKey(appInfo.getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }



        return Jwts.builder().setPayload(string).signWith(privateKey).compact();
    }

    public static Jws<Claims> validateJWT(String jwt, ApplicationInfo appInfo){
        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter(){
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                try {
                    /*return new Signing().getPemPublicKey(
                            appInfo.getPublicKey(),
                            CCConstants.SECURITY_KEY_ALGORITHM);*/
                    String keysetUrl = appInfo.getLtiKeysetUrl();
                    if(keysetUrl == null){
                        if(appInfo.ishomeNode()){
                            keysetUrl = appInfo.getBaseUrl() + "/edu-sharing/rest/lti/v13/jwks";
                        }else {
                            throw new RuntimeException("keyset url is null");
                        }
                    }
                    JWKSet publicKeys = JWKSet.load(new URL(keysetUrl));
                    String keyId = header.getKeyId();
                    if(keyId == null) throw new RuntimeException("missing keyid");
                    JWK jwk = publicKeys.getKeyByKeyId(keyId);
                    if(jwk == null) throw new RuntimeException("no public key found for key: "+keyId);
                    return ((AsymmetricJWK) jwk).toPublicKey();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                } catch (JOSEException e) {
                    throw new RuntimeException(e);
                }
            }
        }).parseClaimsJws(jwt);
    }



    private List<Map<String, Object>> generateContentItems(Node[] nodes){
        List<Map<String, Object>> deepLinks = new ArrayList<>();
        for(Node node : nodes){
            Map<String, Object> deepLink = new HashMap<>();

            deepLink.put(LTIConstants.DEEP_LINK_TYPE, LTIConstants.DEEP_LINK_LTIRESOURCELINK);
            deepLink.put(LTIConstants.DEEP_LINK_TITLE, node.getTitle() != null ? node.getTitle() : node.getName());
            //deepLink.put(LTIConstants.DEEP_LINK_URL, ApplicationInfoList.getHomeRepository().getClientBaseUrl() + "/components/render/"+node.getRef().getId()+"?closeOnBack=true");
            deepLink.put(LTIConstants.DEEP_LINK_URL, ApplicationInfoList.getHomeRepository().getClientBaseUrl() + "/rest/lti/v13/"+LTIConstants.LTI_TOOL_REDIRECTURL_PATH+"/"+node.getRef().getId());

            //@TODO must custom part always be send? is only need when platform is also a tool that has customcontent option
            Map<String,String> custom = new HashMap<>();
            custom.put("nodeId",node.getRef().getId());
            custom.put("repositoryId",node.getRef().getRepo());
            deepLink.put("custom",custom);

            try {
                Map<String,String> thumbnail = new HashMap<>();
                //thumbnail.put("url",node.getPreview().getUrl());
                thumbnail.put("url",new MimeTypesV2().getIcon(node.getType(),NodeServiceFactory.getLocalService().getProperties(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier() ,node.getRef().getId()) ,node.getAspects()));
                thumbnail.put("width",""+node.getPreview().getWidth());
                thumbnail.put("height",""+node.getPreview().getHeight());
                deepLink.put("icon",thumbnail);
            } catch (Throwable e) {
                logger.error(e.getMessage());
            }

            /**
             * @TODO if h5p add scoreMaximum stuff
             */
            //deepLink.put("lineItem", lineItem());


            /**
             * @TODO check if needed
             */
            /*
            Map<String, String> availableDates = new HashMap<>();
            Map<String, String> submissionDates = new HashMap<>();
            Map<String, String> custom = new HashMap<>();
            availableDates.put("startDateTime", "2018-03-07T20:00:03Z");
            availableDates.put("endDateTime", "2022-03-07T20:00:03Z");
            submissionDates.put("startDateTime", "2019-03-07T20:00:03Z");
            submissionDates.put("endDateTime", "2021-08-07T20:00:03Z");
            custom.put("dueDate", "$Resource.submission.endDateTime");
            custom.put("controlValue", "This is whatever I want to write here");
            deepLink.put("available", availableDates);
            deepLink.put("submission", submissionDates);
            deepLink.put("custom", custom);*/
            deepLinks.add(deepLink);
        }
        return deepLinks;
    }

    Map<String, Object> lineItem() {
        Map<String, Object> deepLink = new HashMap<>();

        deepLink.put("scoreMaximum", 87);
        deepLink.put("label", "LTI 1234 Quiz");
        deepLink.put("resourceId", "1234");
        deepLink.put("tag", "myquiztest");
        return deepLink;
    }

    public ApplicationInfo getApplicationInfo() {
        return platform;
    }

    /**
     * validate jwt data to decide if api caller is allowed
     *
     * @param jwt
     * @param validateNodeId
     * @return
     * @throws Exception
     */
    public Jws<Claims> validateForCustomContent(String jwt, boolean validateNodeId) throws Exception{
        /**
         * decode without validating signature to get appId
         */
        String appId = LTIJWTUtil.getValue(jwt,"appId");

        logger.info("appId tool:" + appId);
        if(appId == null) throw new Exception("missing "+ LTIPlatformConstants.CUSTOM_CLAIM_APP_ID);
        ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
        if(appInfo == null || !appInfo.isLtiTool()){
            throw new ValidationException("application is no lti tool");
        }

        /**
         * validate that this message was signed by the tool
         */
        Jws<Claims> jwtObj = LTIJWTUtil.validateJWT(jwt,appInfo);
        //maybe obsolet:
        String validatedAppId = jwtObj.getBody().get(LTIPlatformConstants.CUSTOM_CLAIM_APP_ID,String.class);
        if(!appId.equals(validatedAppId)){
            throw new ValidationException("mismatch appId");
        }

        String user = jwtObj.getBody().get(LTIPlatformConstants.CUSTOM_CLAIM_USER, String.class);
        if(user == null){
            throw new ValidationException("missing "+LTIPlatformConstants.CUSTOM_CLAIM_USER);
        }

        String nodeId = jwtObj.getBody().get(LTIPlatformConstants.CUSTOM_CLAIM_NODEID, String.class);
        if(validateNodeId && nodeId == null){
            throw new ValidationException("missing "+LTIPlatformConstants.CUSTOM_CLAIM_NODEID);
        }

        String token = jwtObj.getBody().get(LTIPlatformConstants.CUSTOM_CLAIM_TOKEN, String.class);
        if(token == null){
            throw new ValidationException("missing "+LTIPlatformConstants.CUSTOM_CLAIM_TOKEN);
        }

        /**
         * this is a backend call so we con not use this: req.getSession().getAttribute(LTIPlatformConstants.LOGIN_INITIATIONS_SESSIONOBJECT);
         */
        LoginInitiationSessionObject sessionObject = AllSessions.getUserLTISessions().get(token);
        if(sessionObject == null){
            throw new ValidationException("no loginInitiationSessionObject found");
        }


        if(!appId.equals(sessionObject.getAppId())){
            throw new ValidationException("wrong appId");
        }


        if(!user.equals(sessionObject.getUser())){
            throw new ValidationException("wrong user");
        }

        if(validateNodeId && !nodeId.equals(sessionObject.getContentUrlNodeId())){
            throw new ValidationException("wrong nodeId");
        }

        Map<String,String> tokenData = new Gson().fromJson(ApiTool.decrpt(token), HashMap.class);
        if(!appId.equals(tokenData.get(LTIPlatformConstants.CUSTOM_CLAIM_APP_ID))){
            throw new ValidationException("mismatch appId");
        }
        if(!user.equals(tokenData.get(LTIPlatformConstants.CUSTOM_CLAIM_USER))){
            throw new ValidationException("mismatch user");
        }
        if(validateNodeId && !nodeId.equals(tokenData.get(LTIPlatformConstants.CUSTOM_CLAIM_NODEID))){
            throw new ValidationException("mismatch nodeId");
        }
        sessionObject.setLastAccessed(System.currentTimeMillis());


        /**
         * extend session runtime: not longer necessary cause content can be written without session (token is used)
         */
        /*Field facadeSessionField = StandardSessionFacade.class.getDeclaredField("session");
        facadeSessionField.setAccessible(true);
        StandardSession stdSession = (StandardSession) facadeSessionField.get(session);
        stdSession.endAccess();
        logger.info("last AccessTime:" + new Date(session.getLastAccessedTime()));*/

        return jwtObj;
    }

    private static LoginInitiationSessionObject getLoginInitiationSessionObject(String token, HttpSession session) throws Exception {
        Map<String,LoginInitiationSessionObject> loginInitiationSessionObjectMap = (
                Map<String,LoginInitiationSessionObject>) session.getAttribute(LTIPlatformConstants.LOGIN_INITIATIONS_SESSIONOBJECT);

        if(loginInitiationSessionObjectMap == null){
            throw new Exception(LTIPlatformConstants.ERROR_MISSING_SESSIONOBJECTS);
        }

        LoginInitiationSessionObject sessionObject = loginInitiationSessionObjectMap.entrySet().stream()
                .filter(e -> token.equals(e.getValue().getToken()))
                .findFirst()
                .orElseThrow(() -> new Exception(LTIPlatformConstants.ERROR_MISSING_SESSIONOBJECT)).getValue();
        return sessionObject;
    }

    /**
     * 1.validates that jwt was signed by platform
     *
     * 2. prevents that a trusted platform can use api endpoints with any user (scoping of username is disabled)
     * by checking that an initial edu-sharing authentication exists that caused an lti tool session.
     *
     * usecase is when an application is registered with both roles: tool and platform.
     * example is an editor registered as a tool that wants to integrate edu-sharing objects as a platform
     *
     *
     * @TODO check that tool and platform are in reality the same system
     *
     * @param jwt
     * @return
     * @throws ValidationException
     * @throws org.json.simple.parser.ParseException
     */
    public Jws<Claims> validateForInitialToolSession(String jwt) throws ValidationException, org.json.simple.parser.ParseException {
        String clientId = LTIJWTUtil.getValue(jwt,"aud");
        if(clientId == null){
            throw new ValidationException("missing clientId");
        }
        String deploymentId = LTIJWTUtil.getValue(jwt, LTIConstants.LTI_DEPLOYMENT_ID);
        if(deploymentId == null){
            throw new ValidationException("missing deploymentId");
        }
        String appId = new RepoTools().getAppId(null,clientId,deploymentId);
        ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
        if(appInfo == null){
            throw new ValidationException("no application found for clientId:" + clientId + " deploymentId:"+deploymentId);
        }

        if(!ApplicationInfo.TYPE_LTIPLATFORM.equals(appInfo.getType())){
            throw new ValidationException("application:" + appId + " is no ltiplatform");
        }

        //validate jwt is signed by platform
        Jws<Claims> claims = LTIJWTUtil.validateJWT(jwt,appInfo);

        String token = claims.getBody().get(LTIPlatformConstants.CUSTOM_CLAIM_TOKEN, String.class);
        if(token == null){
            throw new ValidationException("missing "+LTIPlatformConstants.CUSTOM_CLAIM_TOKEN);
        }

        LoginInitiationSessionObject sessionObject = AllSessions.getUserLTISessions().get(token);
        if(sessionObject == null){
            throw new ValidationException("no LoginInitiationSessionObject found");
        }

        /**
         *  validate token was originally created with tool session
         *
         *  prevent platform can use this api endpoint without an initially created tool session.
         *  ordinarily username scoping is disabled by apps that use this endpoint
         *
         */
        //validate that token appId (tool) is the same as the session appId (tool)
        Map<String,String> tokenData = new Gson().fromJson(ApiTool.decrpt(token), HashMap.class);
        if(!sessionObject.getAppId().equals(tokenData.get(LTIPlatformConstants.CUSTOM_CLAIM_APP_ID))){
            throw new ValidationException("mismatch appId");
        }

        String tokenUser = tokenData.get(LTIPlatformConstants.CUSTOM_CLAIM_USER);
        String sessionUser = sessionObject.getUser();

        //validate that tokenuser is the same as session user
        if(!sessionUser.equals(tokenUser)){
            throw new ValidationException("mismatch user");
        }

        sessionObject.setLastAccessed(System.currentTimeMillis());

        return claims;
    }

    /**
     * returns a value of an jwt body without validating jwt
     *
     * @param jwt
     * @param claim
     * @return
     * @param <T>
     * @throws org.json.simple.parser.ParseException
     */
    public static <T> T getValue(String jwt, String claim) throws org.json.simple.parser.ParseException {
        String[] chunks = jwt.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();

        String header = new String(decoder.decode(chunks[0]));
        String payload = new String(decoder.decode(chunks[1]));
        JSONObject jsonObject = (JSONObject)new JSONParser().parse(payload);
        return (T)jsonObject.get(claim);
    }
}
