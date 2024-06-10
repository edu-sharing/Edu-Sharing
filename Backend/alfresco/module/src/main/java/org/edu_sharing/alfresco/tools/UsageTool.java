package org.edu_sharing.alfresco.tools;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transformed copy of UsageDAO which is available in the Alfresco class space
 */
public class UsageTool {
    private final NodeService nodeService;

    public UsageTool(){
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        nodeService = serviceRegistry.getNodeService();
    }
    public Map<QName, Serializable> getUsage(String lmsId, String courseId, String objectNodeId, String resourceId) throws Exception {
        List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, objectNodeId),
                Collections.singleton(QName.createQName(CCConstants.CCM_TYPE_USAGE)));
        for (ChildAssociationRef child : childAssocList) {
            Map<QName, Serializable> usageNode = nodeService.getProperties(child.getChildRef());
            String tmpAppId = (String) usageNode.get(QName.createQName(CCConstants.CCM_PROP_USAGE_APPID));
            String tmpCourseId = (String) usageNode.get(QName.createQName(CCConstants.CCM_PROP_USAGE_COURSEID));
            String tmpObjectNodeId = (String) usageNode.get(QName.createQName(CCConstants.CCM_PROP_USAGE_PARENTNODEID));
            String tmpResourceId = (String) usageNode.get(QName.createQName(CCConstants.CCM_PROP_USAGE_RESSOURCEID));

            if (lmsId != null
                    && lmsId.equals(tmpAppId)
                    && courseId != null
                    && courseId.equals(tmpCourseId)
                    && objectNodeId.equals(tmpObjectNodeId)
                    && ((resourceId == null && tmpResourceId == null) || (resourceId != null && resourceId
                    .equals(tmpResourceId)))) {
                return usageNode;
            }
        }
        return null;
    }
    public void removeUsage(String lmsId, String courseId, String parentNodeId, String resourceId) throws Exception {
        Map<QName, Serializable> usage = this.getUsage(lmsId, courseId, parentNodeId, resourceId);
        if(usage != null){
            String parentId = (String)usage.get(QName.createQName(CCConstants.CCM_PROP_USAGE_PARENTNODEID));
            String usageId = (String)usage.get(QName.createQName(CCConstants.SYS_PROP_NODE_UID));
            nodeService.removeChild(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId),new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,usageId));
        }else{
            throw new Exception("no usage found for lmsId:"+lmsId +" courseId:"+courseId+ " parentNodeId:"+parentNodeId+" resourceId:"+resourceId);
        }
    }
    public void createUsage(String lmsId, String courseId, String parentNodeId, String resourceId){
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(QName.createQName(CCConstants.CCM_PROP_USAGE_APPID), lmsId);
        properties.put(QName.createQName(CCConstants.CCM_PROP_USAGE_COURSEID), courseId);
        properties.put(QName.createQName(CCConstants.CCM_PROP_USAGE_PARENTNODEID), parentNodeId);
        properties.put(QName.createQName(CCConstants.CCM_PROP_USAGE_RESSOURCEID), resourceId);
        nodeService.createNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentNodeId),
                QName.createQName(CCConstants.CCM_ASSOC_USAGEASPECT_USAGES),
                QName.createQName(CCConstants.CCM_ASSOC_USAGEASPECT_USAGES),
                QName.createQName(CCConstants.CCM_TYPE_USAGE),
                properties
                );
    }
}
