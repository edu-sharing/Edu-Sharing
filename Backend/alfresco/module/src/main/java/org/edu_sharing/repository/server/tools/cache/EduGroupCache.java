package org.edu_sharing.repository.server.tools.cache;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.io.Serializable;
import java.util.*;



public class EduGroupCache {

	public static Logger logger = Logger.getLogger(EduGroupCache.class);
	
	/**
	 * Administration by:
	 * org.edu_sharing.repository.server.tools.EduGroupTool: put when aspect added, remove when aspect removed
	 * org.edu_sharing.alfresco.policy.BeforeEduGroupDeletePolicy remove when edugroup will be deleted
	 * org.edu_sharing.repository.server.MCAlfrescoManager: init when webapp is started
	 * 
	 */
	

	public static ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	// Shared Cache over EHCache
	final static SimpleCache<NodeRef,Map<QName,Serializable>> cache = (SimpleCache<NodeRef,Map<QName,Serializable>>)  AlfAppContextGate.getApplicationContext().getBean("eduSharingEduGroupCache");

	final static SimpleCache<NodeRef,Map<QName,Serializable>> cacheWithFolderAsKey = (SimpleCache<NodeRef,Map<QName,Serializable>>)  AlfAppContextGate.getApplicationContext().getBean("eduSharingEduGroupFolderCache");
	
	public static void put(NodeRef nodeRef, Map<QName,Serializable> props){
		
		synchronized(EduGroupCache.cache){
			EduGroupCache.cache.put(nodeRef, props);
			EduGroupCache.putEduGroupFolder((NodeRef)props.get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR)),props);
		}
		
	}

	private static void putEduGroupFolder(NodeRef nodeRef, Map<QName,Serializable> props){
		if(nodeRef == null){
			String message = "got a null value for edugroupfolder of edu group:";
			if(props != null) message += props.get(ContentModel.PROP_AUTHORITY_NAME);
			logger.error(message);
			return;
		}
		EduGroupCache.cacheWithFolderAsKey.put(nodeRef,props);
	}

	public static Map<QName,Serializable> get(NodeRef nodeRef) {
		return EduGroupCache.cache.get(nodeRef);
	}

	public static Map<QName,Serializable> getByEduGroupfolder(NodeRef nodeRef){
		return EduGroupCache.cacheWithFolderAsKey.get(nodeRef);
	}

	public static void remove(NodeRef nodeRef) {
		synchronized(EduGroupCache.cache){
			NodeRef nodeRefFolder = (NodeRef)EduGroupCache.cache.get(nodeRef).get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
			if(nodeRefFolder != null) {
				EduGroupCache.cacheWithFolderAsKey.remove(nodeRefFolder);
			}
			EduGroupCache.cache.remove(nodeRef);
		}		
	}
	
	public static Collection<NodeRef> getKeys(){
		return EduGroupCache.cache.getKeys();
	}

	public static Collection<NodeRef> getKeysEduGroupFolder(){
		return EduGroupCache.cacheWithFolderAsKey.getKeys();
	}
	
	public static String[] getNames(){
		ArrayList<String> names = new ArrayList<>();
		for(NodeRef nodeRef : cache.getKeys()){
			names.add((String)cache.get(nodeRef).get(ContentModel.PROP_AUTHORITY_NAME));
		}
		return names.toArray(new String[names.size()]);
	}

	public static boolean isAnOrganisationFolder(NodeRef nodeRef){
		if(EduGroupCache.cacheWithFolderAsKey.contains(nodeRef)) return true;
		else return false;
	}

	public static void refresh(){
		synchronized(EduGroupCache.cache){
			logger.info("size before refresh:"+EduGroupCache.cache.getKeys().size());
			clear();
			NodeService nodeServiceAlfresco = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
			serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
				for (NodeRef eduGroupNodeRef : getEduGroupNodeRefs()) {
					Map<QName, Serializable> properties = nodeServiceAlfresco.getProperties(eduGroupNodeRef);
					EduGroupCache.put(eduGroupNodeRef, properties);
				}
				return null;
			});
			logger.info("size after refresh:"+EduGroupCache.cache.getKeys().size());
		}		
	}

	private static void clear(){
		EduGroupCache.cache.clear();
		EduGroupCache.cacheWithFolderAsKey.clear();
	}
	
	public static void refreshByKeepExisting(){
		synchronized(EduGroupCache.cache){
			logger.info("size before refresh:"+EduGroupCache.cache.getKeys().size());
			//EduGroupCache.cache.clear();
			for(NodeRef eduGroupNodeRef : getEduGroupNodeRefs()){
				if(!EduGroupCache.cache.contains(eduGroupNodeRef)) {
					Map<QName, Serializable> properties = serviceRegistry.getNodeService().getProperties(eduGroupNodeRef);
					EduGroupCache.put(eduGroupNodeRef, properties);
				}
			}
			logger.info("size after refresh:"+EduGroupCache.cache.getKeys().size());
		}		
	}
	
	private static List<NodeRef> getEduGroupNodeRefs(){
		logger.info("starting");
		AuthorityService authorityService =serviceRegistry.getAuthorityService();
		NodeService nodeServiceAlfresco = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
		List<NodeRef> result = new ArrayList<>();
		Set<String> allGroups = authorityService.getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);
		for(String authority : allGroups) {
			NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authority);
			if(nodeServiceAlfresco.hasAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {
				result.add(authorityNodeRef);
			}
		}
		logger.info("found groupCount: " + allGroups.size() + " eduGroupCount:" + result.size() );
		return result;
		
	}
	
}
