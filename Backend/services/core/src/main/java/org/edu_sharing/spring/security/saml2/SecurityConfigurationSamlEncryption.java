package org.edu_sharing.spring.security.saml2;

import org.apache.xml.security.encryption.XMLCipherParameters;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.xmlsec.encryption.support.DataEncryptionParameters;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.cert.X509Certificate;
import java.util.Base64;

@ConditionalOnProperty(name = "security.sso.saml.encryptLogoutRequest", havingValue = "true")
@Configuration
public class SecurityConfigurationSamlEncryption {
    @Bean
    Saml2LogoutRequestResolver logoutRequestResolver(RelyingPartyRegistrationRepository registrations) {
        OpenSaml4LogoutRequestResolver logoutRequestResolver =
                new OpenSaml4LogoutRequestResolver(registrations);
        logoutRequestResolver.setParametersConsumer((parameters) -> {
            String name = ((Saml2AuthenticatedPrincipal) parameters.getAuthentication().getPrincipal()).getFirstAttribute("CustomAttribute");
            String format = "urn:oasis:names:tc:SAML:2.0:nameid-format:transient";
            LogoutRequest logoutRequest = parameters.getLogoutRequest();
            NameID nameId = logoutRequest.getNameID();
            nameId.setValue(name);
            nameId.setFormat(format);

            RelyingPartyRegistration.AssertingPartyDetails assertingPartyDetails = parameters.getRelyingPartyRegistration().getAssertingPartyDetails();


            Saml2X509Credential credential = (assertingPartyDetails.getEncryptionX509Credentials().size() > 0 )
                    //when a key descriptor <md:KeyDescriptor use="encryption"> is defined in idp metadata
                    ? assertingPartyDetails.getEncryptionX509Credentials().iterator().next()
                    //else fallback to default certificate
                    : assertingPartyDetails.getVerificationX509Credentials().iterator().next();

            EncryptedID encrypted = encrypted(nameId, credential);
            logoutRequest.setEncryptedID(encrypted);
        });
        return logoutRequestResolver;
    }

    static EncryptedID encrypted(NameID nameId, Saml2X509Credential credential) {
        X509Certificate certificate = credential.getCertificate();
        Encrypter encrypter = getEncrypter(certificate);
        try {
            return encrypter.encrypt(nameId);
        }
        catch (EncryptionException ex) {
            throw new Saml2Exception("Unable to encrypt nameID.", ex);
        }
    }

    private static Encrypter getEncrypter(X509Certificate certificate) {
        String dataAlgorithm = XMLCipherParameters.AES_256;
        String keyAlgorithm = XMLCipherParameters.RSA_1_5;
        BasicCredential dataCredential = new BasicCredential(SECRET_KEY);
        DataEncryptionParameters dataEncryptionParameters = new DataEncryptionParameters();
        dataEncryptionParameters.setEncryptionCredential(dataCredential);
        dataEncryptionParameters.setAlgorithm(dataAlgorithm);
        Credential credential = CredentialSupport.getSimpleCredential(certificate, null);
        KeyEncryptionParameters keyEncryptionParameters = new KeyEncryptionParameters();
        keyEncryptionParameters.setEncryptionCredential(credential);
        keyEncryptionParameters.setAlgorithm(keyAlgorithm);
        Encrypter encrypter = new Encrypter(dataEncryptionParameters, keyEncryptionParameters);
        Encrypter.KeyPlacement keyPlacement = Encrypter.KeyPlacement.valueOf("PEER");
        encrypter.setKeyPlacement(keyPlacement);
        return encrypter;
    }

    private static SecretKey SECRET_KEY = new SecretKeySpec(
            Base64.getDecoder().decode("shOnwNMoCv88HKMEa91+FlYoD5RNvzMTAL5LGxZKIFk="), "AES");

}
