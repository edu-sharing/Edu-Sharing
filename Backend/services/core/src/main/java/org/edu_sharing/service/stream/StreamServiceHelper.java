package org.edu_sharing.service.stream;


import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;

import java.util.ArrayList;
import java.util.List;

public class StreamServiceHelper {
    public static List<String> getCurrentAuthorities() {
        AuthorityService authorityService=AuthorityServiceFactory.getLocalService();
        ArrayList<String> authorities = new ArrayList<>();
        authorities.add(AuthenticationUtil.getFullyAuthenticatedUser());
        AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                authorities.addAll(authorityService.getMemberships(AuthenticationUtil.getFullyAuthenticatedUser()));
                return null;
            }
        });
        if(!authorities.contains(CCConstants.AUTHORITY_GROUP_EVERYONE))
            authorities.add(CCConstants.AUTHORITY_GROUP_EVERYONE);
        return authorities;
    }
    public static boolean canCurrentAuthorityAccessNode(StreamService service,String nodeId) throws Exception {
        return service.canAccessNode(getCurrentAuthorities(), nodeId);
    }
}
