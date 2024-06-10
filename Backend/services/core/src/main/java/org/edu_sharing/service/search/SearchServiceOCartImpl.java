package org.edu_sharing.service.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;

import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchServiceOCartImpl extends SearchServiceAdapter{
	
	private static final String OCA_API = "https://openclipart.org";

	Logger logger = Logger.getLogger(SearchServiceOCartImpl.class);
	
	String repositoryId = null;

	String APIKey = null;
			
	public SearchServiceOCartImpl(String appId) {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		this.APIKey = appInfo.getApiKey(); 

	}

	public static HttpsURLConnection openOCAUrl(URL url) throws KeyManagementException, IOException, NoSuchAlgorithmException{
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		
		// TODO!
		TrustManager[] trustAllCerts = new TrustManager[]{
			    new X509TrustManager() {
			        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			            return null;
			        }
			        public void checkClientTrusted(
			            java.security.cert.X509Certificate[] certs, String authType) {
			        }
			        public void checkServerTrusted(
			            java.security.cert.X509Certificate[] certs, String authType) {
			        }
			    }
			};
		SSLContext sc = SSLContext.getInstance("SSL");
	    sc.init(null, trustAllCerts, new java.security.SecureRandom());
		connection.setSSLSocketFactory(sc.getSocketFactory());
		connection.setHostnameVerifier(new HostnameVerifier() {
		    public boolean verify(String hostname, SSLSession session) {
		      return true;
		    }
		  });
		return connection;
	}

	public  SearchResultNodeRef searchOCA(String repositoryId,String apiKey,String path) throws Exception{
		//String lang=new AuthenticationToolAPI().getCurrentLocale().split("_")[0];

		String jsonString = "";
		URL url=new URL(OCA_API+path);
		HttpsURLConnection connection = openOCAUrl(url);
		connection.connect();		

        InputStreamReader isr = new InputStreamReader(connection.getInputStream(), "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        isr.close();
        connection.disconnect();
        jsonString = sb.toString();

		JSONObject jo = new JSONObject(jsonString);
    	
		Integer nrOfResult = (Integer)jo.getJSONObject("info").get("results");
		JSONArray docs = (JSONArray)jo.getJSONArray("payload");
//		JSONObject docs = (JSONObject)resultsArr.get(0);
//		JSONArray docs = (JSONArray)resultsArr.get  ("docs");

		
		SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
		List<NodeRef> data=new ArrayList<>();
		
		Map<String,Map<String,Object>> result = new HashMap<>();
		for(int i = 0; i < docs.length(); i++){
			JSONObject doc = (JSONObject)docs.get(i);
			
			Map<String,Object> props =  this.getProperties(doc);
			result.put((String)props.get(CCConstants.SYS_PROP_NODE_UID), props);

			org.edu_sharing.service.model.NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(repositoryId, 
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),props);
			data.add(ref);

		}
		searchResultNodeRef.setData(data);
				
		/*

		
			Map<String,Object> properties=new HashMap<>();
			properties.put(CCConstants.SYS_PROP_NODE_UID,map.getNamedItem("id").getNodeValue());
			properties.put(CCConstants.LOM_PROP_GENERAL_TITLE,map.getNamedItem("title").getNodeValue());
			
			properties.put(CCConstants.LOM_PROP_GENERAL_KEYWORD,map.getNamedItem("tags").getNodeValue().replace(" ",CCConstants.MULTIVALUE_SEPARATOR));

			properties.put(CCConstants.CM_ASSOC_THUMBNAILS, map.getNamedItem("image").getNodeValue());
			properties.put(CCConstants.CONTENTURL,map.getNamedItem("url").getNodeValue());
			properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "application/xhtml+xml");
			properties.put(CCConstants.CCM_PROP_IO_WWWURL,map.getNamedItem("url").getNodeValue());
			properties.put(CCConstants.NODETYPE, CCConstants.CCM_TYPE_IO);
			properties.put(CCConstants.CM_PROP_C_CREATOR,map.getNamedItem("author").getNodeValue());

			properties.put(CCConstants.REPOSITORY_ID, "LEARNINGAPPS" );
			properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"LearningApps");
			properties.put(CCConstants.CM_PROP_C_CREATOR,map.getNamedItem("author").getNodeValue());
			properties.put(CCConstants.NODECREATOR_FIRSTNAME,map.getNamedItem("author").getNodeValue());
			properties.put(CCConstants.NODEMODIFIER_FIRSTNAME,map.getNamedItem("author").getNodeValue());
			
			String createdate = map.getNamedItem("created").getNodeValue();

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
			LocalDateTime date  = LocalDateTime.parse(createdate, formatter);
			DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
			properties.put(CCConstants.CM_PROP_C_MODIFIED,date.format(formatter2));
			  
		}*/
		return searchResultNodeRef;

	}
	
	public static String httpGet(String urlStr, Map<String, String> properties) throws IOException {
        if (properties == null) {
            properties = new HashMap<>();
        }
        // open HTTP connection with URL
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // set properties if any do exist
        for (String key : properties.keySet()) {
            conn.setRequestProperty(key, properties.get(key));
        }
        // test if request was successful (status 200)
        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }
        // buffer the result into a string
        InputStreamReader isr = new InputStreamReader(conn.getInputStream(), "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        isr.close();
        conn.disconnect();
        return sb.toString();
    }	

	private Map<String,Object> getProperties(JSONObject doc){
		Map<String,Object> properties = new HashMap<>();
		String id = null;
		try {
			id = doc.getString("id");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String title = null;
		try {
			title = doc.getString("title");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

/*
 * "payload" : [
	 {
		"title" : "Overbalanced (colour)",
		"description" : "A play on words",
		"id" : 294892,
		"svg" : { "url" : "https://openclipart.org/download/294892/OverbalancedColour.svg",
			"png_thumb"
 * 	
 */
		String thumbnail = null;
		JSONObject svg = null;
		try {
			svg = doc.getJSONObject("svg");
			thumbnail = (String)svg.get("png_thumb");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, title);
		properties.put(CCConstants.SYS_PROP_NODE_UID, id);

		//for the importer
		properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, thumbnail);	
		//for the gwt gui no persistent
		properties.put(CCConstants.CM_ASSOC_THUMBNAILS, thumbnail);

		String url = null;
		try {
			url = doc.getString("detail_link");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, url);
		properties.put(CCConstants.CCM_PROP_IO_WWWURL, url);
//			properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, mimetyp);
		properties.put(CCConstants.CONTENTURL,URLTool.getRedirectServletLink(this.repositoryId, id));
		properties.put(CCConstants.REPOSITORY_ID, this.repositoryId );
		properties.put(CCConstants.NODETYPE, CCConstants.CCM_TYPE_IO);
		
		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, "");
		
		try{
		//	properties.putAll(getProperties(id));
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}
		
		
		return properties;
	}
	
	
	public Map<String, Object> getProperties(String nodeId) throws Throwable {
		
		Map<String,Object> properties = new HashMap<>();
		try{
			String result = httpGet(OCA_API   +"/search/json/?byids="+nodeId, null);
			if(result != null && result.trim().length() > 0){
			
				JSONObject jo = new JSONObject(result);//  )new JSONParser().parse(result);
				if(jo != null){	
					

//					String contenturl  = (String)binary.get("@path");
//					String mimetyp = (String)binary.get("@mimetype");
//					contenturl = OCA_API+contenturl;
					
					//properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, contenturl);
					JSONArray payload  = jo.getJSONArray("payload");
					JSONObject ob = payload.getJSONObject(0);
					String url = ob.getString("detail_link");
							//"https://www.deutsche-digitale-bibliothek.de/item/"+nodeId;
					properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, url);
					properties.put(CCConstants.CCM_PROP_IO_WWWURL, url);
	//				properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, mimetyp);
    		        properties.put(CCConstants.CONTENTURL,URLTool.getRedirectServletLink(this.repositoryId, nodeId));
				}
			}
			//URLEncoder.encode(this.APIKey, "UTF-8")
			
			
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return properties;
	}
	
	
	
	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
									  SearchToken searchToken) throws Throwable {
		

		if(!"ngsearch".equals(query)){
			throw new Exception("Only ngsearch query is supported for this repository type, requested "+query);
		}
		
        /*
         * 		String url = baseUrl+"/search";
        		url = url + "?oauth_consumer_key=" + URLEncoder.encode(apiKey, "UTF-8");
        		url = url + "&"+queryParams;
        		url = url + "&offset="+offset;
        		url = url + "&rows=" +rows;
         * 		
         */

		String[] searchWordCriteria=criterias.get("ngsearchword");

		List<String> extSearch = new ArrayList<>();
		
 		String searchWord = searchWordCriteria[0];
		if (searchWord.equals("*") ){
			searchWord="";
		}

		boolean retval;		

 		if(criterias.containsKey("title")) {
 			String ddbTitle =criterias.get("title")[0];
 			if (!ddbTitle.equals("") ){
 				extSearch.add("title:("+ddbTitle+")");
 			}
 		}

 		if(criterias.containsKey("place")) {
 			String ddbPlace =criterias.get("place")[0];
 			if (!ddbPlace.equals("") ){
 				extSearch.add("place:("+ddbPlace+")");
 			}
 		}

 		if(criterias.containsKey("affiliate")) {
 			String ddbPerson =criterias.get("affiliate")[0];
 			if (!ddbPerson.equals("") ){
 				extSearch.add("affiliate:("+ddbPerson+")");
 			}
 		}
		
		HttpsURLConnection connection=null;

/*		if(searchToken.getFrom()%searchToken.getMaxResult()!=0)
			throw new Exception("Pixabay only supports offsets which are dividable by the maxItems count");
	*/	
		String ext = "";
		for (String s : extSearch) {
		    if (!s.equals("") && !extSearch.get(extSearch.size() - 1).equals(s)){
		     ext = ext+s +" AND ";	
		    }else{
			     ext = ext+s;	
		    }
		}
		
		
		try {


//&amount=20&page=2
			String offset = "&page="+searchToken.getFrom();
			String rows = "&amount="+searchToken.getMaxResult();
			
			String uri="/search/json/?query="+org.springframework.extensions.surf.util.URLEncoder.encodeUriComponent(searchWord+" "+ext)+offset+rows;

			searchToken.setQueryString(uri);
			
			return searchOCA(repositoryId,APIKey,uri);
			
		}
		catch(IOException e){
			InputStream is=connection.getErrorStream();
			StringBuilder responseStrBuilder = new StringBuilder();
			String line;
			BufferedReader bR = new BufferedReader(  new InputStreamReader(is));
			while((line =  bR.readLine()) != null){
			    responseStrBuilder.append(line);
			}
			is.close();
			throw new Exception("Error from DDB: "+connection.getResponseCode()+" "+responseStrBuilder.toString());
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}

	}	
	
}
