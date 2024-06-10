package org.edu_sharing.service.editlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.EditLockCache;
import org.edu_sharing.service.InsufficientPermissionException;

public class EditLockServiceImpl implements EditLockService {

	Logger logger = Logger.getLogger(EditLockServiceImpl.class);

	public EditLockServiceImpl() {
	}

	@Override
	public boolean isLocked(NodeRef nodeRef) {

		nodeRef = getOriginalNodeRef(nodeRef);
		LockBy lockBy = EditLockCache.get(nodeRef);

		if (lockBy != null) {
			return true;
		}

		return false;
	}

	@Override
	public boolean isLockedByAnotherUser(NodeRef nodeRef) {

		nodeRef = getOriginalNodeRef(nodeRef);
		LockBy lockBy = EditLockCache.get(nodeRef);

		String currentUser = new AuthenticationToolAPI().getCurrentUser();

		if (lockBy != null) {

			String lockUser = lockBy.getUserName();
			if (currentUser != null && !currentUser.equals(lockUser)) {
				return true;
			} else {
				return false;
			}
		}

		return false;
	}

	public void lock(NodeRef nodeRef) throws LockedException, InsufficientPermissionException {

		nodeRef = getOriginalNodeRef(nodeRef);

		if (!isSystemUser() && isLockedByAnotherUser(nodeRef)) {
			LockBy lockBy = getLock(nodeRef);
			throw new LockedException(lockBy);
		}

		try {
			MCAlfrescoAPIClient apiClient = (MCAlfrescoAPIClient) RepoFactory.getInstance(
					ApplicationInfoList.getHomeRepository().getAppId(),
					Context.getCurrentInstance().getRequest().getSession());
			if (!apiClient.hasPermissions(nodeRef.getId(), new String[] { CCConstants.PERMISSION_WRITE })) {
				throw new InsufficientPermissionException("no write permission");
			}
		} catch (InsufficientPermissionException e) {
			throw e;
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			return;
		}
		LockBy lockBy = new LockBy();
		lockBy.setDate(new Date());

		String sessionId = Context.getCurrentInstance().getRequest().getSession().getId();
		logger.debug("locking node for sessionid:" + sessionId);
		lockBy.setSessionId(sessionId);
		lockBy.setUserName(new AuthenticationToolAPI().getCurrentUser());

		EditLockCache.put(nodeRef, lockBy);
	};

	@Override
	public synchronized void unlock(NodeRef nodeRef) throws LockedException {

		nodeRef = getOriginalNodeRef(nodeRef);

		LockBy lockBy = getLock(nodeRef);
		boolean isLocked = isLockedByAnotherUser(nodeRef);

		if (!isSystemUser() && isLocked) {
			throw new LockedException(lockBy);
		}

		try {
			NodeRef finalNodeRef = nodeRef;
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

				@Override
				public Void doWork() throws Exception {
					try {
						MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
						apiClient.createVersion(finalNodeRef.getId());
					}catch(Throwable e){
						logger.error(e.getMessage(), e);
					}
					return null;
				}
			});
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
		EditLockCache.remove(nodeRef);
	}

	public LockBy getLock(NodeRef nodeRef) {
		nodeRef = getOriginalNodeRef(nodeRef);
		return EditLockCache.get(nodeRef);
	}

	@Override
	public void unlockBySession(String sessionId) {

		logger.debug("clearing session for sessionId:" + sessionId);

		synchronized (EditLockCache.cache) {
			List<NodeRef> toRemove = new ArrayList<>();
			for (NodeRef nodeRef : EditLockCache.getKeys()) {
				LockBy lockBy = EditLockCache.get(nodeRef);
				if (sessionId.equals(lockBy.getSessionId())) {
					toRemove.add(nodeRef);
				}
			}

			for (NodeRef nodeRef : toRemove) {
				try {
					unlock(nodeRef);
					// should not happen:
				} catch (LockedException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	private boolean isSystemUser() {
		String user = (String) AuthenticationUtil.getFullyAuthenticatedUser();
		boolean isSystemUser = false;
		if ("admin".equals(user)) {
			isSystemUser = true;
		}
		if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
			isSystemUser = true;
		}
		return isSystemUser;
	}

	private NodeRef getOriginalNodeRef(final NodeRef nodeRef) {
		
		// is called by sessionlistener.sessiondestroyed so use runas method
		NodeRef result = AuthenticationUtil.runAsSystem(new RunAsWork<NodeRef>() {

			@Override
			public NodeRef doWork() throws Exception {
				try {

					MCAlfrescoAPIClient client = new MCAlfrescoAPIClient();
					List<String> aspects = Arrays.asList(client.getAspects(nodeRef.getId()));

					/**
					 * use original
					 */
					if (aspects.contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
						String originalNodeId = client.getProperty(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
								MCAlfrescoAPIClient.storeRef.getIdentifier(), nodeRef.getId(),
								CCConstants.CCM_PROP_IO_ORIGINAL);
						return new NodeRef(MCAlfrescoAPIClient.storeRef, originalNodeId);
					}
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}

				return null;
			}
		});
		
		if (result == null) result = nodeRef;
		return result;
	}

	@Override
	public Collection<NodeRef> getActiveLocks() {
		return EditLockCache.getKeys();
	}

}
