package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.*;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.guest.GuestService;

public class GuestCagePolicy implements BeforeCreateNodePolicy, BeforeDeleteAssociationPolicy,
										BeforeDeleteChildAssociationPolicy,BeforeDeleteNodePolicy,BeforeMoveNodePolicy,
										BeforeRemoveAspectPolicy,BeforeSetNodeTypePolicy,BeforeUpdateNodePolicy{
	
	PolicyComponent policyComponent;


	GuestService guestService;

	public GuestCagePolicy() {
	}
	
	public void init() {
		policyComponent.bindClassBehaviour(BeforeCreateNodePolicy.QNAME, ContentModel.TYPE_BASE, new JavaBehaviour(this, "beforeCreateNode"));
		policyComponent.bindClassBehaviour(BeforeDeleteAssociationPolicy.QNAME, ContentModel.TYPE_BASE, new JavaBehaviour(this, "beforeDeleteAssociation"));
		policyComponent.bindClassBehaviour(BeforeDeleteChildAssociationPolicy.QNAME, ContentModel.TYPE_BASE, new JavaBehaviour(this, "beforeDeleteChildAssociation"));
		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, ContentModel.TYPE_BASE, new JavaBehaviour(this, "beforeDeleteNode"));
		policyComponent.bindClassBehaviour(BeforeMoveNodePolicy.QNAME, ContentModel.TYPE_BASE, new JavaBehaviour(this, "beforeMoveNode"));
		policyComponent.bindClassBehaviour(BeforeRemoveAspectPolicy.QNAME, ContentModel.TYPE_BASE, new JavaBehaviour(this, "beforeRemoveAspect"));
		policyComponent.bindClassBehaviour(BeforeSetNodeTypePolicy.QNAME, ContentModel.TYPE_BASE, new JavaBehaviour(this, "beforeSetNodeType"));
		policyComponent.bindClassBehaviour(BeforeUpdateNodePolicy.QNAME, ContentModel.TYPE_BASE, new JavaBehaviour(this, "beforeUpdateNode"));
	}
	
	
	public static class GuestPermissionDeniedException extends RuntimeException{

		public GuestPermissionDeniedException(String message) {
			super(message);
		}
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		
	}


	private void checkGuest() throws GuestPermissionDeniedException {
		if(AuthenticationUtil.getFullyAuthenticatedUser() != null
				&& guestService.isGuestUser(AuthenticationUtil.getFullyAuthenticatedUser())
				&& guestService.isGuestUser(AuthenticationUtil.getRunAsUser())){
			throw new GuestPermissionDeniedException("guest has no permissions to do that");
		}
	}
	
	@Override
	public void beforeCreateNode(NodeRef arg0, QName arg1, QName arg2, QName arg3) {
		checkGuest();
		
	}
	
	
	@Override
	public void beforeDeleteAssociation(AssociationRef arg0) {
		checkGuest();
		
	}
	
	@Override
	public void beforeDeleteChildAssociation(ChildAssociationRef arg0) {
		checkGuest();
		
	}
	
	@Override
	public void beforeDeleteNode(NodeRef arg0) {
		checkGuest();
		
	}
	
	@Override
	public void beforeMoveNode(ChildAssociationRef arg0, NodeRef arg1) {
		checkGuest();
		
	}
	
	@Override
	public void beforeRemoveAspect(NodeRef arg0, QName arg1) {
		checkGuest();
		
	}
	
	@Override
	public void beforeSetNodeType(NodeRef arg0, QName arg1, QName arg2) {
		checkGuest();
		
	}
	
	@Override
	public void beforeUpdateNode(NodeRef arg0) {
		checkGuest();
		
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setGuestService(GuestService guestService) {
		this.guestService = guestService;
	}
}
