package org.edu_sharing.spring.security.basic;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

public class EduWebSecurityCustomizer {


    static Config config = LightbendConfigLoader.get();

    public static org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.debug(config.getBoolean("security.sso.debug"));
    }
}
