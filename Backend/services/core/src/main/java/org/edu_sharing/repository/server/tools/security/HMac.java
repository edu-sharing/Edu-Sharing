package org.edu_sharing.repository.server.tools.security;


import org.apache.commons.codec.digest.HmacUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;


public class HMac {

    public static final String ALG_SHA256 = "HmacSHA256";


    Logger logger = Logger.getLogger(HMac.class);

    String sharedSecret;
    String algorithm;


    public HMac(String sharedSecret, String algorithm){
        this.sharedSecret = sharedSecret;
        this.algorithm = algorithm;
    }

    public String calculateHmac(String data) {
        //had problems using HmacUtils as class member and reuse, sometimes got another hex for the same data
        // so reinit
        HmacUtils hmacUtils = new HmacUtils(algorithm,sharedSecret);
        return hmacUtils.hmacHex(data);
    }

    public boolean checkHmac(String data, String hmacHex) {
        return calculateHmac(data).equals(hmacHex);
    }

    public static HMac getInstance() {
        KeyStoreService keyStoreService = new KeyStoreService();

        try {
            String hmacPassword = keyStoreService.readPasswordFromKeyStore(CCConstants.EDU_PASSWORD_KEYSTORE_NAME,
                    ApplicationInfoList.getHomeRepository().getKeyStorePassword(),"",CCConstants.EDU_PASSWORD_USERNAMEHASH);
            return new HMac(hmacPassword,HMac.ALG_SHA256);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
