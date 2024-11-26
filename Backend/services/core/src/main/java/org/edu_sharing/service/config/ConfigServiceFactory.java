package org.edu_sharing.service.config;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.config.model.Config;
import org.edu_sharing.alfresco.service.config.model.KeyValuePair;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.RequestHelper;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ConfigServiceFactory {
	private static final String[] DEFAULT_LANGUAGES = new String[]{"de", "en"};
	public static final String ENFORCED_CONTEXT = "ENFORCED_CONTEXT";
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
			org.edu_sharing.alfresco.service.config.model.Context context = getConfigService().getContextByDomain(getCurrentDomain(req));
			if(context == null) {
				return null;
			}
			return context.id;
		} catch (Exception e) {
			logger.info(e.getMessage(),e);
			return null;
		}
	}
	public static Config getCurrentConfig(HttpServletRequest req) throws Exception {
		try {
			return getConfigService().getConfigByDomain(req==null ? getCurrentDomain() : getCurrentDomain(req));
		}catch(Throwable t) {
			return getConfigService().getConfig();
		}
		
	}
	public static String getCurrentDomain() {
		return getCurrentDomain(Context.getCurrentInstance().getRequest());
	}

	public static String getCurrentDomain(HttpServletRequest req) {
		Object enforcedContext = req.getSession().getAttribute(ENFORCED_CONTEXT);
		if(StringUtils.isNotBlank((String)enforcedContext)){
			return enforcedContext.toString();
		}

		String domain = new RequestHelper(req).getServerName();
		logger.debug("current domain:" + domain);
		return domain;
	}

	public static void enforceContext(String contextId){
        try {
			org.edu_sharing.alfresco.service.config.model.Context context = getConfigService().getContextById(contextId);
			if(context == null){
				throw new IllegalArgumentException(String.format("Context with contextId %s does not exists",contextId));
			}

			if(context.domain == null || context.domain.length == 0){
				throw new IllegalArgumentException(String.format("Context %s doesn't has domains",contextId));
			}

			Optional<String> domain = Arrays.stream(context.domain).findFirst();
			Context.getCurrentInstance().getRequest().getSession().setAttribute(ENFORCED_CONTEXT, domain.get());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
            throw new RuntimeException(e);
        }
	}

	public static void clearEnforcedContext(){
		Context.getCurrentInstance().getRequest().getSession().removeAttribute(ENFORCED_CONTEXT);
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
