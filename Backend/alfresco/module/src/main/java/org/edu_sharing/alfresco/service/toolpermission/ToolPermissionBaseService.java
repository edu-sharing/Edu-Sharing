package org.edu_sharing.alfresco.service.toolpermission;

import com.typesafe.config.Config;
import jakarta.servlet.http.HttpSession;
import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.*;

public class ToolPermissionBaseService {
    private Logger logger = Logger.getLogger(ToolPermissionBaseService.class);
    protected ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
    protected ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    protected PermissionService permissionService = serviceRegistry.getPermissionService();
    protected NodeService nodeService = serviceRegistry.getNodeService();
    protected OwnableService ownableService = serviceRegistry.getOwnableService();
    protected Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
    protected static Map<String,String> toolPermissionNodeCache = new HashMap<>();
    private NodeRef toolPermissionFolder;

    protected boolean isAdmin(){
        try {
            Set<String> testUsetAuthorities = serviceRegistry.getAuthorityService().getAuthorities();
            if(testUsetAuthorities.contains("GROUP_ALFRESCO_ADMINISTRATORS")) {
                return true;
            }
            return AuthenticationUtil.isRunAsUserTheSystemUser();
        }catch(AuthenticationCredentialsNotFoundException ignored){
            // may causes missing security context exceptions
            return false;
        }
    }

