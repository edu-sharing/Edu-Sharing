package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.UserCache;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * create the edu-sharing default folders like "documents" and "images"
 * 
 * @author rudi
 *
 */
public class OnUpdatePersonPropertiesPolicy implements OnCreateNodePolicy, OnUpdateNodePolicy, OnUpdatePropertiesPolicy{

	
	NodeService nodeService;
	PolicyComponent policyComponent;
	ServiceRegistry serviceRegistry;
	
	Logger logger = Logger.getLogger(OnUpdatePersonPropertiesPolicy.class);
	private UserCache userCache;

	public static ThreadLocal<Boolean> constructPersonFolders = new ThreadLocal<>();

	public void init(){
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnUpdateNodePolicy.QNAME, ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onUpdateNode"));
		policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onUpdateProperties"));
		userCache = AlfAppContextGate.getApplicationContext().getBean(UserCache.class);
	}
	
	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		logger.debug("called");
		//NodeRef nodeRef = childAssocRef.getChildRef();
		//new HomeFolderTool(serviceRegistry).constructPersonFolders(nodeRef);
	}
	
	
	@Override
	public void onUpdateNode(NodeRef nodeRef) {
		logger.debug("called");
		
	}
	
	/**
	 * will do the edu folder creation in "onUpdateProperties" instead of "onCreateNode"
	 * 
	 * cause we can not rely on that homeFolder prop is already set on creation
	 * because
	 * 1. sometimes the  onUpdateProperties Method will be called before onCreateNode
	 * 2. sometimes after onCreateNode
	 * 
	 * -> 2. will not have a homeFolder prop
	 */
	
	@Override
	public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {

		//NodeRef homefolder = (NodeRef) nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);
		String username = (String)after.get(ContentModel.PROP_USERNAME);
		userCache.refresh(username);
		logger.debug("username:"+username);
		NodeRef homeFolderNodeRef = (NodeRef)before.get(ContentModel.PROP_HOMEFOLDER);

		if(!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
			QName prop = QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS);
			String personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
			if(personActiveStatus != null && !personActiveStatus.trim().isEmpty()){
				if(serviceRegistry.getNodeService().getProperty(nodeRef,prop) == null && !after.containsKey(prop)){
					serviceRegistry.getNodeService().setProperty(nodeRef, prop, personActiveStatus);
				}
			}
		}
		
		if(homeFolderNodeRef != null){
			logger.debug("will do nothing cause HomeFolder exsisted before this update");
			return;
		}
			
		homeFolderNodeRef = (NodeRef)after.get(ContentModel.PROP_HOMEFOLDER);
		if(homeFolderNodeRef == null){
			logger.debug("will do nothing cause homeFolder is not present in after update props");
			return;
		}

		//create esuid that will be used for user creation in remote repositories
		createESUIDIfNotExists(nodeService, nodeRef);

		if(constructPersonFolders.get() != null && constructPersonFolders.get() == false){
			logger.debug("thread local constructPersonFolders is false. will skip constructPersonFolders.");
			return;
		}
		
		logger.debug("will create edu folders in userhome");
		new HomeFolderTool(serviceRegistry).constructPersonFolders(nodeRef);
		
		if(HttpContext.getCurrentMetadataSet() != null) {
			nodeService.setProperty(homeFolderNodeRef, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET), HttpContext.getCurrentMetadataSet());
		}
	}

	public static boolean createESUIDIfNotExists(NodeService nodeService, NodeRef nodeRef) {
		UUID uuid = UUID.randomUUID();
		QName esUidQName =  QName.createQName(CCConstants.PROP_USER_ESUID);
		if(nodeService.getProperty(nodeRef, esUidQName) == null){
			nodeService.setProperty(nodeRef, esUidQName, uuid.toString());
			return true;
		}
		return false;
	}


	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
