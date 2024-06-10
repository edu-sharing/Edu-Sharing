/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.importer;

import java.io.IOException;
import java.io.StringReader;
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

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class ImportCleaner {

	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();

	Logger logger = Logger.getLogger(ImportCleaner.class);

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	String oaiBaseUrl = null;
	List<String> allowedCataloges = null;
	
	String metadataPrefix = null;
	
	public ImportCleaner(String oaiBaseUrl, List<String> allowedCataloges, String metadataPrefix){
		this.oaiBaseUrl = oaiBaseUrl;
		this.allowedCataloges = allowedCataloges;
		this.metadataPrefix = metadataPrefix;
	}

	public boolean nodeExists(String replicationSourceId, String replicationCatalog) throws org.xml.sax.SAXParseException, ParserConfigurationException, IOException, SAXException, XPathExpressionException{
		
		metadataPrefix = (metadataPrefix == null) ? "oai_lom-de" : metadataPrefix;
		
		// there is no service to check edmond Objects
		if (!allowedCataloges.contains(replicationCatalog.trim())) {
			logger.debug("replicationCatalog:"+replicationCatalog+" is not an allowed catalog for this job. so we can not find out if node"+ replicationSourceId +" exists. returning true");
			return true;
		} else {
			String urlGetRecord = (oaiBaseUrl.contains("?")) ? oaiBaseUrl + "&verb=GetRecord" : oaiBaseUrl + "?verb=GetRecord";
			String url = urlGetRecord + "&identifier=" + replicationSourceId + "&metadataPrefix=" + this.metadataPrefix;
			logger.debug("url:"+url);
			String result = new HttpQueryTool().query(url);
			if (result != null && !result.trim().equals("")) {
				
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document doc = builder.parse(new InputSource(new StringReader(result)));
					//watch out if the node got's the deleted status
					String deleted = (String) xpath.evaluate("/OAI-PMH/GetRecord/record/header/@status", doc, XPathConstants.STRING);
					if(deleted != null && deleted.trim().equals("deleted")){
						logger.info("replicationCatalog:"+replicationCatalog+" replicationSourceId:"+replicationSourceId+" has the deleted state. returning false");
						return false;
					}
					
					//watch out if the node still exists
					String error = (String) xpath.evaluate("/OAI-PMH/error", doc, XPathConstants.STRING);
					String errorcode = (String) xpath.evaluate("/OAI-PMH/error/@code", doc, XPathConstants.STRING);
					if (error == null || error.trim().equals("")) {
						logger.debug("replicationCatalog:"+replicationCatalog+" replicationSourceId:"+replicationSourceId+" exists. returning true");
						return true;
					} else {
						if(errorcode != null && errorcode.trim().equals("idDoesNotExist")){
							logger.info("url:"+url);
							logger.info("replicationCatalog:"+replicationCatalog+" replicationSourceId:"+replicationSourceId+" does NOT exists. returning false");
							return false;
						}else{
							logger.info("url:"+url);
							logger.info("unknown error code "+ errorcode +" for error:"+error+", returning true");
							return true;
						}
					}
				

			}
		}
		return true;
	}
	
	
	public void removeDeletedImportedObjects(List<NodeRef> allNodes) throws Throwable{
		logger.info("starting");
		
		ApplicationInfo homeRep  = ApplicationInfoList.getHomeRepository();
		AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
		Map<String, String>  authInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());
		MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient ) RepoFactory.getInstance(homeRep.getAppId(), authInfo);
		int countDeletedObjects = 0;
		if(allNodes != null){
			
			for(NodeRef entry : allNodes){
				String alfNodeId = entry.getId();
				String importedKatalog = NodeServiceHelper.getProperty(entry,CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
				String importedId = NodeServiceHelper.getProperty(entry,CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
				
				boolean nodeExists = nodeExists(importedId, importedKatalog);
				if(!nodeExists){
					logger.info("Node with REPLICATIONSOURCEID:"+importedId+" REPLICATIONSOURCE:"+importedKatalog+" seems deleted. Delete the imported Object nodeId:"+alfNodeId);
					Map<String, Map<String, Object>> primaryParents = mcAlfrescoBaseClient.getParents(alfNodeId, true);
					Map.Entry<String, Map<String,Object>> primaryParentEntry = primaryParents.entrySet().iterator().next();
					mcAlfrescoBaseClient.removeNode(alfNodeId, primaryParentEntry.getKey());
					countDeletedObjects++;
				}
			}
			logger.info("returns (deleted objects counter:" + countDeletedObjects + ", processed nodes: "+allNodes.size()+")");
		}
	}
	
}
