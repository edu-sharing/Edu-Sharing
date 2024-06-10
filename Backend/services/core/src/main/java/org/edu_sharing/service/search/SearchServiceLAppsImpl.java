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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeServiceLAppsImpl;
import org.edu_sharing.service.search.model.SearchToken;
import org.springframework.extensions.surf.util.URLEncoder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SearchServiceLAppsImpl extends SearchServiceAdapter{
	
	private static final String LAPP_API = "https://learningapps.org/api.php";

	Logger logger = Logger.getLogger(SearchServiceLAppsImpl.class);
	
	String repositoryId = null;

	String APIKey = null;
			
	public SearchServiceLAppsImpl(String appId) {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		APIKey = appInfo.getApiKey(); 

	}
	public static HttpsURLConnection openLAppsUrl(URL url) throws KeyManagementException, IOException, NoSuchAlgorithmException{
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
	
	public static SearchResultNodeRef searchLApps(String repositoryId,String apiKey,String path) throws Exception{
				
		String lang = "DE";
		URL url=new URL(LAPP_API+"?lang="+lang.toUpperCase()+path);
		HttpsURLConnection connection = openLAppsUrl(url);
		connection.connect();
		
		XPathFactory pfactory = XPathFactory.newInstance();
		XPath xpath = pfactory.newXPath();
		
		InputStream is=connection.getInputStream();
		
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse(is);
		NodeList lAppNode = (NodeList) xpath.evaluate("/results/app", doc, XPathConstants.NODESET);
		Integer nc = lAppNode.getLength();
		SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
		List<NodeRef> data=new ArrayList<>();

		for(int i=0;i<lAppNode.getLength();i++){
			
			Node node = lAppNode.item(i);
			
			NamedNodeMap map = node.getAttributes();
			
			Map<String, Object> properties = NodeServiceLAppsImpl.getPropertiesForNode(repositoryId, map);
			  						
			org.edu_sharing.service.model.NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(repositoryId, 
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),properties);
					
			data.add(ref);
		}
		searchResultNodeRef.setNodeCount(data.size());
		searchResultNodeRef.setData(data);
		return searchResultNodeRef;
	}
	
	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
									  SearchToken searchToken) throws Throwable {
		
		if(!MetadataSet.DEFAULT_CLIENT_QUERY.equals(query)){
			throw new Exception("Only ngsearch query is supported for this repository type, requested "+query);
		}
		
		String lappcategorie="0";
		String lappsubcategorie="0";
		String cat="";
		String edusector="";
		String educationsector="";
		String[] searchWordCriteria=criterias.get(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA);

		boolean retval;		

 		if(criterias.containsKey("lappcategorie")) {
 			if (criterias.get("lappcategorie").length != 0 ){
 			lappcategorie=criterias.get("lappcategorie")[0];
 			cat = "&category="+lappcategorie;
 			
 			retval = lappcategorie.contains("#");
	 			if (retval){
	 				String[] parts = lappcategorie.split("#");
	 				lappcategorie    = parts[0];  
	 				lappsubcategorie = parts[1]; 		
	 				cat = "&category="+lappcategorie+"&subcategory="+lappsubcategorie;
	 			}
 			}
 		}
 		
 		if(criterias.containsKey("educationsector"))
 		{	if ( criterias.get("educationsector").length != 0)
 			{educationsector = criterias.get("educationsector")[0];
 		    if (!educationsector.isEmpty()){
 			 edusector = "&stufeStart="+educationsector+"&stufeEnd="+educationsector;
 		    }
 		    }
 		}
		
		if(searchWordCriteria == null) {
 			searchWordCriteria = new String[] {""};
 		}
		
		String searchWord = searchWordCriteria[0];
		if (searchWord.equals("*") ){
			searchWord="";
		}
		HttpsURLConnection connection=null;

		try {
			int page = 0;
			String uri="&search="+URLEncoder.encodeUriComponent(searchWord)+cat+edusector;
			searchToken.setQueryString(uri);
			SearchResultNodeRef result = searchLApps(repositoryId, APIKey, uri);
			if(result.getData() != null) {
				if(searchToken.getFrom() < result.getData().size()) {
					result.setData(result.getData().subList(searchToken.getFrom(), result.getData().size()));
				}
				int toIdx = searchToken.getMaxResult();
				if((result.getData().size() - 1) < toIdx) {
					result.setData(result.getData());
				} else {
					result.setData(result.getData().subList(0, toIdx));
				}

			}
			return result;
		}
		catch (IOException e) {
			InputStream is=connection.getErrorStream();
			StringBuilder responseStrBuilder = new StringBuilder();
			String line;
			BufferedReader bR = new BufferedReader(  new InputStreamReader(is));
			while((line =  bR.readLine()) != null){
			    responseStrBuilder.append(line);
			}
			is.close();
			throw new Exception("Error from LearningApp: "+connection.getResponseCode()+" "+responseStrBuilder.toString());
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}

	}	
	
}
