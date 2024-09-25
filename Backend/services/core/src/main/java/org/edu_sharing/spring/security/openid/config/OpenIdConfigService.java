package org.edu_sharing.spring.security.openid.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigObject;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.guest.GuestConfig;
import org.edu_sharing.spring.security.openid.SecurityConfigurationOpenIdConnect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile(SecurityConfigurationOpenIdConnect.PROFILE_ID)
public class OpenIdConfigService {

    public static final String REPOSITORY_OPENID_CONFIG_PATH = "security.sso.openIdConnect";
    public static final String REPOSITORY_CONTEXT_CONFIG_PATH = "repository.context";


    Config rootConfig = LightbendConfigLoader.get();

    public OpenIdConfig getDefaultConfig() {
        Config config = rootConfig.getConfig("security.sso.openIdConnect");
        return ConfigBeanFactory.create(config, OpenIdConfig.class);
    }

    public OpenIdConfig getConfig(String context) {
        Config rootConfig = LightbendConfigLoader.get();
        if (StringUtils.isBlank(context)) {
            return getDefaultConfig();
        }

        String contextConfigPath = getContextConfigPath(context, REPOSITORY_OPENID_CONFIG_PATH);
        if (!rootConfig.hasPath(contextConfigPath)) {
            return getDefaultConfig();
        }

        Config defaultConfig = rootConfig.getConfig(REPOSITORY_OPENID_CONFIG_PATH);
        Config config = rootConfig.getConfig(contextConfigPath).withFallback(defaultConfig);

        return createConfig(context, config);
    }

    public List<OpenIdConfig> getAllConfigs(){
        List<OpenIdConfig> configs = new ArrayList<>();
        configs.add(getDefaultConfig());

        Config defaultConfig = rootConfig.getConfig(REPOSITORY_OPENID_CONFIG_PATH);

        if (rootConfig.hasPath(REPOSITORY_CONTEXT_CONFIG_PATH)) {
            ConfigObject contextObject = rootConfig.getObject(REPOSITORY_CONTEXT_CONFIG_PATH);
            Config contextConfig = contextObject.toConfig();

            for(String contextId: contextObject.keySet()) {
                String configPath = String.join(".", contextId.contains(".") ? String.format("\"%s\"", contextId) : contextId, REPOSITORY_OPENID_CONFIG_PATH);
                Config openIdConfig = contextConfig.hasPath(configPath)
                        ? contextConfig.getConfig(configPath).withFallback(defaultConfig)
                        : defaultConfig;
                configs.add(createConfig(contextId, openIdConfig));
            }
        }
        return configs;
    }

    private OpenIdConfig createConfig(String contextId, Config config) {
        OpenIdConfig openIdConfig = ConfigBeanFactory.create(config, OpenIdConfig.class);
        openIdConfig.setContextId(contextId);
        return openIdConfig;
    }

    private static String getContextConfigPath(String context, String subPath) {
        return String.join(".", REPOSITORY_CONTEXT_CONFIG_PATH, context, subPath);
    }
}
