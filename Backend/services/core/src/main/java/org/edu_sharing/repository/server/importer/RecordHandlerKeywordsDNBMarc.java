package org.edu_sharing.repository.server.importer;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * - run with file using job class
 * -
 */
public class RecordHandlerKeywordsDNBMarc implements RecordHandlerInterface{

    Logger logger = Logger.getLogger(RecordHandlerKeywordsDNBMarc.class);
    XPathFactory pfactory = XPathFactory.newInstance();
    XPath xpath = pfactory.newXPath();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    HashMap toSafeMap = new HashMap();

    public static String ID = "ID";
    public static String NAME = "NAME";
    public static String SYNONYMS = "SYNONYMS";

    public RecordHandlerKeywordsDNBMarc(String metadataSetId){

    }

    @Override
    public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {
        toSafeMap.clear();
        xpath.reset();
        //not in file
        String identifier = (String) xpath.evaluate("header/identifier", nodeRecord, XPathConstants.STRING);
        //String timeStamp = (String) xpath.evaluate("header/datestamp", nodeRecord, XPathConstants.STRING);

        Node root = null;
        //got record by oai service
        if(identifier != null && !identifier.trim().equals("")){
            root = (Node)xpath.evaluate("metadata/record", nodeRecord, XPathConstants.NODE);
        }else{
            root = nodeRecord;
        }

        //Quelle und Datum der Ersterfassung
        String id = ((String)xpath.evaluate("controlfield[@tag='001']",root,XPathConstants.STRING)).trim();

        //150 = Sachbegriff – Bevorzugte Benennung
        String name = (String)xpath.evaluate("datafield[@tag='913']/subfield[@code='a']",root,XPathConstants.STRING);
        name = name.trim();

        if(name == null || name.equals("")){
            String current150a = (String)xpath.evaluate("datafield[@tag='150']/subfield[@code='a']",root,XPathConstants.STRING);
            String current150g = (String)xpath.evaluate("datafield[@tag='150']/subfield[@code='g']",root,XPathConstants.STRING);
            String current150x = (String)xpath.evaluate("datafield[@tag='150']/subfield[@code='x']",root,XPathConstants.STRING);

            name = current150a;
            if(current150g != null && !current150g.trim().equals("")){
                name = name + " <"+current150g+">";
            }

            if(current150x != null && !current150x.trim().equals("")){
                name = name + " / "+current150x;
            }
        }

        //550 = Sachbegriff Beziehung
        NodeList nodeListRelations = (NodeList)xpath.evaluate("datafield[@tag='450']",root,XPathConstants.NODESET);

        Set<String> synonyms = new LinkedHashSet<String>();
        for(int i = 0; i < nodeListRelations.getLength(); i++){
            String synonym = (String)xpath.evaluate("subfield[@code='a']",nodeListRelations.item(i),XPathConstants.STRING);
            synonyms.add(synonym);
        }

        logger.info("id:"+ id + " name:"+name.trim() +" synonyms:"+synonyms);

        toSafeMap.put(ID,id);
        toSafeMap.put(NAME,name);
        toSafeMap.put(SYNONYMS,synonyms);
    }

    public static void main(String[] args) throws Throwable {
        XPathFactory pfactory = XPathFactory.newInstance();
        XPath xpath = pfactory.newXPath();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        System.out.println("nach DocumentBuilderFactory.newInstance()");

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new FileReader("/Users/mv/Downloads/authorities-sachbegriff_dnbmarc_20201013.mrc.xml")));

        System.out.println("vor doc.getChildNodes()");
        NodeList records = doc.getChildNodes().item(0).getChildNodes();
        System.out.println("nach doc.getChildNodes():" + records.getLength());

        PersistenHandlerKeywordsDNBMarc ph = new PersistenHandlerKeywordsDNBMarc();
        //NodeList records = (NodeList)xpath.evaluate("/record", doc, XPathConstants.NODESET);
        for(int i = 0; i < records.getLength() && i < 100; i++){
            Node node = records.item(i);
            //System.out.println("i:"+node.getNodeName());
            if("record".equals(node.getNodeName())) {
                RecordHandlerKeywordsDNBMarc rh = new RecordHandlerKeywordsDNBMarc(null);
                rh.handleRecord(node,"","");
                ph.safe(rh,"","");
            }
        }
    }

    @Override
    public Map<String, Object> getProperties() {
        return toSafeMap;
    }
}
