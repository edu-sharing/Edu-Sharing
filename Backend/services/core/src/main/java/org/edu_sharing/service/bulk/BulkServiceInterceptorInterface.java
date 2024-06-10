package org.edu_sharing.service.bulk;

import org.alfresco.service.cmr.repository.NodeRef;

import java.util.HashMap;
import java.util.Map;

public interface BulkServiceInterceptorInterface {

    /**
     * Preprocess properties before they get stored and post-processed by the bulk service
     */
    default Map<String, Object> preprocessProperties(Map<String, Object> properties) {
        return properties;
    }

    /**
     * Called after the node has been created
     * Useful to apply additional permissions or changes to the node itself
     */
    default void onNodeCreated(NodeRef nodeRef, Map<String, Object> properties) {}
}
