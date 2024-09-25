package org.edu_sharing.alfresco.service.guest;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigObject;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.io.Serializable;
import java.util.*;

@Slf4j
public class GuestServiceImpl implements GuestService {

    public static final String REPOSITORY_GUEST_CONFIG_PATH = "repository.guest";
    public static final String REPOSITORY_CONTEXT_CONFIG_PATH = "repository.context";

    private final PersonService personService;
    private final MutableAuthenticationService authenticationService;
    private final AuthorityService authorityService;
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final LightbendConfigLoader configLoader;

    public GuestServiceImpl(PersonService personService, MutableAuthenticationService authenticationService, AuthorityService authorityService, RetryingTransactionHelper retryingTransactionHelper, LightbendConfigLoader configLoader) {
        this.personService = personService;
        this.authenticationService = authenticationService;
        this.authorityService = authorityService;
        this.retryingTransactionHelper = retryingTransactionHelper;
        this.configLoader = configLoader;
    }

    @Override
    public void init(){
        createOrUpdateAllGuestUsers();
    }

    @EventListener
    public void onConfigurationChanged(RefreshScopeRefreshedEvent event) {
        if (!event.isCaller()) {
            return;
        }
        createOrUpdateAllGuestUsers();
    }

    @Override
    public GuestConfig getDefaultConfig() {
        Config rootConfig = configLoader.getConfig();
        Config config = rootConfig.getConfig(REPOSITORY_GUEST_CONFIG_PATH);
        return ConfigBeanFactory.create(config, GuestConfig.class);
    }

    @Override
    public GuestConfig getConfig(String context) {
        Config rootConfig = configLoader.getConfig();
        if (StringUtils.isBlank(context)) {
            return getDefaultConfig();
        }

        String contextConfigPath = getContextConfigPath(context);
        if (!rootConfig.hasPath(contextConfigPath)) {
            return getDefaultConfig();
        }

        Config defaultConfig = rootConfig.getConfig(REPOSITORY_GUEST_CONFIG_PATH);
        Config config = rootConfig.getConfig(contextConfigPath).withFallback(defaultConfig);
        return ConfigBeanFactory.create(config, GuestConfig.class);
    }

    @Override
    public List<GuestConfig> getAllGuestConfigs() {
        List<GuestConfig> guestConfigs = new ArrayList<>();
        guestConfigs.add(getDefaultConfig());

        Config rootConfig = configLoader.getConfig();
        Config defaultConfig = rootConfig.getConfig(REPOSITORY_GUEST_CONFIG_PATH);

        if (rootConfig.hasPath(REPOSITORY_CONTEXT_CONFIG_PATH)) {
            ConfigObject contextObject = rootConfig.getObject(REPOSITORY_CONTEXT_CONFIG_PATH);
            Config contextConfig = contextObject.toConfig();
            contextObject.keySet().stream()
                    .map(x -> String.join(".", x.contains(".") ? String.format("\"%s\"", x) : x, REPOSITORY_GUEST_CONFIG_PATH))
                    .map(x -> contextConfig.hasPath(x) ? contextConfig.getConfig(x).withFallback(defaultConfig) : defaultConfig)
                    .map(config -> ConfigBeanFactory.create(config, GuestConfig.class))
                    .forEach(guestConfigs::add);
        }

        return guestConfigs;
    }


    @Override
    public boolean isGuestUser(String authority) {
        return getAllGuestAuthorities().contains(authority);
    }

    private static String getContextConfigPath(String context) {
        return String.join(".", REPOSITORY_CONTEXT_CONFIG_PATH, context, REPOSITORY_GUEST_CONFIG_PATH);
    }

    // TODO cache ?
    @Override
    public Set<String> getAllGuestAuthorities() {
        Set<String> guests = new HashSet<>();
        guests.add("guest"); // alf internal guest user
        guests.add(CCConstants.PROXY_USER); // proxy user used for e.g. lti
        Optional.ofNullable(getDefaultConfig()).map(GuestConfig::getUsername).ifPresent(guests::add);
        getAllGuestConfigs()
                .stream()
                .map(GuestConfig::getUsername)
                .filter(StringUtils::isNotBlank)
                .forEach(guests::add);

        return guests;
    }

    @Override
    public GuestConfig getCurrentGuestConfig() {
        String currentContext = NodeCustomizationPolicies.getEduSharingContext();
        return getConfig(currentContext);
    }

    @Override
    public void createOrUpdateAllGuestUsers() {
        getAllGuestConfigs().forEach(x -> {
            try {
                createOrUpdateGuest(x);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public NodeRef createOrUpdateGuest(GuestConfig guestConfig) {

        if (StringUtils.isBlank(guestConfig.getUsername())) {
            return null;
        }

        return retryingTransactionHelper.doInTransaction(() -> AuthenticationUtil.runAsSystem(() -> {
            NodeRef guestRef = personService.getPersonOrNull(guestConfig.getUsername());
            String password = new KeyTool().getRandomPassword();
//
            if (guestRef == null) {
                Map<QName, Serializable> properties = new HashMap<>(Map.of(
                        ContentModel.PROP_USERNAME, guestConfig.getUsername(),
                        ContentModel.PROP_FIRSTNAME, guestConfig.getUsername(),
                        ContentModel.PROP_LASTNAME, guestConfig.getUsername(),
                        QName.createQName(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION), CCConstants.CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_GUEST));
                guestRef = personService.createPerson(properties);
            }

            if(!authenticationService.getAuthenticationEnabled(guestConfig.getUsername())) {
                authenticationService.createAuthentication(guestConfig.getUsername(), password.toCharArray());
            } else {
                authenticationService.setAuthentication(guestConfig.getUsername(), password.toCharArray());
            }

            Set<String> currentMemberships = new HashSet<>(authorityService.getAuthoritiesForUser(guestConfig.getUsername()));


            Set<String> toRemove = new HashSet<>(currentMemberships);
            toRemove.remove(CCConstants.AUTHORITY_GROUP_EVERYONE);
            guestConfig.getGroups().forEach(toRemove::remove);


            Set<String> toCreate = new HashSet<>(guestConfig.getGroups());
            toCreate.remove(CCConstants.AUTHORITY_GROUP_EVERYONE);
            toCreate.removeAll(currentMemberships);

            toRemove.forEach(x -> authorityService.removeAuthority(x, guestConfig.getUsername()));
            toCreate.forEach(x -> authorityService.addAuthority(x, guestConfig.getUsername()));
            return guestRef;
        }));
    }

    @Override
    public void deleteUnusedGuests() {
        QName affiliationGuestProp = QName.createQName(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION);

        Set<String> allGuestAuthorities = getAllGuestAuthorities();

        String queryExecutionId = null;
        List<String> personsToDelete = new ArrayList<>();
        PagingResults<PersonService.PersonInfo> searchResult;
        do {
            searchResult = personService.getPeople(CCConstants.CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_GUEST, List.of(affiliationGuestProp), null, new PagingRequest(100, queryExecutionId));
            queryExecutionId = searchResult.getQueryExecutionId();

            for (PersonService.PersonInfo person : searchResult.getPage()) {
                if (allGuestAuthorities.contains(person.getUserName())) {
                    continue;
                }
                personsToDelete.add(person.getUserName());
            }
        } while (searchResult.hasMoreItems());

        for(String person : personsToDelete) {
            try {
                retryingTransactionHelper.doInTransaction(() -> AuthenticationUtil.runAsSystem(() -> {
                    personService.deletePerson(person);
                    return null;
                }));
            }catch (Exception e) {
                log.error("Could not delete guest user: {}", e.getMessage(), e);
            }
        }
    }
}
