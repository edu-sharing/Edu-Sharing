package org.edu_sharing.service.authentication;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.EduGroup;

public interface ScopeUserHomeService {
	
	public NodeRef getUserHome(String username, String scope, boolean createIfNotExists);

	EduGroup getOrCreateScopedEduGroup(String authority, String scope);

	public void setManageEduGroupFolders(boolean manageEduGroupFolders);
	
	public boolean isManageEduGroupFolders();

}
