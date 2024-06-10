package org.edu_sharing.repository.update;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@UpdateService
public class Release_1_6_SystemFolderNameRename {


    private final MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
    private final AuthorityService authorityService;

    int counter = 0;

    @Autowired
    public Release_1_6_SystemFolderNameRename(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @UpdateRoutine(
            id = "Release_1_6_SystemFolderNameRename",
            description = "UPDATE AUF 1.6. Renames \"Meine Favoriten\", \"Meine Gruppen\", \"Meine Dateien\" to an i18n value corresponding to the locale set in user.language and user.country. if not set en_EN will be used. Attention SystemFolderNameToDisplayName update must be done before!",
            order = 1600
    )
    public void execute(boolean test) {
        log.info("test:" + test);
        try {

            Set<String> users = authorityService.getAllAuthorities(AuthorityType.USER);
            for (String user : users) {
                log.info("processing user:" + user);
                String homefolderId = mcAlfrescoBaseClient.getHomeFolderID(user);
                Map<String, Map<String, Object>> children = mcAlfrescoBaseClient.getChildren(homefolderId);
                for (Map.Entry<String, Map<String, Object>> entry : children.entrySet()) {
                    String mapType = (String) entry.getValue().get(CCConstants.CCM_PROP_MAP_TYPE);
                    String folderId = (String) entry.getValue().get(CCConstants.SYS_PROP_NODE_UID);
                    if (mapType != null) {
                        if (mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS)) {
                            updateFolder(folderId, children, CCConstants.I18n_USERFOLDER_DOCUMENTS, test);
                        }

                        if (mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE)) {
                            updateFolder(folderId, children, CCConstants.I18n_USERFOLDER_FAVORITES, test);
                        }

                        if (mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP)) {
                            updateFolder(folderId, children, CCConstants.I18n_USERFOLDER_GROUPS, test);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    private void updateFolder(String folderId, Map<String, Map<String, Object>>  suroundingNodes, String i8nKey, boolean test) throws Exception {

        HashMap newProps = new HashMap();

        //get an alfresco installation locale corresponding name
        String displayName = I18nServer.getTranslationDefaultResourcebundle(i8nKey);

        String uniqueValue = new DuplicateFinder().getUniqueValue(suroundingNodes, folderId, CCConstants.CM_NAME, displayName);
        newProps.put(CCConstants.CM_NAME, uniqueValue);

        HashMap i18nTitle = new HashMap();
        i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(i8nKey, "de_DE"));
        i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(i8nKey, "en_EN"));
        i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(i8nKey, "en_US"));
        newProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);


        log.info("folderId:" + folderId + " setting uniqueValue for name:" + uniqueValue);
        if (!test) {
            mcAlfrescoBaseClient.updateNode(folderId, newProps);
        }
        counter++;
    }

}
