package org.edu_sharing.repository.server.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RecordHandlerDublinCoreDMG implements RecordHandlerInterface {

	Log logger = LogFactory.getLog(RecordHandlerDublinCoreDMG.class);
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	String metadataSetId = null;
	Map<String, Object> toSafeMap = new HashMap<>();
	
	int counter = 0;
	
	@Override
	public Map<String, Object> getProperties() {
		return toSafeMap;
	}
	
	public RecordHandlerDublinCoreDMG(String metadataSetId) {
		logger.info("initializing...");
		
		if(metadataSetId == null || metadataSetId.trim().equals("")){
			metadataSetId = "default";
		}
		this.metadataSetId = metadataSetId;
	}
	
	@Override
	public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {
		logger.debug("starting...");
		toSafeMap.clear();
		if(counter > 2000){
			logger.info("to much records for one resumption:"+counter);
			return;
		}
		
		String replicationId = (String) xpath.evaluate("header/identifier", nodeRecord, XPathConstants.STRING);
		
		
		String lrt = "data";
		if("doc-type:image".equals(set)){
			lrt = "image";
		}
		if("doc-type:manim".equals(set)){
			lrt = "animation";
		}
		if("doc-type:iactm".equals(set)){
			lrt = "role_play";
		}
		if("doc-type:video".equals(set)){
			lrt = "video";
		}
		if("doc-type:biogr".equals(set)){
			lrt = "literature";
		}
		
		
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, lrt);
		
		
		String lomCatalogId = null;
		if(replicationId != null && replicationId.split(":").length == 3){
			lomCatalogId = replicationId.substring(0,replicationId.lastIndexOf(":"));
		}
		
		Map<String, Object> generalIdentifierToSafeMap = new HashMap<>();
		generalIdentifierToSafeMap.put(CCConstants.LOM_PROP_IDENTIFIER_ENTRY, replicationId);
		generalIdentifierToSafeMap.put(CCConstants.LOM_PROP_IDENTIFIER_CATALOG, lomCatalogId);
		
		toSafeMap.put(CCConstants.CCM_PROP_IO_OBJECTTYPE, "0");
		toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_IDENTIFIER + "#" + CCConstants.LOM_ASSOC_IDENTIFIER, generalIdentifierToSafeMap);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationId);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, lomCatalogId);
		
		logger.info("lomCatalogId:" + lomCatalogId + " replicationId:" + replicationId);
		
		String title = (String) xpath.evaluate("metadata/dc/title", nodeRecord, XPathConstants.STRING);
		logger.info("title:"+title);
		if(title != null){
			String safeTitle = title.replaceAll( ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), "_");
			safeTitle = clearEnd(safeTitle);
			
			
			logger.info("safeTitle:"+safeTitle);
			toSafeMap.put(CCConstants.CM_NAME, safeTitle);
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_TITLE, title);
			
			String keywords = title;
			keywords = keywords.replaceAll(",", " ");
			String[] splittetTitle = keywords.split(" ");
			ArrayList<String> listTitle = new ArrayList<>();
			if(splittetTitle != null && splittetTitle.length > 0){
				for(String titleEle : splittetTitle){
					titleEle = titleEle.trim();
					if(titleEle.length() > 3){
						listTitle.add(titleEle);
					}
				}
			}
			
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_KEYWORD, listTitle);
			
			
		}
		
		NodeList descriptionList = (NodeList) xpath.evaluate("metadata/dc/description", nodeRecord, XPathConstants.NODESET);
		
		
		//description and thumbnail, description is not a multivalue property in edu-sharing so we concat the dc description multivalue
		String description = null;
		for(int i = 0; i < descriptionList.getLength(); i++){
			Node descNode = descriptionList.item(i);
			String descrString = ((String)xpath.evaluate(".", descNode, XPathConstants.STRING)).trim();
			if(descrString.contains("http://") && (descrString.endsWith(".jpg") || descrString.endsWith(".png") || descrString.endsWith(".gif") || descrString.endsWith(".jpeg"))){
				toSafeMap.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, descrString);
			}else{
				description = (description == null) ? descrString : description + "; "+descrString;
			}
			
			logger.info("descrString:"+descrString);
		}
		if(description  != null){
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, description);
		}
		
		NodeList metadataCreator =  (NodeList) xpath.evaluate("metadata/dc/creator", nodeRecord, XPathConstants.NODESET);
		for(int i = 0; i < metadataCreator.getLength(); i++){
			Node metadataCreatorNode = metadataCreator.item(i);
			String metadataCreatorString = ((String)xpath.evaluate(".", metadataCreatorNode, XPathConstants.STRING)).trim();
			if(!metadataCreatorString.equals("")){
				Map<String,String> vcardMap = new HashMap<>();
				vcardMap.put(CCConstants.VCARD_SURNAME, metadataCreatorString);
				String vcardString = VCardTool.hashMap2VCard(vcardMap);
				
				Map<String,Object> contributerMetadataCreator = new HashMap<>();
				contributerMetadataCreator.put(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY, vcardString);
				contributerMetadataCreator.put(CCConstants.LOM_PROP_CONTRIBUTE_ROLE, "creator");
				toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_CONTRIBUTE + "#" + CCConstants.LOM_ASSOC_META_METADATA_CONTRIBUTE, contributerMetadataCreator);
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR, vcardString);
			}
		}
		
		NodeList contributerPublisher = (NodeList) xpath.evaluate("metadata/dc/publisher", nodeRecord, XPathConstants.NODESET);
		for(int i = 0; i < contributerPublisher.getLength(); i++){
			Node contributerPublisherNode= contributerPublisher.item(i);
			String contributerPublisherString = ((String)xpath.evaluate(".", contributerPublisherNode, XPathConstants.STRING)).trim();
			if(!contributerPublisherString.equals("")){
				Map<String,String> vcardMap = new HashMap<>();
				vcardMap.put(CCConstants.VCARD_SURNAME, contributerPublisherString);
				String vcardString = VCardTool.hashMap2VCard(vcardMap);
				
				Map<String,Object> contributerPublisherMap  = new HashMap<>();
				contributerPublisherMap.put(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY, vcardString);
				contributerPublisherMap.put(CCConstants.LOM_PROP_CONTRIBUTE_ROLE, "publisher");
				toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_CONTRIBUTE + "#" + CCConstants.LOM_ASSOC_LIFECYCLE_CONTRIBUTE, contributerPublisherMap);
				toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER, vcardString);
			}
		}
		
		NodeList identifierList = (NodeList) xpath.evaluate("metadata/dc/identifier", nodeRecord, XPathConstants.NODESET);
		String technicalLocation = null;
		for(int i = 0; i < identifierList.getLength(); i++){
			Node identifierNode = identifierList.item(i);
			String identifierElement = (String)xpath.evaluate(".", identifierNode, XPathConstants.STRING);
			//if it's a protocol like http://
			if(identifierElement.matches("[a-zA-Z]*://.*")){
				technicalLocation = identifierElement;
			}
		}
		
		if(technicalLocation != null && !technicalLocation.trim().equals("")){
			toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, technicalLocation);
		}
		
		String lomRights = (String) xpath.evaluate("metadata/dc/rights", nodeRecord, XPathConstants.STRING);
		toSafeMap.put(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION, lomRights);	
		
		counter++;
	}
	
	String clearEnd(String toClear){
		
		String safeTitle = toClear;
		
		//filename should not end with " "
		safeTitle = safeTitle.trim();
		
		//filename should not end with "."
		if(safeTitle.endsWith(".")){
			safeTitle = safeTitle.substring(0, safeTitle.length() - 1);
			clearEnd(safeTitle);
		}
		
		return safeTitle;
	}
	
}
