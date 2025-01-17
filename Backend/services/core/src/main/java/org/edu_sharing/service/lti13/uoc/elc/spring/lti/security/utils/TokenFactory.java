package org.edu_sharing.service.lti13.uoc.elc.spring.lti.security.utils;

import jakarta.servlet.http.HttpServletRequest;

public class TokenFactory {
    public static String from(HttpServletRequest httpServletRequest) {
        String token = httpServletRequest.getParameter("jwt");
        if (token == null || "".equals(token)) {
            token = httpServletRequest.getParameter("id_token");
        }
        return token;
    }

}

