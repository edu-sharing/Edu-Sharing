package org.edu_sharing.repository.server.tools;

import com.typesafe.config.Config;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceImpl;

import java.io.Serializable;
import java.util.*;

public class InitHelper {
    static Logger logger = Logger.getLogger(InitHelper.class);

    public static void initGroups() throws Exception {
        List<? extends Config> createGroups = LightbendConfigLoader.get().getConfigList("repository.groups.create");
        if (createGroups != null && !createGroups.isEmpty()) {
            AuthorityService authorityService = AuthorityServiceFactory.getLocalService();
            for (Config group : createGroups) {
                String id = group.getString("id");
                if (!id.startsWith(PermissionService.GROUP_PREFIX)) {
                    id = PermissionService.GROUP_PREFIX + id;
                }
                if (authorityService.getAuthorityNodeRef(id) == null) {
                    logger.info("Init group " + id);
                    authorityService.createGroup(id, group.getString("displayName"), null);
                }
            }
        }
    }

    private static final List<QName> userProps = List.of(
            ContentModel.PROP_USERNAME,
            ContentModel.PROP_HOMEFOLDER,
            ContentModel.PROP_FIRSTNAME,
            ContentModel.PROP_LASTNAME,
            ContentModel.PROP_EMAIL,
            ContentModel.PROP_ORGID,
            ContentModel.PROP_HOME_FOLDER_PROVIDER,
            ContentModel.PROP_DEFAULT_HOME_FOLDER_PATH,
            ContentModel.PROP_PRESENCEPROVIDER,
            ContentModel.PROP_PRESENCEUSERNAME,
            ContentModel.PROP_ORGANIZATION,
            ContentModel.PROP_JOBTITLE,
            ContentModel.PROP_LOCATION,
            ContentModel.PROP_PERSONDESC,
            ContentModel.PROP_TELEPHONE,
            ContentModel.PROP_MOBILE,
            ContentModel.PROP_COMPANYADDRESS1,
            ContentModel.PROP_COMPANYADDRESS2,
            ContentModel.PROP_COMPANYADDRESS3,
            ContentModel.PROP_COMPANYPOSTCODE,
            ContentModel.PROP_COMPANYTELEPHONE,
            ContentModel.PROP_COMPANYFAX,
            ContentModel.PROP_COMPANYEMAIL,
            ContentModel.PROP_SKYPE,
            ContentModel.PROP_GOOGLEUSERNAME,
            ContentModel.PROP_INSTANTMSG,
            ContentModel.PROP_USER_STATUS,
            ContentModel.PROP_USER_STATUS_TIME
    );

    public static void initPersons() {
        ServiceRegistry serviceRegistry = AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.class);
        RetryingTransactionHelper retryingTransactionHelper = serviceRegistry.getRetryingTransactionHelper();
        PersonService personService = serviceRegistry.getPersonService();
        MutableAuthenticationService authenticationService = serviceRegistry.getAuthenticationService();
        org.alfresco.service.cmr.security.AuthorityService authorityService = serviceRegistry.getAuthorityService();

        List<? extends Config> createPersons = LightbendConfigLoader.get().getConfigList("repository.persons");
        if (createPersons != null && !createPersons.isEmpty()) {
            for (Config person : createPersons) {
                retryingTransactionHelper.doInTransaction(() -> {
                    String authorityName = person.getString("authorityName");
                    char[] password = person.getString("password").toCharArray();
                    Config profile = person.getConfig("profile");
                    NodeRef guestRef = personService.getPersonOrNull(authorityName);
                    if (guestRef != null) {
                        return null;
                    }

                    Map<QName, Serializable> properties = new HashMap<>(Map.of(
                            ContentModel.PROP_USERNAME, authorityName,
                            QName.createQName(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION), person.getString("primaryAffiliation"))
                    );

                    for (QName property : userProps) {
                        if (profile.hasPath(property.getLocalName())) {
                            String value = profile.getString(property.getLocalName());
                            properties.put(property, value);
                        }
                    }

                    authenticationService.createAuthentication(authorityName, password);
                    personService.createPerson(properties);
                    Set<String> currentMemberships = new HashSet<>(authorityService.getAuthoritiesForUser(authorityName));

                    Set<String> toCreate = new HashSet<>(person.getStringList("groups"));
                    toCreate.remove(CCConstants.AUTHORITY_GROUP_EVERYONE);
                    toCreate.removeAll(currentMemberships);

                    toCreate.forEach(x -> authorityService.addAuthority(x, authorityName));
                    return null;
                });
            }
        }
    }

    public static void initProxyUser() {
        //init proxyuser
        ((AuthorityServiceImpl) AuthorityServiceFactory.getLocalService()).createProxyUser();
    }
}
