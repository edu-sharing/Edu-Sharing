package org.edu_sharing.service.comment;

import java.util.*;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;
import org.edu_sharing.service.notification.Status;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;

public class CommentServiceImpl implements CommentService{

	private NodeService nodeService;
	private PermissionService permissionService;
	private NotificationService notificationService;

	public CommentServiceImpl() {
		this.nodeService=NodeServiceFactory.getLocalService();
		this.permissionService=PermissionServiceFactory.getLocalService();
		this.notificationService = NotificationServiceFactoryUtility.getLocalService();
	}
	
	@Override
	public String addComment(String node,String commentReference, String comment) throws Exception {
		ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_COMMENT_WRITE);
		Map<String, Object> props = new HashMap<>();
		if(StringUtils.isNotBlank(commentReference)) {
			props.put(CCConstants.CCM_PROP_COMMENT_REPLY,new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, commentReference));
		}
		props.put(CCConstants.CM_NAME,"childcomment");
		props.put(CCConstants.CCM_PROP_COMMENT_CONTENT,comment);
		boolean permission = permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),node,CCConstants.PERMISSION_COMMENT);
		if(!permission) {
			throw new InsufficientPermissionException("No permission '"+CCConstants.PERMISSION_COMMENT+"' to add comments to node "+node);
		}

		String nodeId = AuthenticationUtil.runAsSystem(() -> {
			String nodeId1 = nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node, CCConstants.CCM_TYPE_COMMENT, CCConstants.CCM_ASSOC_COMMENT, props);
			permissionService.setPermissions(nodeId1, null, true);
			new RepositoryCache().remove(node);
			return nodeId1;
		});

		notify(node, comment, commentReference, Status.ADDED);
		return nodeId;
	}

	@Override
	public List<ChildAssociationRef> getComments(String node) {
		return this.nodeService.getChildrenChildAssociationRefType(node,CCConstants.CCM_TYPE_COMMENT);
	}

	@Override
	public void editComment(String commentId, String comment) {
		throwIfNoComment(commentId);
		Map<String, Object> props = new HashMap<>();
		props.put(CCConstants.CCM_PROP_COMMENT_CONTENT,comment);
		NodeRef replyTo = new NodeRef(nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), commentId, CCConstants.CCM_PROP_COMMENT_REPLY));
		nodeService.updateNodeNative(commentId, props);
		String parentNode = nodeService.getPrimaryParent(commentId);

		notify(parentNode, comment, replyTo.getId(), Status.CHANGED);
	}

	private void throwIfNoComment(String commentId) {
		if(!nodeService.getType(commentId).equals(CCConstants.CCM_TYPE_COMMENT)){
			throw new IllegalArgumentException("Node "+commentId+" is not a comment");
		}
	}

	@Override
	public void deleteComment(String commentId) {
		throwIfNoComment(commentId);
		String parentNode = nodeService.getPrimaryParent(commentId);
		String comment = nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), commentId, CCConstants.CCM_TYPE_COMMENT);
		NodeRef replyTo = new NodeRef(nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), commentId, CCConstants.CCM_PROP_COMMENT_REPLY));

		new RepositoryCache().remove(parentNode);
		nodeService.removeNode(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),commentId);

		notify(parentNode, comment, replyTo.getId(), Status.REMOVED);
	}

	private void notify(String node, String comment, String commentReference, Status status) {
		String nodeType = null;
		List<String> aspects;
		Map<String, Object> nodeProps;
		try {
			nodeType = nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), node);
			aspects = Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), node));
			nodeProps = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), node);
		} catch (Throwable e) {
			nodeProps = new HashMap<>();
			aspects = new ArrayList<>();
		}

		notificationService.notifyComment(node, comment, commentReference, nodeType, aspects, nodeProps, status);
	}

	
}
