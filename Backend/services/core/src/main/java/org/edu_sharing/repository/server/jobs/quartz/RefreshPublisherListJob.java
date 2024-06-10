/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class RefreshPublisherListJob extends AbstractJob {

	public static final String CONFIG_IGNORE_ENTRIES_KEY = "ignore_entries";
	public static final String CONFIG_VCARD_PROPS = "vcardprops";
	public static final String CONFIG_VALUESPACE_PROP = "valuespaceprop";
	public static final String CONFIG_FILEPATH = "filepath";
	
	Logger logger = Logger.getLogger(RefreshPublisherListJob.class);

	@Override
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		JobDataMap jobDataMap = jobContext.getJobDetail().getJobDataMap();

		String ignoreEntries = (String)jobDataMap.get(CONFIG_IGNORE_ENTRIES_KEY);
		String vcardProps = (String)jobDataMap.get(CONFIG_VCARD_PROPS);
		String valuespaceProp = (String)jobDataMap.get(CONFIG_VALUESPACE_PROP);
		
		//String filePath = "tomcat/shared/classes/org/edu_sharing/metadataset/valuespace_learnline2_0_contributer.xml";
		String filePath = (String)jobDataMap.get(CONFIG_FILEPATH);
		
		if(vcardProps == null || vcardProps.trim().equals("")){
			throw new JobExecutionException(CONFIG_VCARD_PROPS + " is a mandatory!");
		}
		
		if(valuespaceProp == null || valuespaceProp.trim().equals("")){
			throw new JobExecutionException(CONFIG_VALUESPACE_PROP + " is a mandatory!");
		}
		
		if(filePath == null || filePath.trim().equals("")){
			throw new JobExecutionException(CONFIG_FILEPATH + " is a mandatory!");
		}
		
		
		
		ApplicationInfo homeRep  = ApplicationInfoList.getHomeRepository();
		try{
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
			Map<String, String> authInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());
			
			AdminServiceFactory.getInstance().writePublisherToMDSXml(vcardProps, valuespaceProp, ignoreEntries, filePath, authInfo);

			MetadataReader.refresh();
		}catch(org.alfresco.webservice.authentication.AuthenticationFault e){
			logger.error("AuthenticationFault while excecuting RefreshPublisherListJob:"+e.getMessage());
			throw new JobExecutionException(e);
		}catch(Throwable e){
			logger.error("Throwable while excecuting RefreshPublisherListJob"+e.getMessage());
			throw new JobExecutionException(e);
		}
	}

	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}

	
}