    private boolean hasToolPermissionWithoutCache(String toolPermission) {
        AuthenticationUtil.RunAsWork<String> workTP= () -> {
            try {
                return getToolPermissionNodeId(toolPermission, true);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        };

        try{
            if (isAdmin()) {
                return true;
            }
        }catch(Exception e){

        }

        String toolNodeId = AuthenticationUtil.runAsSystem(workTP);
        if(toolNodeId == null){
            logger.warn("Could not fetch toolpermission " + toolPermission + "in alfresco context, fallback to false");
            return false;
        }
        AccessStatus accessStatus = permissionService.hasPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, toolNodeId), PermissionService.READ);
        AccessStatus accessStatusDenied = permissionService.hasPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, toolNodeId), CCConstants.PERMISSION_DENY);
        if(accessStatusDenied.equals(AccessStatus.ALLOWED)) {
            logger.debug("Toolpermission "+toolPermission+" has explicit Deny permission");
        }
        return accessStatus.equals(AccessStatus.ALLOWED) && !accessStatusDenied.equals(AccessStatus.ALLOWED);
    }


    public boolean hasToolPermission(String toolPermission) {
        return hasToolPermission(toolPermission, false);
    }

    /**
     *
     * @param toolPermission
     * @param renew should the cache be skipped / renewed
     * @return
     */
    public boolean hasToolPermission(String toolPermission, boolean renew) {
        /**
         * try to use session cache
         */
        // system user is not restricted
        if(AuthenticationUtil.isRunAsUserTheSystemUser()) {
            return true;
        }
        Boolean hasToolPerm = false;
        HttpSession session = null;
        if(Context.getCurrentInstance() != null){
            session = Context.getCurrentInstance().getRequest().getSession();
            hasToolPerm = (Boolean)session.getAttribute(toolPermission);
        }else{
            renew = true;
        }

        if(hasToolPerm == null || renew){
            hasToolPerm = hasToolPermissionWithoutCache(toolPermission);
            if(session != null) {
                session.setAttribute(toolPermission, hasToolPerm);
            }
        }
        return hasToolPerm;
    }


    public NodeRef getEdu_SharingSystemFolderBase() throws Throwable{
        if(!isAdmin()){
            throw new Exception("Admin group required");
        }
        NodeRef companyHome = repositoryHelper.getCompanyHome();
        NodeRef edu_SharingSysMap = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE));
        NodeRef result;
        if(edu_SharingSysMap == null){
            result = createNewFolder(companyHome, CCConstants.I18n_SYSTEMFOLDER_BASE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
        }else{
            result = edu_SharingSysMap;
        }
        return result;
    }

    private NodeRef createNewFolder(NodeRef parentRef, String name, String mapType) {
        NodeRef result;
        String folderName = I18nServer.getTranslationDefaultResourcebundle(name);
        Map<QName, Serializable> props  = new HashMap<>();
        props.put(QName.createQName(CCConstants.CM_NAME), folderName);

        MLText i18nTitle = new MLText();
        i18nTitle.addValue(new Locale("de_DE"),I18nServer.getTranslationDefaultResourcebundle(name, "de_DE"));
        i18nTitle.addValue(new Locale("en_EN"),I18nServer.getTranslationDefaultResourcebundle(name, "en_EN"));
        i18nTitle.addValue(new Locale("en_US"),I18nServer.getTranslationDefaultResourcebundle(name, "en_US"));

        props.put(QName.createQName(CCConstants.CM_PROP_C_TITLE), i18nTitle);
        props.put(QName.createQName(CCConstants.CCM_PROP_MAP_TYPE), mapType);
        result = nodeService.createNode(parentRef,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(QName.createValidLocalName(folderName)),
                QName.createQName(CCConstants.CCM_TYPE_MAP),
                props).getChildRef();
        permissionService.setInheritParentPermissions(result,false);
        return result;
    }


    public Collection<String> getAllToolPermissions(boolean refresh){
        if(!refresh && !toolPermissionNodeCache.keySet().isEmpty()){
            return toolPermissionNodeCache.keySet();
        }
        AuthenticationUtil.RunAsWork<List<String>> runas = new AuthenticationUtil.RunAsWork<List<String>>() {
            @Override
            public List<String> doWork() throws Exception {
                List<String> result = new ArrayList<>();
                try {
                    List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(getEdu_SharingToolPermissionsFolder());
                    for(ChildAssociationRef childAssocRef : childAssocRefs) {
                        String name = (String) nodeService.getProperty(childAssocRef.getChildRef(), QName.createQName(CCConstants.CM_NAME));
                        toolPermissionNodeCache.put(name, childAssocRef.getChildRef().getId());
                        result.add(name);
                    }
                }catch(Throwable e) {
                    logger.error(e.getMessage(), e);
                }
                return result;
            }
        };

        return AuthenticationUtil.runAsSystem(runas);
    }

    public List<String> getAllAvailableToolPermissions(){
        return getAllAvailableToolPermissions(false);
    }
    public List<String> getAllAvailableToolPermissions(boolean renew){
        List<String> allowed=new ArrayList<>();
        for(String permission : this.getAllToolPermissions(renew)){
            if(hasToolPermission(permission, renew))
                allowed.add(permission);
        }
        return allowed;
    }

    public NodeRef getEdu_SharingToolPermissionsFolder() throws Throwable{
        if(toolPermissionFolder!=null)
            return toolPermissionFolder;
        logger.info("fully: "+AuthenticationUtil.getFullyAuthenticatedUser() +" runAs:"+AuthenticationUtil.getRunAsUser());
        NodeRef systemFolder = getEdu_SharingSystemFolderBase();
        NodeRef edu_SharingSystemFolderToolPermissions = nodeService.getChildByName(systemFolder, ContentModel.ASSOC_CONTAINS, I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TOOLPERMISSIONS));
        NodeRef result = null;
        if(edu_SharingSystemFolderToolPermissions == null){
            logger.info("ToolPermission Folder does not exist. will create it.");
            result = createNewFolder(systemFolder,
                    CCConstants.I18n_SYSTEMFOLDER_TOOLPERMISSIONS,
                    CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TOOLPERMISSIONS);
        }else{
            result = edu_SharingSystemFolderToolPermissions;
        }
        this.toolPermissionFolder=result;
        return result;
    }

    protected void initToolPermissions(List<String> toolPermissions) throws Throwable{

        for(String toolPermission : toolPermissions){
            getToolPermissionNodeId(toolPermission, true);
        }
        List<? extends Config> list = LightbendConfigLoader.get().getConfigList("repository.toolpermissions.managed");
        if(!list.isEmpty()) {
            list.forEach((value) -> {
                try {
                    String id = value.getString("id");
                    // String nodeid = getToolPermissionNodeId(id, false);
                    // only create and set permissions if not yet exists
                    // if(nodeid == null) {
                    logger.info("auto-configure toolpermission " + id);
                    String nodeid = getToolPermissionNodeId(id, true);
                    permissionService.deletePermissions(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeid));
                    nodeService.setProperty(
                            new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeid),
                            QName.createQName(CCConstants.CCM_PROP_TOOLPERMISSION_SYSTEM_MANAGED),
                            true
                    );

                    if(value.hasPath("allowed")) {
                        List<String> allowed = value.getStringList("allowed");
                        for (String authority : allowed) {
                            permissionService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeid),
                                    authority, CCConstants.PERMISSION_READ, true);
                        }
                    }
                    if(value.hasPath("denied")) {
                        List<String> denied = value.getStringList("denied");
                        for (String authority : denied) {
                            permissionService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeid),
                                    authority, CCConstants.PERMISSION_DENY, true);
                        }
                    }
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public String getToolPermissionNodeId(String toolPermission, boolean createIfNotExists) throws Throwable{
        if(toolPermissionNodeCache.containsKey(toolPermission)) {
            String nodeId=toolPermissionNodeCache.get(toolPermission);
            // validate that the cached node is not deleted
            if(nodeService.exists(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId))) {
                return nodeId;
            }
        }
        NodeRef systemFolderId = getEdu_SharingToolPermissionsFolder();


        NodeRef sysObject = nodeService.getChildByName(systemFolderId, ContentModel.ASSOC_CONTAINS, toolPermission);

        if(sysObject == null){
            if(createIfNotExists) {
                return createToolpermission(toolPermission).getId();
            } else {
                 return null;
            }
        }else{
            String nodeId=sysObject.getId();
            toolPermissionNodeCache.put(toolPermission, nodeId);
            return nodeId;
        }
    }

    public String getToolPermissionNodeId(String toolPermission) throws Throwable{
        return getToolPermissionNodeId(toolPermission, true);
    }

    protected NodeRef createToolpermission(String toolPermission) throws Throwable {
        logger.info("ToolPermission" + toolPermission+ " does not exists. will create it.");
        Map<QName, Serializable> props = new HashMap();
        props.put(QName.createQName(CCConstants.CM_NAME), toolPermission);

        NodeRef result = nodeService.createNode(getEdu_SharingToolPermissionsFolder(),
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(QName.createValidLocalName(toolPermission)),
                QName.createQName(CCConstants.CCM_TYPE_TOOLPERMISSION),
                props).getChildRef();
        //set admin as owner cause if it was created by runAs with admin the current user not the runas on is taken
        serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
            ownableService.setOwner(result, ApplicationInfoList.getHomeRepository().getUsername());
            policyBehaviourFilter.disableBehaviour();
            nodeService.setProperty(result, QName.createQName(CCConstants.CM_PROP_C_CREATOR), ApplicationInfoList.getHomeRepository().getUsername());
            nodeService.setProperty(result, QName.createQName(CCConstants.CM_PROP_C_MODIFIER), ApplicationInfoList.getHomeRepository().getUsername());
            policyBehaviourFilter.enableBehaviour();
            return null;
        });

        if(getAllDefaultAllowedToolpermissions().contains(toolPermission) || toolPermission.startsWith(CCConstants.CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX)){
            logger.info("ToolPermission" + toolPermission+ " is allowed by default. Will set GROUP_EVERYONE.");
            //permissionService.setPermissions(result,PermissionService.ALL_AUTHORITIES, new String[]{CCConstants.PERMISSION_READ}, false);
            permissionService.setPermission(result, PermissionService.ALL_AUTHORITIES, CCConstants.PERMISSION_READ, true);
        }else{
            permissionService.deletePermissions(result);
        }

        toolPermissionNodeCache.put(toolPermission, result.getId());
        return result;
    }
    public List<String> getAllDefaultAllowedToolpermissions(){
        List<String> toInit=getAllPredefinedToolPermissions();
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL); // safe
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_EDITORIAL); // editorial collections
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CURRICULUM); // curriculum collections
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING); // pin collections
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_HANDLESERVICE); // use handle id
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_USAGE_STATISTIC); // get all usages across all nodes (as system)
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK); // give feedback on collections
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_USER);
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_NODES);
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_MEDIACENTER_MANAGE);
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_CONTROL_RESTRICTED_ACCESS);
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_PUBLISH_COPY);
        toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_MAP_LINK);
        return toInit;
    }
    public List<String> getAllPredefinedToolPermissions(){
        List<String> toInit=new ArrayList<>();
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE);

        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_STREAM);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_LINK);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SAFE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE_SAFE);

        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_LICENSE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_UNCHECKEDCONTENT);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_WORKSPACE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_FILES);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL);

        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CHANGE_OWNER);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_EDITORIAL);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CURRICULUM);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_HANDLESERVICE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_USAGE_STATISTIC);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COMMENT_WRITE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_USER);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_NODES);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_RATE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_RATE_WRITE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_RATE_READ);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_SUGGESTION_WRITE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_SUGGESTION_READ);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_MANAGE_RELATIONS);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_VIDEO_AUDIO_CUT);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_MEDIACENTER_MANAGE);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_PUBLISH_COPY);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_MAP_LINK);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_SIGNUP_GROUP);

        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CONTROL_RESTRICTED_ACCESS);


        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CONTROL_RESTRICTED_ACCESS);
        toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PROPOSAL);

        addConnectorToolpermissions(toInit);
        return toInit;
    }
    protected void addConnectorToolpermissions(List<String> toInit){
        // this method must be overriden in the edu-sharing context!!!
    }

}
