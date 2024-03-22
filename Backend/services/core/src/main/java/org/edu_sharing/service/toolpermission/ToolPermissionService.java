package org.edu_sharing.service.toolpermission;

import jakarta.servlet.http.HttpSession;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.connector.Connector;
import org.edu_sharing.alfresco.service.connector.ConnectorList;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionBaseService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.connector.ConnectorServiceFactory;

import java.util.*;

public class ToolPermissionService extends ToolPermissionBaseService {
	private Logger logger = Logger.getLogger(ToolPermissionService.class);
	org.edu_sharing.service.nodeservice.NodeService eduNodeService;
	// list of old TP (key) and target(s) to migrate it to
	static Map<String, Collection<String>> MIGRATE_TP_LIST = new HashMap<>();
	static {
		MIGRATE_TP_LIST.put("TOOLPERMISSION_COLLECTION_FEEDBACK", Collections.singletonList(CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK));
		MIGRATE_TP_LIST.put("TOOLPERMISSION_RATE",Arrays.asList(CCConstants.CCM_VALUE_TOOLPERMISSION_RATE_READ, CCConstants.CCM_VALUE_TOOLPERMISSION_RATE_WRITE));
	}

	public void setEduNodeService(org.edu_sharing.service.nodeservice.NodeService eduNodeService) {
		this.eduNodeService = eduNodeService;
	}
	/**
	 * Clears previously stored tool permissions in the current http session, e.g. when user changes
	 */
	public void invalidateSessionCache() {
		try{
			HttpSession session = Context.getCurrentInstance().getRequest().getSession();
			for(String tp : this.getAllToolPermissions(false)){
				session.removeAttribute(tp);
			}
		}catch(Throwable t){
			// may fails when no session is active, not an issue
		}
	}

	public boolean hasToolPermissionForConnector(String connectorId){
		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
		String scope=authTool.getScope();
		if(scope==null)
			scope="";
		else
			scope="_"+scope;
		return hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX + connectorId+scope);
	}

	public boolean hasToolPermissionForRepository(String repositoryId){
		if(!ApplicationInfoList.getAppInfoIds().contains(repositoryId)) {
			throw new IllegalArgumentException("Repository with id " + repositoryId + " is unknown");
		}
		return hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX + repositoryId);
	}

	public void init(){
		try {
			migrateToolpermissions();
			initToolPermissions(getAllPredefinedToolPermissions());
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	private void migrateToolpermissions() {
		MIGRATE_TP_LIST.forEach((oldTp, newTp) -> {
			String oldTpId = null;
			try {
				oldTpId = getToolPermissionNodeId(oldTp, false);
				if(oldTpId == null) {
					logger.debug(oldTpId + " does not exist anymore, skip migration");
					return;
				}
				Set<AccessPermission> oldPermissions = serviceRegistry.getPermissionService().getAllSetPermissions(
						new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, oldTpId)
				);
				oldPermissions.forEach((p) -> {
					logger.info("Old permission " + oldTp + ": " + p.getAuthority() + ", " + p.getPermission());
				});
				if(newTp.stream().allMatch((tp) -> {

					boolean exists = false;
					try {
						exists = getToolPermissionNodeId(tp, false) != null;
					} catch (Throwable e) {
						logger.error(e);
					}
					if (exists) {
						logger.warn("Can not migrate tp " + oldTp + " to " + tp + ": new tp already exists");
					} else {
						try {
							String newTpId = getToolPermissionNodeId(tp, true);
							logger.info("created new tp successfully: " + tp);
							PermissionService permissionsService = this.serviceRegistry.getPermissionService();
							// @TODO: we could maybe copy the permissions of the old to the new one
							return true;
						} catch (Throwable e) {
							logger.error(e);
						}
					}
					return false;
				})) {
					logger.info("tp " + oldTp +" migrated successfully, deleting old tp");
					nodeService.deleteNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, oldTpId));
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	protected void addConnectorToolpermissions(List<String> toInit) {
		ConnectorList connectorList =  ConnectorServiceFactory.getConnectorList(this);
		for(Connector c : connectorList.getConnectors()){
			String tp = CCConstants.CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX + c.getId();
			toInit.add(tp);

			String tp_safe = tp + "_safe";
			toInit.add(tp_safe);
		}
	}
}
