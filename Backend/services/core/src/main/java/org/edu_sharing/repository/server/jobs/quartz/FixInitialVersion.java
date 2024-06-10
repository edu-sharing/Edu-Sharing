package org.edu_sharing.repository.server.jobs.quartz;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class FixInitialVersion extends AbstractJob {

	Logger logger = Logger.getLogger(FixInitialVersion.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	VersionService versionService = serviceRegistry.getVersionService();
	SearchService searchService = serviceRegistry.getSearchService();
	
	NodeService nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");
	
	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
	
	private static final int PAGE_SIZE = 100;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				execute(0);
				return null;
			}
		};
		AuthenticationUtil.runAsSystem(runAs);
		
		
	}
	
	private void execute(int page) {
		logger.info("page:" + page);
		SearchParameters sp = new SearchParameters();
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		sp.setSkipCount(page);
		sp.setMaxItems(PAGE_SIZE);
		
		sp.setQuery("ISUNSET:\"cclom:version\" AND TYPE:\"ccm:io\"");
		
		logger.info("query:" + sp.getQuery());
		ResultSet resultSet = searchService.query(sp);
		
		
		logger.info("page " + page + " from " + resultSet.getNumberFound());
		
		for(NodeRef nodeRef : resultSet.getNodeRefs()) {
			
			VersionHistory vh = versionService.getVersionHistory(nodeRef);
			if(vh == null) {
				logger.info("creating initial version for:" + nodeRef +"  " + nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
				
				Map<String, Serializable> transFormedProps = transformQNameKeyToString(nodeService.getProperties(nodeRef));
				transFormedProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);
				
				serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
					@Override
					public Void execute() throws Throwable {
						policyBehaviourFilter.disableBehaviour(nodeRef);
						versionService.createVersion(nodeRef, transFormedProps);
						policyBehaviourFilter.enableBehaviour(nodeRef);
						return null;
					}
				});
			}
		}
		
		if(resultSet.hasMore()) {
			execute(page + PAGE_SIZE);
		}
	}
	
	Map<String,Serializable> transformQNameKeyToString(Map<QName, Serializable> props){
		Map<String,Serializable> result = new HashMap<>();
		for(Map.Entry<QName,Serializable> entry : props.entrySet()){
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}
	
	
	@Override
	public Class[] getJobClasses() {
		super.addJobClass(FixInitialVersion.class);
		return allJobs;
	}
}
