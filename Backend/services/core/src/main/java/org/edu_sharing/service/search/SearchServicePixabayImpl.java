package org.edu_sharing.service.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeServicePixabayImpl;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.URLEncoder;

public class SearchServicePixabayImpl extends SearchServiceAdapter{

	private static final String PIXABAY_API = "https://pixabay.com/api/";
	public static final String PIXABAY_LICENSE_URL = "https://pixabay.com/de/service/terms/#license";

	Logger logger = Logger.getLogger(SearchServicePixabayImpl.class);
	
	String repositoryId = null;

	String APIKey = null;
	private static Cache<String, String> searchCache = CacheBuilder.newBuilder()
			.maximumSize(100)
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build();

	public SearchServicePixabayImpl(String appId) {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		APIKey = appInfo.getApiKey(); 

	}
	public static HttpsURLConnection openPixabayUrl(URL url) throws KeyManagementException, IOException, NoSuchAlgorithmException{
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		// required, otherwise 403 will be thrown
		conn.setRequestProperty("User-Agent", "edu-sharing pixabay API");
		return conn;

		// not required, the server should have a current java jdk version!
		/*HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		
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
		*/
	}
	public static SearchResultNodeRef searchPixabay(String repositoryId,String apiKey,String path) throws Exception{
		String lang=new AuthenticationToolAPI().getCurrentLocale().split("_")[0];
		String url=PIXABAY_API+"?key="+URLEncoder.encodeUriComponent(apiKey)+"&lang="+lang+path;
		String jsonString=searchCache.getIfPresent(url);
		if(jsonString!=null){
			// already fetched
		}
		else{
			URL urlURL=new URL(url);
			HttpsURLConnection connection = openPixabayUrl(urlURL);
			connection.connect();
			InputStream is=connection.getInputStream();
			StringBuilder responseStrBuilder = new StringBuilder();
			String line;
			BufferedReader bR = new BufferedReader(  new InputStreamReader(is));
			while((line =  bR.readLine()) != null){
			    responseStrBuilder.append(line);
			}
			is.close();
			jsonString=responseStrBuilder.toString();
			searchCache.put(url,jsonString);
		}
		JSONObject result= new JSONObject(jsonString);       
		JSONArray array=result.getJSONArray("hits");
		SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
		List<NodeRef> data=new ArrayList<>();
		searchResultNodeRef.setNodeCount(result.getInt("total"));
		searchResultNodeRef.setData(data);
		for(int i=0;i<array.length();i++){
			JSONObject json=array.getJSONObject(i);

			Object id = json.get("id");
			Map<String,Object> properties=new HashMap<>();
			properties.put(CCConstants.SYS_PROP_NODE_UID,id.toString());
			properties.put(CCConstants.CM_PROP_C_MODIFIED,System.currentTimeMillis());
			properties.put(CCConstants.LOM_PROP_GENERAL_TITLE,json.getString("tags"));
			properties.put(CCConstants.LOM_PROP_GENERAL_KEYWORD,json.getString("tags").replace(", ",CCConstants.MULTIVALUE_SEPARATOR));
			String[] split=json.getString("webformatURL").split("\\.");;
			String extension=split[split.length-1].toLowerCase();
			if(extension.equals("jpg"))
				properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "image/jpeg");
			if(extension.equals("png"))
				properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "image/png");
			if(json.getString("type").equals("film"))
				properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "video/*");
			properties.put(CCConstants.CM_NAME,json.getString("tags")+"."+extension);
			properties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,CCConstants.COMMON_LICENSE_CUSTOM);
			properties.put(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION, PIXABAY_LICENSE_URL);
			properties.put(CCConstants.CM_PROP_C_CREATOR,json.getString("user"));
			properties.put(CCConstants.NODECREATOR_FIRSTNAME,json.getString("user"));
			properties.put(CCConstants.NODEMODIFIER_FIRSTNAME,json.getString("user"));
			String author=VCardTool.nameToVCard(json.getString("user"));
			properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR,author);
			properties.put(CCConstants.VIRT_PROP_USAGECOUNT,json.getInt("downloads"));
			properties.put(CCConstants.CCM_PROP_IO_WIDTH,json.get("imageWidth"));
			properties.put(CCConstants.CCM_PROP_IO_HEIGHT,json.get("imageHeight"));
			properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"pixabay");
			//properties.put(CCConstants.CONTENTURL,json.getString("pageURL"));
			properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION,json.getString("pageURL"));
			properties.put(CCConstants.CCM_PROP_IO_WWWURL,json.getString("pageURL"));
			properties.put(CCConstants.CONTENTURL,URLTool.getRedirectServletLink(repositoryId, id.toString()));
			properties.put(CCConstants.VIRT_PROP_PERMALINK,json.getString("pageURL"));
			Pattern p = Pattern.compile(".*\\/\\/.*\\/.*_(.*)\\..*");
			String thumb = json.getString("previewURL");
			Matcher m = p.matcher(thumb);
			if (m.find()) {
				/*
				Supported sizes:
					_1024_1280.jpg
					_1280.jpg
					_1440_2560.jpg
					_150.jpg
					__180.jpg
					__240.jpg
					__340.jpg
					__480.jpg
					_640.jpg
					_960_720.jpg
				 */
				thumb = thumb.substring(0,m.start(1)) + "1280" + thumb.substring(m.end(1));
				properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL,thumb);
			}
			//properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL,json.getString("webformatURL"));//.replace("_640", "_960"));
			properties.put(CCConstants.DOWNLOADURL,URLTool.getDownloadServletUrl(id.toString(),null,true, repositoryId));
			org.edu_sharing.service.model.NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(repositoryId, 
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),properties);
			data.add(ref);
			NodeServicePixabayImpl.updateCache(properties);
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
		String imageType="all";
		String orientation="all";
		String[] searchWordCriteria=criterias.get(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA);
		if(criterias.containsKey("editors_choice"))
			editorsChoice=criterias.get("editors_choice")[0].equals("true");
		if(criterias.containsKey("image_type"))
			imageType = criterias.get("image_type")[0];
		if(criterias.containsKey("orientation"))
			orientation = criterias.get("orientation")[0];
		
		if(searchWordCriteria == null){
			searchWordCriteria = new String[] {"*"};
		}
		String searchWord = searchWordCriteria[0];
		if(searchToken.getFrom()%searchToken.getMaxResult()!=0)
			throw new Exception("Pixabay only supports offsets which are dividable by the maxItems count");
		try {
			String sortBy="popular";
			if(CCConstants.getValidLocalName(CCConstants.CM_PROP_C_MODIFIED).equals(searchToken.getSortDefinition().getFirstSortBy())){
				sortBy="latest";
			}
			int page=1 + searchToken.getFrom()/searchToken.getMaxResult();
			String uri="&q="+URLEncoder.encodeUriComponent(searchWord)+
					"&editors_choice="+editorsChoice+
					"&image_type="+imageType+
					"&orientation="+orientation+
					"&safesearch=true"+
					"&order="+sortBy+
					"&per_page="+searchToken.getMaxResult()+
					"&page="+page;
			searchToken.setQueryString(uri);
			return searchPixabay(repositoryId,APIKey,uri);
			
		}
		catch (Throwable t) {
			logger.warn(t.getMessage(),t);
			throw new Exception("Error communicating with the Pixabay API");
		}

	}

}
