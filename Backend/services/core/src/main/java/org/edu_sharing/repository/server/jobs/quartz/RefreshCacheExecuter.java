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
package org.edu_sharing.repository.server.jobs.quartz;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.MCBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.RepositoryCacheTool;


public class RefreshCacheExecuter {

	protected Log logger = LogFactory.getLog(RefreshCacheExecuter.class);
	
	public void excecute(String rootFolderId, boolean sticky, Map<String, String> authInfo) throws Throwable {
		if (authInfo == null) {
			String userName = ApplicationInfoList.getHomeRepository().getUsername();
			String password = ApplicationInfoList.getHomeRepository().getPassword();
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(ApplicationInfoList.getHomeRepository().getAppId());
			authInfo = authTool.createNewSession(userName, password);
		}

		logger.info("rootFolderId:"+rootFolderId);
		if (rootFolderId == null || rootFolderId.trim().equals("")) {
			
			
			MCBaseClient mcBaseClient = RepoFactory.getInstance(ApplicationInfoList.getHomeRepository().getAppId(), authInfo);
			MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) mcBaseClient;
			String repositoryRoot = mcAlfrescoBaseClient.getRepositoryRoot();
			String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
			Map<String, Object> importFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
					OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
			if (importFolderProps != null) {
				rootFolderId = (String) importFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
			}

		}
		
		if (authInfo != null && rootFolderId != null) {
			RepositoryCacheTool cache = new RepositoryCacheTool();
			if(sticky){
				cache.buildStickyCache(authInfo, rootFolderId);
			}else{
				cache.buildNewCache(authInfo, rootFolderId);
			}
		}
	}
}
