package org.edu_sharing.service.admin;

import com.google.gson.Gson;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SystemFolder {

    static Logger logger = Logger.getLogger(SystemFolder.class);

    static ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    static Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
    static ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    public static NodeRef getSystemObject(String name) throws Throwable {
        String folder = ToolPermissionServiceFactory.getInstance().getEdu_SharingSystemFolderBase().getId();
        NodeService nodeService = NodeServiceFactory.getLocalService();
        String node = nodeService.findNodeByName(folder, name);
        if (node == null) {
            Map<String, Object> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, name);
            node = nodeService.createNodeBasic(folder, CCConstants.CCM_TYPE_IO, props);
        }
        return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node);
    }

    public static <T> T getSystemObjectContent(String name, Class<T> clazz) {
        return AuthenticationUtil.runAsSystem(() -> {
            try {
                NodeRef node = getSystemObject(name);
                InputStream content = NodeServiceHelper.getContent(node);
                if (content == null) {
                    return clazz.newInstance();
                }
                return new Gson().fromJson(new InputStreamReader(content), clazz);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
                return clazz.newInstance();
            }
        });
    }

    static NodeRef systemFolderBase = null;
    static NodeRef sitesFolder = null;
    private static NodeRef personFolder;

    public static NodeRef getSystemFolderBase() {
        if (systemFolderBase == null) {

            AuthenticationUtil.runAsSystem(() -> {
                try {
                    systemFolderBase = ToolPermissionServiceFactory.getInstance().getEdu_SharingSystemFolderBase();
                } catch (Throwable e) {
                    logger.error(e.getMessage());
                }
                return null;
            });

        }
        return systemFolderBase;
    }

    public static NodeRef getSitesFolder(){
        if(sitesFolder == null) {
            AuthenticationUtil.runAsSystem(() -> {
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                sitesFolder = serviceRegistry.getNodeService().getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, "Sites");
                return null;
            });
        }
        return sitesFolder;
    }

    public static NodeRef getPersonFolder() {
        if(personFolder == null) {
            personFolder = AuthenticationUtil.runAsSystem(() -> {
                try {
                    org.alfresco.service.cmr.repository.NodeRef system = serviceRegistry.getNodeService().getChildAssocs(repositoryHelper.getRootHome()).stream().filter(
                            (rel) -> rel.getQName().getLocalName().equals("system")
                    ).map(ChildAssociationRef::getChildRef).findFirst().get();
                    return serviceRegistry.getNodeService().getChildAssocs(system).stream().filter(
                            (rel) -> rel.getQName().getLocalName().equals("people")
                    ).map(ChildAssociationRef::getChildRef).findFirst().get();
                }catch(Throwable t) {
                    logger.warn("Could not resolve people folder", t);
                    return null;
                }
            });
        }
        return personFolder;

    }
}
