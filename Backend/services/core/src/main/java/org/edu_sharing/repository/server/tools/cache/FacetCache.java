/**
 *
 */
package org.edu_sharing.repository.server.tools.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FacetCache {
    /**
     * Map<LuceneQuery,Map<ToCountProp,Map<wert,count>>>
     */
    private static Map<String, Map<String, Map<String, Integer>>> facetCache = new ConcurrentHashMap<String, Map<String, Map<String, Integer>>>();

    public void add(String cacheId, String property, String value, Integer count) {
        Map<String, Map<String, Integer>> propsToCountMap = facetCache.get(cacheId);
        if (propsToCountMap == null) {
            propsToCountMap = new ConcurrentHashMap<>();
            facetCache.put(cacheId, propsToCountMap);
        }

        Map<String, Integer> valuesCountmap = propsToCountMap.get(property);
        if (valuesCountmap == null) {
            valuesCountmap = new ConcurrentHashMap<>();
            propsToCountMap.put(property, valuesCountmap);
        }
        valuesCountmap.put(value, count);
    }

    public Map<String, Integer> get(String cacheId, String property) {
        Map<String, Map<String, Integer>> propsToCountMap = facetCache.get(cacheId);
        if (propsToCountMap != null) {
            Map<String, Integer> synchMap = propsToCountMap.get(property);
            if (synchMap != null && synchMap.size() > 0) {
                //we need a normal hashMap it seems that ConcurrentHashMaps can not be rpc serialized by gwt
                Map<String, Integer> toReturn = new HashMap<>();
                for (Map.Entry<String, Integer> entry : synchMap.entrySet()) {
                    toReturn.put(entry.getKey(), entry.getValue());
                }
                return toReturn;
            }
        }
        return null;
    }

    public static Map<String, Map<String, Map<String, Integer>>> getFacetCache() {
        return facetCache;
    }

}
