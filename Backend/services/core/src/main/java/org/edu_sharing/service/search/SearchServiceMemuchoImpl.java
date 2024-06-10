package org.edu_sharing.service.search;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
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


public class SearchServiceMemuchoImpl extends SearchServiceAdapter{

	private static final String MEMUCHO_API = "https://memucho.de/api/edusharing/search?pageSize=100&term=";
	//http://stage.memucho.de/api/edusharing/topic?id=

	Logger logger = Logger.getLogger(SearchServiceMemuchoImpl.class);

	String repositoryId = null;

	public SearchServiceMemuchoImpl(String appId) {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();
	}

	public static HttpURLConnection openMemuchoUrl(URL url) throws KeyManagementException, IOException, NoSuchAlgorithmException{
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
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

	public static SearchResultNodeRef searchMemucho( String searchWord, String repositoryId) throws Exception {

		String url = MEMUCHO_API + searchWord;
		String jsonString;

		URL urlURL = new URL(url);
		HttpURLConnection connection = openMemuchoUrl(urlURL);
		connection.connect();
		InputStream is = connection.getInputStream();
		StringBuilder responseStrBuilder = new StringBuilder();
		String line;
		BufferedReader bR = new BufferedReader(new InputStreamReader(is));
		while ((line = bR.readLine()) != null) {
			responseStrBuilder.append(line);
		}
		is.close();
		jsonString = responseStrBuilder.toString();
		JSONObject result = new JSONObject(jsonString);

		JSONArray array = result.getJSONArray("Items");
		SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
		List<NodeRef> data=new ArrayList<>();
		searchResultNodeRef.setData(data);
		searchResultNodeRef.setNodeCount(searchResultNodeRef.getNodeCount() + array.length());
		for (int i = 0; i < array.length(); i++) {
			JSONObject json = array.getJSONObject(i);
				Map<String, Object> properties = new HashMap<>();
			properties.put(CCConstants.SYS_PROP_NODE_UID, json.getString("TopicId"));
			properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, json.getString("Name"));
			properties.put(CCConstants.CM_NAME, json.getString("Name"));
			properties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,  json.getString("Licence"));
			properties.put(CCConstants.CCM_PROP_IO_WWWURL, json.getString("ItemUrl"));
			properties.put(CCConstants.CM_PROP_C_MODIFIED, System.currentTimeMillis());
			String author=VCardTool.nameToVCard(json.getString("Author"));
			properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR,author);
			properties.put(CCConstants.CM_ASSOC_THUMBNAILS, json.getString("ImageUrl"));
			properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"memucho");
			properties.put(CCConstants.CONTENTURL,json.getString("ItemUrl"));
			properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION,json.getString("ItemUrl"));
System.out.println(json);
				NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(repositoryId,
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), properties);
				data.add(ref);

			}
		return searchResultNodeRef;
	}

	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
									  SearchToken searchToken) throws Throwable {
				
		if(!MetadataSet.DEFAULT_CLIENT_QUERY.equals(query)){
			throw new Exception("Only ngsearch query is supported for this repository type, requested "+query);
		}
		
		boolean editorsChoice=false;
		String[] searchWordCriteria=criterias.get(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA);
		if(searchWordCriteria == null){
			searchWordCriteria = new String[] {""};
		}
		String searchWord = searchWordCriteria[0];
		HttpsURLConnection connection=null;

		try {

			return searchMemucho(searchWord, this.repositoryId);
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
			throw new Exception("Error from Memucho: "+connection.getResponseCode()+" "+responseStrBuilder.toString());
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}

	}

}
