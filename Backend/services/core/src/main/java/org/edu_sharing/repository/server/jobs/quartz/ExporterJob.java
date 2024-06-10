package org.edu_sharing.repository.server.jobs.quartz;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.repo.node.MLPropertyInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.exporter.OAILOMExporter;
import org.edu_sharing.repository.server.exporter.OAILOMWithSubobjectsExporter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.oai.OAIExporterFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ExporterJob extends AbstractJob {

	public static final String PARAM_LUCENE_FILTER = "lucenefilter";

	public static final String PARAM_OUTPUT_DIR = "outputdir";
	
	public static final String PARAM_WITH_SUBOBJECTS = "withsubobjects";

	Log logger = LogFactory.getLog(ExporterJob.class);

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		Map jobDataMap = context.getJobDetail().getJobDataMap();
		String luceneFilter = (String) jobDataMap.get(PARAM_LUCENE_FILTER);
		String outputdir = (String) jobDataMap.get(PARAM_OUTPUT_DIR);
		
		Boolean withSubObjects = Boolean.valueOf((String) jobDataMap.get(PARAM_WITH_SUBOBJECTS));

		if (luceneFilter != null && outputdir != null) {

			try {
			
				ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
				
				Map<String, String> authInfo = new AuthenticationToolAPI().createNewSession(appInfo.getUsername(), appInfo.getPassword());
				MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient(authInfo);
				String[] nodeIds = apiClient.searchNodeIds(luceneFilter);

				if (nodeIds != null) {
					logger.info("found " + nodeIds.length + " to export with " + PARAM_LUCENE_FILTER + ": " + luceneFilter);

					for (String nodeId : nodeIds) {

						if(withSubObjects){
							new OAILOMWithSubobjectsExporter(nodeId).export(outputdir);
						}else{
							OAIExporterFactory.getOAILOMExporter().export(outputdir,nodeId);
						}
					}

				}else{
					logger.info("found nothing with " + PARAM_LUCENE_FILTER + ": " + luceneFilter);
				}
			} catch (ParserConfigurationException e) {
				logger.error(e.getMessage(), e);
			} catch (FileNotFoundException e) {
				logger.error(e.getMessage(), e);
			}catch(Throwable e){
				logger.error(e.getMessage(), e);
			}

			

		}

	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}

}
