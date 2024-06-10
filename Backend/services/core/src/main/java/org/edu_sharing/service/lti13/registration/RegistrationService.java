package org.edu_sharing.service.lti13.registration;

import com.google.gson.Gson;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.restservices.ltiplatform.v13.LTIPlatformConstants;
import org.edu_sharing.restservices.ltiplatform.v13.model.OpenIdConfiguration;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.admin.SystemFolder;
import org.edu_sharing.service.lti13.LTIConstants;
import org.edu_sharing.service.lti13.RepoTools;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.version.VersionService;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    Logger logger = Logger.getLogger(RegistrationService.class);
    public static final long DYNAMIC_REGISTRATION_TOKEN_EXPIRY = TimeUnit.DAYS.toMillis(1);

    private final VersionService versionService;

    @NotNull
    public OpenIdConfiguration getLtiPlatformOpenIdConfiguration() {
        ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
        OpenIdConfiguration oidconf = new OpenIdConfiguration();
        oidconf.setIssuer(homeRepository.getClientBaseUrl());
        /**
         * @TODO token stuff
         */
        //oidconf.setToken_endpoint();
        oidconf.setToken_endpoint_auth_methods_supported(Arrays.asList("private_key_jwt"));
        oidconf.setToken_endpoint_auth_signing_alg_values_supported(Arrays.asList(SignatureAlgorithm.RS256.getValue()));
        oidconf.setJwks_uri(homeRepository.getClientBaseUrl()+"/rest/lti/v13/jwks");
        oidconf.setAuthorization_endpoint(homeRepository.getClientBaseUrl()+"/rest/ltiplatform/v13/auth");
        oidconf.setRegistration_endpoint(homeRepository.getClientBaseUrl()+"/rest/ltiplatform/v13/openid-registration");
        oidconf.setToken_endpoint(homeRepository.getClientBaseUrl()+"/rest/ltiplatform/v13/token");
        oidconf.setResponse_types_supported(Arrays.asList("id_token"));
        oidconf.setClaims_supported(Arrays.asList("sub","iss","given_name","family_name","email"));

        OpenIdConfiguration.LTIPlatformConfiguration ltiPlatformConfiguration = new OpenIdConfiguration.LTIPlatformConfiguration();
        OpenIdConfiguration.LTIPlatformConfiguration.Message msgDeepLink = new OpenIdConfiguration.LTIPlatformConfiguration.Message();
        msgDeepLink.setType("LtiDeepLinkingRequest");
        OpenIdConfiguration.LTIPlatformConfiguration.Message msgResourceLink = new OpenIdConfiguration.LTIPlatformConfiguration.Message();
        msgResourceLink.setType("LtiResourceLinkRequest");
        ltiPlatformConfiguration.getMessages_supported().add(msgDeepLink);
        ltiPlatformConfiguration.getMessages_supported().add(msgResourceLink);
        ltiPlatformConfiguration.setProduct_family_code("edu-sharing");
        ltiPlatformConfiguration.setVersion(versionService.getVersionNoException(VersionService.Type.REPOSITORY));
        oidconf.setLtiPlatformConfiguration(ltiPlatformConfiguration);
        return oidconf;
    }


    public DynamicRegistrationToken generate() throws Throwable{
        NodeRef systemObject = SystemFolder.getSystemObject(CCConstants.CCM_VALUE_IO_NAME_LTI_REGISTRATION_NODE_NAME);
        ACL acl = PermissionServiceFactory.getLocalService().getPermissions(systemObject.getId());
        if(acl.isInherited()) {
            PermissionServiceFactory.getLocalService().setPermissionInherit(systemObject.getId(), false);
        }
        DynamicRegistrationTokens systemObjectContent = SystemFolder.getSystemObjectContent(
                        CCConstants.CCM_VALUE_IO_NAME_LTI_REGISTRATION_NODE_NAME,
                DynamicRegistrationTokens.class);
        String token = UUID.randomUUID().toString();
        DynamicRegistrationToken dynamicRegistrationToken = new DynamicRegistrationToken();
        dynamicRegistrationToken.setUrl(ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/rest/lti/v13/registration/dynamic/"+token);
        dynamicRegistrationToken.setTsCreated(System.currentTimeMillis());
        dynamicRegistrationToken.setToken(token);
        systemObjectContent.getRegistrationLinks().add(dynamicRegistrationToken);
        write(systemObjectContent);
        return dynamicRegistrationToken;
    }

    public DynamicRegistrationTokens get(){
        DynamicRegistrationTokens drts = SystemFolder.getSystemObjectContent(
                CCConstants.CCM_VALUE_IO_NAME_LTI_REGISTRATION_NODE_NAME,
                DynamicRegistrationTokens.class);
        for(DynamicRegistrationToken t : drts.getRegistrationLinks()){
            t.validate();
        }
        return drts;
    }

    public void write(DynamicRegistrationTokens toWrite) throws Throwable{
        String json = new Gson().toJson(toWrite);
        NodeServiceHelper.writeContentText(SystemFolder.getSystemObject( CCConstants.CCM_VALUE_IO_NAME_LTI_REGISTRATION_NODE_NAME),json);
    }

    public void remove(DynamicRegistrationToken token) throws Throwable{
        DynamicRegistrationTokens tokens = get();
        tokens.getRegistrationLinks().remove(token);
        write(tokens);
    }

    public void ltiDynamicRegistration(String openidConfiguration, String registrationToken, String eduSharingRegistrationToken) throws Throwable {

        if(eduSharingRegistrationToken == null || eduSharingRegistrationToken.trim().equals("")){
            throw new Exception("no eduSharingRegistrationToken provided");
        }


        DynamicRegistrationToken foundToken = get().get(eduSharingRegistrationToken);
        if(foundToken == null){
            throw new Exception("eduSharing registration token provided is invalid");
        }

        if((System.currentTimeMillis() - foundToken.getTsCreated()) > DYNAMIC_REGISTRATION_TOKEN_EXPIRY ){
            //remove(foundToken);
            throw new Exception("eduSharing registration token expired");
        }

        if(!foundToken.isValid()){
            throw new Exception("eduSharing registration token already used");
        }

        if(openidConfiguration == null){
            throw new Exception("no openidConfiguration present");
        }


        String platformConfiguration = new HttpQueryTool().query(openidConfiguration);
        JSONParser jsonParser = new JSONParser();
        JSONObject oidConfig = (JSONObject) jsonParser.parse(platformConfiguration);
        String issuer = (String) oidConfig.get("issuer");
        /**
         * @TODO it seems that moodle can not be validated like spec
         * validate https://www.imsglobal.org/spec/lti-dr/v1p0#issuer-and-openid-configuration-url-match
         *
         * 3.5.1 Issuer and OpenID Configuration URL Match
         */
        String keySetUrl = (String) oidConfig.get("jwks_uri");
        if(keySetUrl == null){
            throw new Exception("no jwks_uri provided");
        }
        String authorizationEndpoint = (String) oidConfig.get("authorization_endpoint");
        if(authorizationEndpoint == null){
            throw new Exception("no authorization_endpoint provided");
        }
        String registrationEndpoint = (String) oidConfig.get("registration_endpoint");
        if(registrationEndpoint == null){
            throw new Exception("no registration_endpoint provided");
        }

        String authTokenUrl = (String) oidConfig.get("token_endpoint");
        if(authTokenUrl == null){
            throw new Exception("no token_endpoint provided");
        }

        List<String> claimsSupported = (List<String>) oidConfig.get("claims_supported");
        ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("application_type","web");
        JSONArray respTypes = new JSONArray();
        respTypes.add("id_token");
        jsonResponse.put("response_types", respTypes);
        jsonResponse.put("initiate_login_uri",homeApp.getClientBaseUrl()+"/rest/lti/v13/oidc/login_initiations");
        String redirectUrl = homeApp.getClientBaseUrl()+"/rest/lti/v13/lti13";
        JSONArray ja = new JSONArray();
        ja.add(redirectUrl);
        jsonResponse.put("redirect_uris",ja);
        jsonResponse.put("client_name",homeApp.getAppCaption());
        jsonResponse.put("jwks_uri",homeApp.getClientBaseUrl()+"/rest/lti/v13/jwks");
        //alt: http://192.168.16.221/edu-sharing/assets/images/logo.svg
        String logo = homeApp.getClientBaseUrl()+"/assets/images/app-icon.svg";
        jsonResponse.put("logo_uri",logo);
        jsonResponse.put("token_endpoint_auth_method", "private_key_jwt");
        JSONObject ltiDeepLink = new JSONObject();
        ltiDeepLink.put("type","LtiDeepLinkingRequest");
        ltiDeepLink.put("target_link_uri",redirectUrl);
        ltiDeepLink.put("label","add an edu-sharing content object");
        ltiDeepLink.put("label#de","Ein edu-sharing Inhalt hinzufuegen");
        JSONArray messages = new JSONArray();
        messages.add(ltiDeepLink);
        JSONObject toolConfig = new JSONObject();
        toolConfig.put("domain",homeApp.getDomain());
        toolConfig.put("messages",messages);
        toolConfig.put("claims", claimsSupported);
        jsonResponse.put(LTIConstants.LTI_REGISTRATION_TOOL_CONFIGURATION,toolConfig);
        //jsonResponse.put("token_endpoint_auth_method","private_key_jwt");
        HttpPost post = new HttpPost();
        post.setEntity(new StringEntity(jsonResponse.toJSONString()));
        post.setURI(new URI(registrationEndpoint));
        post.setHeader("Content-Type","application/json");
        post.setHeader("Accept","application/json");

        if(registrationToken != null && !registrationToken.trim().equals("")){
            post.setHeader("Authorization","Bearer "+ registrationToken);
        }

        String result = new HttpQueryTool().query(null,null,post,false);

        JSONObject registrationResult;
        try {
            registrationResult = (JSONObject) jsonParser.parse(result);
        }catch(ParseException e){
            /**
             * filter non json i.i when moodle notices and warnings are enabled
             *
             * <b>Warning</b>:  Illegal string offset 'type' in <b>/var/www/html/moodle/mod/lti/classes/local/ltiopenid/registration_helper.php</b> on line <b>172</b><br />
             * <br />
             * <b>Warning</b>:  Illegal string offset 'type' in <b>/var/www/html/moodle/mod/lti/classes/local/ltiopenid/registration_helper.php</b> on line <b>172</b><br />
             * <br />
             * <b>Warning</b>:  Illegal string offset 'type' in <b>/var/www/html/moodle/mod/lti/classes/local/ltiopenid/registration_helper.php</b> on line <b>172</b><br />
             * <br />
             * <b>Warning</b>:  Illegal string offset 'type' in <b>/var/www/html/moodle/mod/lti/classes/local/ltiopenid/registration_helper.php</b> on line <b>172</b><br />
             * <br />
             * <b>Notice</b>:  Undefined property: stdClass::$id in <b>/var/www/html/moodle/mod/lti/openid-registration.php</b> on line <b>56</b><br />
             *
             */

            int start=result.indexOf('{');
            int end=result.lastIndexOf('}');
            String json=result.substring(start,end+1);
            registrationResult = (JSONObject) jsonParser.parse(json);
            logger.warn("registration result could only be parsed after html cleanup. maybe disable warnings and notices on platform side.");
        }
        String clientId = (String)registrationResult.get("client_id");
        /**
         * {"client_id":"IcOCHxHupFSZz2Z","response_types":["id_token"],"jwks_uri":"https:\/\/localhost.localdomain\/edu-sharing\/rest\/lti\/v13\/jwks",
         * "initiate_login_uri":"https:\/\/localhost.localdomain\/edu-sharing\/rest\/lti\/v13\/oidc\/login_initiations",
         * "grant_types":["client_credentials","implicit"],"redirect_uris":["https:\/\/localhost.localdomain\/edu-sharing\/rest\/lti\/v13\/lti13"],
         * "application_type":"web","token_endpoint_auth_method":"private_key_jwt","client_name":"local",
         * "logo_uri":"\/edu-sharing\/images\/logos\/edu_sharing_com_login.svg","scope":"",
         * "https:\/\/purl.imsglobal.org\/spec\/lti-tool-configuration":
         *  {"version":"1.3.0","deployment_id":"5","target_link_uri":"https:\/\/localhost.localdomain",
         * "domain":"localhost.localdomain","description":"","claims":["sub","iss"]}}
         */
        JSONObject ltiToolConfigInfo = (JSONObject)registrationResult.get("https://purl.imsglobal.org/spec/lti-tool-configuration");
        String deploymentId = (String)ltiToolConfigInfo.get("deployment_id");

        registerPlatform(issuer, clientId, deploymentId, authorizationEndpoint, keySetUrl,null,authTokenUrl, foundToken);
    }

    public void registerPlatform(String platformId,
                                 String clientId, String deploymentId,
                                 String authenticationRequestUrl,
                                 String keysetUrl,
                                 String keyId,
                                 String authTokenUrl) throws Exception{
        registerPlatform(platformId, clientId, deploymentId, authenticationRequestUrl, keysetUrl, keyId, authTokenUrl,null);
    }
    public void registerPlatform(String platformId,
                                  String clientId, String deploymentId,
                                  String authenticationRequestUrl,
                                  String keysetUrl,
                                  String keyId,
                                  String authTokenUrl, DynamicRegistrationToken token) throws Exception{
        Map<String,String> properties = new HashMap<>();
        String appId = new RepoTools().getAppId(platformId,clientId,deploymentId);
        properties.put(ApplicationInfo.KEY_APPID, appId);
        properties.put(ApplicationInfo.KEY_TYPE, ApplicationInfo.TYPE_LTIPLATFORM);
        properties.put(ApplicationInfo.KEY_LTI_DEPLOYMENT_ID, deploymentId);
        properties.put(ApplicationInfo.KEY_LTI_ISS, platformId);
        properties.put(ApplicationInfo.KEY_LTI_CLIENT_ID, clientId);
        properties.put(ApplicationInfo.KEY_LTI_OIDC_ENDPOINT, authenticationRequestUrl);
        properties.put(ApplicationInfo.KEY_LTI_AUTH_TOKEN_ENDPOINT,authTokenUrl);
        properties.put(ApplicationInfo.KEY_LTI_KEYSET_URL,keysetUrl);
        if(keyId != null) properties.put(ApplicationInfo.KEY_LTI_KID,keyId);

        /*JWKSet publicKeys = JWKSet.load(new URL(keysetUrl));
        if(publicKeys == null){
            throw new Exception("no public key found");
        }
        JWK jwk = (keyId == null || keyId.trim().isEmpty()) ? publicKeys.getKeys().get(0) : publicKeys.getKeyByKeyId(keyId);

        if(jwk == null){
            throw new Exception("no public key found for keyId:" + keyId);
        }

        String pubKeyString = "-----BEGIN PUBLIC KEY-----\n"
                + new String(new Base64().encode(((AsymmetricJWK) jwk).toPublicKey().getEncoded())) + "-----END PUBLIC KEY-----";
        properties.put(ApplicationInfo.KEY_PUBLIC_KEY, pubKeyString);*/
        AdminServiceFactory.getInstance().addApplication(properties);

        if(token != null){
            token.setRegisteredAppId(appId);
            DynamicRegistrationTokens dynamicRegistrationTokens = get();
            dynamicRegistrationTokens.update(token);
            try {
                write(dynamicRegistrationTokens);
            } catch (Throwable e) {
                logger.error(e.getMessage(),e);
            }
        }
    }

    public ApplicationInfo ltiDynamicToolRegistration(JSONObject registrationPayload, Jwt registrationToken) throws Exception{

        List<String> responseTypes = (List<String>)registrationPayload.get("response_types");
        String initiateLoginUri = (String)registrationPayload.get("initiate_login_uri");
        List<String> redirectUris = (List<String>)registrationPayload.get("redirect_uris");
        String clientName =  (String)registrationPayload.get("client_name");
        String jwksuri = (String)registrationPayload.get("jwks_uri");
        String tokenEndpointAuthMethod = (String)registrationPayload.get("token_endpoint_auth_method");
        String applicationType =  (String)registrationPayload.get("application_type");
        String logoUri = (String)registrationPayload.get("logo_uri");

        JSONObject ltiToolConfig = (JSONObject)registrationPayload.get(LTIConstants.LTI_REGISTRATION_TOOL_CONFIGURATION);
        String domain = (String)ltiToolConfig.get("domain");
        String targetLinkUri =  (String)ltiToolConfig.get("target_link_uri");
        List<String> claims = (List<String>)ltiToolConfig.get("claims");
        String description =  (String)ltiToolConfig.get("description");
        JSONObject customParameters = (JSONObject)ltiToolConfig.get("custom_parameters");
        JSONArray ltiToolConfigMessages = (JSONArray)ltiToolConfig.get("messages");
        String targetLinkUriDL = null;
        if(ltiToolConfigMessages != null){
            for(int i = 0; i < ltiToolConfigMessages.size(); i++){
                JSONObject message = (JSONObject)ltiToolConfigMessages.get(i);
                String messageType = (String)message.get("type");
                if("LtiDeepLinkingRequest".equals(messageType)){
                    targetLinkUriDL = (String) message.get("target_link_uri");
                }
            }
        }


        // Validate domain and target link.
        if (domain == null || domain.trim().isEmpty()) {
            throw new Exception("missing_domain");
        }

        if(targetLinkUri != null){
            if(!targetLinkUri.contains(domain)){
                throw new Exception("domain_targetlinkuri_mismatch");
            }
        }

        if(!responseTypes.contains("id_token")){
            throw new Exception("invalid_response_types");
        }

        if(redirectUris == null || redirectUris.size() == 0){
            throw new Exception("missing_redirect_uris");
        }

        if(!tokenEndpointAuthMethod.equals("private_key_jwt")){
            throw new Exception("invalid_token_endpoint_auth_method");
        }

        if (!"web".equals(applicationType)) {
            throw new Exception("invalid_application_type");
        }

        DefaultClaims body = (DefaultClaims)registrationToken.getBody();
        String sub = (String) body.get("sub");


        return registerTool(sub, initiateLoginUri, jwksuri, targetLinkUri, StringUtils.join(redirectUris,","), logoUri,
                (customParameters != null) ? customParameters.toJSONString() : null, description, clientName, targetLinkUriDL, null);
    }

    /**
     *
     * @param clientId
     * @param initiateLoginUri
     * @param jwksuri
     * @param targetLinkUri
     * @param redirectUris
     * @param logoUri
     * @param customParameters
     * @param description
     * @param clientName
     * @param targetLinkUriDeepLink
     * @param toolUrl  tool url will be used to find applications that can handle resourcelinks independent on appId. so that the existing resourcelinks still work when a tool application is removed and registered again.
     * @return
     * @throws Exception
     */
    public ApplicationInfo registerTool(String clientId, String initiateLoginUri, String jwksuri,
                                        String targetLinkUri, String redirectUris, String logoUri,
                                        String customParameters, String description, String clientName, String targetLinkUriDeepLink, String toolUrl) throws Exception {
        Map<String,String> properties = new HashMap<>();

        /**
         * fallback to required redrect Urls
         */
        if(toolUrl == null){
            toolUrl = redirectUris.split(",")[0];
        }

        Integer lastDeploymentId = 0;
        for(ApplicationInfo a : ApplicationInfoList.getApplicationInfos().values()) {
            String firstRedirectUrl = redirectUris.split(",")[0];
            String firstRedirectUrlExisting = (a.getLtitoolRedirectUrls() != null) ? a.getLtitoolRedirectUrls().split(",")[0] : null;
            if (ApplicationInfo.TYPE_LTITOOL.equals(a.getType()) && firstRedirectUrl.equals(firstRedirectUrlExisting)) {
                try {
                    int dId = Integer.parseInt(a.getLtiDeploymentId());
                    if (lastDeploymentId < dId) {
                        lastDeploymentId = dId;
                    }

                } catch (Exception e) {
                }
            }
        }
        lastDeploymentId++;


        /**
         * leave out the issuer here, cause edu-sharing as a platform generates a clientId and deploymentId.
         * for tools no issuer is defined in standard.
         */
        String appId = new RepoTools().getAppId(null, clientId, Integer.toString(lastDeploymentId));
        properties.put(ApplicationInfo.KEY_APPID, appId);
        properties.put(ApplicationInfo.KEY_APPCAPTION,clientName);
        properties.put(ApplicationInfo.KEY_TYPE, ApplicationInfo.TYPE_LTITOOL);
        properties.put(ApplicationInfo.KEY_LTI_CLIENT_ID, clientId);
        properties.put(ApplicationInfo.KEY_LTITOOL_LOGININITIATIONS_URL, initiateLoginUri);
        properties.put(ApplicationInfo.KEY_LTITOOL_TARGET_LINK_URI,targetLinkUri);
        if(targetLinkUriDeepLink != null){
            properties.put(ApplicationInfo.KEY_LTITOOL_TARGET_LINK_URI_DEEPLINK,targetLinkUriDeepLink);
        }
        properties.put(ApplicationInfo.KEY_LTITOOL_REDIRECT_URLS,redirectUris);
        if(customParameters != null){
            properties.put(ApplicationInfo.KEY_LTITOOL_CUSTOM_PARAMETERS, customParameters);
            JSONObject jo = (JSONObject) new JSONParser().parse(customParameters);
            if(jo.get(LTIPlatformConstants.CUSTOM_CLAIM_GET_CONTENTAPIURL)  != null){
                properties.put(ApplicationInfo.KEY_LTITOOL_CUSTOMCONTENT_OPTION,"true");
            }
            // if the remote lti tool supports content fetching, check if it can handle a custom resource type
            // this way, the app can open for manually uploaded files
            if(jo.get(LTIPlatformConstants.CUSTOM_CLAIM_RESOURCE_TYPE)  != null){
                properties.put(ApplicationInfo.KEY_LTI_RESOURCE_TYPE, (String) jo.get(LTIPlatformConstants.CUSTOM_CLAIM_RESOURCE_TYPE));
            }
        }
        properties.put(ApplicationInfo.KEY_LOGO,logoUri);
        properties.put(ApplicationInfo.KEY_LTI_KEYSET_URL,jwksuri);
        properties.put(ApplicationInfo.KEY_LTI_DEPLOYMENT_ID, Integer.toString(lastDeploymentId));
        properties.put(ApplicationInfo.KEY_LTITOOL_DESCRIPTION, description);
        properties.put(ApplicationInfo.KEY_LTITOOL_URL,toolUrl);



        /*JWKSet publicKeys = JWKSet.load(new URL(jwksuri));
        if(publicKeys == null || publicKeys.getKeys() == null || publicKeys.getKeys().size() == 0){
            throw new Exception("no public key found");
        }
        JWK jwk = publicKeys.getKeys().get(0);

        if(jwk == null){
            throw new Exception("no public key found for jwksuri:" + jwksuri);
        }

        String pubKeyString = "-----BEGIN PUBLIC KEY-----\n"
                + new String(new Base64().encode(((AsymmetricJWK) jwk).toPublicKey().getEncoded())) + "-----END PUBLIC KEY-----";
        properties.put(ApplicationInfo.KEY_PUBLIC_KEY, pubKeyString);*/
        AdminServiceFactory.getInstance().addApplication(properties);
        return ApplicationInfoList.getRepositoryInfoById(appId);
    }

    public static String generateNewClientId(){
        return RandomStringUtils.random(15, true, true);
    }

}
