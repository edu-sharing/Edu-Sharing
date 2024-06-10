/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.repository.client.tools.CCConstants;


public class I18nServer {
	
	private static Log logger = LogFactory.getLog(I18nServer.class);
	
	public static final String defaultResourceBundle = PropertiesHelper.Config.PATH_CONFIG + PropertiesHelper.Config.PathPrefix.DEFAULTS_METADATASETS + "/i18n/mds";
	
	/**
	 * returns I18n value for locale found in system properties user.language and user.country
	 * if not set en_EN will be the default locale
	 * 
	 * @param key
	 * @return
	 */
	public static String getTranslationDefaultResourcebundle(String key){
		// Context is not available here
		//String locale = (Context.getCurrentInstance() != null) ? Context.getCurrentInstance().getLocale() : "de_DE";
		String language = System.getProperty("user.language");
		String country = System.getProperty("user.country");
		
		if(language == null || language.trim().equals("")){
			language = "en";
		}
		if(country == null || country.trim().equals("")){
			country = language.toUpperCase();
		}
		
		return I18nServer.getTranslationDefaultResourcebundle(key, language+"_"+country);
	}
	public static String getTranslationDefaultResourcebundleNoException(String key){
		try{
			return getTranslationDefaultResourcebundle(key);
		}catch(Throwable t){
			logger.warn("I18nServer missing translation for key "+key+" in bundle "+defaultResourceBundle);
			return key;
		}
	}

	/**
	 * @DEPRECATED
	 */
	public static String getTranslationDefaultResourcebundle(String key, String locale){
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
	private static String getTranslation(String key, String locale, String resourceBoundle){
		String language = null;
		String country = null;
		if(locale != null){
			String[] splitted = locale.split("_");
			if(splitted != null && splitted.length == 2){
				language = splitted[0];
				country = splitted[1];
			}
		}
		return getTranslation(key, language,country,resourceBoundle);
	}
	
	
	public static String getTranslationDefaultResourcebundle(String key,String language,String country){
		return getTranslation(key,language,country,defaultResourceBundle);
	}
	
	public static Map<String,String> permViewMapper = null;
	public final static String getPermissionCaption(String permKey){
		if(permViewMapper == null){
			permViewMapper = new HashMap<>();
			permViewMapper.put(CCConstants.PERMISSION_READ, "dialog_inviteusers_perm_read");
			permViewMapper.put(CCConstants.PERMISSION_READ_PREVIEW, "dialog_inviteusers_perm_readpreview");
			permViewMapper.put(CCConstants.PERMISSION_READ_ALL, "dialog_inviteusers_perm_readall");
			permViewMapper.put(CCConstants.PERMISSION_WRITE, "dialog_inviteusers_perm_write");
			permViewMapper.put(CCConstants.PERMISSION_DELETE, "dialog_inviteusers_perm_delete");
			permViewMapper.put(CCConstants.PERMISSION_DELETE_CHILDREN, "dialog_inviteusers_perm_deletechildren");
			permViewMapper.put(CCConstants.PERMISSION_DELETE_NODE, "dialog_inviteusers_perm_deletenode");
			permViewMapper.put(CCConstants.PERMISSION_ADD_CHILDREN, "dialog_inviteusers_perm_addchildren");
			permViewMapper.put(CCConstants.PERMISSION_CONSUMER, "dialog_inviteusers_perm_consumer");
			permViewMapper.put(CCConstants.PERMISSION_CONSUMER_METADATA, "dialog_inviteusers_perm_consumermetadata");
			permViewMapper.put(CCConstants.PERMISSION_EDITOR, "dialog_inviteusers_perm_editor");
			permViewMapper.put(CCConstants.PERMISSION_CONTRIBUTER, "dialog_inviteusers_perm_contributer");
			permViewMapper.put(CCConstants.PERMISSION_COORDINATOR, "dialog_inviteusers_perm_coordinator");
			permViewMapper.put(CCConstants.PERMISSION_COLLABORATOR, "dialog_inviteusers_perm_collaborator");
			permViewMapper.put(CCConstants.PERMISSION_CC_PUBLISH, "dialog_inviteusers_perm_ccpublish");
			permViewMapper.put(CCConstants.PERMISSION_READPERMISSIONS, "dialog_inviteusers_perm_readpermissions");
			permViewMapper.put(CCConstants.PERMISSION_CHANGEPERMISSIONS, "dialog_inviteusers_perm_changepermissions");
		}
		String caption = permViewMapper.get(permKey);
		caption = (caption == null) ? permKey : caption; 
		return caption;
	}

	private static String getTranslation(String key,String language,String country, String resourceBoundle){
		Locale currentLocale;
        ResourceBundle messages;

        if(language == null || country == null){
        	currentLocale = Locale.ROOT;
        }else{
        	currentLocale = new Locale(language, country);
        }
		try {
			messages = ResourceBundle.getBundle(resourceBoundle, currentLocale);
//			messages = PropertiesHelper.Config.getResourceBundleForFile(resourceBoundle + "_" + currentLocale.toString() + ".properties");
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
		return messages.getString(key);
	}
	
}
