package org.edu_sharing.spring.security.saml2;

import com.typesafe.config.Config;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.security.basic.CSRFConfig;
import org.edu_sharing.spring.security.basic.EduAuthSuccsessHandler;
import org.edu_sharing.spring.security.basic.EduWebSecurityCustomizer;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.saml2.Saml2LogoutConfigurer;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.Random;

import static org.springframework.security.config.Customizer.withDefaults;

@Profile("samlEnabled")
@EnableWebSecurity()
@Configuration
public class SecurityConfigurationSaml {

    Config config = LightbendConfigLoader.get();

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return EduWebSecurityCustomizer.webSecurityCustomizer();
    }

    @Bean
    SecurityFilterChain app(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/shibboleth")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
                )

                .saml2Login((login) -> login

                        .successHandler(new EduAuthSuccsessHandler())
                        //don't use this cause it uses SavedRequestAwareAuthenticationSuccessHandler
                        //.defaultSuccessUrl("/shibboleth")
                )
                .sessionManagement(s -> s.sessionFixation().none())

                .saml2Logout(logout -> logout.withObjectPostProcessor(
                        switchPost2GetProcessor()
                ))
                /**
                 * saml2 logout is using logoutSuccessHandler from default logout config
                 * @see org.springframework.security.config.annotation.web.configurers.saml2Saml2LogoutConfigurer
                 */
                .logout(logout -> logout.logoutSuccessHandler(new EduSimpleUrlLogoutSuccessHandler()))
                .saml2Metadata(withDefaults());

        CSRFConfig.config(http);

        return http.build();
    }

    /**
     * change LogoutFilter Method from POST to GET
     *  org.springframework.security.config.annotation.web.configurers.saml2.Saml2LogoutConfigurer z.273
     *  creates a POST Mapping for the Logout Filter. in edu-sharing we use GET to trigger logout.
     *
     *  we have to do the same things they do in private Method  Saml2LogoutConfigurer.createLogoutMatcher()
     */
    @NotNull
    private ObjectPostProcessor<LogoutFilter> switchPost2GetProcessor() {
        return new ObjectPostProcessor<LogoutFilter>() {
            @Override
            public <O extends LogoutFilter> O postProcess(O logoutFilter) {

                //switch to get
                RequestMatcher logoutRequestMatcher = new AntPathRequestMatcher("/logout", "GET");
                Class<?>[] declaredClasses = Saml2LogoutConfigurer.class.getDeclaredClasses();
                for (Class<?> innerClass : declaredClasses) {
                    if (innerClass.getName().contains("Saml2RequestMatcher")) {
                        Constructor<?> constructor = innerClass.getDeclaredConstructors()[0];
                        constructor.setAccessible(true);
                        try {
                            Object o = constructor.newInstance(SecurityContextHolder.getContextHolderStrategy());
                            logoutFilter.setLogoutRequestMatcher(new AndRequestMatcher(logoutRequestMatcher, (RequestMatcher) o));
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                return logoutFilter;
            }
        };
    }

    public class EduSimpleUrlLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler{
        @Override
        public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

            String successTarget = "/shibboleth";
            try {
                successTarget = ConfigServiceFactory.getCurrentConfig(request).getValue("logout.next", successTarget);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //override at runtime
            this.setDefaultTargetUrl(successTarget);
            super.onLogoutSuccess(request, response, authentication);
        }
    }



    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        try(InputStream inputStream = config.getBoolean("security.sso.saml.useHomeApplicationKeys")
                ? IOUtils.toInputStream(getHomeAppPrivateKey(),"UTF-8")
                : new ClassPathResource(config.getString("security.sso.saml.privateKey.location")).getInputStream()){

            Converter<InputStream, RSAPrivateKey> converter = RsaKeyConverters.pkcs8();
            RSAPrivateKey privateKey = converter.convert(inputStream);

            RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
                    .fromMetadataLocation(config.getString("security.sso.saml.idp.metadata.url"))
                    .registrationId("one")
                    .decryptionX509Credentials(
                            (c) -> c.add(Saml2X509Credential.decryption(privateKey, relyingPartyCertificate())))
                    .signingX509Credentials(
                            (c) -> c.add(Saml2X509Credential.signing(privateKey, relyingPartyCertificate())))
                    .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo")
                    //.singleLogoutServiceResponseLocation(ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/shibboleth")
                    //.singleLogoutServiceBinding(Saml2MessageBinding.POST)
                    .build();
            return new InMemoryRelyingPartyRegistrationRepository(relyingPartyRegistration);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private String getHomeAppPrivateKey(){
        String begin = "-----BEGIN PRIVATE KEY-----";
        String end = "-----END PRIVATE KEY-----";
        String privKey = ApplicationInfoList.getHomeRepository().getPrivateKey();
        privKey = privKey.trim();
        privKey = privKey.replaceAll("(.{64})", "$1"+System.lineSeparator());

        if(!privKey.startsWith(begin)){
            privKey = begin + System.lineSeparator() +privKey;
        }
        if(!privKey.endsWith(end)){
            privKey= privKey + System.lineSeparator() + end;
        }
        System.out.println(privKey);
        return privKey;
    }

    private String getHomeAppCert(){
        String begin = "-----BEGIN CERTIFICATE-----";
        String end = "-----END CERTIFICATE-----";
        String cert = ApplicationInfoList.getHomeRepository().getCertificate();
        cert = cert.replace(begin,"");
        cert = cert.replace(end,"");
        cert = cert.trim();
        cert = cert.replaceAll("(.{64})", "$1"+System.lineSeparator());
        cert = begin + System.lineSeparator() + cert;
        cert = cert + System.lineSeparator() +end;
        System.out.println(cert);
        return cert;
    }

    X509Certificate relyingPartyCertificate() {
        try(InputStream inputStream = config.getBoolean("security.sso.saml.useHomeApplicationKeys")
                ? IOUtils.toInputStream(getHomeAppCert(),"UTF-8")
                : new ClassPathResource(config.getString("security.sso.saml.publicKey.location")).getInputStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
