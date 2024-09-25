package org.edu_sharing.service.permission;

import java.util.*;
import java.util.stream.Collectors;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.model.PermissionModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

public class PermissionServiceHelper {
	private static ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
	private static PermissionModel permissionModel = (PermissionModel) alfApplicationContext.getBean("permissionsModelDAO");

	/**
	 * permission list resolved by elastic
	 */
	public static final String[] PERMISSIONS = new String[] {
			 org.alfresco.service.cmr.security.PermissionService.READ,
			 CCConstants.PERMISSION_READ_PREVIEW,
			 CCConstants.PERMISSION_READ_ALL,
			 CCConstants.PERMISSION_COMMENT,
			 CCConstants.PERMISSION_EMBED,
			 CCConstants.PERMISSION_FEEDBACK,
			 CCConstants.PERMISSION_RATE_READ,
			 CCConstants.PERMISSION_DOWNLOAD_CONTENT,
			 org.alfresco.service.cmr.security.PermissionService.ADD_CHILDREN,
			 org.alfresco.service.cmr.security.PermissionService.READ_PERMISSIONS,
			 org.alfresco.service.cmr.security.PermissionService.CHANGE_PERMISSIONS,
			 org.alfresco.service.cmr.security.PermissionService.READ_CHILDREN,
			 org.alfresco.service.cmr.security.PermissionService.DELETE_CHILDREN,
			 org.alfresco.service.cmr.security.PermissionService.WRITE,
			 org.alfresco.service.cmr.security.PermissionService.DELETE,
			 CCConstants.PERMISSION_CC_PUBLISH};
	private PermissionService permissionService;
		public PermissionServiceHelper(PermissionService permissionService){
			this.permissionService=permissionService;
		}

    public static HashSet<String> getExplicitAuthoritiesFromACL(ACL acl) {
        return Arrays.stream(acl.getAces()).
                filter((ace) -> !ace.isInherited()).
                map(ACE::getAuthority).
                collect(Collectors.toCollection(HashSet::new));
    }

	public static Boolean isNodePublic(NodeRef nodeRef) {
		return AuthenticationUtil.runAs(() -> hasPermission(nodeRef, CCConstants.PERMISSION_READ), org.alfresco.service.cmr.security.PermissionService.GUEST_AUTHORITY);
	}

	/**
	 * resolves all indirect permissions a permission is granting
	 * i.e. ReadAll -> Read, ReadPreview, ReadContent...
	 * @param perm
	 * @return
	 */
    public static Set<String> getAllIncludingPermissions(String perm) {
		PermissionReference pr = permissionModel.getPermissionReference(null, perm);
		return permissionModel.getGranteePermissions(pr).stream().map(PermissionReference::getName).collect(Collectors.toUnmodifiableSet());
	}

    public void validatePermissionOrThrow(String nodeId, String permissionName) {
		if(!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,permissionName))
			throw new PermissionException(nodeId,permissionName);
	}

	public Map<String, Boolean> hasAllPermissions(String storeProtocol,String storeId,String nodeId){
			return permissionService.hasAllPermissions(storeProtocol, storeId, nodeId, PERMISSIONS);
		}
		public Map<String, Boolean> hasAllPermissions(String nodeId){
			return hasAllPermissions(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
		}
		public static List<String> getPermissionsAsString(Map<String,Boolean> hasPermissions){
			List<String> result = new ArrayList<>();

			for (String permission : PERMISSIONS) {

				if(hasPermissions.get(permission) == null || 
						 !hasPermissions.get(permission))
					continue;
				result.add(permission);
			}

			return result;
			
		}
	public static boolean hasPermission(NodeRef nodeRef, String permission){
			return PermissionServiceFactory.getLocalService().hasPermission(nodeRef.getStoreRef().getProtocol(),
					nodeRef.getStoreRef().getIdentifier(),
					nodeRef.getId(),
					permission);
	}
	public static boolean hasPermission(NodeRef nodeRef, String authority, String permission){
		return PermissionServiceFactory.getLocalService().hasPermission(nodeRef.getStoreRef().getProtocol(),
				nodeRef.getStoreRef().getIdentifier(),
				nodeRef.getId(),
				authority,
				permission);
	}
}
