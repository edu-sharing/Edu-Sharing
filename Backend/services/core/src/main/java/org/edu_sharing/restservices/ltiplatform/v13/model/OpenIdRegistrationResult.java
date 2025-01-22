package org.edu_sharing.restservices.ltiplatform.v13.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenIdRegistrationResult {
    private String client_id;
    private List<String> response_types;
    private String jwks_uri;
    private String initiate_login_uri;
    private List<String> grant_types;
    private List<String> redirect_uris;
    private String application_type;
    private String token_endpoint_auth_method;
    private String client_name;
    private String logo_uri;
    private String scope;

    @JsonProperty("https://purl.imsglobal.org/spec/lti-tool-configuration")
    private LTIToolConfiguration ltiToolConfiguration;

    @Data
    public static class LTIToolConfiguration {
        private String version;
        private String deployment_id;
        private String target_link_uri;
        private String domain;
        private String description;
        private List<String> claims;
    }
}
