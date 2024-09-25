/**
 *
 */
package org.edu_sharing.repository.server.tools;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserEnvironmentTool {

    private static Log logger = LogFactory.getLog(UserEnvironmentTool.class);
    MCAlfrescoAPIClient mcBaseClient = new MCAlfrescoAPIClient();
    String username = null;

    private final NodeService nodeService;

    public UserEnvironmentTool() throws Throwable {
        this(AuthenticationUtil.getFullyAuthenticatedUser());
    }

    public void createAllSystemFolders() {
        AuthenticationUtil.runAsSystem(() -> {
            getEdu_SharingSystemFolderBase();
            getEdu_SharingSystemFolderUpdate();
            getEdu_SharingConfigFolder();
            getEdu_SharingContextFolder();
            getEdu_SharingMediacenterFolder();
            getEdu_SharingReportsFolder();
            getEdu_SharingNotifyFolder();
            getEdu_SharingServiceFolder();
            getEdu_SharingTemplateFolder();
            getEdu_SharingValuespaceFolder();
            return null;
        });
    }

    /**
     * use this for running this class in an runAs context
     *
     * @throws Throwable
     */
    public UserEnvironmentTool(String runAsUser) {
        this(NodeServiceFactory.getLocalService(), runAsUser);
    }

    public UserEnvironmentTool(NodeService nodeService) {
        this(nodeService, AuthenticationUtil.getFullyAuthenticatedUser());
    }

    public UserEnvironmentTool(NodeService nodeService, String runAsUser) {
        username = runAsUser;
        this.nodeService = nodeService;
    }

    public UserEnvironmentTool(String repositoryId, Map<String, String> authInfo) {
        this(NodeServiceFactory.getLocalService(), repositoryId, authInfo);
    }

    public UserEnvironmentTool(NodeService nodeService, String repositoryId, Map<String, String> authInfo) {
        this(nodeService, authInfo.get(CCConstants.AUTH_USERNAME));
    }

    public String getDefaultUserDataFolder() throws Exception {

        String homeFolderId = mcBaseClient.getHomeFolderID(username);

        logger.info("homefolder:" + homeFolderId);
        String result = null;

        Map<String, Object> defaultDataFolderProps = mcBaseClient.getChild(homeFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS);
        if (defaultDataFolderProps != null) {
            result = (String) defaultDataFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
        } else {
            logger.error("something went wrong! no datafolder for current user " + username + " found!");
        }

        return result;
    }

    public String getDefaultImageFolder() throws Exception {

        String homeFolderId = mcBaseClient.getHomeFolderID(username);

        logger.info("homefolder:" + homeFolderId);
        String result = null;

        Map<String, Object> defaultImageFolderProps = mcBaseClient.getChild(homeFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_IMAGES);
        if (defaultImageFolderProps != null) {
            result = (String) defaultImageFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
        } else {
            logger.error("something went wrong! no image folder for current user " + username + " found!");
        }

        return result;
    }

    public String getEdu_SharingSystemFolderBase() throws Exception {
        if (!mcBaseClient.isAdmin() && !AuthenticationUtil.isRunAsUserTheSystemUser()) {
            throw new Exception("Admin group required");
        }
        String companyHomeNodeId = mcBaseClient.getCompanyHomeNodeId();
        NodeRef edu_SharingSysMap = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, companyHomeNodeId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);

        String result = null;
        if (edu_SharingSysMap == null) {

            String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE);
            HashMap newEdu_SharingSysMapProps = new HashMap();
            newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);

            HashMap i18nTitle = new HashMap();
            i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "de_DE"));
            i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "en_EN"));
            i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "en_US"));

            newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
            newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
            result = mcBaseClient.createNode(companyHomeNodeId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
        } else {
            result = edu_SharingSysMap.getId();
        }
        return result;
    }

    public String getEdu_SharingSystemFolderUpdate() throws Exception {
        if (!mcBaseClient.isAdmin() && !AuthenticationUtil.isRunAsUserTheSystemUser()) {
            throw new Exception("Admin group required");
        }

        String systemFolderId = getEdu_SharingSystemFolderBase();
        Map<String, Object> edu_SharingSystemFolderUpdate = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_UPDATE);

        String result = null;
        if (edu_SharingSystemFolderUpdate == null) {
            String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_UPDATE);
            HashMap newEdu_SharingSysMapProps = new HashMap();
            newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);

            HashMap i18nTitle = new HashMap();
            i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_UPDATE, "de_DE"));
            i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_UPDATE, "en_EN"));
            i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_UPDATE, "en_US"));

            newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
            newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_UPDATE);
            result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
        } else {
            result = (String) edu_SharingSystemFolderUpdate.get(CCConstants.SYS_PROP_NODE_UID);
        }
        return result;
    }

    public String getEdu_SharingNotifyFolder() throws Exception {
        String systemFolderId = getEdu_SharingSystemFolderBase();

        String currentScope = NodeServiceInterceptor.getEduSharingScope();

        //String notifyRootFolderName = (currentScope == null || currentScope.trim().isEmpty()) ? CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY : CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY + "_" + currentScope;

        String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY);
        systemFolderName = (currentScope == null || currentScope.trim().isEmpty()) ? systemFolderName : systemFolderName + "_" + currentScope;
        NodeRef edu_SharingSystemFolderNotify = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, systemFolderName);
        String result = null;
        if (edu_SharingSystemFolderNotify == null) {

            HashMap newEdu_SharingSysMapProps = new HashMap();
            newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);

            HashMap i18nTitle = new HashMap();
            i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY, "de_DE"));
            i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY, "en_EN"));
            i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY, "en_US"));

            newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
            newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);
            result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
        } else {
            result = edu_SharingSystemFolderNotify.getId();
        }
        return result;
    }

    public String getEdu_SharingContextFolder() throws Exception {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_SERVICE, CCConstants.I18n_SYSTEMFOLDER_CONTEXT);
    }

    public String getEdu_SharingConfigFolder() throws Exception {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_SERVICE, CCConstants.I18n_SYSTEMFOLDER_CONFIG);
    }

    public String getEdu_SharingMediacenterFolder() throws Exception {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_MEDIACENTER, CCConstants.I18n_SYSTEMFOLDER_MEDIACENTER);
    }

    public String getEdu_SharingReportsFolder() throws Exception {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_REPORTS, CCConstants.I18n_SYSTEMFOLDER_REPORTS);
    }

    public String getEdu_SharingServiceFolder() throws Exception {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_SERVICE, CCConstants.I18n_SYSTEMFOLDER_SERVICE);
    }

    public String getEdu_SharingTemplateFolder() throws Exception {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE, CCConstants.I18n_SYSTEMFOLDER_TEMPLATE);
	    /*
		String systemFolderId = getEdu_SharingSystemFolderBase();
		Map< mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE);
		String result = null;
		if(edu_SharingSystemFolderTemplate == null){
			String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE);
			HashMap newEdu_SharingSysMapProps  = new HashMap();
			newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
			
			HashMap i18nTitle = new HashMap();
			i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE, "de_DE"));
			i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE, "en_EN"));
			i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE, "en_US"));
			
			newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
			newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE);
			result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
		}else{
			result = (String)edu_SharingSystemFolderTemplate.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return result;
		*/
    }

    public String getOrCreateSystemFolderByName(String constantName, String i18nId) throws Exception {
        String result;
        String systemFolderId = getEdu_SharingSystemFolderBase();
        Map<String, Object> edu_SharingSystemFolderTemplate = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, constantName);
        if (edu_SharingSystemFolderTemplate == null) {
            String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(i18nId);
            Map<String, Object> newEdu_SharingSysMapProps = new HashMap<>();
            newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);

            Map<String, String> i18nTitle = new HashMap<>();
            i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(i18nId, "de_DE"));
            i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(i18nId, "en_EN"));
            i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(i18nId, "en_US"));

            newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
            newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, constantName);
            result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
        } else {
            result = (String) edu_SharingSystemFolderTemplate.get(CCConstants.SYS_PROP_NODE_UID);
        }
        return result;
    }


    /**
     * returns a folder where notify Objects can be safed
     *
     * @return
     * @throws Throwable
     */
    public String getEdu_SharingNotifyFolderToSafe() throws Exception {
        String notifyFolder = getEdu_SharingNotifyFolder();

        String year = new Integer(Calendar.getInstance().get(Calendar.YEAR)).toString();
        String yearMapId = getMap(notifyFolder, year, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);

        String month = new Integer(Calendar.getInstance().get(Calendar.MONTH)).toString();
        String monthMapId = getMap(yearMapId, month, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);

        String day = new Integer(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).toString();
        String dayMapId = getMap(monthMapId, day, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);

        return dayMapId;
    }

    public String getEdu_SharingValuespaceFolder() throws Exception {
        String systemFolderId = getEdu_SharingSystemFolderBase();
        Map<String, Object> edu_SharingSystemFolderValuespace = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_VALUESPACE);
        String result = null;
        if (edu_SharingSystemFolderValuespace == null) {
            String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_VALUESPACE);
            Map<String, Object> newEdu_SharingSysMapProps = new HashMap<>();
            newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);

            Map<String, String> i18nTitle = new HashMap<>();
            i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_VALUESPACE, "de_DE"));
            i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_VALUESPACE, "en_EN"));
            i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_VALUESPACE, "en_US"));

            newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
            newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_VALUESPACE);
            result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
        } else {
            result = (String) edu_SharingSystemFolderValuespace.get(CCConstants.SYS_PROP_NODE_UID);
        }
        return result;
    }


    /**
     * returns the child map with name. when it does not exist it will be created
     *
     * @param parentId
     * @param name
     * @return
     * @throws Throwable
     */
    private String getMap(String parentId, String name, String mapType) throws Exception {
        NodeRef child = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, name);
        if (child == null) {
            Map<String, Object> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, name);
            props.put(CCConstants.CM_PROP_TITLE, name);
            if (mapType != null) {
                props.put(CCConstants.CCM_PROP_MAP_TYPE, mapType);
            }
            return mcBaseClient.createNode(parentId, CCConstants.CCM_TYPE_MAP, props);
        } else {
            return child.getId();
        }
    }


}
