package org.edu_sharing.repository.server.jobs.quartz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.importer.OAIPMHLOMValidator;

public class OAIXMLValidatorJob extends ImporterJob{

	Logger logger = Logger.getLogger(OAIPMHLOMImporter.class);
	
	
	protected void start(String urlImport, String oaiBaseUrl, String metadataSetId, String metadataPrefix, String[] sets) {
		try{
			OAIPMHLOMImporter importer = new OAIPMHLOMValidator(oaiBaseUrl, -1, -1, metadataPrefix, sets);
			importer.startImport();
	
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public Class[] getJobClasses() {
		List<Class> classList = new ArrayList<>(Arrays.asList(allJobs));
		classList.add(OAIXMLValidatorJob.class);
		return classList.toArray(new Class[classList.size()]);
	}
	
}
