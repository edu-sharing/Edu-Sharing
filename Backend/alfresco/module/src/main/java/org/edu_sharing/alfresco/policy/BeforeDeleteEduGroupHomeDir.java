
package org.edu_sharing.alfresco.policy;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.repository.client.tools.CCConstants;

public class BeforeDeleteEduGroupHomeDir implements NodeServicePolicies.BeforeDeleteNodePolicy {

	PolicyComponent policyComponent;
	AuthorityService authorityService;
	AuthenticationService authenticationService;
	NodeService nodeService;
	
	public static final QName TYPE_MAP = QName.createQName(CCConstants.CCM_TYPE_MAP);
	
	public void init(){
		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, TYPE_MAP , new JavaBehaviour(this, "beforeDeleteNode"));
	}
	
	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {
		if(nodeService.hasAspect(nodeRef, OrganisationService.ASPECT_EDUGROUP_FOLDER)) {
			if(!new Helper(authorityService).isAdmin(authenticationService.getCurrentUserName()) 
					&& !AuthenticationUtil.isRunAsUserTheSystemUser()){
				throw new SystemFolderDeleteDeniedException("you are not allowed to remove this folder!");
			}
		}
	}
	
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
}
