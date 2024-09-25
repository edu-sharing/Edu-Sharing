package org.edu_sharing.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.config.model.*;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentToolFactory;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.spring.scope.refresh.ContextRefreshUtils;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.edu_sharing.util.CheckedFunction;
import org.json.JSONObject;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService, ApplicationListener<RefreshScopeRefreshedEvent> {
    private static String CACHE_KEY = "CLIENT_CONFIG";
    // we use a non-serializable Config as value because this is a local cache and not distributed
    private static SimpleCache<String, Config> configCache = AlfAppContextGate.getApplicationContext().getBean("eduSharingConfigCache", SimpleCache.class);
    private static SimpleCache<String, Context> contextCache = AlfAppContextGate.getApplicationContext().getBean("eduSharingContextCache", SimpleCache.class);

    private static final Unmarshaller jaxbUnmarshaller;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final NodeService nodeService;
    private final PermissionService permissionService;
    private final UserEnvironmentTool userEnvironmentTool;
    private final RetryingTransactionHelper retryingTransactionHelper;

    static {
        Unmarshaller jaxbUnmarshaller1;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
            jaxbUnmarshaller1 = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            jaxbUnmarshaller1 = null;
            log.error(e.getMessage(), e);
        }
        jaxbUnmarshaller = jaxbUnmarshaller1;
    }

    public ConfigServiceImpl(NodeService nodeService, PermissionService permissionService, UserEnvironmentToolFactory userEnvironmentToolFactory, RetryingTransactionHelper retryingTransactionHelper) {
        this.nodeService = nodeService;
        this.permissionService = permissionService;
        this.userEnvironmentTool = userEnvironmentToolFactory.createUserEnvironmentTool();
        this.retryingTransactionHelper = retryingTransactionHelper;
    }

    @Override
    public Config getConfig() throws Exception {
        if (!"true".equalsIgnoreCase(ApplicationInfoList.getHomeRepository().getDevmode()) && configCache.getKeys().contains(CACHE_KEY)) {
            return configCache.get(CACHE_KEY);
        }
        InputStream is = getConfigInputStream();
        Config config;
        synchronized (jaxbUnmarshaller) {
            config = (Config) jaxbUnmarshaller.unmarshal(is);
        }
        is.close();
        configCache.put(CACHE_KEY, config);
        return config;
    }

    private InputStream getConfigInputStream() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String file = LightbendConfigLoader.getConfigFileLocation(PropertiesHelper.Config.CONFIG_FILENAME, PropertiesHelper.Config.PathPrefix.DEFAULTS);
        InputStream is = classLoader.getResourceAsStream(file);
        if (is == null)
            throw new IOException(file + " missing");
        return is;
    }

    @Override
    public void deleteContext(String id) throws Exception {
        String eduSharingSystemFolderContext = userEnvironmentTool.getEdu_SharingContextFolder();
        nodeService.removeNode(id, eduSharingSystemFolderContext, false);
        ContextRefreshUtils.refreshContext();
    }

    @Override
    public Context getContext(String domain) throws Exception {
        if(StringUtils.isBlank(domain)) {
            return null;
        }
        buildContextCache();
        return contextCache.get(domain);
    }

    @Override
    public List<Context> getAvailableContext() throws Exception {
        buildContextCache();
        return contextCache.getKeys()
                .stream()
                .map(contextCache::get)
                .filter(Objects::nonNull) //maybe gets null while we are iterating over
                .collect(Collectors.toList());
    }

    private void buildContextCache() throws Exception {
        if (contextCache.getKeys().isEmpty()) {
            Config config = getConfig();
            if (config.contexts != null && config.contexts.context != null) {
                for (Context context : config.contexts.context) {
                    if (context.domain == null) {
                        continue;
                    }

                    for (String dom : context.domain) {
                        contextCache.put(dom, context);
                    }
                }
            }

            AuthenticationUtil.runAsSystem(() -> {
                String eduSharingSystemFolderContext = userEnvironmentTool.getEdu_SharingContextFolder();
                Map<String, Map<String, Object>> dynamicContextObjects = nodeService.getChildrenPropsByType(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduSharingSystemFolderContext, CCConstants.CCM_TYPE_CONTEXT);
                dynamicContextObjects
                        .values()
                        .stream()
                        .map(x -> x.get(CCConstants.CCM_PROP_CONTEXT_CONFIG).toString())
                        .map(CheckedFunction.wrap(x -> objectMapper.readValue(x, Context.class), null))
                        .filter(Objects::nonNull)
                        .forEach(x -> Arrays.stream(x.domain).forEach(y -> contextCache.put(y, x)));

                return null;
            });
        }
    }

    @Override
    public Context createOrUpdateContext(Context context) {
        return AuthenticationUtil.runAsSystem(() -> {
            try {
                if (context.id == null || !nodeService.exists(context.id)) {
                    String eduSharingSystemFolderContext = userEnvironmentTool.getEdu_SharingContextFolder();
                    context.id = nodeService.createNode(eduSharingSystemFolderContext, CCConstants.CCM_TYPE_CONTEXT, Map.of());
                }

                nodeService.setProperty(context.id, CCConstants.CCM_PROP_CONTEXT_CONFIG, objectMapper.writeValueAsString(context), true);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
            ContextRefreshUtils.refreshContext();
            return context;
        });
    }

    @Override
    public Config getConfigByDomain(String domain) throws Exception {
        Context context = getContext(domain);
        if (context == null) {
            throw new IllegalArgumentException("Context with domain " + domain + " does not exists");
        }
        return getConfigByContext(context);

    }

    @Override
    public Config getConfigByContext(Context context) throws Exception {
        if (!"true".equalsIgnoreCase(ApplicationInfoList.getHomeRepository().getDevmode()) && configCache.getKeys().contains(CACHE_KEY + "_" + context)) {
            return configCache.get(CACHE_KEY + "_" + context);
        }
        Config config = getConfig().deepCopy();
        overrideValues(config.values, context.values);
        if (context.language != null)
            config.language = overrideLanguage(config.language, context.language);
        if (context.variables != null)
            config.variables = overrideVariables(config.variables, context.variables);
        configCache.put(CACHE_KEY + "_" + context, config);
        return config;
    }

    @Override
    public DynamicConfig setDynamicValue(String key, boolean readPublic, JSONObject object) throws Throwable {
        if (!AuthorityServiceFactory.getLocalService().isGlobalAdmin()) {
            throw new NotAnAdminException();
        }
        String folder = userEnvironmentTool.getEdu_SharingConfigFolder();
        String nodeId;
        try {
            NodeRef child = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, folder, CCConstants.CCM_TYPE_CONFIGOBJECT, CCConstants.CM_NAME, key);
            nodeId = child.getId();
        } catch (Throwable t) {
            // does not exists -> we will create a new node
            Map<String, String[]> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, new String[]{key});
            nodeId = nodeService.createNode(folder, CCConstants.CCM_TYPE_CONFIGOBJECT, props);
        }

        Map<String, Object> props = new HashMap<>();
        props.put(CCConstants.CCM_PROP_CONFIGOBJECT_VALUE, object.toString());
        nodeService.updateNodeNative(nodeId, props);
        DynamicConfig result = new DynamicConfig();
        result.setNodeId(nodeId);
        result.setValue(object.toString());
        List<ACE> aces = new ArrayList<>();
        if (readPublic) {
            ACE ace = new ACE();
            ace.setAuthority(CCConstants.AUTHORITY_GROUP_EVERYONE);
            ace.setAuthorityType(Authority.Type.EVERYONE.name());
            ace.setPermission(CCConstants.PERMISSION_CONSUMER);
            aces.add(ace);
        }
        permissionService.setPermissions(nodeId, aces, false);
        return result;
    }

    @Override
    public DynamicConfig getDynamicValue(String key) throws Throwable {
        String folder = AuthenticationUtil.runAsSystem(() -> {
            try {
                return userEnvironmentTool.getEdu_SharingConfigFolder();
            } catch (Throwable throwable) {
                return null;
            }
        });
        NodeRef child = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, folder, CCConstants.CCM_TYPE_CONFIGOBJECT, CCConstants.CM_NAME, key);
        if (child == null)
            throw new IllegalArgumentException(key);
        String value = NodeServiceHelper.getProperty(child, CCConstants.CCM_PROP_CONFIGOBJECT_VALUE);
        DynamicConfig result = new DynamicConfig();
        result.setNodeId(child.getId());
        result.setValue(value);
        return result;
    }

    private Variables overrideVariables(Variables values, Variables override) {
        if (values == null)
            return override;
        if (override == null)
            return values;
        overrideList(values.variable, override.variable);
        return values;
    }

    private void overrideList(List<KeyValuePair> list, List<KeyValuePair> override) {
        for (KeyValuePair obj : override) {
            list.remove(obj);
            list.add(obj);
        }
    }

    private List<Language> overrideLanguage(List<Language> values, List<Language> override) {
        if (values == null)
            return override;
        if (override == null)
            return values;
        for (Language language : override) {
            for (Language language2 : values) {
                if (language.language.equals(language2.language)) {
                    overrideList(language2.string, language.string);
                }
            }
        }
        return values;
    }

    private void overrideValues(Values values, Values override) throws
            IllegalArgumentException, IllegalAccessException {
        if (override == null) {
            return;
        }

        Class<?> c = override.getClass();
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            if (field.get(override) != null)
                field.set(values, field.get(override));
        }

    }

    private void refresh() {
        configCache.clear();
        contextCache.clear();
        try {
            getConfig();
        } catch (Exception e) {
            log.error("error refreshing client config: " + e.getMessage(), e);
        }
    }

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        refresh();
    }
}
