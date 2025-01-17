package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;


/**
 * prevent deleting of system created maps like groupfolder and documents folder
 * @author rudi
 *
 */
public class BeforeMapDeletePolicy implements BeforeDeleteNodePolicy {

	
	NodeService nodeService;
	PolicyComponent policyComponent;
	AuthenticationService authenticationService;
	PersonService personService;
	AuthorityService authorityService;
	
	Logger logger = Logger.getLogger(BeforeMapDeletePolicy.class);
	
	public void init(){
		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "beforeDeleteNode"));
	}
	
	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {
		String mapType = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
		
		if(mapType != null && (mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS) 
				|| mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP) 
				|| mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE))){
			if(AuthenticationUtil.isRunAsUserTheSystemUser()){
				return;
			}
			String currentUser = authenticationService.getCurrentUserName();
			ChildAssociationRef childAssocRef = nodeService.getPrimaryParent(nodeRef);
			NodeRef currentPersonNodeRef = personService.getPerson(currentUser);
			NodeRef homeFolderNodeRef = (NodeRef)nodeService.getProperty(currentPersonNodeRef, ContentModel.PROP_HOMEFOLDER);
			if( homeFolderNodeRef.equals(childAssocRef.getParentRef()) && !new Helper(authorityService).isAdmin(authenticationService.getCurrentUserName()) ){
				throw new SystemFolderDeleteDeniedException("you are not allowed to remove this folder!");
			}
			
		}
	}
	
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
	
	
	
	
	
}
