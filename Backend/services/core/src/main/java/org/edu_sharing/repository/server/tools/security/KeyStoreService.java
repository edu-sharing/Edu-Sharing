package org.edu_sharing.repository.server.tools.security;

import org.alfresco.repo.content.ContentStore;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Random;


public class KeyStoreService {

    public static String SYMETRIC_KEYSTORE_TYPE = "JCEKS";
    public static String ASYMETRIC_KEYSTORE_TYPE = "JKS";


    Logger logger = Logger.getLogger(KeyStoreService.class);

    File keyStoreRoot = null;

    String keyStoreType = SYMETRIC_KEYSTORE_TYPE;

    Random random = new SecureRandom();

    public KeyStoreService(){
        this.keyStoreRoot = getRootLocation();
    }

    public KeyStoreService(String keyStoreType){
        this.keyStoreRoot = getRootLocation();
        this.keyStoreType = keyStoreType;
    }

    public KeyStoreService(File keyStoreRoot){
        this.keyStoreRoot = keyStoreRoot;
    }


    private File getRootLocation() {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ContentStore store = (ContentStore) applicationContext.getBean("fileContentStore");
        return new File(store.getRootLocation()).getParentFile();
    }

    private File getKeyStoreLocation(String name){

        String keyStoreFileExtension = (this.keyStoreType.equals(SYMETRIC_KEYSTORE_TYPE)) ? ".jceks" : ".jks";
        String fileName = name + keyStoreFileExtension;
        String filePath =  this.keyStoreRoot.getAbsolutePath() +"/"+ fileName;
        File f = new File(filePath);
        return f;
    }

    /**
     *
     * @param name filename prefix for .jks file
     * @param keyStorePassword
     * @return
     */
    public KeyStore getKeyStore(String name, String keyStorePassword) {

        KeyStore ks;
        try {
            //JCEKS supports symetric keys, JKS supports asymetric keys
            ks = KeyStore.getInstance("JCEKS");
            File f = getKeyStoreLocation(name);
            if (!f.exists()) {
                logger.info("keystore file does not exist." + f.getAbsolutePath() +" creating...");
                //init keystore
                ks.load(null,keyStorePassword.toCharArray());
                storeKeyStore(ks,keyStorePassword,f);
            }
            ks.load(new FileInputStream(f), keyStorePassword.toCharArray());
            return ks;
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void storeKeyStore(KeyStore ks, String keyStorePassword, File f) throws Exception{
        try (FileOutputStream fos = new FileOutputStream(f)) {
            ks.store(fos, keyStorePassword.toCharArray());
        } catch (FileNotFoundException e) {
            logger.error("could not write to keystore file");
            throw e;
        }
    }


    /**
     * https://gist.github.com/d-smith/c6a9d84e33466a530ca8
     *
     * @param keyStoreName
     * @param keyStorePassword
     * @param passwordPassword
     * @param alias
     * @param password
     * @throws Exception
     */
    public void writePasswordToKeyStore(String keyStoreName, String keyStorePassword, String passwordPassword, String alias, String password)
            throws Exception {

        KeyStore keyStore = getKeyStore(keyStoreName, keyStorePassword);
        KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(passwordPassword.toCharArray());

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
        SecretKey generatedSecret =
                factory.generateSecret(new PBEKeySpec(
                        password.toCharArray(),
                        getNextSalt(),
                        13
                ));

        keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(
                generatedSecret), keyStorePP);

        storeKeyStore(keyStore,keyStorePassword,getKeyStoreLocation(keyStoreName));
    }

    public byte[] getNextSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * https://gist.github.com/d-smith/c6a9d84e33466a530ca8
     *
     * @param keyStoreName
     * @param keyStorePassword
     * @param passwordPassword
     * @param passwordAlias
     * @return
     * @throws Exception
     */
    public String readPasswordFromKeyStore(String keyStoreName,String keyStorePassword, String passwordPassword, String passwordAlias) throws Exception {
        KeyStore keyStore = getKeyStore(keyStoreName, keyStorePassword);

        KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(passwordPassword.toCharArray());

        KeyStore.SecretKeyEntry ske =
                (KeyStore.SecretKeyEntry)keyStore.getEntry(passwordAlias, keyStorePP);

        if(ske == null){
            logger.warn("alias not found in keystore:" + passwordAlias);
            return null;
        }

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
        PBEKeySpec keySpec = (PBEKeySpec)factory.getKeySpec(
                ske.getSecretKey(),
                PBEKeySpec.class);

        return new String(keySpec.getPassword());
    }
}
