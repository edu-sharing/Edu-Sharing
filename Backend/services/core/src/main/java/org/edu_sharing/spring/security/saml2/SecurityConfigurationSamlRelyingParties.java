package org.edu_sharing.spring.security.saml2;

import com.typesafe.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Profile("samlEnabled")
@Configuration
public class SecurityConfigurationSamlRelyingParties {

    Config config = LightbendConfigLoader.get();

    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        try(InputStream pkInputStream = getPKInputStream()){

            Converter<InputStream, RSAPrivateKey> converter = RsaKeyConverters.pkcs8();
            RSAPrivateKey privateKey = converter.convert(pkInputStream);

            String globalSPRegistrationId = "one";


            Collection<RelyingPartyRegistration.Builder> b = null;
            String cfgMetadataPath = "security.sso.saml.idp.metadata";
            if(config.hasPath(cfgMetadataPath +".content")){
                b = RelyingPartyRegistrations.collectionFromMetadata(IOUtils.toInputStream(config.getString(cfgMetadataPath +".content")));
            }else if(config.hasPath(cfgMetadataPath +".filePath")){
                File f = new File(config.getString(cfgMetadataPath +".filePath"));
                b = RelyingPartyRegistrations.collectionFromMetadata(FileUtils.openInputStream(f));
            }else if(config.hasPath(cfgMetadataPath + ".url")){
                b = RelyingPartyRegistrations
                        .collectionFromMetadataLocation(config.getString(cfgMetadataPath + ".url"));
            }

            if(b == null){
                throw new RuntimeException("no IDP metadata configured");
            }

            Map<String, AtomicInteger> rpIds = new HashMap<>();

            List<RelyingPartyRegistration> relyingPartyRegistration = b
                    .stream().map((builder) -> {
                        String relyingPartyId = getRelyingPartyId(builder);
                        int count = rpIds.computeIfAbsent(relyingPartyId, k -> new AtomicInteger(0)).incrementAndGet();
                        relyingPartyId = (count > 1) ? relyingPartyId + "-" + count : relyingPartyId;
                        return builder.registrationId(relyingPartyId)
                                .entityId("{baseUrl}/saml2/service-provider-metadata/" + globalSPRegistrationId)
                                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/" + globalSPRegistrationId)
                                .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo")
                                .signingX509Credentials(
                                        (c) -> c.add(Saml2X509Credential.signing(privateKey, relyingPartyCertificate())))
                                .decryptionX509Credentials(
                                        (c) -> c.add(Saml2X509Credential.decryption(privateKey, relyingPartyCertificate())))
                                .build();
                    }).collect(Collectors.toList());
            return new InMemoryRelyingPartyRegistrationRepository(relyingPartyRegistration);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * tries to get idp domain
     * @param b
     * @return
     */
    String getRelyingPartyId(RelyingPartyRegistration.Builder b){
        try {
            String entityId = b.build().getAssertingPartyDetails().getEntityId();
            if(!entityId.startsWith("http://") && !entityId.startsWith("https://")) {
                entityId = "http://" + entityId;
            }
            URI uri = new URI(entityId);
            String host = uri.getHost();
            return host != null ? host : uri.toString();
        } catch (URISyntaxException e) {
            return UUID.randomUUID().toString();
        }
    }

    private @NotNull InputStream getPKInputStream() throws IOException {
        if(config.getBoolean("security.sso.saml.useHomeApplicationKeys")){
            return  IOUtils.toInputStream(cleanupPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey()), "UTF-8");
        }else if(config.hasPath("security.sso.saml.privateKey.value")){
            return IOUtils.toInputStream(config.getString("security.sso.saml.privateKey.value"), "UTF-8");
        }else{
            return new ClassPathResource(config.getString("security.sso.saml.privateKey.location")).getInputStream();
        }
    }

    private String cleanupPrivateKey(String key){
        String begin = "-----BEGIN PRIVATE KEY-----";
        String end = "-----END PRIVATE KEY-----";
        String privKey = key;
        privKey = privKey.trim();
        privKey = privKey.replaceAll("(.{64})", "$1"+System.lineSeparator());

        if(!privKey.startsWith(begin)){
            privKey = begin + System.lineSeparator() +privKey;
        }
        if(!privKey.endsWith(end)){
            privKey= privKey + System.lineSeparator() + end;
        }
        return privKey;
    }

    private String cleanupCert(String value){
        String begin = "-----BEGIN CERTIFICATE-----";
        String end = "-----END CERTIFICATE-----";
        String cert = value;
        cert = cert.replace(begin,"");
        cert = cert.replace(end,"");
        cert = cert.trim();
        cert = cert.replaceAll("(.{64})", "$1"+System.lineSeparator());
        cert = begin + System.lineSeparator() + cert;
        cert = cert + System.lineSeparator() +end;
        return cert;
    }

    X509Certificate relyingPartyCertificate() {
        try(InputStream inputStream = getCertInputStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private @NotNull InputStream getCertInputStream() throws IOException {
        if(config.getBoolean("security.sso.saml.useHomeApplicationKeys")){
            return IOUtils.toInputStream(cleanupCert(ApplicationInfoList.getHomeRepository().getCertificate()), "UTF-8");
        }else if(config.hasPath("security.sso.saml.certificate.value")){
            return IOUtils.toInputStream(config.getString("security.sso.saml.certificate.value"), "UTF-8");
        }else{
            return new ClassPathResource(config.getString("security.sso.saml.certificate.location")).getInputStream();
        }

    }
}
