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

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;


public class HasPermissionsWork implements AuthenticationUtil.RunAsWork<java.util.Map<String,Boolean>>{
	
	Logger logger = Logger.getLogger( HasPermissionsWork.class);
	String userId = null;
	String[] permissions = null;
	String nodeId = null;
	
	StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	PermissionService permissionService = null;
	
	public HasPermissionsWork(PermissionService _permissionService, String _userId, String[] _permissions, String _nodeId) {
		userId = _userId;
		permissions = _permissions;
		nodeId = _nodeId;
		permissionService = _permissionService;
	}
	@Override
	public Map<String,Boolean> doWork() throws Exception {
		Map<String,Boolean> result = new HashMap<>();
		for(String permission:permissions){
			
			logger.debug("NEWnodeId:"+nodeId);
			logger.debug("permission:"+permission);
			logger.debug("storeRef:"+storeRef);
			
			NodeRef nodeRef = new NodeRef(storeRef, nodeId);
			logger.debug("nodeRef:"+nodeRef);
			
			AccessStatus accessStatus = permissionService.hasPermission(nodeRef, permission);
			
			logger.debug("accessStatus:"+accessStatus);
			
			
			if(accessStatus.equals(AccessStatus.ALLOWED)){
				result.put(permission, new Boolean(true));
			}else{
				result.put(permission, new Boolean(false));
			}
		}
		
		return result;
	}
}
