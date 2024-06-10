package org.edu_sharing.repository.server.importer;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RecordHandlerMods implements RecordHandlerInterface {
	
	Logger logger = Logger.getLogger(RecordHandlerMods.class);
	
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	String metadataSetId = null;
	Map<String, Object> toSafeMap = new HashMap<>();
	
	public RecordHandlerMods(String metadataSetId) {
		if(metadataSetId == null || metadataSetId.trim().equals("")){
			metadataSetId = "default";
		}
		this.metadataSetId = metadataSetId;
	}
	
	@Override
	public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {
		logger.debug("starting...");
		toSafeMap.clear();
		
		Node nodeMods = (Node) xpath.evaluate("metadata/mods", nodeRecord, XPathConstants.NODE);
		
		String replicationId = (String) xpath.evaluate("header/identifier", nodeRecord, XPathConstants.STRING);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationId);
		
		//take the first setSpec as catalogId
		String catalogId = (String) xpath.evaluate("header/setSpec", nodeRecord, XPathConstants.STRING);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, catalogId);
		
		String title = (String) xpath.evaluate("titleInfo/title", nodeMods, XPathConstants.STRING);
		String safeTitle = NodeServiceHelper.cleanupCmName(title);
		toSafeMap.put(CCConstants.CM_NAME, safeTitle);
		toSafeMap.put(CCConstants.LOM_PROP_GENERAL_TITLE, title);
		
		String description = (String) xpath.evaluate("abstract", nodeMods, XPathConstants.STRING);
		toSafeMap.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, description);
		
		String keywords = (String) xpath.evaluate("subject/topic", nodeMods, XPathConstants.STRING);
		if(keywords != null) {
			String[] splitted = keywords.split(",");
			List<String> kws = new ArrayList<>();
			for(String kw : splitted) {
				kws.add(kw);
			}
			
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_KEYWORD, kws);
		}
		
		String thumbnailUrl = (String) xpath.evaluate("location/url[@access='preview']", nodeMods, XPathConstants.STRING);
		toSafeMap.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, thumbnailUrl);
			
		String technicalLocation = (String) xpath.evaluate("location/url[@access='raw object']", nodeMods, XPathConstants.STRING);
		toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, technicalLocation);

		String wwwurl = (String) xpath.evaluate("location/url[@access='object in context']", nodeMods, XPathConstants.STRING);
		toSafeMap.put(CCConstants.CCM_PROP_IO_WWWURL, wwwurl);
		
		String lrt = (String) xpath.evaluate("typeOfResource", nodeMods, XPathConstants.STRING);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, lrt);
		
		String language = (String) xpath.evaluate("language/languageTerm", nodeMods, XPathConstants.STRING);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LANGUAGE, language);
		
		NodeList contributer = (NodeList) xpath.evaluate("name[@type='personal']", nodeMods, XPathConstants.NODESET);
		
		for(int i = 0; i < contributer.getLength(); i++) {
			String role = (String) xpath.evaluate("role/roleTerm[@type='text']", contributer.item(i), XPathConstants.STRING);
			String firstName = (String) xpath.evaluate("namePart[@type='given']", contributer.item(i), XPathConstants.STRING);
			String lastName = (String) xpath.evaluate("namePart[@type='family']", contributer.item(i), XPathConstants.STRING);
			Map<String,String> properties = new HashMap<>();
			properties.put(CCConstants.VCARD_SURNAME, lastName );
			properties.put(CCConstants.VCARD_GIVENNAME, firstName );
			if(role.equals("Author")) {
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR, VCardTool.hashMap2VCard(properties));
			}
			//@TODO more roles
		}
		
		toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, technicalLocation);
		
		String licenseLink = (String) xpath.evaluate("accessCondition[@type='use and reproduction']/@href", nodeMods, XPathConstants.STRING);
		if(licenseLink != null && licenseLink.trim().length() > 0) {
			String rawLicense = licenseLink.split("#")[1];
			if("cc_by-sa_4.0".equals(rawLicense)) {
				toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_SA);
				toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION, "4.0");
			}
		}
		//@TODO other licenses
		
		
		String classiFicationSdnb = (String) xpath.evaluate("classification[@authority='sdnb']", nodeMods, XPathConstants.STRING);
		if(classiFicationSdnb != null) {
			String classificationName = getClassificationDisplayName("SDNB",classiFicationSdnb);
			if(classificationName != null) {
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ID, classiFicationSdnb);
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY, classificationName);
			}
		}
		
		
		
	}
	
	private String getClassificationDisplayName(String classificationId, String classificationEntryId) throws IOException, JSONException, SAXException, ParserConfigurationException, XPathExpressionException {
		URL url = new URL("https://oer.uni-due.de/api/v1/classifications");
		String classificastionList = query(url);
		JSONObject jsClassificastionList = new JSONObject(classificastionList);
		JSONArray jsClassificastionArray = jsClassificastionList.getJSONArray("mycoreclass");
		for(int i = 0; i < jsClassificastionArray.length(); i++) {
			JSONObject classentry = jsClassificastionArray.getJSONObject(i);
			String id = classentry.getString("ID");
			if(classificationId.equals(id)) {
				String link = classentry.getString("href");
				String classificationResult = query(new URL(link));
				XPathFactory pfactory = XPathFactory.newInstance();
				XPath xpath = pfactory.newXPath();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(new InputSource(new StringReader(classificationResult)));
				
				String result = (String)xpath.evaluate("/mycoreclass/categories/category[@ID='" + classificationEntryId + "']/label[@lang='de']/@text", doc, XPathConstants.STRING);
				return result;
			}
		}
		return null;
	}
	
	private String query(URL url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "application/json");
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);

		con.connect();
		String result = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);
		con.disconnect();
		return result;
	}
	
	@Override
	public Map<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return toSafeMap;
	}

}
