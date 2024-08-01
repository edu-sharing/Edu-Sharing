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

import java.lang.reflect.Constructor;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.importer.*;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class ImporterJob extends AbstractInterruptableJob {

	public static Logger logger=Logger.getLogger(ImporterJob.class);
	private JobExecutionContext context;

	public ImporterJob() {

	}
	@Override
	public void executeInterruptable(JobExecutionContext context) throws JobExecutionException {
		String username = (String) context.getJobDetail().getJobDataMap().get(OAIConst.PARAM_USERNAME);
		
		if(username == null || username.trim().equals("")) {
			throw new JobExecutionException("no user provided");
		}

		RunAsWork<Void> runAs = new RunAsWork<Void>() {

			@Override
			public Void doWork() throws Exception {
				start(context,context.getJobDetail().getJobDataMap());
				return null;
			}

		};
		AuthenticationUtil.runAs(runAs, username);
		
	}
	public String start(JobDataMap jobDataMap){
		this.jobDataMap=jobDataMap;
		return start(null,jobDataMap);
	}
	private String start(JobExecutionContext context, Map jobDataMap) {
		logger.info("starting");

		Object setsParamObj = jobDataMap.get(OAIConst.PARAM_OAI_SETS);
		// String setsParam = (String) jobDataMap.get("sets");
		List<String> splitted = null;
		if (setsParamObj instanceof String) {
			splitted = new ArrayList(Arrays.asList(((String) setsParamObj).split(",")));
		} else {
			splitted = (List) setsParamObj;
		}

		String urlImport = null;
		String[] sets = null;
		if(splitted!=null) {
			for (String set : splitted) {
				if (set.contains("http://")) {
					urlImport = set.trim();
				}
			}
			if (urlImport != null) {
				splitted.remove(urlImport);
			}
			sets = splitted.toArray(new String[splitted.size()]);
		}
		String oaiBaseUrl = getParam(OAIConst.PARAM_OAI_BASE_URL,jobDataMap);
		String metadataPrefix = getParam(OAIConst.PARAM_OAI_METADATA_PREFIX,jobDataMap);
		metadataPrefix = (metadataPrefix == null || metadataPrefix.trim().equals("")) ? "oai_lom-de" : metadataPrefix;
		String metadataSetId = getParam(OAIConst.PARAM_METADATASET_ID,jobDataMap);

		String recordHandlerClass = getParam(OAIConst.PARAM_RECORDHANDLER,jobDataMap);
		String binaryHandlerClass = getParam(OAIConst.PARAM_BINARYHANDLER,jobDataMap);

		String persistentHandlerClass = getParam(OAIConst.PARAM_PERSISTENTHANDLER,jobDataMap);

		String importerClass = getParam(OAIConst.PARAM_IMPORTERCLASS,jobDataMap);
		
		String oaiIds = getParam(OAIConst.PARAM_OAI_IDS,jobDataMap);
		
		String[] idArr = (oaiIds != null) ? oaiIds.split(",") : null;

		// force update if single ids should be updated
		if(idArr != null && idArr.length > 0){
			jobDataMap.put(OAIConst.PARAM_FORCE_UPDATE, true);
		}

		Date from = null;
		Date until = null;
		try {
			from = OAIConst.DATE_FORMAT.parse((String)jobDataMap.get(OAIConst.PARAM_FROM));
			until = OAIConst.DATE_FORMAT.parse((String)jobDataMap.get(OAIConst.PARAM_UNTIL));
		} catch (ParseException e) {
			logger.error(e.getMessage());
		}catch (NullPointerException e){
			logger.debug("from/until not set");
		}

		if(from == null && until == null){
			String periodInDaysStr = getParam(OAIConst.PARAM_PERIOD_IN_DAYS,jobDataMap);
			if(periodInDaysStr != null && !periodInDaysStr.trim().equals("")) {
				try {
					Long periodInDays = Long.parseLong(periodInDaysStr);
					Long periodInMs = periodInDays * 24 * 60 * 60 * 1000;
					until = new Date();
					from = new Date((until.getTime() - periodInMs));
					logger.info("using from:" + from + " until:" + until);
				}catch (NumberFormatException e){
					logger.error(e.getMessage());
				}
			}
		}


		byte[] xmlData= (byte[]) jobDataMap.get(OAIConst.PARAM_XMLDATA);


		this.context = context;
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		String finalUrlImport = urlImport;
		String finalMetadataPrefix = metadataPrefix;
		String[] finalSets = sets;
		if (xmlData != null) {
			return start(xmlData, recordHandlerClass, binaryHandlerClass);
		}
		start(finalUrlImport, oaiBaseUrl, metadataSetId, finalMetadataPrefix, finalSets, recordHandlerClass, binaryHandlerClass,persistentHandlerClass, importerClass, idArr, from, until);
		return null;
	}

	private String getParam(String key, Map jobDataMap){
		return StringUtils.trim((String) jobDataMap.get(key));
	}

	private String start(byte[] xmlData, String recordHandlerClass, String binaryHandlerClass){
		try {
			OAIPMHLOMImporter importer = new OAIPMHLOMImporter();
			Constructor<RecordHandlerInterface> recordHandler = null;
			Constructor<BinaryHandler> binaryHandler = null;

			if (recordHandlerClass != null) {
				Class tClass = Class.forName(recordHandlerClass);
				recordHandler = tClass.getConstructor(String.class);
			} else {
				recordHandler = (Constructor) RecordHandlerLOM.class.getConstructor(String.class);
			}
			if (binaryHandlerClass != null) {
				Class tClass = Class.forName(binaryHandlerClass);
				binaryHandler = tClass.getConstructor();
			} else {
				binaryHandler = null;
			}

			logger.info("importer:" + importer.getClass().getName());

			importer.setBinaryHandler(binaryHandler);
			// quick import: do not build up cache, always import xml file
			importer.setPersistentHandler(new PersistentHandlerEdusharing(this,importer,false));
			importer.setRecordHandler(recordHandler);
			importer.setJob(this);
			importer.setSet("xml-import");
			return importer.startImport(xmlData);
		}catch(Throwable t){
			logger.error(t.getMessage(),t);
			return null;
		}
	}

	protected void start(String urlImport, String oaiBaseUrl, String metadataSetId, String metadataPrefix,
						 String[] sets, String recordHandlerClass, String binaryHandlerClass, String persistentHandlerClass, String importerClass, String[] idList, Date from, Date until) {
		try {
			for(String set : sets) {
				Importer importer = null;
				if (importerClass != null) {
					Class tClass = Class.forName(importerClass);
					Constructor<Importer> constructor = tClass.getConstructor();
					importer = constructor.newInstance();
				} else {
					importer = new OAIPMHLOMImporter();
				}
				try {
					JobHandler.getInstance().updateJobName(context==null ? null : context.getJobDetail(), "Importer Job " + importer.getClass().getSimpleName() + " " + new URL(oaiBaseUrl).getHost());
				} catch (Throwable t) {
				}

				Constructor<RecordHandlerInterface> recordHandler = null;
				Constructor<BinaryHandler> binaryHandler = null;
				Constructor<PersistentHandlerInterface> persistentHandler = null;

				if (recordHandlerClass != null) {
					Class tClass = Class.forName(recordHandlerClass);
					recordHandler = tClass.getConstructor(String.class);
				} else {
					recordHandler = (Constructor)RecordHandlerLOM.class.getConstructor(String.class);
				}
				if (binaryHandlerClass != null) {
					Class tClass = Class.forName(binaryHandlerClass);
					binaryHandler = tClass.getConstructor();
				} else {
					binaryHandler = null;
				}

				if(persistentHandlerClass != null){
					Class tClass = Class.forName(persistentHandlerClass);
					persistentHandler = tClass.getConstructor();
				}else{
					persistentHandler = null;
				}

				logger.info("importer:" + importer.getClass().getName());

				importer.setBaseUrl(oaiBaseUrl);
				importer.setBinaryHandler(binaryHandler);
				importer.setMetadataPrefix(metadataPrefix);
				importer.setNrOfRecords(-1);
				importer.setNrOfResumptions(-1);
				if(persistentHandler != null){
					importer.setPersistentHandler(persistentHandler.newInstance());
				}else{
					importer.setPersistentHandler(new PersistentHandlerEdusharing(this,importer,false));
				}

				importer.setSet(set);
				importer.setMetadataSetId(metadataSetId);
				importer.setRecordHandler(recordHandler);
				importer.setJob(this);
				importer.setFrom(from);
				importer.setUntil(until);
				if (urlImport != null) {
					RecordHandlerLOM recordHandlerLom = new RecordHandlerLOM(null);
					((OAIPMHLOMImporter) importer).importOAIObjectsFromFile(urlImport, recordHandlerLom);
					new RefreshCacheExecuter().excecute(null, true, null);
					return;
				}

				if (idList != null && idList.length > 0) {
					importer.startImport(idList);
					return;
				}

				long millisec = System.currentTimeMillis();
				logger.info("starting import of set "+set);
				importer.startImport();
				logger.info("finished import in " + (System.currentTimeMillis() - millisec) / 1000 + " secs");
		}
		// refresh cache after importing
		if (!isInterrupted && !(idList != null && idList.length > 0))
			new RefreshCacheExecuter().excecute(null, true, null);

		} catch (Throwable e) {
			logger.error(e.getMessage(),e);
			e.printStackTrace();
		}
	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}
}
