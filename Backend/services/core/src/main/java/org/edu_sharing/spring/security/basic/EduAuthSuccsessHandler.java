package org.edu_sharing.spring.security.basic;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.edu_sharing.spring.security.openid.SilentLoginAuthorizationRequestResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *  redirects to /shibboleth after successfull auth
 *
 *  notice:
 *  SavedRequestAwareAuthenticationSuccessHandler is default which always redircts to /samllogout url with GET which is not vailable
 *      * after logout/login again
 */
@Component
public class EduAuthSuccsessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired(required = false)
    SilentLoginAuthorizationRequestResolver silentLoginAuthorizationRequestResolver;

    public EduAuthSuccsessHandler(){
        super("/shibboleth");
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        if(silentLoginAuthorizationRequestResolver != null && silentLoginAuthorizationRequestResolver.protectedPathNeedsSilentLogin(request)){
            return silentLoginAuthorizationRequestResolver.getSilentLoginPath();
        }else {
            return super.determineTargetUrl(request, response);
        }
    }
}