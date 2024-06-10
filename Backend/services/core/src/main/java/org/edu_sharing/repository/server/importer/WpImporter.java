package org.edu_sharing.repository.server.importer;

import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.RepoFactory;
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
				<value>post</value>
			</param>
			<param>
				<key>oai_base_url</key>
				<value>http://localhost/wordpress/wp-json/wp/v2/</value>
			</param>
			
		</params>
	</job>
 *
 */
public class WpImporter implements Importer{

	Logger logger = Logger.getLogger(WpImporter.class);
	
	String set = "posts";
    String endpoint; 
    
    PersistentHandlerInterface persistentHandler;
	
	public List<String> getPostTags(Integer postId){
	
		String url = this.endpoint+"tags?post="+postId.toString();

		String result = new HttpQueryTool().query(url);
		
		List<String> generalKeywords = new ArrayList<>();

	try{ 
		JSONArray ja = (JSONArray) new JSONParser().parse(result);
		
		for(int i = 0; i < ja.size(); i++){
			
			JSONObject jo = (JSONObject)ja.get(i);
			generalKeywords.add(jo.get("name").toString());
			
		}
	}catch(Throwable e){
		logger.error(e.getMessage(), e);
	}
	
	return generalKeywords;	
	}
	
	public WpImporter(){
		
	}
	
	@Override
	public void setBaseUrl(String baseUrl) {
		this.endpoint = baseUrl;
	}
	
	@Override
	public void setBinaryHandler(Constructor<BinaryHandler> binaryHandler) {
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
			
			String url = this.endpoint + set;

			String result = new HttpQueryTool().query(url);

				
			try{
				JSONArray ja = (JSONArray)new JSONParser().parse(result);
		    	
				for(int i = 0; i < ja.size(); i++){
					
					Map<String,Object> eduProps = new HashMap<>();
					JSONObject jo = (JSONObject)ja.get(i);
					
					eduProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"wp");
					eduProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, jo.get("guid").toString());
						
					String date = jo.get("modified").toString();

					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					Date dateObj = sdf.parse(date);
					SimpleDateFormat sdfEdu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss");
					
					eduProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP,  sdfEdu.format(dateObj));
					eduProps.put(CCConstants.LOM_PROP_GENERAL_TITLE,jo.get("slug").toString());
					
					
					String name = jo.get("slug").toString().replaceAll(ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), "_");
					
					eduProps.put(CCConstants.CM_NAME, name);

					JSONObject joContent =  (JSONObject) new JSONParser().parse( jo.get("content").toString());

					eduProps.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION,  joContent.get("rendered").toString().replaceAll("\\<.*?\\>;", ""));
					
					String strPid = jo.get("id").toString();
					Integer pid = Integer.parseInt(strPid);

					List<String> generalKeywords = getPostTags(pid); 

					String link = (String)jo.get("link");

					eduProps.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, link);
					eduProps.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, ApplicationInfoList.getHomeRepository().getWebsitepreviewrenderservice()+"/?url="+link+"&scale=0.4");
					eduProps.put(CCConstants.LOM_PROP_GENERAL_KEYWORD, generalKeywords);
					
					eduProps.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_SA);
					
					Map<String,String> wpContributer = new HashMap<>();
					wpContributer.put(CCConstants.VCARD_SURNAME, "wordpress");
					wpContributer.put(CCConstants.VCARD_ORG, "wordpress");
					
					String vcard = VCardTool.hashMap2VCard(wpContributer);
					eduProps.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER,vcard);
					eduProps.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR,vcard);
					eduProps.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR,vcard);
					
					//just for filling search widget
					eduProps.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER,"mebisWp");
					
					persistentHandler.safe(new RecordHandlerStatic(eduProps), null, "mebiswb_"+set);
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

	public static void main(String[] args){
		Importer i = new WpImporter();
		i.setBaseUrl("http://127.0.0.1/wordpress/wp-json/wp/v2/");
		i.setSet("posts");
		
		try{
			i.startImport();
		}catch(Throwable e){
			e.printStackTrace();
		}
	}

	@Override
	public void setFrom(Date from) {

	}

	@Override
	public void setUntil(Date until) {

	}
}
