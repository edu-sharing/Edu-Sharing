package org.edu_sharing.repository.server.tools;

import java.io.Serializable;
import java.util.*;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

public class NodeTool {

    public static String createOrGetNodeByName(String parentId, String[] path) {

        return createOrGetNodeByName(parentId, List.of(path));
    }

    public static String createOrGetNodeByName(String parentId, List<String> path) {

        if (!path.isEmpty()) {
            String name = path.get(0);
            NodeRef child = NodeServiceFactory.getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, name);

            if (child == null) {
                Map<String, Serializable> _props = new HashMap<>();
                _props.put(CCConstants.CM_NAME, name);
                parentId =  NodeServiceFactory.getLocalService().createNodeBasic(parentId, CCConstants.CCM_TYPE_MAP, _props);
            } else {
                parentId = child.getId();
            }

            return createOrGetNodeByName(parentId, path.subList(1, path.size()));
        }

        return parentId;

    }

}
