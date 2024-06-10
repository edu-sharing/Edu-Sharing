package org.edu_sharing.spring;

import org.edu_sharing.spring.security.basic.SecurityConfigurationBasic;
import org.edu_sharing.spring.security.openid.SecurityConfigurationOpenIdConnect;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import({SecurityConfigurationSaml.class, SecurityConfigurationOpenIdConnect.class, SecurityConfigurationBasic.class})
public class SpringConfigRoot {
}
