package org.edu_sharing.service.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.jobs.quartz.ImmediateJobListener;
import org.edu_sharing.repository.server.jobs.quartz.JobHandler;
import org.edu_sharing.repository.server.jobs.quartz.RefreshCacheJob;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class FillImpObjPropertiesCache implements ApplicationListener<ContextRefreshedEvent>{

	Logger logger = Logger.getLogger(FillImpObjPropertiesCache.class);
			
	
	public FillImpObjPropertiesCache() {
		
		logger.info("constructor started");
		
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		logger.info("context refreshed");
		logger.info("starting refresh cache job");
		
		MCAlfrescoAPIClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
		String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
		Map<String, Object> importFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
				OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
		
		if(importFolderProps == null || importFolderProps.isEmpty()) {
			logger.error("No import folder available");
			return;
		}
		
		Map<String, Object> params = new HashMap<>();
		params.put("rootFolderId", importFolderProps.get(CCConstants.SYS_PROP_NODE_UID));
		params.put("sticky", "true");
		try {
			
			Class job = RefreshCacheJob.class;
			
			ImmediateJobListener jobListener = JobHandler.getInstance(event.getApplicationContext()).startJob(job, params);
			if(jobListener != null && jobListener.isVetoed()){
				throw new Exception("job was vetoed by " + jobListener.getVetoBy());
			}
			
			
			
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	
}
