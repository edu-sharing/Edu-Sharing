package org.edu_sharing.restservices.ltiplatform.v13.model;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ManualRegistrationData {

    private String toolName;
    private String toolUrl;
    private String toolDescription;
    private String keysetUrl;
    private String loginInitiationUrl;
    private List<String> redirectionUrls;
    @Schema(description = "JSON Oject where each value is a string. Custom parameters to be included in each launch to this tool. If a custom parameter is also defined at the message level, the message level value takes precedence. The value of the custom parameters may be substitution parameters as described in the LTI Core [LTI-13] specification")
    private List<String> customParameters;
    private String logoUrl;
    @JsonProperty(required = true)
    @Schema(description = "The default target link uri to use unless defined otherwise in the message or link definition")
    private String targetLinkUri;
    @Schema(description = "The target link uri to use for DeepLing Message")
    private String targetLinkUriDeepLink;
    @JsonProperty(required = true)
    @Schema(description = "Name of the Tool to be presented to the End-User. Localized representations may be included as described in Section 2.1 of the [OIDC-Reg] specification")
    private String clientName;
}
