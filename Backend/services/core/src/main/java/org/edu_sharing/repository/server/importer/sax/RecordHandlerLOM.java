package org.edu_sharing.repository.server.importer.sax;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class RecordHandlerLOM implements RecordHandlerInterface, ContentHandler {

    Logger logger = Logger.getLogger(RecordHandlerLOM.class);

    String metadataSetId = null;

    String cursor = null;
    String set = null;

    String currentValue = null;
    Attributes currentAtts = null;

    boolean currentRecordIsDeleted = false;

    String error = null;
    String errorCode = null;

    //specialAtts
    String currentContributeRole = null;
    String currentContributeEntity = null;

    String currentRelationKind = null;
    String currentRelationDescription = null;

    Map<String, Object> properties = new HashMap<>();

    public RecordHandlerLOM(String metadataSetId) {
        this.metadataSetId = metadataSetId;
        if (this.metadataSetId == null) {
            this.metadataSetId = "default";
        }

    }

    List<String> openedElements = new ArrayList<>();

    @Override
    public void handleRecord(InputStream isRecord) throws SAXException, IOException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();

        // Stream to xml file
        InputSource inputSource = new InputSource(isRecord);

        xmlReader.setContentHandler(this);

        // Start parsing
        xmlReader.parse(inputSource);
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

        currentValue = "";
        openedElements.add(localName);
        currentAtts = atts;


        String parentLocalName = (openedElements.size() > 1) ? openedElements.get(openedElements.size() - 2).toLowerCase() : "";
        String lowerLocalName = localName.toLowerCase();

        if (parentLocalName.equals("record") && lowerLocalName.equals("header")) {

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

        //reset deleted status
        if (parentLocalName.equals("record") && lowerLocalName.equals("header")) {
            currentRecordIsDeleted = false;
        }

        if (parentLocalName.equals("oai-pmh") && lowerLocalName.equals("error")) {
            this.error = currentValue;
            this.errorCode = currentAtts.getValue("code");
        }

        if (parentLocalName.equals("header") && lowerLocalName.equals("identifier")) {
            handleSingleValue(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
        }

        if (parentLocalName.equals("header") && lowerLocalName.equals("datestamp")) {
            handleSingleValue(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);
        }

        /**
         * general
         */
        if (parentLocalName.equals("identifier") && lowerLocalName.equals("catalog")) {
            handleSingleValue(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
        }

        if (grandParentLocalName.equals("general") && parentLocalName.equals("title") && lowerLocalName.equals("string")) {
            handleMultiLangValue(CCConstants.LOM_PROP_GENERAL_TITLE);
        }

        if (parentLocalName.equals("general") && lowerLocalName.equals("language")) {
            handleMultiValue(CCConstants.LOM_PROP_GENERAL_LANGUAGE);
        }

        if (grandParentLocalName.equals("general") && parentLocalName.equals("description") && lowerLocalName.equals("string")) {
            handleMultiLangValue(CCConstants.LOM_PROP_GENERAL_DESCRIPTION);
        }

        if (grandParentLocalName.equals("general") && parentLocalName.equals("keyword") && lowerLocalName.equals("string")) {
            handleMultiValueMultiLang(CCConstants.LOM_PROP_GENERAL_KEYWORD);
        }

        //@TODO structure
        //@TODO aggregationLevel

        /**
         * lifeCycle
         */
        if (grandParentLocalName.equals("lifecycle") && parentLocalName.equals("version") && lowerLocalName.equals("string")) {
            handleMultiLangValue(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
        }

        if (grandParentLocalName.equals("lifecycle") && parentLocalName.equals("status") && lowerLocalName.equals("value")) {
            handleSingleValue(CCConstants.LOM_PROP_LIFECYCLE_STATUS);
        }

        if (grandGrandParentLocalName.equals("lifecycle") && grandParentLocalName.equals("contribute") && parentLocalName.equals("role") && lowerLocalName.equals("value")) {
            currentContributeRole = currentValue;
            handleLCContributer();
        }

        if (grandParentLocalName.equals("lifecycle") && parentLocalName.equals("contribute") && lowerLocalName.equals("entity")) {
            currentContributeEntity = currentValue;
            handleLCContributer();
        }

        /**
         * metadata
         */
        if (grandGrandParentLocalName.equals("metametadata") && grandParentLocalName.equals("contribute") && parentLocalName.equals("role") && lowerLocalName.equals("value")) {
            currentContributeRole = currentValue;
            handleMDContributer();
        }

        if (grandParentLocalName.equals("metametadata") && parentLocalName.equals("contribute") && lowerLocalName.equals("entity")) {
            currentContributeEntity = currentValue;
            handleMDContributer();
        }

        /**
         * technical
         */
        if (parentLocalName.equals("technical") && lowerLocalName.equals("format")) {
            handleSingleValue(CCConstants.LOM_PROP_TECHNICAL_FORMAT);
        }
        if (parentLocalName.equals("technical") && lowerLocalName.equals("location")) {
            handleSingleValue(CCConstants.LOM_PROP_TECHNICAL_LOCATION);
        }

        /**
         * educational
         */
        if (grandParentLocalName.equals("educational") && parentLocalName.equals("learningresourcetype") && lowerLocalName.equals("value")) {
            handleMultiValue(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE);
        }

        if (grandParentLocalName.equals("educational") && parentLocalName.equals("intendedenduserrole") && lowerLocalName.equals("value")) {
            handleMultiValue(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE);
        }

        if (grandParentLocalName.equals("educational") && parentLocalName.equals("context") && lowerLocalName.equals("value")) {
            handleMultiValue(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT);
        }

        if (grandParentLocalName.equals("educational") && parentLocalName.equals("typicalagerange") && lowerLocalName.equals("string")) {
            handleMultiLangValue(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE);
        }

        if (parentLocalName.equals("educational") && lowerLocalName.equals("language")) {
            handleMultiValue(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LANGUAGE);
        }

        /**
         * rights
         */
        if (grandParentLocalName.equals("rights") && parentLocalName.equals("copyrightandotherrestrictions") && lowerLocalName.equals("value")) {
            if (currentValue.equals("yes") || currentValue.equals("no")) {
                Boolean copyRightValBool = (currentValue.equals("yes")) ? true : false;
                handleSingleValue(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT, copyRightValBool.toString());
            } else {
                logger.info("unknown value for copyRightVal:" + currentValue);
            }
        }

        if (grandParentLocalName.equals("rights") && parentLocalName.equals("description") && lowerLocalName.equals("string")) {
            handleMultiLangValue(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION);
        }

        /**
         * relation
         */
        if (grandParentLocalName.equals("relation") && parentLocalName.equals("kind") && lowerLocalName.equals("value")) {
            currentRelationKind = currentValue;
        }

        if (grandGrandParentLocalName.equals("relation") && grandParentLocalName.equals("resource") && parentLocalName.equals("description") && lowerLocalName.equals("string")) {
            currentRelationDescription = currentValue;
        }

        /**
         * classification
         */

        if (grandParentLocalName.equals("taxonpath") && parentLocalName.equals("taxon") && lowerLocalName.equals("id")) {

            handleMultiValue(CCConstants.CCM_PROP_IO_REPL_TAXON_ID);
        }

        if (grandGrandParentLocalName.equals("taxonpath") && grandParentLocalName.equals("taxon") && parentLocalName.equals("entry") && lowerLocalName.equals("string")) {

            handleMultiValueMultiLang(CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY);
        }

        //cleanup stack
        if (openedElements.get(openedElements.size() - 1).equals(localName)) {
            logger.debug("will remove " + localName + " from stack");
            openedElements.remove(openedElements.size() - 1);

        } else {
            String message = "something went wrong closed element is not the last on stack.";
            logger.error(message);
            throw new SAXException(message);
        }

    }

    @Override
    public void endDocument() throws SAXException {

        if (error != null) {
            logger.error("error:" + error + " errorcode:" + errorCode);

            properties.clear();
            return;
        }

        if (currentRecordIsDeleted) {
            logger.info("current record is deleted" + properties.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));
            properties.clear();
            return;
        }

        properties.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, this.metadataSetId);

    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        currentValue += new String(ch, start, length);
    }


    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    private String adaptValue(String value) {
        logger.debug("starting");
        String result = org.htmlparser.util.Translate.decode(value);
        logger.debug("returning");
        return result;
    }

    protected void handleSingleValue(String alfKey) {
        handleSingleValue(alfKey, currentValue);
    }

    protected void handleSingleValue(String alfKey, String value) {
        properties.put(alfKey, value);
    }

    protected void handleMultiLangValue(String alfKey) {
        Map<String, String> propertyVal = (Map<String, String>) properties.get(alfKey);
        if (propertyVal == null) propertyVal = new HashMap<>();
        String language = currentAtts.getValue("language");
        if (language != null && currentValue != null) {

            // country is needed so that i18n is working properly
            if (language.length() == 2) {
                language = language + "_" + language.toUpperCase();
            }

            propertyVal.put(language, adaptValue(currentValue));
            properties.put(alfKey, propertyVal);
        }
    }

    protected void handleMultiValue(String alfKey) {
        handleMultiValue(alfKey, currentValue);
    }

    protected void handleMultiValue(String alfKey, String value) {
        List<String> propertyValue = (List<String>) properties.get(alfKey);
        if (propertyValue == null) {
            propertyValue = new ArrayList<>();
        }
        propertyValue.add(value);
        properties.put(alfKey, propertyValue);
    }

    protected void handleMultiValueMultiLang(String alfKey) {
        List<Map<String, Object>> propertyValue = (List<Map<String, Object>>) properties.get(alfKey);
        if (propertyValue == null) {
            propertyValue = new ArrayList<Map<String, Object>>();
        }

        String language = currentAtts.getValue("language");
        if (language != null && currentValue != null) {
            // country is needed so that i18n is working properly
            if (language.length() == 2) {
                language = language + "_" + language.toUpperCase();
            }

            Map<String, Object> mlVal = new HashMap<>();
            mlVal.put(language, adaptValue(currentValue));
            propertyValue.add(mlVal);
            properties.put(alfKey, propertyValue);
        }
    }

    protected void handleLCContributer() {
        if (currentContributeRole != null && currentContributeEntity != null) {
            String lc_property = CCConstants.getLifecycleContributerProp(currentContributeRole.trim());
            if (lc_property != null && !lc_property.trim().equals("")) {
                handleMultiValue(lc_property, currentContributeEntity);
            } else {
                logger.warn("can not map lifecycle contributer role " + currentContributeRole + " to edu-sharing property");
            }

            currentContributeRole = null;
            currentContributeEntity = null;
        }
    }

    protected void handleMDContributer() {
        if (currentContributeRole != null && currentContributeEntity != null) {
            String lc_property = CCConstants.getMetadataContributerProp(currentContributeRole.trim());
            if (lc_property != null && !lc_property.trim().equals("")) {
                handleMultiValue(lc_property, currentContributeEntity);
            } else {
                logger.warn("can not map lifecycle contributer role " + currentContributeRole + " to edu-sharing property");
            }

            currentContributeRole = null;
            currentContributeEntity = null;
        }
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
