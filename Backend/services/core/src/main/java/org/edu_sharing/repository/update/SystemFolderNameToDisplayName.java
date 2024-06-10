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
package org.edu_sharing.repository.update;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@UpdateService
public class SystemFolderNameToDisplayName {
	
	private final NodeService nodeService;
	private final AuthorityService authorityService;
	private final MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

	int counter = 0;

	@Autowired
	public SystemFolderNameToDisplayName(NodeService nodeService, AuthorityService authorityService) {
		this.nodeService = nodeService;
		this.authorityService = authorityService;
	}

	@UpdateRoutine(
			id = "SystemFolderNameToDisplayName",
			description = "UPDATE AUF 1.4.0. System Ordner des Users (Favoritenordner, Meine Gruppen, Meine Dateien) bekommen ein lesbares CM_NAME Property. Dieses Update muss durchgeführt werden bevor User das System Nutzen sonst werden neue Systemordner angelegt.",
			order = 1401
	)
	public void execute(boolean test) {
		log.info("test:" + test);
		try {

			Set<String> users = authorityService.getAllAuthorities(AuthorityType.USER);
			for (String user : users) {

				String homefolderId = mcAlfrescoBaseClient.getHomeFolderID(user);
				Map<String, Map<String, Object>> children = mcAlfrescoBaseClient.getChildren(homefolderId);
				for (Map.Entry<String, Map<String, Object>> entry : children.entrySet()) {
					String name = (String) entry.getValue().get(CCConstants.CM_NAME);
					String type = (String) entry.getValue().get(CCConstants.NODETYPE);
					String folderId = (String) entry.getValue().get(CCConstants.SYS_PROP_NODE_UID);
					if (type.equals(CCConstants.CCM_TYPE_MAP) && name.equals(CCConstants.BASKETS_PARENT_PREFIX + user)) {

						log.info("updateing user:" + user + " folderId:" + folderId + " folderName" + name);

						//get an alfresco installation locale corresponding name
						String userFavoritesFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_USERFOLDER_FAVORITES);
						updateFolder(folderId, children, CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE, userFavoritesFolderName, test);

					}

					if (type.equals(CCConstants.CM_TYPE_FOLDER) && name.equals(CCConstants.CC_DEFAULT_USER_DATA_FOLDER_NAME)) {
						log.info("updateing user:" + user + " folderId:" + folderId + " folderName" + name);
						//nodeService.getS
						log.info("setting type to Map for folderId:"+folderId);
						if(!test){
							nodeService.setType(new NodeRef(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),folderId), QName.createQName(CCConstants.CCM_TYPE_MAP));
						}
						
						updateFolder(folderId, children, CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS, I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_USERFOLDER_DOCUMENTS), test);
					}
					
					
					if (type.equals(CCConstants.CCM_TYPE_MAP) && name.equals("DEFAULTGROUPSFOLDER")) {
						log.info("updateing user:" + user + " folderId:" + folderId + " folderName" + name);
						
						//get an alfresco installation locale corresponding name
						String userGroupsFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_USERFOLDER_GROUPS);
						updateFolder(folderId, children, CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP, userGroupsFolderName, test);
					}

				}

			}
			
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		log.info("nr of updated objects:" + counter);
	}

	private void updateFolder(String folderId, Map<String,Map<String,Object>> suroundingNodes, String mapType, String displayName, boolean test) throws Exception {

		Map<String,Object> newProps = new HashMap<>();

		String uniqueValue = new DuplicateFinder().getUniqueValue(suroundingNodes, folderId, CCConstants.CM_NAME, displayName);
		newProps.put(CCConstants.CM_NAME, uniqueValue);
		newProps.put(CCConstants.CM_PROP_C_TITLE, uniqueValue);
		newProps.put(CCConstants.CCM_PROP_MAP_TYPE, mapType);

		log.info("folderId:" + folderId + " setting uniqueValue for title and name:" + uniqueValue + " and " + CCConstants.CCM_PROP_MAP_TYPE + " " + newProps.get(CCConstants.CCM_PROP_MAP_TYPE));
		if (!test) {
			mcAlfrescoBaseClient.updateNode(folderId, newProps);
		}
		counter++;
	}
}
