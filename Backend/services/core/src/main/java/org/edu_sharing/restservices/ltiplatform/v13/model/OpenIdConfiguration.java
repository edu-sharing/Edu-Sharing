package org.edu_sharing.restservices.ltiplatform.v13.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OpenIdConfiguration {

    private String issuer;
    private String token_endpoint;
    private List<String> token_endpoint_auth_methods_supported;
    private List<String> token_endpoint_auth_signing_alg_values_supported;
    private String jwks_uri;
    private String authorization_endpoint;
    private String registration_endpoint;
    private List<String> scopes_supported;
    private List<String> response_types_supported;
    private List<String> subject_types_supported;
    private List<String> id_token_signing_alg_values_supported;
    private List<String> claims_supported;
    @JsonProperty("https://purl.imsglobal.org/spec/lti-platform-configuration")
    private LTIPlatformConfiguration ltiPlatformConfiguration;

    @Data
    public static class LTIPlatformConfiguration {
        private String product_family_code;
        private String version;
        private List<Message> messages_supported = new ArrayList<>();
        private List<String> variables;

        @Data
        public static class Message {
            private String type;
            private List<String> placements;
        }
    }

}
