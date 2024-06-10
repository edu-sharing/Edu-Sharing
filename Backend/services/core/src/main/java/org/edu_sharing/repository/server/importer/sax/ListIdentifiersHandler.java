package org.edu_sharing.repository.server.importer.sax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.importer.BinaryHandler;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.importer.PersistentHandlerInterface;
import org.edu_sharing.repository.server.importer.RecordHandlerInterfaceBase;
import org.edu_sharing.repository.server.jobs.quartz.OAIConst;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public  class ListIdentifiersHandler extends DefaultHandler {

	Logger logger = Logger.getLogger(ListIdentifiersHandler.class);

	List<String> openedElements = new ArrayList<>();

	String currentValue = null;
	String currentResumptionToken = null;
	Attributes currentAtts = null;

	String currentIdentifier = null;
	String currentTimeStamp = null;

	boolean currentRecordIsDeleted = false;

	String completeListSize = null;
	String cursor = null;
	String set = null;
	String oaiBaseUrl = null;
	String metadataPrefix = null;

	String esMetadataSetId = null;

	PersistentHandlerInterface persistentHandler = null;

	RecordHandlerInterfaceBase recordHandler = null;

	BinaryHandler binaryHandler = null;

	boolean addSetToGetRecordUrl = false;

	String currentUrl = null;

	public ListIdentifiersHandler() {

	}

	public ListIdentifiersHandler(String oaiBaseUrl, String set, String metadataPrefix) {
		this(null, null, null, oaiBaseUrl, set, metadataPrefix, null, false, null, null);
	}

	public ListIdentifiersHandler(String oaiBaseUrl, String set, String metadataPrefix, Date from, Date until) {
		this(null, null, null, oaiBaseUrl, set, metadataPrefix, null, false, from, until);
	}

	public ListIdentifiersHandler(RecordHandlerInterfaceBase recordHandler,
								  PersistentHandlerInterface persistentHandler,
								  BinaryHandler binaryHandler,
								  String oaiBaseUrl,
								  String set,
								  String metadataPrefix,
								  String esMetadataSetId,
								  boolean addSetToGetRecordUrl, Date from, Date until) {

		this.oaiBaseUrl = oaiBaseUrl;
		this.metadataPrefix = metadataPrefix;
		this.esMetadataSetId = esMetadataSetId;
		this.persistentHandler = persistentHandler;
		this.set = set;
		this.recordHandler = recordHandler;
		this.binaryHandler = binaryHandler;
		this.addSetToGetRecordUrl = addSetToGetRecordUrl;
		String url = this.oaiBaseUrl + "?verb=ListIdentifiers&metadataPrefix=" + metadataPrefix + "&set=" + set;
		if(from != null && until != null){
			url += "&from=" + OAIConst.DATE_FORMAT.format(from);
			url += "&until=" + OAIConst.DATE_FORMAT.format(until);
		}
		handleIdentifiersList(url);

	}

	public void handleIdentifiersList(String url) {

		currentUrl = new String(url);

		String result = OAIPMHLOMImporter.getQueryTool().query(url);


		try {

			InputSource inputSource = new InputSource(new ByteArrayInputStream(result.getBytes(Charset.forName("utf-8"))));

			XMLReader xmlReader = XMLReaderFactory.createXMLReader();

			// Stream to xml file
			xmlReader.setContentHandler(this);

			// Start parsing
			xmlReader.parse(inputSource);

		} catch (SAXException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue += new String(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		openedElements.add(localName);
		currentValue = "";
		currentAtts = atts;

		String parentLocalName = (openedElements.size() > 1) ? openedElements.get(openedElements.size() - 2).toLowerCase() : "";
		String lowerLocalName = localName.toLowerCase();
		if (parentLocalName.equals("listidentifiers") && lowerLocalName.equals("header")) {

			String status = currentAtts.getValue("status");
			if (status != null && status.trim().equals("deleted")) {
				currentRecordIsDeleted = true;
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		currentValue = currentValue.trim();

		String parentLocalName = (openedElements.size() > 1) ? openedElements.get(openedElements.size() - 2).toLowerCase() : "";

		String grandParentLocalName = (openedElements.size() > 2) ? openedElements.get(openedElements.size() - 3).toLowerCase() : "";

		String grandGrandParentLocalName = (openedElements.size() > 3) ? openedElements.get(openedElements.size() - 4).toLowerCase() : "";

		String lowerLocalName = localName.toLowerCase();

		if (parentLocalName.equals("oai-pmh") && lowerLocalName.equals("error")) {
			logger.warn("oai service error:" + currentValue + " code:" + currentAtts.getValue("code") + " " +" cursor:"+cursor+ " set:"+set +" metadataPrefix:" + metadataPrefix + " currentUrl:"+currentUrl);
		}

		// reset deleted status
		if (parentLocalName.equals("listidentifiers") && lowerLocalName.equals("header")) {

			currentRecordIsDeleted = false;
		}

		if (grandParentLocalName.equals("listidentifiers") && parentLocalName.equals("header") && lowerLocalName.equals("identifier")) {
			currentIdentifier = currentValue;
			if (currentTimeStamp != null) {
				handleRecord(currentIdentifier, currentTimeStamp);
			}
		}

		if (grandParentLocalName.equals("listidentifiers") && parentLocalName.equals("header") && lowerLocalName.equals("datestamp")) {
			currentTimeStamp = currentValue;
			if (currentIdentifier != null) {
				handleRecord(currentIdentifier, currentTimeStamp);
			}
		}

		if (parentLocalName.equals("listidentifiers") && lowerLocalName.equals("header")) {
			// reset the data pair
			currentIdentifier = null;
			currentTimeStamp = null;
		}

		if (parentLocalName.equals("listidentifiers") && lowerLocalName.equals("resumptiontoken")) {
			currentResumptionToken = currentValue;
			completeListSize = currentAtts.getValue("completeListSize");
			cursor = currentAtts.getValue("cursor");
		}

		// cleanup stack
		if (openedElements.get(openedElements.size() - 1).equals(localName)) {
			logger.debug("will remove " + localName + " from stack");
			openedElements.remove(openedElements.size() - 1);

		} else {
			String message = "something went wrong closed element is not the last on stack.";
			logger.error(message);
			throw new SAXException(message);
		}
	}

	/**
	 * must be called inside a /record/header processing so that currentRecordIsDeleted has the right context
	 * @param replId
	 * @param timeStamp
	 */
	protected void handleRecord(String replId, String timeStamp) {

		if (isCurrentRecordDeleted()) {
			logger.info("record " + currentIdentifier + " is deleted");
			return;
		}

		boolean mustBePersisted = persistentHandler.mustBePersisted(replId, timeStamp);

		logger.info("record " + currentIdentifier + " must be persisted " + mustBePersisted);

		if (mustBePersisted) {

			String url = getRecordUrl(replId);
			logger.info("url:" + url);
			String result = OAIPMHLOMImporter.getQueryTool().query(url);

			try {


				/**
				 * prevent a Invalid byte 2 of 3-byte UTF-8 sequence. org.xml.sax.SAXParseException: Invalid byte 2 of 3-byte UTF-8 sequence.
				 * with explicit utf-8 encoding
				 */
				if(recordHandler instanceof RecordHandlerInterface) {
					((RecordHandlerInterface)recordHandler).handleRecord(new ByteArrayInputStream(result.getBytes(Charset.forName("utf-8"))));

					if (recordHandler.getProperties().size() > 0) {
						persistentHandler.safe(recordHandler, cursor, set);
					} else {
						logger.warn("no properties found for object:" + replId);
					}
				}
				if(recordHandler instanceof org.edu_sharing.repository.server.importer.RecordHandlerInterface) {

					try{
						XPathFactory pfactory = XPathFactory.newInstance();
						XPath xpath = pfactory.newXPath();
						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						DocumentBuilder builder = factory.newDocumentBuilder();
						Document doc = builder.parse(new InputSource(new StringReader(result)));
						String errorcode = (String)xpath.evaluate("/OAI-PMH/error", doc, XPathConstants.STRING);
						if(errorcode == null || errorcode.trim().equals("")){
							Node nodeRecord = (Node)xpath.evaluate("/OAI-PMH/GetRecord/record", doc, XPathConstants.NODE);
							((org.edu_sharing.repository.server.importer.RecordHandlerInterface)recordHandler).handleRecord(nodeRecord, cursor, set);

							logger.info("staring exists check:" + replId);
							boolean exists = persistentHandler.exists(replId);
							logger.info("finished exists check" + replId+ " exists:" + exists);


							String nodeId = persistentHandler.safe((org.edu_sharing.repository.server.importer.RecordHandlerInterface) recordHandler, cursor, set);
							if(nodeId != null) {
								if(binaryHandler != null){
									binaryHandler.safe(nodeId, (org.edu_sharing.repository.server.importer.RecordHandlerInterface) recordHandler,nodeRecord, set);
								}

								if(!exists) {
									new MCAlfrescoAPIClient().createVersion(nodeId);
								}
							}
						}else{
							logger.error(errorcode);
						}
					}catch(org.xml.sax.SAXParseException e){
						logger.error("SAXParseException occured: cursor:"+cursor+ " set:"+set +" identifier:"+ replId );
						logger.error(e.getMessage(),e);
					}catch(Throwable e){
						logger.error("Throwable occured at set: "+set+", identifier: "+replId);
						logger.error(e.getMessage(),e);
					}

				}


			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} catch (SAXParseException e) {
				logger.error("oai document with id:" + currentValue + " is not well formed");
				logger.error(e.getMessage(), e);
			} catch (SAXException e) {
				logger.error(e.getMessage(), e);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public String getRecordUrl(String replicationSourceId) {
		String getRecordUrl = this.oaiBaseUrl + "?verb=GetRecord&identifier=" + replicationSourceId + "&metadataPrefix=" + this.metadataPrefix;
		if(this.addSetToGetRecordUrl) getRecordUrl += "&set=" + this.set;
		return getRecordUrl;
	}

	public boolean isCurrentRecordDeleted() {
		return currentRecordIsDeleted;
	}

	@Override
	public void startDocument() throws SAXException {
		currentResumptionToken = null;
		currentValue = null;
	}

	@Override
	public void endDocument() throws SAXException {

		logger.info("end with cursor:" + cursor);

		if (currentResumptionToken != null && !currentResumptionToken.trim().equals("")) {
			if (currentResumptionToken != null && currentResumptionToken.trim().length() > 0 && new Integer(completeListSize) > new Integer(cursor)) {
				String url = this.oaiBaseUrl + "?verb=ListIdentifiers&resumptionToken=" + currentResumptionToken;
				this.handleIdentifiersList(url);
			}
		}

	}
}
