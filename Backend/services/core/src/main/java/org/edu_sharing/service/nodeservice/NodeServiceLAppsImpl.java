package org.edu_sharing.service.nodeservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.model.NodeRef;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeServiceLAppsImpl extends NodeServiceAdapter{

	private static final String LAPP_API = "https://learningapps.org/api.php";

	public NodeServiceLAppsImpl(String appId) {
		super(appId);
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {

		String path = "getappbyid="+nodeId;

		String lang = "DE";
		URL url=new URL(LAPP_API+"?"+path);
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
		searchResultNodeRef.setNodeCount(nc); //@todo
		searchResultNodeRef.setData(data);		

		Node node = lAppNode.item(0);
		NamedNodeMap map = node.getAttributes();

		
		return getPropertiesForNode(this.appId,map);
   }

	public static Map<String,Object> getPropertiesForNode(String appId, NamedNodeMap map) {
		Map<String,Object> properties=new HashMap<>();
		properties.put(CCConstants.SYS_PROP_NODE_UID,map.getNamedItem("id").getNodeValue());
		properties.put(CCConstants.LOM_PROP_GENERAL_TITLE,map.getNamedItem("title").getNodeValue());
		
		properties.put(CCConstants.LOM_PROP_GENERAL_KEYWORD,map.getNamedItem("tags").getNodeValue().replace(" ",CCConstants.MULTIVALUE_SEPARATOR));

		properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, map.getNamedItem("image").getNodeValue());
		properties.put(CCConstants.CONTENTURL,map.getNamedItem("url").getNodeValue());
		properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "application/xhtml+xml");
		properties.put(CCConstants.CCM_PROP_IO_WWWURL,map.getNamedItem("url").getNodeValue());
		properties.put(CCConstants.NODETYPE, CCConstants.CCM_TYPE_IO);
		properties.put(CCConstants.CM_PROP_C_CREATOR,map.getNamedItem("author").getNodeValue());

		String author=VCardTool.nameToVCard(map.getNamedItem("author").getNodeValue());
		properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR,author);
		
		properties.put(CCConstants.REPOSITORY_ID, "LEARNINGAPPS" );
		properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"LearningApps");
		properties.put(CCConstants.CM_PROP_C_CREATOR,map.getNamedItem("author").getNodeValue());
		properties.put(CCConstants.NODECREATOR_FIRSTNAME,map.getNamedItem("author").getNodeValue());
		properties.put(CCConstants.NODEMODIFIER_FIRSTNAME,map.getNamedItem("author").getNodeValue());
		
		String createdate = map.getNamedItem("created").getNodeValue();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
		LocalDateTime date  = LocalDateTime.parse(createdate, formatter);
		DateTimeFormatter formatter2 = DateTimeFormatter.ISO_DATE_TIME;//ofPattern("dd.MM.yyyy", Locale.GERMAN);
		properties.put(CCConstants.CM_PROP_C_MODIFIED,Date.from(date.atZone(ZoneId.systemDefault()).toInstant()).getTime());
		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION,"https://learningapps.org/view"+map.getNamedItem("id").getNodeValue());


		String nodeurl = URLTool.getRedirectServletLink(appId, map.getNamedItem("id").getNodeValue());
		properties.put(CCConstants.CONTENTURL,nodeurl);
		return properties;
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
}
