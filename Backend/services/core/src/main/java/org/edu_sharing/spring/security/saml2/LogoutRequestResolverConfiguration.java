package org.edu_sharing.spring.security.saml2;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.impl.SSODescriptorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.logout.Saml2LogoutRequest;
import org.springframework.security.saml2.provider.service.registration.OpenSamlAssertingPartyDetails;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;

import java.util.List;


@Configuration
@Profile("samlEnabled")
public class LogoutRequestResolverConfiguration {
    @Bean
    Saml2LogoutRequestResolver logoutRequestResolver(RelyingPartyRegistrationRepository registrations) {
        return new LogoutRequestResolver(registrations);
    }

    public static class LogoutRequestResolver implements Saml2LogoutRequestResolver {
        Logger logger = Logger.getLogger(LogoutRequestResolver.class);
        OpenSaml4LogoutRequestResolver wrapped;
        RelyingPartyRegistrationRepository registrations;

        public LogoutRequestResolver(RelyingPartyRegistrationRepository registrations){
            this.registrations = registrations;
            this.wrapped = new OpenSaml4LogoutRequestResolver(registrations);
            this.wrapped.setParametersConsumer((parameters) -> {

                EntityDescriptor entityDescriptor = ((OpenSamlAssertingPartyDetails) parameters.getRelyingPartyRegistration().getAssertingPartyDetails()).getEntityDescriptor();
                if (entityDescriptor != null) {
                    List<RoleDescriptor> roleDescriptors = entityDescriptor.getRoleDescriptors();
                    if(roleDescriptors != null && roleDescriptors.size() > 0) {
                        List<NameIDFormat> nameIDFormats = ((SSODescriptorImpl) roleDescriptors.get(0)).getNameIDFormats();
                        if(nameIDFormats != null && nameIDFormats.size() > 0) {
                            NameIDFormat nameIDFormat = nameIDFormats.get(0);

                            NameID nameId = parameters.getLogoutRequest().getNameID();
                            if(nameId != null && nameId.getFormat() == null) {
                                nameId.setFormat(nameIDFormat.getURI());
                            }
                        }
                    }
                }
            });
        }

        @Override
        public Saml2LogoutRequest resolve(HttpServletRequest request, Authentication authentication) {

            Saml2LogoutRequest req = wrapped.resolve(request, authentication);
            if(req != null) {
                return req;
            }

            /**
             * fix 401 blank page
             * fallback when idp metadata does not LogoutService defined
             *
             *  spring-security 6.2
             *  LogoutSuccessHandler for saml can not be customized
             *  org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2RelyingPartyInitiatedLogoutSuccessHandler z78
             */

            Object principal = authentication.getPrincipal();
            String id = null;
            if (principal instanceof Saml2AuthenticatedPrincipal) {
                id = ((Saml2AuthenticatedPrincipal) principal).getRelyingPartyRegistrationId();
            }

            if(id == null){
                logger.info("no registered relying party id found");
                return null;
            }

            RelyingPartyRegistration rpRegistration =  this.registrations.findByRegistrationId(id);

            //return fake saml request
            return Saml2LogoutRequest.withRelyingPartyRegistration(rpRegistration)
                    .location("/components/error/SSO_SAML_IDP_LOGOUT_UNDEFINED")
                    .parameters(stringStringMap -> {})
                    .samlRequest("")
                    .binding(Saml2MessageBinding.REDIRECT).relayState("fix")
                    .parametersQuery(f -> null).build();
        }
    }
}
