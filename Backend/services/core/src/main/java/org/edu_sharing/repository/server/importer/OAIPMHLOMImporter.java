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
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.typesafe.config.Config;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.quartz.ImporterJob;
import org.edu_sharing.repository.server.jobs.quartz.OAIConst;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OAIPMHLOMImporter implements Importer{

	// best thread scaling seems to be at a max at 4-8 threads
	public static int getThreadCount(){
		Config config= LightbendConfigLoader.get().getConfig("importer.threads");
		return Math.max(config.getInt("min")
				,Math.min(config.getInt("max"),
				Runtime.getRuntime().availableProcessors()-config.getInt("minSpare")));
	}

	private int totalResults = 0;
	Logger logger = Logger.getLogger(OAIPMHLOMImporter.class);
	
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	public static final String FOLDER_NAME_IMPORTED_OBJECTS = "IMP_OBJ";
	
	public String metadataPrefix = "oai_elixier";//oai_lom
	
	private Constructor<RecordHandlerInterface> recordHandler;
	
	PersistentHandlerInterface persistentHandler;
	
	Constructor<BinaryHandler> binaryHandler;
	int nrOfResumptions = -1;
	int nrOfRecords = -1;
	
	String set = "contake";
	
	String oai_base_url = null;
	
	String urlGetRecors = "";
	private ImporterJob job;
	private String metadataSetId;

	Date from;
	Date until;

	/**
	 * @param oai_base_url
	 * @param recordHandler
	 * @param nrOfResumptions
	 * @param nrOfRecords
	 * @param metadataPrefix
	 * @param sets
	 * @throws Exception
	 */
	/*
	public OAIPMHLOMImporter(String oai_base_url,PersistentHandlerInterface persistentHandler, RecordHandlerInterface recordHandler, BinaryHandler binaryHandler, int nrOfResumptions, int nrOfRecords, String metadataPrefix, String[] sets) throws Exception{
		
		this.oai_base_url = oai_base_url;
		this.recordHandler = recordHandler;
		this.persistentHandler = persistentHandler;
		this.binaryHandler = binaryHandler;
		this.nrOfResumptions = nrOfResumptions;
		this.nrOfRecords = nrOfRecords;
		this.sets = sets;
		this.metadataPrefix = metadataPrefix;
	}
	*/
	public OAIPMHLOMImporter(){
	}

	public void startImport() throws Throwable{
		
		//take identifiers list cause some of the sets don't work: XML-Verarbeitungsfehler: nicht wohlgeformt
		String url = this.oai_base_url+"?verb=ListIdentifiers&metadataPrefix="+this.metadataPrefix;
		String setUrl = set.equals("-none-") ? url :  url+"&set="+set;
		if(this.from != null && this.until != null){
			String fromString = OAIConst.DATE_FORMAT.format(this.from);
			String untilString = OAIConst.DATE_FORMAT.format(this.until);
			setUrl+="&from="+fromString;
			setUrl+="&until="+untilString;
		}

		this.updateWithIdentifiersList(setUrl);
	}
	
	public static void main(String[] args){
		/*
		String[] sets = new String[]{"melt","elixier","lehreronline","mbnrw","siemens"};
	
		String url = "http://daunddort.de/cp/oai_pmh/oai.php?verb=ListIdentifiers&metadataPrefix=oai_elixier";
		try{
			
			//OAIPMHLOMImporter importer = new OAIPMHLOMImporter(new RecordHandlerElixier(),-1,-1,"oai_elixier",sets);
			
			//OAIPMHLOMImporter importer = new OAIPMHLOMImporter(new RecordHandlerLOM(new PersistentHandlerDB()),2,2,"oai_lom",sets);
			OAIPMHLOMImporter importer = new OAIPMHLOMImporter();
			importer.setBaseUrl("http://daoderdort.de/cp/oai_pmh/oai.php");
			importer.setBinaryHandler(null);
			importer.setMetadataPrefix("oai_lom");
			importer.setNrOfRecords(-1);
			importer.setNrOfResumptions(-1);
			importer.setPersistentHandler(new PersistentHandlerInterface() {
				
				@Override
				public String safe(Map props, String cursor, String set) throws Throwable {
					return null;
				}
				
				@Override
				public boolean mustBePersisted(String replId, String timeStamp) {
					return false;
				}
				
			});
			importer.setRecordHandler(RecordHandlerLOMTest.class.getConstructor(String.class));
			importer.setSet(sets[0]);
			importer.startImport();
		}catch(Throwable e){
			e.printStackTrace();
		}
		*/
	}
	
	public void updateWithRecordsList(String url,String set) throws Throwable{
		
		String queryResult = getQueryTool().query(url);
		if(queryResult != null){
			//cursor
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(queryResult)));
			
			String cursor = (String)xpath.evaluate("/OAI-PMH/ListRecords/resumptionToken/@cursor", doc, XPathConstants.STRING);
			String completeListSize = (String)xpath.evaluate("/OAI-PMH/ListRecords/resumptionToken/@completeListSize", doc, XPathConstants.STRING);
			String token = (String)xpath.evaluate("/OAI-PMH/ListRecords/resumptionToken", doc, XPathConstants.STRING);
			
			handleSubResult(doc,cursor,set);
			
			if(token != null && token.trim().length() > 0 && new Integer(completeListSize) > new Integer(cursor)){
				
				String urlNext = oai_base_url+"?verb=listRecords&resumptionToken="+token;
				logger.info("starting the next resumption! set:"+set+"cursor:"+cursor+" completeListSize:"+completeListSize +" token:"+token);
				
				updateWithRecordsList(urlNext,set);
			} else {
				logger.info("no more resumption. import finished!");
			}
		}
	}
	
	public void handleSubResult(Document doc,String cursor,String set) throws Throwable{
		String errorcode = (String)xpath.evaluate("/OAI-PMH/error ", doc, XPathConstants.STRING);
		if(errorcode == null || errorcode.trim().equals("")){
			NodeList nodeList = (NodeList)xpath.evaluate("/OAI-PMH/ListRecords/record", doc, XPathConstants.NODESET);
			int nrOfRs = this.nrOfRecords;
			if(nrOfRs == -1 || nrOfRs > nodeList.getLength()){
				nrOfRs = nodeList.getLength();
			}
			for(int i = 0; i < nrOfRs; i++){
				logger.info("node:" + (i+1) +" from:"+nrOfRs);
				Node nodeRecord = nodeList.item(i);
				RecordHandlerInterface handler = getRecordHandler();
				handler.handleRecord(nodeRecord,cursor,set);
				String nodeId = persistentHandler.safe(handler, cursor, set);
				if(getBinaryHandler() != null){
					getBinaryHandler().safe(nodeId, handler,nodeRecord, set);
				}
				new MCAlfrescoAPIClient().createVersion(nodeId);
			}
		}else{
			logger.error("/OAI-PMH/error:" + errorcode);
		}
	}
	
	public void updateWithIdentifiersList(String url) throws Throwable{
		if(job!=null && job.isInterrupted()){
			logger.info("Will cancel oai fetching, job is aborted");
			return;
		}
		logger.info("url:"+url);
		long currentTime = System.currentTimeMillis();
		String queryResult = getQueryTool().query(url);
		logger.info("ListIdentifiers;"+url+";" + (System.currentTimeMillis() - currentTime));
		if(queryResult != null){
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(queryResult)));
			
			String cursor = (String)xpath.evaluate("/OAI-PMH/ListIdentifiers/resumptionToken/@cursor", doc, XPathConstants.STRING);
			String completeListSize = (String)xpath.evaluate("/OAI-PMH/ListRecords/resumptionToken/@completeListSize", doc, XPathConstants.STRING);
			String token = (String)xpath.evaluate("/OAI-PMH/ListIdentifiers/resumptionToken", doc, XPathConstants.STRING);
			
			token = URIUtil.encodeQuery(token);
			
			handleIdentifierList(doc,cursor,set);
			//&& completeListSize != null && cursor != null &&  (new Integer(completeListSize) > new Integer(cursor))
		
			if(token != null && token.trim().length() > 0 ){
				try{
					//int countAllRecords = new Integer(completeListSize);
					//int maxResumptions = countAllRecords/100; //100 werden pro resumption immer geliefert
					
					String urlNext = this.oai_base_url+"?verb=ListIdentifiers&resumptionToken="+token;
					//logger.info("starting the next resumption! set:"+set+" cursor:"+cursor+" completeListSize:"+completeListSize +" token:"+token);
					
					if(nrOfResumptions > -1){
						Integer cursorAsNumber = new Integer(cursor);
						int actualNrOfResumption = cursorAsNumber / 100;
						if(actualNrOfResumption <= this.nrOfResumptions){
							updateWithIdentifiersList(urlNext);
						}
					}else{
						logger.info("token:"+token);
						updateWithIdentifiersList(urlNext);
					}
				}catch(NumberFormatException e){
					logger.error(e.getMessage(),e);
				}
			}else{
				logger.info("no more resumption. import finished!");
			}
		}
		else{
			logger.warn("Result for query url "+url+" was empty!");
		}
		executor.shutdown();
	}

	
	private ExecutorService executor = Executors.newFixedThreadPool(getThreadCount(), r -> {
		Thread t = new Thread(r);
		t.setPriority(Thread.NORM_PRIORITY-1);
		return t;
	});
	public void handleIdentifierList(Document docIdentifiers, String cursor, String set) throws Throwable{
		NodeList nodeList = (NodeList)xpath.evaluate("/OAI-PMH/ListIdentifiers/header", docIdentifiers, XPathConstants.NODESET);
		int nrOfRs = this.nrOfRecords;
		if(nrOfRs == -1 || nrOfRs > nodeList.getLength()){
			nrOfRs = nodeList.getLength();
		}
		List<Callable<Void>> threads = new ArrayList<>();
		final String authority = AuthenticationUtil.getFullyAuthenticatedUser();
		if(job!=null && job.isInterrupted()) {
			shutdown();
			return;
		}
		long time=System.currentTimeMillis();
		for(int i = 0; i < nrOfRs;i++){
			if(i > MAX_PER_RESUMPTION){
				logger.error("only " +MAX_PER_RESUMPTION +" for one resumption token are allowed here");
				break;
			}
			final Node headerNode = nodeList.item(i);
			threads.add(()->{
				AuthenticationUtil.runAs(()-> {
					String identifier = (String) xpath.evaluate("identifier", headerNode, XPathConstants.STRING);
					try {
						if(job!=null && job.isInterrupted()){
							shutdown();
						}
						String timeStamp = (String) xpath.evaluate("datestamp", headerNode, XPathConstants.STRING);
						logger.debug("import "+identifier+" "+timeStamp);
						String status = (String) xpath.evaluate("@status", headerNode, XPathConstants.STRING);
						if (status != null && status.trim().equals("deleted")) {

							logger.info("Object with Identifier:" + identifier + " is deleted. Will continue with the next one");
							return null;
						}

						if (persistentHandler.mustBePersisted(identifier, timeStamp)) {
							logger.info("identifier:" + identifier + " timeStamp: " + timeStamp + " will be created/updated");
							handleGetRecordStuff(cursor, identifier);
						} else {
							logger.debug("identifier:" + identifier + " timeStamp: " + timeStamp + " will NOT be updated");
						}
					} catch (Throwable t) {
						logger.warn("Error while handling identifier "+identifier,t);
					}
					return null;
				},authority);
				return null;
			});
		}
		// wait until all previously started threads have finished
		executor.invokeAll(threads);
		totalResults+=threads.size();
		time=(System.currentTimeMillis()-time);
		if(threads.size()>0) {
			logger.info("Import progress: "+totalResults+" entries processed so far");
			logger.info(getThreadCount() + " Threads finished (" + threads.size() + ", " + (time / 1000) + " s -> " + (time / threads.size()) + "ms per entry)");
		}
	}

	private void shutdown() {
		logger.info("Will cancel import, job is aborted");
		executor.shutdownNow();
	}

	private String getRecordAsString(String identifier) {
		String url = getRecordUrl(identifier);

		long currentTime = System.currentTimeMillis();
		String result = getQueryTool().query(url);
		logger.info("GetRecord;"+url+";" + (System.currentTimeMillis() - currentTime));
		return result;
	}

	public static HttpQueryTool getQueryTool() {
		HttpQueryTool httpTool = new HttpQueryTool();
		httpTool.setTimeout(180 * 1000);
		return httpTool;
	}

	private String getRecordUrl(String identifier) {
		String url = oai_base_url+"?verb=GetRecord"+"&identifier="+identifier+"&metadataPrefix="+metadataPrefix;
		if(oai_base_url.contains("sodis")) {
			url+= "&set=" +set;
		}
		return url;
	}
	
	public void startImport(String[] oaiIDs) {
		for(String oaiID : oaiIDs) {
			String url = getRecordUrl(oaiID);
			logger.info("url record:"+url);
			String result = getQueryTool().query(url);
			if(result != null && !result.trim().equals("")){
				handleGetRecordStuff("IDList",oaiID);
			}
		}
	}

	@Override
	public void setJob(ImporterJob importerJob) {
		this.job = importerJob;
		job.onInterrupted(this::shutdown);
	}

	public static final int MAX_PER_RESUMPTION = 5000;
	
	protected void handleGetRecordStuff( String cursor, String identifier){
		try{
			long time=System.currentTimeMillis();
			Document doc = getRecordAsDoc(identifier);
			if(doc==null){
				logger.info("Fetching of "+identifier+" failed, skipping entry!");
				return;
			}
			logger.debug("Fetching oai "+identifier+" took "+(System.currentTimeMillis()-time)+" ms");
			handleRecordDoc(doc,cursor,identifier);
		}catch(org.xml.sax.SAXParseException e){
			logger.error("SAXParseException occured: cursor:"+cursor+ " set:"+set +" identifier:"+ identifier );
			logger.error(e.getMessage(),e);
		}catch(Throwable e){
			logger.error("Throwable occured at set: "+set+", identifier: "+identifier);
			logger.error(e.getMessage(),e);
		}
	}

	/**
	 *
	 * @param doc
	 * @param cursor
	 * @param identifier may be null, just for logging
	 */
	protected void handleRecordDoc(Document doc,String cursor,String identifier){
		try{
			String errorcode = (String)xpath.evaluate("/OAI-PMH/error", doc, XPathConstants.STRING);
			if(errorcode == null || errorcode.trim().equals("")){
				Node nodeRecord = getRecordNodeFromDoc(doc);
				handleRecordInternal(cursor, nodeRecord);
			}else{
				logger.error(errorcode);
			}
		}catch(org.xml.sax.SAXParseException e){
			logger.error("SAXParseException occured: cursor:"+cursor+ " set:"+set +" identifier:"+ identifier );
			logger.error(e.getMessage(),e);
		}catch(Throwable e){
			logger.error("Throwable occured at set: "+set+", identifier: "+identifier);
			logger.error(e.getMessage(),e);
		}
	}

	private String handleRecordInternal(String cursor, Node nodeRecord) throws Throwable {
		RecordHandlerInterface handler = getRecordHandler();
		handler.handleRecord(nodeRecord, cursor, set);
		String nodeId = persistentHandler.safe(handler, cursor, set);
		if(nodeId != null) {
			if(getBinaryHandler() != null){
				getBinaryHandler().safe(nodeId, handler,nodeRecord, set);
			}
			if(job!=null &&
					(job.getJobDataMap().getBoolean(OAIConst.PARAM_FORCE_UPDATE) || job.getJobDataMap().getBoolean(OAIConst.PARAM_NO_VERSION))){
				NodeServiceFactory.getLocalService().deleteVersionHistory(nodeId);
			}
			NodeServiceFactory.getLocalService().createVersion(nodeId);
		}
		return nodeId;
	}

	public Node getRecordNodeFromDoc(Document doc) throws XPathExpressionException {
		return (Node) xpath.evaluate("/OAI-PMH/GetRecord/record", doc, XPathConstants.NODE);
	}

	public Document getRecordAsDoc(String identifier) throws ParserConfigurationException, SAXException, IOException {
		String result = getRecordAsString(identifier);
		return stringToXML(result);
	}

	private Document stringToXML(String result) throws ParserConfigurationException, SAXException, IOException {
		if(result==null)
			return null;
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(result)));
	}

	public static final String URLIMPORT_SIGN = "URLIMPORT";
	
	/**
	 * @param urlToFile
	 * @throws Throwable
	 */
	public void importOAIObjectsFromFile(String urlToFile, RecordHandlerInterface recordHandlerLom) throws Throwable{
		DocumentBuilder builder = factory.newDocumentBuilder();
		
		String queryResult = getQueryTool().query(urlToFile);
		
		Document doc = builder.parse(new InputSource(new StringReader(queryResult)));
		
		NodeList records = (NodeList)xpath.evaluate("/records/record", doc, XPathConstants.NODESET);
		
		String[] splitted = urlToFile.split("/");
		String setName = splitted[splitted.length -1];
		setName = setName.replace(".", "_");
		setName = URLIMPORT_SIGN + "_"+System.currentTimeMillis() + setName; 
		for(int i = 0; i < records.getLength(); i++){
			
			Node record = records.item(i);
			int cursor = (int)(i/100);
			
			try{
								
				logger.info("cursor:"+cursor+" i:"+i);
				//it seems when we use the record Node the whole Document will be processed. this makes the handling of record slower and slower.
				//so we create a new document that consists only of the record part 
				Source source = new DOMSource(record);
	            StringWriter stringWriter = new StringWriter();
	            Result result = new StreamResult(stringWriter);
	            TransformerFactory factory = TransformerFactory.newInstance();
	            Transformer transformer = factory.newTransformer();
	            transformer.transform(source, result);
	           
	            String recordAsString = stringWriter.getBuffer().toString();
	            
	            Document standaloneRecordDoc = builder.parse(new InputSource(new StringReader(recordAsString)));
	            
	            //cause we want the content of record:
	            Node standaloneRecordNode = (Node)xpath.evaluate("record", standaloneRecordDoc, XPathConstants.NODE);
	            
	            xpath.reset();
	            recordHandlerLom.handleRecord(standaloneRecordNode, new Integer(cursor).toString(), setName);
	            persistentHandler.safe(recordHandlerLom, new Integer(cursor).toString(), setName);
				
			} catch(Throwable e) {
				logger.error(e.getMessage(),e);
			}
		}
		
	}	
	
	@Override
	public void setBaseUrl(String baseUrl) {
		this.oai_base_url = baseUrl;
	}
	
	@Override
	public void setBinaryHandler(Constructor<BinaryHandler> binaryHandler) {
		this.binaryHandler = binaryHandler;
	}
	
	@Override
	public void setMetadataPrefix(String metadataPrefix) {
		this.metadataPrefix = metadataPrefix;
	}
	
	@Override
	public void setNrOfRecords(int nrOfRecords) {
		this.nrOfRecords = nrOfRecords;
	}
	
	@Override
	public void setNrOfResumptions(int nrOfResumptions) {
		this.nrOfResumptions = nrOfResumptions;		
	}
	@Override
	public void setPersistentHandler(PersistentHandlerInterface persistentHandler) {
		this.persistentHandler = persistentHandler;
	}

	public RecordHandlerInterface getRecordHandler(){
		try {
			RecordHandlerInterface handler = this.recordHandler.newInstance(metadataSetId);
			return handler;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}
	private BinaryHandler getBinaryHandler(){
		if(binaryHandler==null)
			return null;
		try {
			BinaryHandler handler = this.binaryHandler.newInstance();
			return handler;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}
	@Override
	public void setRecordHandler(Constructor<RecordHandlerInterface> recordHandler) {
		this.recordHandler = recordHandler;
	}
	
	@Override
	public void setSet(String set) {
		this.set = set;
	}

	@Override
	public void setMetadataSetId(String metadataSetId) {
		this.metadataSetId = metadataSetId;
	}

	public String startImport(byte[] xml) throws Throwable {
		Document xmlRecord = stringToXML(new String(xml));
		Node main = xmlRecord.getFirstChild();
		if(!main.getNodeName().equals("metadata")){
			Document newDoc = factory.newDocumentBuilder().newDocument();
			Node metadata = newDoc.appendChild(newDoc.createElement("metadata"));
			metadata.appendChild(newDoc.adoptNode(main));
			xmlRecord=newDoc;
			//xmlRecord.removeChild(main);
		}

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(xmlRecord), new StreamResult(writer));
		String output = writer.getBuffer().toString();
		logger.info("Import xml "+output);
		return handleRecordInternal(null,xmlRecord);
	}

	@Override
	public void setFrom(Date from) {
		this.from = from;
	}

	@Override
	public void setUntil(Date until) {
		this.until = until;
	}
}
