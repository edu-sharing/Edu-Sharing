package org.edu_sharing.alfresco.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Slf4j
public class LightbendConfigLoader {

    private static LightbendConfigLoader instance = null;

    // we use a non-serializable Config as value because this is a local cache and not distributed
    public static String BASE_FILE = "edu-sharing.reference.conf";
    public static String DEPLOYMENT_FILE = "edu-sharing.deployment.conf";
    public static String OVERRIDE_FILE = "edu-sharing.override.conf";
    public static String CUSTOM_FILE = "edu-sharing.application.conf";


    /**
     * parsing of lightbend config is expensive
     * use @org.edu_sharing.alfresco.lightbend.LightbendConfigCache
     * @return Config
     * @deprecated please use the bean instance instead
     */
    @Deprecated(since = "9.1", forRemoval = true)
    public static Config get() {
        return instance == null ? null : instance.getConfig();
    }


    private final SimpleCache<String, Config> configCache;// = (SimpleCache) AlfAppContextGate.getApplicationContext().getBean("eduSharingLightBendConfigCache");

    public LightbendConfigLoader(SimpleCache<String, Config> configCache) {
        this.configCache = configCache;
        instance = this;
    }


    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onConfigurationChanged(RefreshScopeRefreshedEvent event){
        configCache.clear();
    }


    /**
     * parsing of lightbend config is expensive
     * use @org.edu_sharing.alfresco.lightbend.LightbendConfigCache
     * @return Config
     */
    public Config getConfig() {
        if(configCache.get("config") != null) {
            //return ConfigFactory.parseString(configCache.get("config"), ConfigParseOptions.defaults());
            return configCache.get("config");
        }
        String base = getConfigFileLocation(BASE_FILE, PropertiesHelper.Config.PathPrefix.DEFAULTS);
        String custom = getConfigFileLocation(CUSTOM_FILE, PropertiesHelper.Config.PathPrefix.DEFAULTS);
        String overrideDefaults = getConfigFileLocation(OVERRIDE_FILE, PropertiesHelper.Config.PathPrefix.DEFAULTS);
        String deploymentCluster = getConfigFileLocation(DEPLOYMENT_FILE, PropertiesHelper.Config.PathPrefix.CLUSTER);
        String overrideCluster = getConfigFileLocation(OVERRIDE_FILE, PropertiesHelper.Config.PathPrefix.CLUSTER);
        String deploymentNode = getConfigFileLocation(DEPLOYMENT_FILE, PropertiesHelper.Config.PathPrefix.NODE);
        String overrideNode = getConfigFileLocation(OVERRIDE_FILE, PropertiesHelper.Config.PathPrefix.NODE);
        Config config = ConfigFactory.parseResourcesAnySyntax(overrideNode)
                .withFallback(ConfigFactory.parseResourcesAnySyntax(deploymentNode))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(overrideCluster))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(deploymentCluster))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(overrideDefaults))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(custom))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(base))
                .resolve();
        configCache.put("config", config);
        return config;
    }

    public static String getConfigFileLocation(String configFileName, PropertiesHelper.Config.PathPrefix pathPrefix) {
        return PropertiesHelper.Config.PATH_CONFIG + pathPrefix + "/" + configFileName;
    }
}
