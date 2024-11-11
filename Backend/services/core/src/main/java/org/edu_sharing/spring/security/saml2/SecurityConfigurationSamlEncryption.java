package org.edu_sharing.spring.security.saml2;

import org.apache.xml.security.encryption.XMLCipherParameters;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.encryption.support.DataEncryptionParameters;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

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
            logoutRequest.setNameID(null);
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

    private static Encrypter getEncrypter(X509Certificate certificate)  {
        String dataAlgorithm = XMLCipherParameters.AES_256;
        BasicCredential dataCredential = new BasicCredential(generateSecretKey());
        DataEncryptionParameters dataEncryptionParameters = new DataEncryptionParameters();
        dataEncryptionParameters.setEncryptionCredential(dataCredential);
        dataEncryptionParameters.setAlgorithm(dataAlgorithm);

        String keyAlgorithm = XMLCipherParameters.RSA_OAEP;
        KeyEncryptionParameters keyEncryptionParameters = new KeyEncryptionParameters();
        BasicX509Credential x509Credential = new BasicX509Credential(certificate);
        keyEncryptionParameters.setEncryptionCredential(x509Credential);
        keyEncryptionParameters.setAlgorithm(keyAlgorithm);


        X509KeyInfoGeneratorFactory factory = new X509KeyInfoGeneratorFactory();
        factory.setEmitEntityIDAsKeyName(true);
        factory.setEmitX509SubjectName(true);
        factory.setEmitX509IssuerSerial(true);


        keyEncryptionParameters.setKeyInfoGenerator(factory.newInstance());

        Encrypter encrypter = new Encrypter(dataEncryptionParameters, keyEncryptionParameters);
        encrypter.setKeyPlacement(Encrypter.KeyPlacement.PEER);

        return encrypter;
    }

    private static SecretKey generateSecretKey() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGen.init(256);  // for AES-256
        SecretKey secretKey = keyGen.generateKey();
        return secretKey;
    }

}
