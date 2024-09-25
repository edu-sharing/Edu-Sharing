package org.edu_sharing.repository.client.tools;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.config.model.KeyValuePair;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.json.JSONObject;

import jakarta.servlet.ServletContext;
import java.io.File;

/**
 * Class to load language data from the angular i18n files (json)
 */
public class I18nAngular {
    public static final String GENDER_SEPARATOR = "*";
    public static Logger logger=Logger.getLogger(I18nAngular.class);
    public static String getTranslationAngular(String scope,String key){
        return getTranslationAngular(scope,key,new AuthenticationToolAPI().getCurrentAngularLanguage());
    }

    public static JSONObject getLanguageStrings() throws Exception{
        String language=new AuthenticationToolAPI().getCurrentAngularLanguage();
        ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
        File[] dirs = new File(context.getRealPath("/assets/i18n/")).listFiles(File::isDirectory);
        JSONObject result=new JSONObject();
        for(File dir : dirs) {
            File i18n = new File(dir, language + ".json");
            // fallback to the base language
            if(language.startsWith("de-") && !i18n.exists()) {
                // ignore missing files
                continue;
            }
            String json = FileUtils.readFileToString(i18n,"UTF-8");
            JSONObject jsonObject = new JSONObject(json);
            try {
                for (String key : JSONObject.getNames(jsonObject)) {
                    result.put(key, jsonObject.get(key));
                }
            }catch(NullPointerException e){
                // the override is usually empty, can be ignored
            }
        }
        return result;
    }
    /**
     * Get the translation
     * @param scope equals the folder name in assets/i18n, e.g. workspace, common, search
     * @param key The key. Usually UPPERCASE. You can use "." for getting sub-keys, e.g. "WORKSPACE.TOAST.MY_KEY"
     * @param language
     * @return
     */
    private static String getTranslationAngular(String scope,String key,String language){
        try {
            String override=getTranslationFromOverride(key,language);
            if(override!=null)
                return replaceGenderSeperator(override);
            // Using global instance singe it only is used for file reading
            ServletContext servletContext = Context.getGlobalContext();
            if(servletContext == null) {
                logger.debug("Trying to fetch angular translation before context initalization");
                return key;
            }
            File i18nFile = new File(servletContext.getRealPath("/assets/i18n/"+scope+"/"+language+".json"));
            // fallback to the base language
            if(language.startsWith("de-") && !i18nFile.exists()) {
                return getTranslationAngular(scope, key, "de");
            }
            String json=FileUtils.readFileToString(i18nFile,"UTF-8");
            JSONObject object=new JSONObject(json);
            String[] list=key.split("\\.");
            for(int i=0;i<list.length-1;i++){
                object=object.getJSONObject(list[i]);
            }
            String result = object.getString(list[list.length-1]);
            return replaceGenderSeperator(result);
        } catch (Exception e) {
            if(language.startsWith("de-")) {
                return getTranslationAngular(scope, key, "de");
            }
            logger.info("No translation in Angular found for " + scope + " " + key + " " + language);
            return key;
        }
    }

    private static String replaceGenderSeperator(String i18n) {
        return i18n.replace("{{GENDER_SEPARATOR}}", GENDER_SEPARATOR);
    }

    /**
     * try to find the given override string in the client.config
     * will return null if it is not overriden
     * @param key
     * @param language
     */
    private static String getTranslationFromOverride(String key, String language) {
        try {
            for (KeyValuePair pair:ConfigServiceFactory.getLanguageData()) {
                if(pair.key.equals(key))
                    return pair.value;
            }
        }catch(Exception e){
            logger.debug(e);
        }
        return null;
    }

    public static String getPermissionDescription(String permKey){
        return getTranslationAngular("common", "NOTIFICATION.PERMISSION." + permKey.toLowerCase());
    }
}
