package org.edu_sharing.service.search;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeServiceBrockhausImpl;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.util.StreamUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchServiceBrockhausImpl extends SearchServiceAdapter{

	private static final String BROCKHAUS_API = "https://api2.brockhaus.de/search";
	private final String apiKey;
	private final ApplicationInfo appInfo;

	Logger logger = Logger.getLogger(SearchServiceBrockhausImpl.class);
	String repositoryId = null;

	public SearchServiceBrockhausImpl(String appId) {
		this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();
		this.apiKey = appInfo.getApiKey();
	}
	public SearchResultNodeRef searchBrockhaus(String path) throws Exception{
		String url=BROCKHAUS_API+path;

		HttpQueryTool query = new HttpQueryTool();
		Map<String, String> headers=new HashMap<>();
		headers.put("Content-Type","application/json");
		headers.put("Accept","application/json");
		JSONObject result = new JSONObject(query.query(url,headers,null));
		result=result.getJSONObject("result");
		JSONArray documents=result.getJSONArray("document");

		SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
		List<NodeRef> data=new ArrayList<>();
		searchResultNodeRef.setNodeCount(result.getInt("numFound"));
		searchResultNodeRef.setData(data);
		for(int i=0;i<documents.length();i++){
			JSONObject document=documents.getJSONObject(i);

			Map<String,Object> properties=new HashMap<>();
			// swagger doesn't like / as %2F encoded, so we try to prevent issues by mapping the data
			properties.put(CCConstants.SYS_PROP_NODE_UID,document.getString("url").replace("/","%2f"));
			properties.put(CCConstants.CM_PROP_C_MODIFIED,System.currentTimeMillis());

			properties.put(CCConstants.CM_NAME,document.getString("title"));
			properties.put(CCConstants.LOM_PROP_GENERAL_TITLE,document.getString("title"));
			properties.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION,document.getString("summary"));
			properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "application/xhtml+xml");

			if(document.has("thumbnail")) {
				properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, document.getString("thumbnail"));
			}else {
				properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, new MimeTypesV2(ApplicationInfoList.getHomeRepository()).getPreviewPath() + "link.svg");
			}
			properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"brockhaus");
			//String contentUrl=buildUrl(apiKey,document.getString("url"));
			//properties.put(CCConstants.CONTENTURL,URLTool.getRedirectServletLink(repositoryId, document.getString("url")));
			properties.put(CCConstants.CONTENTURL,buildUrl(apiKey, (String) properties.get(CCConstants.SYS_PROP_NODE_UID)));
			properties.put(CCConstants.CCM_PROP_IO_WWWURL,buildUrl(apiKey, (String) properties.get(CCConstants.SYS_PROP_NODE_UID)));

			NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(repositoryId,
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),properties);
			data.add(ref);

			NodeServiceBrockhausImpl.updateCache(properties);

		}
		return searchResultNodeRef;
	}
	public static String buildUrl(String apiKey,String id){
		String prefix = "";
		if(id.startsWith("%2fkilex")) {
			prefix = "/junior";
		}
		return "https://www.brockhaus.de/portal/user/"+URLEncoder.encodeUriComponent(apiKey)+"?url="+URLEncoder.encodeUriComponent(prefix + "/ecs/" + id.replace("%2f", "/"));
	}
	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
									  SearchToken searchToken) throws Throwable {
				
		if(!MetadataSet.DEFAULT_CLIENT_QUERY.equals(query)){
			throw new Exception("Only ngsearch query is supported for this repository type, requested "+query);
		}

		String[] searchWordCriteria=criterias.get(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA);
		if(searchWordCriteria == null){
			searchWordCriteria = new String[] {
					!StringUtils.isEmpty(appInfo.getRecommend_objects_query()) ?
							appInfo.getRecommend_objects_query() : "*"};
		}
		String searchWord = searchWordCriteria[0];
		String src="ecs";
		if(criterias.get("src")!=null)
			src=criterias.get("src")[0];

		try {
			String uri="?q="+URLEncoder.encodeUriComponent(searchWord)+
					"&src="+URLEncoder.encodeUriComponent(src)+
					"&grouped=1"+
					"&offset="+searchToken.getFrom()+
					"&size="+searchToken.getMaxResult();

			searchToken.setQueryString(uri);
			return searchBrockhaus(uri);
			
		}
		catch (Throwable t) {
			logger.warn(t.getMessage(),t);
			throw new Exception("Error communicating with the Brockhaus API");
		}

	}

}
