package org.edu_sharing.spring.security.saml2;

import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.impl.SSODescriptorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.registration.OpenSamlAssertingPartyDetails;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;

import java.util.List;


@Configuration
@Profile("samlEnabled")
public class LogoutRequestResolverConfiguration {
    @Bean
    Saml2LogoutRequestResolver logoutRequestResolver(RelyingPartyRegistrationRepository registrations) {
        OpenSaml4LogoutRequestResolver logoutRequestResolver =
                new OpenSaml4LogoutRequestResolver(registrations);
        logoutRequestResolver.setParametersConsumer((parameters) -> {

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
        return logoutRequestResolver;
    }
}
