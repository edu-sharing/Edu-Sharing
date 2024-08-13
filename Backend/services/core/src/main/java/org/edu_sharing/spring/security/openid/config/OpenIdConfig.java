package org.edu_sharing.spring.security.openid.config;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OpenIdConfig {
   String issuer;
   String clientId;
   String secret;
   String contextId;
}
