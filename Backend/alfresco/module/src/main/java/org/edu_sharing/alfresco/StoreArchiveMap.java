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
package org.edu_sharing.alfresco;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * 
 * This class is used for disabling Archiving for the whole repository
 * the cause is:
 * queryChildren delivers also archived Nodes, this causes errors in CC Alfresco access Classes when 
 * trying to accessg an deleted Node with the wrong Store
 * 
 * 120109: When is an Archived node created:
 * an node will be archived when it's linked in Folder B and the primary parent is folder A and the node is deleted in folder A.
 * Than an archived Node appears in Folder B. Why there is no archived node in Folder A, I don't know.
 * 
 * also we don't want to get deleted Nodes
 * 
 * another cause is the Performance when calling queryChildren delivers also the deleted Nodes.
 * we didn't found an possibility for configuring the repositoryService to not deliver archived Nodes
 * 
 * this class overwrites the bean with the id storeArchiveMap in the file /alfresco/extension/custom-repository-context.xml
 * org.alfresco.repo.node.StoreArchiveMap
 * 
 * another possibility would be to add the sys:temporary aspect to the our types so they would not be archived
 * 
 * But with overwriting this bean we got an global switch for enabling/diabling archiving for all nodes
 * 
 * http://wiki.alfresco.com/wiki/Disabling_Document_Archival
 * 
 * 
 * 
 * This class simply does nothing
 */
public class StoreArchiveMap extends org.alfresco.repo.node.StoreArchiveMap{
	
		Log logger = LogFactory.getLog(StoreArchiveMap.class);
	 
		private Map<StoreRef, StoreRef> storeArchiveMap;
		
	    public StoreArchiveMap()
	    {
	    	storeArchiveMap = new HashMap<>(0);
	    	logger.info("archiving of Nodes is disabled by this class");
	    }
	    
	    public Map<StoreRef, StoreRef> getArchiveMap()
	    {
	    	return storeArchiveMap;
	    }
	    
	    public void setArchiveMap(Map<String, String> archiveMap)
	    {
	       return;
	    }  
}
