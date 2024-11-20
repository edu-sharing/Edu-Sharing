/**
 *
 */
package org.edu_sharing.repository.server.tools;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.repository.client.tools.CCConstants;

@Slf4j
public class I18nServer {

    public static final String NONE = "none";

    public static final String defaultResourceBundle = PropertiesHelper.Config.PATH_CONFIG + PropertiesHelper.Config.PathPrefix.DEFAULTS_METADATASETS + "/i18n/mds";
	private static final Map<String, String> permViewMapper = new HashMap<>() {{
        put(CCConstants.PERMISSION_READ, "dialog_inviteusers_perm_read");
        put(CCConstants.PERMISSION_READ_PREVIEW, "dialog_inviteusers_perm_readpreview");
        put(CCConstants.PERMISSION_READ_ALL, "dialog_inviteusers_perm_readall");
        put(CCConstants.PERMISSION_WRITE, "dialog_inviteusers_perm_write");
        put(CCConstants.PERMISSION_DELETE, "dialog_inviteusers_perm_delete");
        put(CCConstants.PERMISSION_DELETE_CHILDREN, "dialog_inviteusers_perm_deletechildren");
        put(CCConstants.PERMISSION_DELETE_NODE, "dialog_inviteusers_perm_deletenode");
        put(CCConstants.PERMISSION_ADD_CHILDREN, "dialog_inviteusers_perm_addchildren");
        put(CCConstants.PERMISSION_CONSUMER, "dialog_inviteusers_perm_consumer");
        put(CCConstants.PERMISSION_CONSUMER_METADATA, "dialog_inviteusers_perm_consumermetadata");
        put(CCConstants.PERMISSION_EDITOR, "dialog_inviteusers_perm_editor");
        put(CCConstants.PERMISSION_CONTRIBUTER, "dialog_inviteusers_perm_contributer");
        put(CCConstants.PERMISSION_COORDINATOR, "dialog_inviteusers_perm_coordinator");
        put(CCConstants.PERMISSION_COLLABORATOR, "dialog_inviteusers_perm_collaborator");
        put(CCConstants.PERMISSION_CC_PUBLISH, "dialog_inviteusers_perm_ccpublish");
        put(CCConstants.PERMISSION_READPERMISSIONS, "dialog_inviteusers_perm_readpermissions");
        put(CCConstants.PERMISSION_CHANGEPERMISSIONS, "dialog_inviteusers_perm_changepermissions");
    }};



    /**
     * returns I18n value for locale found in system properties user.language and user.country
     * if not set en_EN will be the default locale
     *
     * @param key
     * @return
     */
    public static String getTranslationDefaultResourcebundle(String key) {
        // Context is not available here
        //String locale = (Context.getCurrentInstance() != null) ? Context.getCurrentInstance().getLocale() : "de_DE";
        String language = System.getProperty("user.language");
        String country = System.getProperty("user.country");

        if (StringUtils.isBlank(language)) {
            language = "en";
        }
        if (StringUtils.isBlank(country)) {
            country = language.toUpperCase();
        }

        return I18nServer.getTranslationDefaultResourcebundle(key, language + "_" + country);
    }

    public static String getTranslationDefaultResourcebundleNoException(String key) {
        try {
            return getTranslationDefaultResourcebundle(key);
        } catch (Throwable t) {
            log.warn("I18nServer missing translation for key " + key + " in bundle " + defaultResourceBundle);
            return key;
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static String getTranslationDefaultResourcebundle(String key, String locale) {
        return MetadataReader.getTranslation("mds", key, null, locale);
        // return getTranslation(key,locale,defaultResourceBundle);
    }

    /**
     * if locale == null Locale.ROOT will be taken
     * @param key
     * @param locale must have both country and language i.e.: de_DE
     * @param resourceBoundle
     * @return
     */
    private static String getTranslation(String key, String locale, String resourceBoundle) {
        if(NONE.equals(locale)){
            return key;
        }

        String language = null;
        String country = null;
        if (locale != null) {
            String[] splitted = locale.split("_");
            if (splitted.length == 2) {
                language = splitted[0];
                country = splitted[1];
            }
        }
        return getTranslation(key, language, country, resourceBoundle);
    }


    public static String getTranslationDefaultResourcebundle(String key, String language, String country) {
        return getTranslation(key, language, country, defaultResourceBundle);
    }


    public static String getPermissionCaption(String permKey) {
        String caption = permViewMapper.get(permKey);
        caption = (caption == null) ? permKey : caption;
        return caption;
    }

    private static String getTranslation(String key, String language, String country, String resourceBoundle) {
        Locale currentLocale;
        ResourceBundle messages;

        if (language == null || country == null) {
            currentLocale = Locale.ROOT;
        } else {
            currentLocale = new Locale(language, country);
        }
        try {
            messages = ResourceBundle.getBundle(resourceBoundle, currentLocale);
//			messages = PropertiesHelper.Config.getResourceBundleForFile(resourceBoundle + "_" + currentLocale.toString() + ".properties");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return messages.getString(key);
    }

}
