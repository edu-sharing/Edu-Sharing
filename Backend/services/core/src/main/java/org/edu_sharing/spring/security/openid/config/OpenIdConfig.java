package org.edu_sharing.spring.security.openid.config;

import com.typesafe.config.Optional;
import lombok.Data;

@Data
public class OpenIdConfig {
   private String issuer;
   private String clientId;
   private String secret;
   @Optional
   private String contextId;
}
