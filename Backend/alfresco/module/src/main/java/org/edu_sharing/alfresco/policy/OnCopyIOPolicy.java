package org.edu_sharing.alfresco.policy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.copy.CopyServicePolicies.OnCopyCompletePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.alfresco.RestrictedAccessException;

public class OnCopyIOPolicy implements OnCopyCompletePolicy, CopyServicePolicies.BeforeCopyPolicy {
	
	PolicyComponent policyComponent;
	
	NodeService nodeService;
	PermissionService permissionService;

	Logger logger = Logger.getLogger(OnCopyIOPolicy.class);
	
	QName versionProp = QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
	
	public void init(){
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCopyComplete"),
               QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCopyComplete"));
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "beforeCopy"),
				QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "beforeCopy"));
	}


	@Override
	public void beforeCopy(QName qName, NodeRef sourceNode, NodeRef targetNode) {
		Boolean restrictedAccess = (Boolean) nodeService.getProperty(sourceNode, QName.createQName(CCConstants.CCM_PROP_RESTRICTED_ACCESS));

		if(restrictedAccess != null && restrictedAccess &&
				!permissionService.hasPermission(sourceNode, CCConstants.PERMISSION_CHANGEPERMISSIONS).equals(AccessStatus.ALLOWED)){
			throw new RestrictedAccessException(sourceNode.getId());
		}
	}

	public static void removeCopiedUsages(NodeService nodeService, NodeRef targetNodeRef) {
		if(!nodeService.getType(targetNodeRef).equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {
			return;
		}
		List<ChildAssociationRef> assocs = nodeService.getChildAssocs(targetNodeRef, Collections.singleton(QName.createQName(CCConstants.CCM_TYPE_USAGE)));
		for(ChildAssociationRef assoc: assocs) {
			Logger.getLogger(OnCopyIOPolicy.class).debug("Deleting usage: " + assoc.getChildRef() + " from node " + assoc.getParentRef());
			nodeService.deleteNode(assoc.getChildRef());
		}
	}
	@Override
	public void onCopyComplete(QName classRef, NodeRef sourceNodeRef, NodeRef targetNodeRef, boolean copyToNewNode, Map<NodeRef, NodeRef> copyMap) {
		logger.info("will set " + versionProp.toPrefixString() + " to 1.0");
		nodeService.setProperty(targetNodeRef, versionProp, "1.0");
		
		QName publishedAspect = QName.createQName(CCConstants.CCM_ASPECT_PUBLISHED);
		
		QName collectionAspect = QName.createQName(CCConstants.CCM_ASPECT_COLLECTION);
		
		ChildAssociationRef childNodeRef = nodeService.getPrimaryParent(targetNodeRef);
		boolean parentIsCollection = nodeService.hasAspect(childNodeRef.getParentRef(), collectionAspect);
		
		if(nodeService.hasAspect(targetNodeRef, publishedAspect) 
				&& !parentIsCollection) {
			nodeService.removeAspect(targetNodeRef, publishedAspect);
		}
		
		//remove old permissionhistory, current entry will be added by edu-sharing NodeDao
		if(nodeService.hasAspect(targetNodeRef, QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY))) {
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
		}

		// remove stats
		if(nodeService.hasAspect(targetNodeRef, QName.createQName(CCConstants.CCM_ASPECT_TRACKING))) {
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_TRACKING_DOWNLOADS));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_TRACKING_VIEWS));
		}

		removeCopiedUsages(nodeService, targetNodeRef);
	}
	
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}
}
