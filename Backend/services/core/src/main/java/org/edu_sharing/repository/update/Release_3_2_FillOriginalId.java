package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@UpdateService
public class Release_3_2_FillOriginalId {

	private final ArrayList<NodeRef> processedEduGroups = new ArrayList<>();
	private final ArrayList<NodeRef> eduGroupFolderNodeIds = new ArrayList<>();
	private final RetryingTransactionHelper retryingTransactionHelper;
	private final NodeService nodeService;
	private final BehaviourFilter policyBehaviourFilter;


	OrganisationService organisationService;

	//very important: use the alfrescoDefaultDbNodeService defined in custom-core-services-context.xml
	//cause of overwriten getChild... methods in org.edu_sharing.alfresco.fixes.DbNodeServiceImpl
	//this can lead to a problem, that every edugroupfolder is processed for all members of the edugroup again
	@Autowired
	public Release_3_2_FillOriginalId(RetryingTransactionHelper retryingTransactionHelper, @Qualifier("alfrescoDefaultDbNodeService") NodeService nodeService, @Qualifier("policyBehaviourFilter")BehaviourFilter policyBehaviourFilter, OrganisationService organisationService ) {
		this.retryingTransactionHelper = retryingTransactionHelper;
		this.nodeService = nodeService;
		this.policyBehaviourFilter = policyBehaviourFilter;
		this.organisationService = organisationService;

		try{
			for(Map<QName, Serializable> orgProps : organisationService.getOrganisations()){
				NodeRef eduGroupFolderNodeId = (NodeRef)orgProps.get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
				eduGroupFolderNodeIds.add(eduGroupFolderNodeId);
			}
		}catch(Throwable e){
			throw new RuntimeException(e.getMessage());
		}
	}



	@UpdateRoutine(
			id="Release_3_2_FillOriginalId",
			description = "remembers original id for colletion ref objects to use solr grouping",
			order = 3201,
			auto = true
	)
	public void execute(boolean test){
		setOrignalIds(nodeService.getRootNode(MCAlfrescoAPIClient.storeRef), test);
	}
	
	void setOrignalIds(NodeRef parent, boolean test) {
		List<ChildAssociationRef> childAssocRef = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef child : childAssocRef){
			NodeRef noderef = child.getChildRef();
			String name = (String)nodeService.getProperty(child.getChildRef(),ContentModel.PROP_NAME);
			String nodeType = nodeService.getType(child.getChildRef()).toString();
			if(CCConstants.CCM_TYPE_IO.equals(nodeType)){
				log.info("updateing node:"+ noderef +" in "+ nodeService.getPath(child.getChildRef()));
				if(!test){
					retryingTransactionHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
						policyBehaviourFilter.disableBehaviour(child.getChildRef());
						nodeService.setProperty(child.getChildRef(), QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL), noderef.getId());
						policyBehaviourFilter.enableBehaviour(child.getChildRef());
						return null;
					});
					
					
				}
			}else if(CCConstants.CCM_TYPE_MAP.equals(nodeType) 
					|| CCConstants.CM_TYPE_FOLDER.equals(nodeType)){
				if(eduGroupFolderNodeIds.contains(noderef)){
					if(processedEduGroups.contains(noderef)){
						log.info("already processed edugroup: " + name);
						continue;
					}else{
						log.info("remembering edugroup: "+name+ " " + noderef);
						processedEduGroups.add(noderef);
					}
				}
				setOrignalIds(noderef, test);
			}
		}
	}
}