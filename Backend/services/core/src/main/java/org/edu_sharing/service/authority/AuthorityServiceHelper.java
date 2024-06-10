package org.edu_sharing.service.authority;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AuthorityServiceHelper {

    /**
     * take the current runAs alfresco user and check if it is an admin normally
     */
    public static boolean isAdmin() {
        return isAdmin(null);
    }

    /**
     * when username is null serviceRegistry.getAuthorityService().getAuthorities() is used.
     * This can be called by NON admin user while calling serviceRegistry.getAuthorityService().getAuthoritiesForUser(username)
     * you need to be an admin.
     * @see public-services-security-context.xml:
     * getAuthorities=ACL_ALLOW
     * getAuthoritiesForUser=ACL_METHOD.ROLE_ADMINISTRATOR
     *
     * @param username
     * @return
     */
    public static boolean isAdmin(String username) {
        try {
            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            Set<String> testUsetAuthorities = (username == null)
                    ? serviceRegistry.getAuthorityService().getAuthorities()
                    : serviceRegistry.getAuthorityService().getAuthoritiesForUser(username);
            return testUsetAuthorities.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
        } catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {

        }
        return false;
    }

    public static Map<String, Double> getDefaultAuthoritySearchFields() {
        Map<String, Double> fields = new HashMap<>();
        if(isAdmin()) {
            fields.put("userName", 1.);
        }
        fields.put("email", 2.);
        fields.put("firstName", 5.);
        fields.put("lastName", 5.);
        return fields;
    }

    public static NodeRef getAuthorityNodeRef(String user) {
        return AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(user);
    }
}
