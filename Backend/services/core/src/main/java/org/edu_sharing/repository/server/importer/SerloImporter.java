package org.edu_sharing.repository.server.importer;

import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.jobs.quartz.ImporterJob;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * 
 * @author rudi
 *
 *	
 	<job>
		<class>org.edu_sharing.repository.server.jobs.quartz.ImporterJob</class>
		<trigger>Cron[0 0/5 * * * ?]</trigger>
		<params>
			<param>
				<key>sets</key>
				<value>article,course,video,text-exercise</value>
			</param>
			<param>
				<key>oai_base_url</key>
				<value>https://de.serlo.org/entity/api/json/export/</value>
			</param>
			
		</params>
	</job>
	
	Admin Tools Toolkit:
	org.edu_sharing.repository.server.jobs.quartz.ImporterJob
	{"sets":"article,course,video,text-exercise","oai_base_url":"https://de.serlo.org/entity/api/json/export/","importer_class":"org.edu_sharing.repository.server.importer.SerloImporter","binary_handler":"org.edu_sharing.repository.server.importer.BinaryHandlerSerlo"}
 *
 */
public class SerloImporter implements Importer{

	Logger logger = Logger.getLogger(SerloImporter.class);
	
	String set = "serlo";
	
	PersistentHandlerInterface persistentHandler;
	
	BinaryHandler binaryHandler = new BinaryHandlerSerlo();
	
	String serloUrl;
	
	public SerloImporter(){
		
	}
	
	public static void main(String[] args){
		SerloImporter si = new SerloImporter();
		si.setBaseUrl("https://de.serlo.org/entity/api/json/export/");
		si.setSet("article");
	}
	
	@Override
	public void setBaseUrl(String baseUrl) {
		this.serloUrl = baseUrl;
	}
	
	@Override
	public void setBinaryHandler(Constructor<BinaryHandler> binaryHandler) {
		try {
			this.binaryHandler = binaryHandler.newInstance();
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	@Override
	public void setMetadataPrefix(String metadataPrefix) {
	}
	
	@Override
	public void setNrOfRecords(int nrOfRecords) {
		
	}
	
	@Override
	public void setNrOfResumptions(int nrOfResumptions) {
		
	}
	@Override
	public void setPersistentHandler(PersistentHandlerInterface persistentHandler) {
		this.persistentHandler = persistentHandler;
	}
	
	@Override
	public void setRecordHandler(Constructor<RecordHandlerInterface> recordHandler) {
		
	}
	
	@Override
	public void setSet(String set) {
		this.set = set;
		
	}
	
	@Override
	public void startImport() throws Throwable {
		HttpClient client = new HttpClient();
		client.getParams().setParameter("http.useragent", "Test Client");
		
		//client.getParams().setParameter("http.useragent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:35.0) Gecko/20100101 Firefox/35.0");
		
		String url = serloUrl + set;

		HttpGet method = new HttpGet(url);
		String result = new HttpQueryTool().query(url, null, method,true);
		
		try{
			JSONArray ja = (JSONArray)new JSONParser().parse(result);
	    	
			for(int i = 0; i < ja.size(); i++){
				
				Map<String,Object> eduProps = new HashMap<>();
				JSONObject jo = (JSONObject)ja.get(i);
				
			
				/**
				 * String replicationId = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
		String lomCatalogId = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
		String timestamp = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);
				 */
				
				eduProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"serlo");
				eduProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, (String)jo.get("guid"));
					
				JSONObject lm = (JSONObject)jo.get("lastModified");
				String date = (String)lm.get("date");
				//"2014-10-31 16:56:50.000000" --> to edu-sharing PersistenHandlerFormat
				date = date.replace(".000000", "");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date dateObj = sdf.parse(date);
				SimpleDateFormat sdfEdu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss");
				
				eduProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP,  sdfEdu.format(dateObj));
				eduProps.put(CCConstants.LOM_PROP_GENERAL_TITLE,jo.get("title").toString());
				
				
				String name = jo.get("title").toString().replaceAll(ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), "_");
				name = name.trim();
				eduProps.put(CCConstants.CM_NAME, name);
				eduProps.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, (String) jo.get("description"));
				
				List<String> generalKeywords = new ArrayList<>();
				if(jo.get("keywords") instanceof JSONObject){
					JSONObject kw = (JSONObject)jo.get("keywords");
					for(Object valueKW: kw.values()){
						generalKeywords.add((String)valueKW);
					}
				}else if(jo.get("keywords") instanceof JSONArray) {
					JSONArray kw = (JSONArray)jo.get("keywords");
					for(Object valueKW : kw.toArray()){
						generalKeywords.add((String)valueKW);
					}
				}
				
				
				JSONArray categories = (JSONArray)jo.get("categories");
				
				

				
				
