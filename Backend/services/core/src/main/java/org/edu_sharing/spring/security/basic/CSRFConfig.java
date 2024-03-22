package org.edu_sharing.spring.security.basic;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class CSRFConfig {

    static Config config = LightbendConfigLoader.get();

    public static HttpSecurity config(HttpSecurity http) throws Exception{

        if(config.hasPath("security.sso.disableCsrf") && config.getBoolean("security.sso.disableCsrf")){
            http.csrf(AbstractHttpConfigurer::disable);
        }else {

            CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
            XorCsrfTokenRequestAttributeHandler delegate = new XorCsrfTokenRequestAttributeHandler();
            // set the name of the attribute the CsrfToken will be populated on
            delegate.setCsrfRequestAttributeName("_csrf");
            // Use only the handle() method of XorCsrfTokenRequestAttributeHandler and the
            // default implementation of resolveCsrfTokenValue() from CsrfTokenRequestHandler
            CsrfTokenRequestHandler requestHandler = delegate::handle;

            http.authorizeHttpRequests((authorize) -> authorize
                    .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
            ).csrf((csrf) -> csrf
                    .csrfTokenRepository(tokenRepository)
                    .csrfTokenRequestHandler(requestHandler)
            );
        }
        return http;
    }

}
