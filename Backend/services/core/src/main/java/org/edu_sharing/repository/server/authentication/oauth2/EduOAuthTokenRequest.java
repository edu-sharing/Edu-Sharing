package org.edu_sharing.repository.server.authentication.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import org.apache.http.entity.ContentType;

/**
 * wrapper for OAuthTokenRequest since the library is too old and does not support tomcat 10 request schema
 */
public class EduOAuthTokenRequest {
    private final HttpServletRequest request;

    public EduOAuthTokenRequest(HttpServletRequest request) {
        this.request = request;
        if(!this.request.getMethod().equals(HttpMethod.POST)) {
            throw new IllegalArgumentException("Wrong HTTP method");
        }
        if(!this.request.getContentType().equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
            throw new IllegalArgumentException("Wrong HTTP content type");
        }
    }

    public String getClientId() {
        return this.request.getParameter("client_id");
    }

    public String getClientSecret() {
        return this.request.getParameter("client_secret");
    }

    public String getGrantType() {
        return this.request.getParameter("grant_type");
    }

    public String getUsername() {
        return this.request.getParameter("username");
    }

    public String getPassword() {
        return this.request.getParameter("password");
    }

    public String getRefreshToken() {
        return this.request.getParameter("refresh_token");
    }
}
