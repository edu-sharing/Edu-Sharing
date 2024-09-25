package org.edu_sharing.alfresco.service;

import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;

public interface EduSharingCustomPermissionService {
    /**
     * if you want to filter which orgs should be used for the fuzzy, "local" search, you can provide a custom Service
     * */
    default List<String> getLocalOrganizations(List<String> allOrganizations) {
        return allOrganizations;
    }

    /*default EsAccessStatus hasPermission(NodeRef nodeRef) {
        return null;
    }*/

    /*
    enum EsAccessStatus {

        // access is granted
        ALLOWED,
        // access is denied
        DENIED,
        // access is enforced to be denied, i.e. triggered by an interceptor. It
        // should not be allowed to grant access afterwards
        DENIED_ENFORCE
    }
     */



}
