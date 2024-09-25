package org.edu_sharing.alfresco.service.guest;

import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;
import java.util.Set;

public interface GuestService {
    void init();

    GuestConfig getDefaultConfig();

    GuestConfig getConfig(String context);

    List<GuestConfig> getAllGuestConfigs();

    boolean isGuestUser(String authority);

    // TODO cache ?
    Set<String> getAllGuestAuthorities();

    GuestConfig getCurrentGuestConfig();

    void createOrUpdateAllGuestUsers();

    NodeRef createOrUpdateGuest(GuestConfig guestConfig);

    void deleteUnusedGuests();
}
