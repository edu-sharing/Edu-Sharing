package org.edu_sharing.service.nodeservice;

import org.alfresco.service.cmr.repository.NodeRef;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface PropertiesSetInterceptor {

    /**
     * called when the node properties are set/updated
     * You can add or remove data in the returned set
     */
    Map<String, Object> beforeSetProperties(PropertiesGetInterceptor.PropertiesContext context);
    default SetInterceptorTiming getInterceptorTiming() {
        return SetInterceptorTiming.BeforeAlfrescoInterceptors;
    }

    enum SetInterceptorTiming {
        /**
         * run this interceptor before the alfresco interceptors are running
         * Note: This interceptors will not have aspects present when a node is in creation state!
         */
        BeforeAlfrescoInterceptors,
        /**
         * run this interceptor after the alfresco interecptors ran
         * Note: This type is more expensive since it needs to re-fetch properties from alfresco
         */
        AfterAlfrescoInterceptors,
    }

}
