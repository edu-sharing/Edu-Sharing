package org.edu_sharing.service.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.ISO8601DateFormat;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.URLEncoder;

public class SearchServiceTutoryImpl extends SearchServiceAdapter{
	
	private String repositoryId;



	public SearchServiceTutoryImpl(String appId) {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
	}

	
	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
									  SearchToken searchToken) throws Throwable {
		
		if(!MetadataSet.DEFAULT_CLIENT_QUERY.equals(query)){
			throw new Exception("Only ngsearch query is supported for this repository type, requested "+query);
		}
		
		String[] searchWordCriteria=criterias.get(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA);
		if(searchWordCriteria == null){
			searchWordCriteria = new String[] {"*"};
		}
		
		String searchWord = searchWordCriteria[0];
		String url = "https://www.tutory.de/api/v1/worksheet?q="+URLEncoder.encodeUriComponent(searchWord) + "&page="+searchToken.getFrom()+"&pageSize="+searchToken.getMaxResult()+"&public=1";
		
		
		JSONObject result= new JSONObject(getHttpResult(url));
		JSONArray jsonArray = result.getJSONArray("worksheets");
		
		SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
		List<NodeRef> data=new ArrayList<>();
		searchResultNodeRef.setNodeCount(result.getInt("total"));
		searchResultNodeRef.setData(data);
		
		for(int i = 0; i < jsonArray.length(); i++) {
			JSONObject worksheet = jsonArray.getJSONObject(i);
			
			org.edu_sharing.service.model.NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(repositoryId, 
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),getProperties(worksheet));
			data.add(ref);
		}
		
		return searchResultNodeRef;
	}
	
	public static Map<String,Object> getProperties(JSONObject worksheet) throws JSONException{
		Map<String,Object> properties=new HashMap<>();
		String id = worksheet.getString("id");
		properties.put(CCConstants.SYS_PROP_NODE_UID,id);
		properties.put(CCConstants.LOM_PROP_GENERAL_TITLE,worksheet.getString("name"));
		properties.put(CCConstants.CM_NAME,worksheet.getString("name"));
		String description = worksheet.getString("description");
		if(description != null && !description.trim().equals("")) {
			properties.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION,description);
		}
		String thumbUrl = "https://www.tutory.de/worksheet/"+id+".jpg";
		
		//for the source icon
		properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, "tutory");
		properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, thumbUrl);
		properties.put(CCConstants.CM_ASSOC_THUMBNAILS, thumbUrl);
		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, "https://www.tutory.de/worksheet/"+id);
		properties.put(CCConstants.CCM_PROP_IO_WWWURL, "https://www.tutory.de/worksheet/"+id);
		
		String created = worksheet.getString("createdAt");
		Date createdDate = ISO8601DateFormat.parse(created);
		if(createdDate != null) {
			properties.put(CCConstants.CM_PROP_C_CREATED, createdDate.getTime());
		}
		
		String updated = worksheet.getString("updatedAt");
		Date updatedDate = ISO8601DateFormat.parse(updated);
		if(updatedDate != null) {
			properties.put(CCConstants.CM_PROP_C_MODIFIED, updatedDate.getTime());
		}
		
		/**
		 * @todo license
		 */
		return properties;
	}
	
	public static String getHttpResult(String url) throws KeyManagementException, NoSuchAlgorithmException, MalformedURLException, IOException {
		HttpsURLConnection connection = openUrl(new URL(url));
		connection.connect();
		InputStream is=connection.getInputStream();
		StringBuilder responseStrBuilder = new StringBuilder();
		String line;
		BufferedReader bR = new BufferedReader(  new InputStreamReader(is));
		while((line =  bR.readLine()) != null){
		    responseStrBuilder.append(line);
		}
		is.close();
		String jsonString = responseStrBuilder.toString();
		return jsonString;
	}
	
	
	
	public static HttpsURLConnection openUrl(URL url) throws KeyManagementException, IOException, NoSuchAlgorithmException{
		return (HttpsURLConnection) url.openConnection();
	}
}
