package org.edu_sharing.service.repoproxy;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import java.util.HashMap;

public abstract class RepoProxyAbstract implements RepoProxy {

	public boolean myTurn(String repoId) {
		String homeRepo = ApplicationInfoList.getHomeRepository().getAppId();
		if(repoId.equals(RepositoryDao.HOME)) {
			repoId = homeRepo;
		}
		// validate if user is allowed to access data from this repo
		if(!ToolPermissionServiceFactory.getInstance().hasToolPermissionForRepository(repoId)) {
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX + repoId);
		}

		if (homeRepo.equals(repoId)) {
			return false;
		}
		ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(repoId);

		if (repoInfo != null && repoInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_ALFRESCO)) {
			return true;
		}
		
		return false;
	}

	/**
	 * returns a remote node id if the repo is switched, otherwise null (then it's a local node)
	 * @param repoId
	 * @param nodeRef
	 * @return
	 */
	public RemoteRepoDetails myTurn(String repoId, NodeRef nodeRef) {
		if(myTurn(repoId)){
			return new RemoteRepoDetails(repoId, nodeRef.getId());
		}
		try{
			if(NodeServiceHelper.hasAspect(nodeRef, CCConstants.CCM_ASPECT_REMOTEREPOSITORY)){
				String remoteRepo = NodeServiceHelper.getProperty(nodeRef, CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID);
				if(myTurn(remoteRepo)){
					String remoteNode = NodeServiceHelper.getProperty(nodeRef, CCConstants.CCM_PROP_REMOTEOBJECT_NODEID);
					return new RemoteRepoDetails(remoteRepo, remoteNode);
				}
			}
		} catch (Throwable ignored){ }
		return null;
	}
	public RemoteRepoDetails myTurn(String repoId, String node) {
		return myTurn(repoId, node == null ? null : new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node));
	}
	
	@Override
	public HashMap<String, String> remoteAuth(ApplicationInfo repoInfo, String username, boolean validate) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}
