package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.time.DateUtils;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;

import javax.security.auth.x500.X500Principal;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Slf4j
public class KeyGenerator {


	public void execute(boolean test) {
		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();

		Signing s = new Signing();

		KeyPair kp = s.generateKeys();
		try {
			String file = PropertiesHelper.Config.getAbsolutePathForConfigFile(
					PropertiesHelper.Config.getPropertyFilePath(CCConstants.REPOSITORY_FILE_HOME)
			);
			if (homeRepo.getPublicKey() == null) {
				log.info("will set public key");
				if (!test) {

					String pubKeyString = "-----BEGIN PUBLIC KEY-----\n"
							+ new String(new Base64().encode(kp.getPublic().getEncoded())) + "-----END PUBLIC KEY-----";

					PropertiesHelper.setProperty(ApplicationInfo.KEY_PUBLIC_KEY, pubKeyString,
							file, PropertiesHelper.XML);
				}

			}

			if (homeRepo.getPrivateKey() == null) {
				log.info("will set private key");

				if (!test) {
					PropertiesHelper.setProperty(ApplicationInfo.KEY_PRIVATE_KEY,
							new String(new Base64().encode(kp.getPrivate().getEncoded())),
							file, PropertiesHelper.XML);
				}

			}

			if(homeRepo.getCertificate() == null){
				//on first startup the private key / pub key would be null
				if(homeRepo.getPrivateKey() == null || homeRepo.getPublicKey() == null){
					ApplicationInfoList.refresh();
					homeRepo = ApplicationInfoList.getHomeRepository();
				}

				log.info("will set certificate");
				PrivateKey pemPrivateKey = s.getPemPrivateKey(homeRepo.getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);
				PublicKey pemPublicKey = s.getPemPublicKey(homeRepo.getPublicKey(), CCConstants.SECURITY_KEY_ALGORITHM);
				String cert = encodeCertificate(generateCertificate(homeRepo,pemPrivateKey,pemPublicKey));
				if (!test) {
					PropertiesHelper.setProperty(ApplicationInfo.KEY_CERTIFICATE,
							cert,
							file, PropertiesHelper.XML);
				}
			}

			if(homeRepo.getLtiKid() == null){
				log.info("will set lti kid");

				if (!test) {
					PropertiesHelper.setProperty(ApplicationInfo.KEY_LTI_KID,
							UUID.randomUUID().toString(),
							file, PropertiesHelper.XML);
				}

			}

			ApplicationInfoList.refresh();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}


	public X509Certificate generateCertificate(ApplicationInfo homeApp, PrivateKey privateKey, PublicKey publicKey) throws Exception {
		X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();

		String cn = "CN={domain}, OU=Unknown, O={domain}, L=UnkownCity, ST=UnkownState, C=UnkownCountry";
		cn = cn.replaceAll("\\{domain}",homeApp.getDomain());
		X500Principal issuer = new X500Principal("CN="+cn);
		X500Principal subject = new X500Principal("CN="+cn);

		certGenerator.setSerialNumber(BigInteger.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
		certGenerator.setIssuerDN(issuer);
		certGenerator.setSubjectDN(subject);
		certGenerator.setPublicKey(publicKey);
		long now = System.currentTimeMillis();
		certGenerator.setNotBefore(new Date(now));
		certGenerator.setNotAfter(DateUtils.addYears(new Date(now),10)); // Valid for 10 year
		certGenerator.setSignatureAlgorithm("SHA256WithRSAEncryption");

		return certGenerator.generate(privateKey, "BC");
	}

	public static String encodeCertificate(X509Certificate cert) throws Exception {
		byte[] certBytes = cert.getEncoded();
		return java.util.Base64.getEncoder().encodeToString(certBytes);
	}

	public static void main(String[] args) {

		Signing s = new Signing();

		KeyPair kp = s.generateKeys();

		System.out.println(new String(new Base64().encode(kp.getPublic().getEncoded())));

		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(1024);
			KeyPair keyPair = keyPairGenerator.genKeyPair();
			String publicKeyFilename = "public";

			byte[] publicKeyBytes = keyPair.getPublic().getEncoded();

			FileOutputStream fos = new FileOutputStream(publicKeyFilename);
			fos.write(publicKeyBytes);
			fos.close();

			System.out.println(PropertiesHelper.getProperty(ApplicationInfo.KEY_PRIVATE_KEY,
					CCConstants.REPOSITORY_FILE_HOME, PropertiesHelper.XML));

			Signing signing = new Signing();

			PrivateKey pk = signing.getPemPrivateKey(PropertiesHelper.getProperty(ApplicationInfo.KEY_PRIVATE_KEY,
					CCConstants.REPOSITORY_FILE_HOME, PropertiesHelper.XML), CCConstants.SECURITY_KEY_ALGORITHM);

			String data = "hello world";
			byte[] signature = signing.sign(pk, data, CCConstants.SECURITY_SIGN_ALGORITHM);

			PublicKey pubK = signing.getPemPublicKey(PropertiesHelper.getProperty(ApplicationInfo.KEY_PUBLIC_KEY,
					CCConstants.REPOSITORY_FILE_HOME, PropertiesHelper.XML), CCConstants.SECURITY_KEY_ALGORITHM);
			System.out.println("verify:" + signing.verify(pubK, signature, data, CCConstants.SECURITY_SIGN_ALGORITHM));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
