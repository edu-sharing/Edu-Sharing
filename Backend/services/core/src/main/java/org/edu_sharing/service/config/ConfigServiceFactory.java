package org.edu_sharing.service.config;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.config.model.Config;
import org.edu_sharing.alfresco.service.config.model.KeyValuePair;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.edu_sharing.repository.server.RequestHelper;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.List;

public class ConfigServiceFactory {
    private static final String[] DEFAULT_LANGUAGES = new String[]{"de", "en"};
	static Logger logger = Logger.getLogger(ConfigServiceFactory.class);

	public static ConfigService getConfigService(){
		return ApplicationContextFactory.getApplicationContext().getBean(ConfigService.class);
	}
	public static Config getCurrentConfig() throws Exception {
		if(Context.getCurrentInstance()!=null)
			return getCurrentConfig(Context.getCurrentInstance().getRequest());
		return getConfigService().getConfig();
	}
	public static String getCurrentContextId(){
		if(Context.getCurrentInstance()!=null)
			return getCurrentContextId(Context.getCurrentInstance().getRequest());
		return null;
	}
	public static String getCurrentContextId(HttpServletRequest req){
		try {
			org.edu_sharing.alfresco.service.config.model.Context context = getConfigService().getContext(getCurrentDomain(req));
			if(context == null) {
				return null;
			}
			return context.id;
		} catch (Exception e) {
			logger.info(e.getMessage(),e);
			return null;
		}
	}
	public static Config getCurrentConfig(ServletRequest req) throws Exception {
		try {
			return getConfigService().getConfigByDomain(req==null ? getCurrentDomain() : getCurrentDomain(req));
		}catch(Throwable t) {
			return getConfigService().getConfig();
		}
		
	}
	public static String getCurrentDomain() {
		return getCurrentDomain(Context.getCurrentInstance().getRequest());
	}
	public static String getCurrentDomain(ServletRequest req) {
		String domain = new RequestHelper(req).getServerName();
		logger.debug("current domain:" + domain);
		return domain;
	}

	public static List<KeyValuePair> getLanguageData(List<Language> languages,String language) {
		if(languages!=null && languages.size()>0) {
			for(org.edu_sharing.alfresco.service.config.model.Language entry : languages) {
				if(entry.language.equalsIgnoreCase(language))
					return entry.string;
			}
			logger.debug("no language override entries found in config for language "+language);
		}
		return null;
	}
	public static List<KeyValuePair> getLanguageData(List<Language> languages) {
		String language=new AuthenticationToolAPI().getCurrentLanguage();
		return getLanguageData(languages,language);
	}
	public static List<KeyValuePair> getLanguageData(String language) throws Exception {
		return getLanguageData(getCurrentConfig().language,language);
	}
	public static List<KeyValuePair> getLanguageData() throws Exception {
		String language=new AuthenticationToolAPI().getCurrentLanguage();
		return getLanguageData(language);
	}


	/**
	 * get supported languages for the current config or returns @DEFAULT_LANGUAGES
	 * @return
	 */
	public static String[] getSupportedLanguages() {
		try {
			return ConfigServiceFactory.getCurrentConfig().getValue("language", DEFAULT_LANGUAGES);
		}catch(Throwable t){
			return DEFAULT_LANGUAGES;
		}
	}
}
