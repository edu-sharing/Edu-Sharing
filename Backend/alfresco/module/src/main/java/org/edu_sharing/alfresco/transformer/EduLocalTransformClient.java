package org.edu_sharing.alfresco.transformer;

import org.alfresco.repo.rendition2.LocalTransformClient;
import org.alfresco.repo.rendition2.RenditionDefinition2;
import org.alfresco.repo.rendition2.RenditionDefinition2Impl;
import org.alfresco.repo.rendition2.TransformDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.HashMap;
import java.util.Map;

import static org.edu_sharing.alfresco.transformer.EduLocalTransformServiceRegistry.TRANSFORM_OPTION_RESOURCETYPE;

public class EduLocalTransformClient extends LocalTransformClient {

    private static Log logger = LogFactory.getLog(EduLocalTransformClient.class);

    NodeService nodeService;

    @Override
    public void checkSupported(NodeRef sourceNodeRef, RenditionDefinition2 renditionDefinition, String sourceMimetype, long sourceSizeInBytes, String contentUrl) {
        RenditionDefinition2 copy = adaptOptions(renditionDefinition,sourceNodeRef);
        super.checkSupported(sourceNodeRef, copy, sourceMimetype, sourceSizeInBytes, contentUrl);
    }

    @Override
    public void transform(NodeRef sourceNodeRef, RenditionDefinition2 renditionDefinition, String user, int sourceContentHashCode) {
        RenditionDefinition2 copy = adaptOptions(renditionDefinition,sourceNodeRef);
        super.transform(sourceNodeRef, copy, user, sourceContentHashCode);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    private RenditionDefinition2 adaptOptions(RenditionDefinition2 original, NodeRef sourceNodeRef){
        RenditionDefinition2 result;

        String resourceType = (String)nodeService.getProperty(sourceNodeRef, QName.createQName(CCConstants.CCM_PROP_CCRESSOURCETYPE));

        if(resourceType != null && !resourceType.trim().isEmpty()){
            logger.info("adding edu-sharing option resourceType:" + resourceType);
            Map<String,String> options = new HashMap<>(original.getTransformOptions());
            options.put(TRANSFORM_OPTION_RESOURCETYPE, resourceType);

            if (original instanceof TransformDefinition) {
                TransformDefinition td = ((TransformDefinition) original);
                result = new TransformDefinition(td.getTransformName(),original.getTargetMimetype(),options,td.getClientData(),td.getReplyQueue(),td.getRequestId(),null);
            }else{
                result = new RenditionDefinition2Impl(original.getRenditionName()
                        ,original.getTargetMimetype(),
                        options,
                        null);
            }
        }else result = original;

        return result;
    }
}
