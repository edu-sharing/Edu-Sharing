package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.importer.PersistenHandlerKeywordsDNBMarc;
import org.edu_sharing.repository.server.importer.RecordHandlerKeywordsDNBMarc;
import org.edu_sharing.repository.server.importer.sax.RecordHandlerInterface;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.*;


public class ImportFactualTermsFromFileSax implements ContentHandler  {


    String currentValue = null;
    Attributes currentAtts = null;
    List<String> openedElements = new ArrayList<>();

    String currentDatafieldTag = null;

    HashMap currentProperties = null;

    String current150a = null;
    String current150g = null;
    String current150x = null;

    String current913 = null;

    Set<String> symonyms = new HashSet<>();

    PersistenHandlerKeywordsDNBMarc persistentHandler;
    AbstractJob job;

    Logger logger = Logger.getLogger(ImportFactualTermsFromFileSax.class);

    int processedKeywords = 0;
    List<String> errorKeywords = new ArrayList<>();

    public ImportFactualTermsFromFileSax(URI uri, PersistenHandlerKeywordsDNBMarc persistentHandler, AbstractJob job) throws Throwable {
        this.persistentHandler = persistentHandler;
        this.job = job;
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();

        // Stream to xml file
        InputSource inputSource = new InputSource(uri.toURL().openStream());

            xmlReader.setContentHandler(this);

        // Start parsing
        xmlReader.parse(inputSource);
    }

    @Override
    public void setDocumentLocator(Locator locator) {

    }

    @Override
    public void startDocument() throws SAXException {

    }

    @Override
    public void endDocument() throws SAXException {
        logger.info("finished:" + processedKeywords +" processed."+errorKeywords.size()+" errors");
        if(errorKeywords.size() > 0){
            logger.error(StringUtils.join(errorKeywords, ", "));
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {

    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

        if(job.isInterrupted()){
            throw new SAXException("job was interrupted");
        }

        if(localName.equals("record")){
            currentProperties = new HashMap<>();
        }

        currentValue = "";
        openedElements.add(localName);

        if(localName.equals("datafield")){
            currentDatafieldTag = atts.getValue("tag");
        }
        currentAtts = atts;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        currentValue = currentValue.trim();
        String parentLocalName = (openedElements.size() > 1) ? openedElements.get(openedElements.size() - 2).toLowerCase() : "";

        if(localName.equals("controlfield")){
           String tag = currentAtts.getValue("tag");
           if(tag.equals("001")){
               currentProperties.put(RecordHandlerKeywordsDNBMarc.ID,currentValue);
           }
        }

        //current150
        if(currentDatafieldTag != null && "150".equals(currentDatafieldTag)){
            if(localName.equals("subfield") && currentAtts != null && "a".equals(currentAtts.getValue("code"))){
                current150a = currentValue;
            }
            if(localName.equals("subfield") && currentAtts != null && "g".equals(currentAtts.getValue("code"))){
                current150g = currentValue;
            }
            if(localName.equals("subfield") && currentAtts != null && "x".equals(currentAtts.getValue("code"))){
                current150x = currentValue;
            }
        }

        if(currentDatafieldTag != null && "913".equals(currentDatafieldTag)){
            if(localName.equals("subfield") && currentAtts != null && "a".equals(currentAtts.getValue("code"))){
                current913 = currentValue;
            }
        }


        if(currentDatafieldTag != null && "450".equals(currentDatafieldTag)){
            if(localName.equals("subfield") && currentAtts != null && "a".equals(currentAtts.getValue("code"))){
                logger.info("setting symonym:"+currentValue);
                symonyms.add(currentValue);
            }
        }

        if(localName.equals("datafield")){
            currentDatafieldTag = null;
        }

        if(localName.equals("record")){
            currentProperties.put(RecordHandlerKeywordsDNBMarc.SYNONYMS,symonyms);

            if(current150a != null){
                String value = current150a;
                if(current150g != null){
                    value = value + " <"+current150g+">";
                }

                if(current150x != null){
                    value = value + " / "+current150x;
                }
                logger.info("setting name:"+value);
                currentProperties.put(RecordHandlerKeywordsDNBMarc.NAME,value);
            }

            if(currentProperties.get(RecordHandlerKeywordsDNBMarc.NAME) == null && current913 != null){

                String value = current913;

                logger.info("missing 150 using 913:"+ value +" id:"+currentProperties.get(RecordHandlerKeywordsDNBMarc.ID));
                currentProperties.put(RecordHandlerKeywordsDNBMarc.NAME, value);
            }
            try {
                this.persistentHandler.safe(new RecordHandlerInterface() {
                    @Override
                    public void handleRecord(InputStream isRecord) throws Throwable {

                    }

                    @Override
                    public Map<String, Object> getProperties() {
                        return currentProperties;
                    }
                },"","");
                processedKeywords++;
            } catch (Throwable throwable) {
                errorKeywords.add((String)currentProperties.get(RecordHandlerKeywordsDNBMarc.ID));
                logger.error(throwable.getMessage(), throwable);
            }

            currentProperties.clear();
            symonyms.clear();
            current913 = null;
            current150g = null;
            current150x = null;
            current150a = null;
        }

        if(openedElements.get(openedElements.size() - 1).equals(localName)){
            openedElements.remove(openedElements.size() - 1);
        }else{
            String message = "something went wrong closed element is not the last on stack.";
            logger.error(message);
            throw new SAXException(message);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        currentValue += new String(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {

    }

    @Override
    public void skippedEntity(String name) throws SAXException {

    }
}
