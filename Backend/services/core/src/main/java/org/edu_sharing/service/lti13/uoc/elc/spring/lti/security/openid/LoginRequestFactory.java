package org.edu_sharing.service.lti13.uoc.elc.spring.lti.security.openid;

import edu.uoc.elc.lti.tool.oidc.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;

public class LoginRequestFactory {
    public static LoginRequest from(HttpServletRequest request) {
        return LoginRequest.builder()
                .iss(request.getParameter("iss"))
                .login_hint(request.getParameter("login_hint"))
                .target_link_uri(request.getParameter("target_link_uri"))
                .lti_message_hint(request.getParameter("lti_message_hint"))
                .lti_deployment_id(request.getParameter("lti_deployment_id"))
                .client_id(request.getParameter("client_id"))
                .build();
    }
}
