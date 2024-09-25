package org.edu_sharing.alfresco.policy;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnMoveNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.repository.client.tools.CCConstants;

public class OnCreateNodePolicyOrgAdministrators implements OnCreateNodePolicy, OnMoveNodePolicy {

	PermissionService permissionService;
	
	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	ServiceRegistry serviceRegistry;
	
	SearchService searchService;
	
	Repository repositoryHelper;
	
	public void init() {
		
		//OnCreateNode
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onCreateNode"));
		
		//OnMoveNode
		policyComponent.bindClassBehaviour(OnMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onMoveNode"));
		policyComponent.bindClassBehaviour(OnMoveNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "onMoveNode"));
		policyComponent.bindClassBehaviour(OnMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onMoveNode"));
		policyComponent.bindClassBehaviour(OnMoveNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onMoveNode"));
		
		searchService = serviceRegistry.getSearchService();
	}
	
	
	@Override
	public void onMoveNode(ChildAssociationRef parentRef, ChildAssociationRef childRef) {
		String adminGroup = getAdminGroup(childRef);
		if(adminGroup != null && !adminGroup.trim().equals("")) {
			permissionService.setPermission(childRef.getChildRef(), adminGroup, PermissionService.COORDINATOR, true);
		}
	}
	
	
	@Override
	public void onCreateNode(ChildAssociationRef childRef) {
		String adminGroup = getAdminGroup(childRef);
		if(adminGroup != null && !adminGroup.trim().equals("")) {
			permissionService.setPermission(childRef.getChildRef(), adminGroup, PermissionService.COORDINATOR, true);
		}
	}
	
	private String getAdminGroup(ChildAssociationRef childRef) {
		NodeRef companyHome = repositoryHelper.getCompanyHome();

		NodeRef currentNode = childRef.getParentRef();
		NodeRef organisationNode = null;
		while(!companyHome.equals(currentNode) && organisationNode == null && currentNode != null){
			
			if(nodeService.hasAspect(currentNode, OrganisationService.ASPECT_EDUGROUP_FOLDER)){
				organisationNode = currentNode;
				break;
			}
			
			currentNode = nodeService.getPrimaryParent(currentNode).getParentRef();
		}
		
		if(organisationNode != null){
			NodeRef orgNodeRef = (NodeRef)nodeService.getProperty(organisationNode,OrganisationService.PROP_EDUGROUP_FOLDER_ORGANISATION);
			Map<QName, Serializable> eduGroupProps = nodeService.getProperties(orgNodeRef);
			NodeRef eduGroupNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, (String)eduGroupProps.get(ContentModel.PROP_NODE_UUID));
			List<ChildAssociationRef> childGroups = nodeService.getChildAssocs(eduGroupNodeRef);
			for(ChildAssociationRef childGroup : childGroups){
				String grouptype = (String)nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
				if(CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(grouptype)){
					String authorityName = (String)nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
					return authorityName;
				}
			}
		}
		return null;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repositoryHelper = repositoryHelper;
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