				String schoolContextChain = "";
				String themeChain = "";
				for(int c = 0; c < categories.size(); c++){
					
		
					String cat = (String)categories.get(c);
					
					List<String> cats = Arrays.asList(cat.split("/"));
					
					
					if(cats.get(1).equals("Deutschland") && cats.size() == 6){
						String bundesLand = null;
						String fach = null;
						String klassenStufe = null;
						
						String schulart = null;
						String thema = null;
						
						fach = cats.get(0);
						bundesLand = cats.get(2);
						schulart = cats.get(3);
						klassenStufe = cats.get(4).replaceAll("[a-zA-Z\\. ]*", "");
						thema = cats.get(5);
						
						
						Map.Entry<String, String> eduFach = null;
						Map.Entry<String, String> eduKlassenStufe = null;
						Map.Entry<String, String> eduBundesland = null;
						Map.Entry<String, String> eduSchulart = null;

						
						Map.Entry<String,String> sekundarStufe1 = null;
						Map.Entry<String,String> sekundarStufe2 = null;
						

						if(schulart.equals("Gymnasium")){
							if(eduKlassenStufe != null){
								int klassenstufe = new Integer(eduKlassenStufe.getValue());
								if(klassenstufe > 9){
									eduSchulart = sekundarStufe2;
								}
							}
							
						}
						
						if(eduSchulart == null){
							eduSchulart = sekundarStufe1;
						}
					
						
						String schoolContext  = "";
						//bundesland
						if(eduBundesland != null) schoolContext += eduBundesland.getKey();
						schoolContext += CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR;
						
						//schulart
						if(eduSchulart != null) schoolContext += eduSchulart.getKey();
						schoolContext += CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR;
						
						//fach
						if(eduFach != null) schoolContext += eduFach.getKey();
						schoolContext += CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR;
							
						
						//Jahrgang
						if(eduKlassenStufe != null) schoolContext += eduKlassenStufe.getKey();
						schoolContext += CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR;
						
						//Thema
						if(thema != null) schoolContext += thema;
						
						schoolContextChain += schoolContext + CCConstants.MULTIVALUE_SEPARATOR;
						themeChain += thema + CCConstants.MULTIVALUE_SEPARATOR;
					}
					
					
					
				}
				
				eduProps.put(CCConstants.CCM_PROP_IO_SCHOOLCONTEXT, schoolContextChain);
				eduProps.put(CCConstants.CCM_PROP_IO_SCHOOLTOPIC, themeChain);
				
				String link = (String)jo.get("link");
				link = link.replaceAll("\\\\", "");
				link = "https://de.serlo.org"+link;
				eduProps.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, link);
				eduProps.put(CCConstants.CCM_PROP_IO_WWWURL, link);
				//eduProps.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, "http://web-screenshot.serlo.org:2341/?url="+link+"&scale=0.4");
				eduProps.put(CCConstants.LOM_PROP_GENERAL_KEYWORD, generalKeywords);
				
				eduProps.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_SA);
				
				Map<String,String> serloContributer = new HashMap<>();
				serloContributer.put(CCConstants.VCARD_SURNAME, "serlo");
				serloContributer.put(CCConstants.VCARD_ORG, "serlo");
				serloContributer.put(CCConstants.VCARD_EMAIL, "info-de@serlo.org");
				serloContributer.put(CCConstants.VCARD_CITY, "München");
				
				
				String vcard = VCardTool.hashMap2VCard(serloContributer);
				eduProps.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER,vcard);
				eduProps.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR,vcard);
				eduProps.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR,vcard);
				
				//just for filling search widget
				eduProps.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER,"serlo");
				
				String nodeId = persistentHandler.safe(new RecordHandlerStatic(eduProps), null, "serlo_"+set);
				binaryHandler.safe(nodeId, new RecordHandlerStatic(eduProps), null, set);
			}
			
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
		}
		
	}
	
	@Override
	public void startImport(String[] oaiIDs) {
		logger.error("not implemented yet");
	}

	@Override
	public void setJob(ImporterJob importerJob) {

	}

	@Override
	public void setMetadataSetId(String metadataSetId) {

	}

	@Override
	public RecordHandlerInterface getRecordHandler() {
		return null;
	}

	@Override
	public void setFrom(Date from) {

	}

	@Override
	public void setUntil(Date until) {

	}
}
