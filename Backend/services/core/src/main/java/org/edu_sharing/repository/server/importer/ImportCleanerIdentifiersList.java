package org.edu_sharing.repository.server.importer;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.quartz.RemoveDeletedImportsFromSetJob;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * 
 * @author mv
 *
 */
public class ImportCleanerIdentifiersList {
	
	Logger logger = Logger.getLogger(ImportCleanerIdentifiersList.class);
	
	String oaiBaseUrl = null;
	
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	List<String> nodeAtOaiService = new ArrayList<>();
	
	/**
	 * <nodeuuid,replicationsourceid>
	 */
	Map<String,String> allNodesInSet;
	
	MCAlfrescoAPIClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
	
	List<String> toDeleteList = new ArrayList<>();

	public ImportCleanerIdentifiersList(String baseUrl, String set, String metadataPrefix, boolean testMode) {
		try {
			
			oaiBaseUrl = baseUrl;
			readAllNodesInRepository(set);
			
			
			String url = oaiBaseUrl + "?verb=ListIdentifiers&set=" + set + "&metadataPrefix=" + metadataPrefix;
			readAllNodesAtOaiService(url, true);
			
			logger.info("found:" + allNodesInSet.size() +" in repository for set " + set);
			logger.info("found:" + nodeAtOaiService.size() +" at oai service " + oaiBaseUrl + " set:" + set);
			if(allNodesInSet.size()<nodeAtOaiService.size()){
				logger.warn("It seems that you have not yet imported the whole oai set");
			}
			if(nodeAtOaiService.size() == 0){
				throw new RuntimeException("Got no nodes from oai, will cancel the delete job");
			}

			for(Map.Entry<String, String> entry : allNodesInSet.entrySet()) {
				if(!nodeAtOaiService.contains(entry.getValue())){
					toDeleteList.add(entry.getKey());
				}
			}
			
			int deletedCounter = 0;
			for(String toDelete : toDeleteList) {
				logger.info("will delete replicationsourceid:" + allNodesInSet.get(toDelete) +" alfresco id:" + toDelete +" cause it does not longer exist in set");
				
				if(!testMode) {
					NodeServiceFactory.getLocalService().removeNode(toDelete, null, false);
				}
				deletedCounter++;
			}
			
			if(testMode) {
				logger.info("runned in TESTMODE no nodes where deleted. set param " + RemoveDeletedImportsFromSetJob.PARAM_TESTMODE + " to true. " + deletedCounter + " would be deleted");
			}else {
				logger.info("finished! deleted " + deletedCounter + " nodes");
			}
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private void readAllNodesInRepository(String set) throws Throwable {
		String importFolder=PersistentHandlerEdusharing.prepareImportFolder();
		NodeRef setFolderRef = NodeServiceFactory.getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,importFolder, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
				set);
		if(setFolderRef==null){
			throw new IllegalArgumentException("Set folder "+set+" was not found. Please check your "+OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS+" folder");
		}
		List<NodeRef> allNodes = NodeServiceFactory.getLocalService().getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, setFolderRef.getId(), Collections.singletonList(CCConstants.CCM_TYPE_IO), RecurseMode.Folders);
		allNodesInSet=new HashMap<>();
		allNodes.parallelStream().forEach((entry)-> {
			AuthenticationUtil.runAsSystem(()->allNodesInSet.put(entry.getId(), NodeServiceHelper.getProperty(entry, CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID)));
		});
		logger.info("readAllNodesInRepository finished for set "+set+", found "+allNodesInSet.size()+" nodes");
	}
	

	
	private void readAllNodesAtOaiService(String url, boolean primaryCall) throws Throwable{
		
		logger.info("url:"+url);
		Integer completeListSize = null;

		String queryResult = new HttpQueryTool().query(url);
		if(queryResult != null){
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(queryResult)));
			
			String cursor = (String)xpath.evaluate("/OAI-PMH/ListIdentifiers/resumptionToken/@cursor", doc, XPathConstants.STRING);
			String size = (String)xpath.evaluate("/OAI-PMH/ListIdentifiers/resumptionToken/@completeListSize", doc, XPathConstants.STRING);
			if(primaryCall && size!=null){
				completeListSize = Integer.parseInt(size);
			}
			String token = (String)xpath.evaluate("/OAI-PMH/ListIdentifiers/resumptionToken", doc, XPathConstants.STRING);
			if(token!=null) {
				token = URIUtil.encodeQuery(token);

				int deletedCount = handleIdentifierList(doc);
				if(completeListSize!=null){
					completeListSize -= deletedCount;
				}
				//&& completeListSize != null && cursor != null &&  (new Integer(completeListSize) > new Integer(cursor))

				if (token.trim().length() > 0) {
					try {
						String urlNext = this.oaiBaseUrl + "?verb=ListIdentifiers&resumptionToken=" + token;
						readAllNodesAtOaiService(urlNext, false);

					} catch (NumberFormatException e) {
						logger.error(e.getMessage(), e);
					}
				} else {
					logger.info("no more resumption. oai querying finished!");
				}
			}
		}
		if(primaryCall && completeListSize != null && nodeAtOaiService.size() != completeListSize){
			throw new IllegalStateException("Count of completeListSize (" + completeListSize+") does not match with fetched oai count (" + nodeAtOaiService.size()+")");
		}
	}
	
	private int handleIdentifierList(Document docIdentifiers) throws Throwable{
		NodeList nodeList = (NodeList)xpath.evaluate("/OAI-PMH/ListIdentifiers/header", docIdentifiers, XPathConstants.NODESET);
		int deletedCount = 0;
		int nrOfRs = nodeList.getLength();
		for(int i = 0; i < nrOfRs;i++){
			Node headerNode = nodeList.item(i);
			String identifier = (String)xpath.evaluate("identifier", headerNode, XPathConstants.STRING);

			String status = (String)xpath.evaluate("@status", headerNode, XPathConstants.STRING);
			if(status != null && status.trim().equals("deleted")){
				deletedCount++;
				logger.info("Object with Identifier:"+identifier+" is deleted. Will continue with the next one");
				continue;
			}
			
			nodeAtOaiService.add(identifier);
		}
		return deletedCount;
	}
}
