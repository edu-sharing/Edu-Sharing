package org.edu_sharing.spring.security.openid;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * adds an prompt=none to authorization request if request matches configured path
 * https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
 */
public class SilentLoginAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;

    public static String DEFAULT_SILENT_LOGIN_PATH = "/rest/authentication/v1/validateSSOSession";

    private String silentLoginPath = DEFAULT_SILENT_LOGIN_PATH;

    public SilentLoginAuthorizationRequestResolver(ClientRegistrationRepository clientRegistration) {
        this.defaultAuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistration, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest =
                this.defaultAuthorizationRequestResolver.resolve(request);

        if(silentLoginPath.equals(getCombinedPath(request)) && authorizationRequest != null){
            return customAuthorizationRequest(authorizationRequest);
        }
        return authorizationRequest;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest =
                this.defaultAuthorizationRequestResolver.resolve(
                        request, clientRegistrationId);

        if(silentLoginPath.equals(getCombinedPath(request)) && authorizationRequest != null){
            return customAuthorizationRequest(authorizationRequest);
        }

        return authorizationRequest;
    }

    public String getSilentLoginPath() {
        return silentLoginPath;
    }

    public String getCombinedPath(HttpServletRequest request){
        DefaultSavedRequest r = (DefaultSavedRequest)request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
        if(r != null) return getCombinedPath(r);

        String combinedPath = request.getServletPath();
        if (request.getPathInfo() != null) {
            combinedPath += request.getPathInfo();
        }
        return combinedPath;
    }


    private String getCombinedPath(DefaultSavedRequest r){
        String combinedPath = r.getServletPath();
        if (r.getPathInfo() != null) {
            combinedPath += r.getPathInfo();
        }
        return combinedPath;
    }

    public boolean protectedPathNeedsSilentLogin(HttpServletRequest request){
        return getCombinedPath(request).equals(getSilentLoginPath());
    }



    private OAuth2AuthorizationRequest customAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest) {

        Map<String, Object> additionalParameters =
                new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
        additionalParameters.put("prompt", "none");
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }
}
