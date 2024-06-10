/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools.cache;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.beans.BeansException;

import com.google.common.collect.ImmutableSet;

public class RepositoryCache implements Cache {
	
	interface InternalCache extends Cache {
		
		void setCache(Map<String, Map<String, Object>> cache);
		boolean isShared();
		
	}
	
	private static final Log LOG = LogFactory.getLog(RepositoryCache.class);
	
	private static final ImmutableSet<String> allowedTypes = 
			ImmutableSet.of(
					CCConstants.CCM_TYPE_TOOLPERMISSION,
					CCConstants.CCM_TYPE_IO,
					CCConstants.CCM_TYPE_MAP,
					CCConstants.CCM_TYPE_MAPRELATION, 
					CCConstants.CM_TYPE_FOLDER);
		
	private static InternalCache CACHE;
	
	static {

		try {

			// Shared Cache over EHCache
			final SimpleCache<String, Map<String,Object>> sharedCache = 
					(SimpleCache<String, Map<String,Object>>) 
						AlfAppContextGate.getApplicationContext().getBean("eduSharingPropertiesCache");
			
			CACHE = new InternalCache() {

				@Override
				public Map<String, Object> get(String nodeId) {
					
					synchronized(sharedCache) {
						return sharedCache.get(nodeId);
					}
					
				}
	
				@Override
				public void remove(String nodeId) {
					
					synchronized(sharedCache) {
						sharedCache.remove(nodeId);
					}
					
				}
	
				@Override
				public void put(String nodeId, Map props) {
					
					synchronized(sharedCache) {
						sharedCache.put(nodeId, props);
					}
					
				}
	
				@Override
				public void setCache(Map<String, Map<String, Object>> cache) {
					
					synchronized(sharedCache) {
					
						sharedCache.clear();
	
						for (Map.Entry<String, Map<String, Object>> entry : cache.entrySet()) {
							
							sharedCache.put(entry.getKey(), entry.getValue());
							
						}
						
					}					
				}
	
	
				@Override
				public boolean isShared() {
					return true;
				}
			};
			
		} catch (BeansException e) {
			
			// Local Cache over HashMap
			final Map<String, WeakReference<Map<String,Object>>> localCache = 
					new HashMap<>();
			
			CACHE = new InternalCache() {

				@Override
				public Map<String, Object> get(String nodeId) {
					
					synchronized(localCache) {
						WeakReference<Map<String,Object>> ref = localCache.get(nodeId);
						return (ref != null ? ref.get() : null);
					}
					
				}

				@Override
				public void remove(String nodeId) {
					
					synchronized(localCache) {
						localCache.remove(nodeId);
					}
					
				}

				@Override
				public void put(String nodeId, Map props) {
					
					synchronized(localCache) {
						localCache.put(nodeId, new WeakReference<Map<String, Object>>(props));
					}
					
				}

				@Override
				public void setCache(Map<String, Map<String, Object>> entries) {
					
					synchronized(localCache) {
					
						localCache.clear();
						
						for (Map.Entry<String, Map<String, Object>> entry : entries.entrySet()) {
							put(entry.getKey(), entry.getValue());
						}
						
					}
				}

				@Override
				public boolean isShared() {
					return false;
				}
				
			};
			
		}		
			
		LOG.info("RepositoryCache <"+ CACHE.toString() + "> initialized --> isSharedCache: " + CACHE.isShared());
	}
	
	public void put(String nodeId, Map props) {
		
		String currentType = (String) props.get(CCConstants.NODETYPE); 
		
		if (RepositoryCache.allowedTypes.contains(currentType)) {
		
			CACHE.put(nodeId, props);
			
		}
	}

	public Map<String, Object> get(String nodeId) {
		
		return CACHE.get(nodeId);
		
	}

	public void remove(String nodeId) {
		CACHE.remove(nodeId);
		// run as system since it will access node service operations which could cause AccessDenied in usage contexts
		AuthenticationUtil.runAsSystem(() -> {
			PreviewCache.purgeCache(nodeId);
			return null;
		});
	}
	
	public static void setCache(Map<String, Map<String, Object>> cache) {
		
		CACHE.setCache(cache);
		
	}
	
}
