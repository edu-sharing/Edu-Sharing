package org.edu_sharing.service.nodeservice;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class NodeServiceAdapterCached extends NodeServiceAdapter{
    private static final ConcurrentMap<String, Map<String,Object>> propertyCache = new ConcurrentLinkedHashMap.Builder<String, Map<String,Object>>()
            .maximumWeightedCapacity(1000)
            .build();

    public NodeServiceAdapterCached(String appId) {
        super(appId);
    }
    public static void updateCache(Map<String,Object> properties) {
        propertyCache.put((String)properties.get(CCConstants.SYS_PROP_NODE_UID), properties);
    }
    @Override
    public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
        if (propertyCache.containsKey(nodeId))
            return propertyCache.get(nodeId);
        return null;
    }
}
