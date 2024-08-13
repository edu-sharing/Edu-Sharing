package org.edu_sharing.spring.security.openid;

import com.typesafe.config.Config;
import io.opentelemetry.api.internal.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.security.basic.CSRFConfig;
import org.edu_sharing.spring.security.basic.EduAuthSuccsessHandler;
import org.edu_sharing.spring.security.basic.EduWebSecurityCustomizer;
import org.edu_sharing.spring.security.openid.config.OpenIdConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Profile(SecurityConfigurationOpenIdConnect.PROFILE_ID)
@EnableWebSecurity()
@Configuration
public class SecurityConfigurationOpenIdConnect {

    Logger logger = Logger.getLogger(SecurityConfigurationOpenIdConnect.class);

    public static final String PROFILE_ID = "openidEnabled";

    public static final String DEFAULT_REGISTRATION_ID = "OPENID_DEFAULT";

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return EduWebSecurityCustomizer.webSecurityCustomizer();
    }

    @Autowired
    EduAuthSuccsessHandler eduAuthSuccsessHandler;

    @Autowired
    OpenIdConfigService configService;

    @Autowired
    @Lazy // Lazy annotation to break the circular dependency
    SilentLoginAuthorizationRequestResolver silentLoginAuthorizationRequestResolver;

    @Bean
    SecurityFilterChain app(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        //   .requestMatchers("/shibboleth").authenticated()
                        //   .requestMatchers("/**").permitAll()
                        /**
                         * we have to use ant matchers here cause the new spring-security version 6.2
                         * tries to use mvc matchers cause it is in classpath. but we don't use mvc matcher,
                         * which causes NoSuchBeanDefinitionException mvcHandlerMappingIntrospector
                         *
                         * org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry diff 6.1 vs 6.2
                         */
                        .requestMatchers(new AntPathRequestMatcher("/shibboleth")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher(silentLoginAuthorizationRequestResolver.getSilentLoginPath())).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
                )

                .oauth2Login(login -> login
                        //redirect to login page with angular does fallback to default domain, so request attributes not longer available
                        //so it's not useabe in angular dev mode at the moment
                        .loginPage("/sso")
                        .failureHandler(new CustomErrorHandler())
                        .successHandler(eduAuthSuccsessHandler)
                        .authorizationEndpoint(ae -> ae.authorizationRequestResolver(silentLoginAuthorizationRequestResolver)))
                .sessionManagement(s -> s.sessionFixation().none())
                //frontchannel logout triggerd by edu-sharing gui
                .logout((logout) ->
                        logout.logoutSuccessHandler(oidcLogoutSuccessHandler()))
                //backchannel logout
                .oidcLogout((logout) -> logout
                        .backChannel((bcLogout) -> bcLogout.logoutUri(ApplicationInfoList.getHomeRepository().getBaseUrl() + "/edu-sharing/logout"))
                ).headers(h -> h.frameOptions(f -> f.disable()));

        CSRFConfig.config(http);

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository(configService)){
                    @Override
                    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

                        String targetUrl = super.determineTargetUrl(request,response,authentication);


                        String idpRedirectUrl = "";
                        String successTarget = "/shibboleth";
                        try {
                            successTarget = ConfigServiceFactory.getCurrentConfig(request).getValue("logout.next", successTarget);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        if(!successTarget.startsWith("http")){
                            UriComponents successUrlComp = UriComponentsBuilder
                                    .fromHttpUrl(UrlUtils.buildFullRequestUrl(request)).build();

                            idpRedirectUrl = successUrlComp.getScheme()+"://"+successUrlComp.getHost();

                            int port = successUrlComp.getPort();
                            if(port != 80 && port != 443 && port > 0){
                                idpRedirectUrl += ":"+port;
                            }

                            idpRedirectUrl += request.getContextPath() + successTarget;
                        }else{
                            idpRedirectUrl = successTarget;
                        }

                        return UrlTool.replaceParam(targetUrl,"post_logout_redirect_uri",idpRedirectUrl);
                    }
                };
        return oidcLogoutSuccessHandler;
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(OpenIdConfigService configService) {
        logger.info("starting oidc registration");
        List<ClientRegistration> registrations = new ArrayList<>();
        configService.getAllConfigs().forEach(config -> registrations.add(ClientRegistrations
                .fromIssuerLocation(config.getIssuer())
                .clientId(config.getClientId())
                .clientSecret(config.getSecret())
                .scope("openid")
                .registrationId(StringUtils.isNullOrEmpty(config.getContextId()) ? DEFAULT_REGISTRATION_ID : config.getContextId())
                .build()));
        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    SilentLoginAuthorizationRequestResolver silentLoginAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository){
        logger.info("starting init silentLoginAuthorizationRequestResolver");
        return new SilentLoginAuthorizationRequestResolver(clientRegistrationRepository);
    }
}
