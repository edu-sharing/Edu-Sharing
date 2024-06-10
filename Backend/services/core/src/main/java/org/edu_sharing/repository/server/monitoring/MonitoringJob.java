package org.edu_sharing.repository.server.monitoring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.modeler.Registry;
import org.edu_sharing.alfresco.monitoring.Application;
import org.edu_sharing.alfresco.monitoring.MonitoringDao;
import org.edu_sharing.repository.server.jobs.quartz.AbstractJob;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class MonitoringJob extends AbstractJob{
	
	Logger logger = Logger.getLogger(MonitoringJob.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		try{
			MonitoringDao dao = new MonitoringDao();
			
			
			logger.info("**********DATABASE****************");
			for(Map.Entry<String, String> entry : dao.getDataBasePoolInfo().entrySet()){
				logger.info(entry.getKey() + ":"  + entry.getValue());
			}
			
			logger.info("**********THREADS****************");
			
			for(String mbean : dao.getMBeans()){
				
				logger.info("");
				
				
				logger.info("MBEAN:" + mbean);
				
				Map<String,String> attributes = dao.getMBeanAttributes(mbean, MonitoringDao.mbeanAttributes);
				for(Map.Entry<String,String> entry : attributes.entrySet()){
					logger.info(entry.getKey() + ":"  + entry.getValue());
				}
			}
			
			
			logger.info("**********SessionCount****************");
			Map<Application, Integer> map = dao.getSessionCount("localhost");
			for(Map.Entry<Application, Integer> entry : map.entrySet()){
				logger.info(entry.getKey().getName() + " " + entry.getValue());
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		
	}
	
	@Override
	public Class<?>[] getJobClasses() {
		Class<?>[] result = Arrays.copyOf(allJobs, allJobs.length + 1);
	    result[result.length - 1] = MonitoringJob.class;
		return result;
	}

}
