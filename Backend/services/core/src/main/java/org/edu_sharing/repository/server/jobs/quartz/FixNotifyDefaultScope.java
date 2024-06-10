package org.edu_sharing.repository.server.jobs.quartz;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class FixNotifyDefaultScope extends AbstractJob {
	
	Logger logger = Logger.getLogger(FixNotifyDefaultScope.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");
	
	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
	
	PrintWriter out;
	ArrayList<NodeRef> processedEduGroups = new ArrayList<>();
	ArrayList<NodeRef> eduGroupFolderNodeIds = new ArrayList<>();
	
	int processedNodeCounter = 0;
	
	public static final String PARAM_ROOT_NODE = "ROOT_NODE";
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String rootNodeId = (String)context.getJobDetail().getJobDataMap().get(PARAM_ROOT_NODE);
		if(rootNodeId == null) {
			logger.error("no " +PARAM_ROOT_NODE+" provided");
			return;
		}
		
		
		AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
				@Override
				public Void doWork() throws Exception {
					try {
						setDefautScope(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,rootNodeId),false);
					return null;
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						logger.error(e.getMessage(),e);
					}
					return null;
				}
			};
			AuthenticationUtil.runAsSystem(runAs);
		
	}
	
	@Override
	public Class[] getJobClasses() {
		this.addJobClass(FixNotifyDefaultScope.class);
		return allJobs;
	}
	
	
	void setDefautScope(NodeRef parent, boolean test) throws Throwable{
		List<ChildAssociationRef> childAssocRef = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef child : childAssocRef){
			NodeRef noderef = child.getChildRef();
			String name = (String)nodeService.getProperty(child.getChildRef(),ContentModel.PROP_NAME);
			String nodeType = nodeService.getType(child.getChildRef()).toString();
			
			logger.info("name:" + name +" nodeType:" +nodeType +" noderef:" +noderef);
			
			
			
			if(CCConstants.CCM_TYPE_NOTIFY.equals(nodeType)){
				logger.info("updateing node:"+ noderef +" in "+ nodeService.getPath(child.getChildRef()) +" processedNodeCounter:" + processedNodeCounter);
				
				if(nodeService.hasAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE))){
					logger.info("already processed node");
					continue;
				}
				
				if(!test){
					
					Map<QName,Serializable> aspectProps = new HashMap<>();
					aspectProps.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), null);
					
					
					serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
						@Override
						public Void execute() throws Throwable {
							policyBehaviourFilter.disableBehaviour(child.getChildRef());
							nodeService.addAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), aspectProps);
							policyBehaviourFilter.enableBehaviour(child.getChildRef());
							return null;
						}
					});
					
				}
			}else if(CCConstants.CCM_TYPE_MAP.equals(nodeType) || CCConstants.CM_TYPE_FOLDER.equals(nodeType) ){
				logger.info("updateing Map:"+ noderef +" in "+ nodeService.getPath(child.getChildRef()));
				
				if(!test){
					Map<QName,Serializable> aspectProps = new HashMap<>();
					aspectProps.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), null);
					serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
						@Override
						public Void execute() throws Throwable {
							if(nodeService.hasAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE))){
								logger.info("already processed node");
								return null;
							}
							policyBehaviourFilter.disableBehaviour(child.getChildRef());
							nodeService.addAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), aspectProps);
							policyBehaviourFilter.enableBehaviour(child.getChildRef());
							return null;
						}
					});
				}
				
				if(eduGroupFolderNodeIds.contains(noderef)){
					if(processedEduGroups.contains(noderef)){
						if(this.out != null) this.out.println("already processed edugroup: " + name);
						logger.info("already processed edugroup: " + name);
						continue;
					}else{
						logger.info("remembering edugroup: "+name+ " " + noderef);
						processedEduGroups.add(noderef);
					}
				}
				
				setDefautScope(noderef, test);
			}
			processedNodeCounter++;
			
		}
	}

}
