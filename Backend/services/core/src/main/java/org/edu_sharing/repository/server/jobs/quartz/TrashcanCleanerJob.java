package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TrashcanCleanerJob extends AbstractJob {

	protected static final long DEFAULT_TIME_TO_KEEP = -1;
	protected static final int DEFAULT_DELETE_BATCH_COUNT = 1000;
	
	private static final String KEY_TIME_TO_KEEP = "timeToKeep";
	private static final String KEY_DELETE_BATCH_COUNT = "batchSize";
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		
		try {
		
			long time =   jobDataMap.containsKey(KEY_TIME_TO_KEEP)
						? jobDataMap.getLong(KEY_TIME_TO_KEEP)
						: DEFAULT_TIME_TO_KEEP;
			
			int batch =   jobDataMap.containsKey(KEY_DELETE_BATCH_COUNT)
						? jobDataMap.getInt(KEY_DELETE_BATCH_COUNT)
						: DEFAULT_DELETE_BATCH_COUNT;

			new TrashcanCleaner(time , batch).execute(); 	
						
		} catch (Throwable t) {
			throw new JobExecutionException(t);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getJobClasses() {
		
		Class[] result = Arrays.copyOf(allJobs, allJobs.length + 1);
	    result[result.length - 1] = TrashcanCleanerJob.class;
		return result;
	}
	
	public static class TrashcanCleaner {

		private final long timeToKeep;
		private final int batchSize;
		
		private final Logger logger = Logger.getLogger(TrashcanCleaner.class);

		private final String ASSOC_CHILDREN = "{" + NamespaceService.SYSTEM_MODEL_1_0_URI + "}children";
		private final String PROP_ARCHIVED_DATE = "{" + NamespaceService.SYSTEM_MODEL_1_0_URI + "}archivedDate";
		
		TrashcanCleaner (long timeToKeep, int batchSize) {
			
			this.timeToKeep = timeToKeep;
			this.batchSize = batchSize;			
		}		
		
		void execute() throws Throwable {

 			if (timeToKeep <= 0) {

				logger.info("Cleaning trashcan will be skipped (timeToKeep undefined).");
				return;
			}

			ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
			Map<String, String> authInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());

			MCAlfrescoAPIClient apiClient = (MCAlfrescoAPIClient) RepoFactory.getInstance(null, authInfo);
			
			StoreRef storeRef = new StoreRef("archive://SpacesStore");
			
			logger.info("Cleaning trashcan starting (timeToKeep: " + timeToKeep + ", batchSize: " + batchSize +")");

			Map<String, Map<String, Object>> childrenByAssociation =
					apiClient.getChildrenByAssociation(
						storeRef, 
						apiClient.getRootNode(storeRef), 
						ASSOC_CHILDREN);
			
			int i = 0;
			if (childrenByAssociation != null) {

				logger.info("Cleaning trashcan scanning (size: " + childrenByAssociation.size() +")");

				long now = System.currentTimeMillis();
				
				for (Entry<String, Map<String, Object>> entry : childrenByAssociation.entrySet()) {
					
					String archivedDate = (String) entry.getValue().get(PROP_ARCHIVED_DATE);
					
					if (archivedDate == null) {
						
						continue;
					}					

					long lifeTime = now - Long.parseLong(archivedDate);
					
					if (lifeTime <= timeToKeep) {
						
						logger.debug("Cleaning trashcan will be skipped (node: " + entry.getKey() + ", lifeTime: " + lifeTime +")");
						continue;
					}

					if (++i > batchSize) {

						logger.info("Cleaning trashcan will be skipped (batchSize reached)");
						break;
					}

					apiClient.removeNode(storeRef, entry.getKey());
					logger.info("Cleaning trashcan performed (node: " + entry.getKey() + ")");
				}				
			}
			
			logger.info("Cleaning trashcan finished (size: " + i + ")");
			
		}
		
	}

}
