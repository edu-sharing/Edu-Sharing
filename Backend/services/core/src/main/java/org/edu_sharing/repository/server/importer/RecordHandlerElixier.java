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

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RecordHandlerElixier implements RecordHandlerInterface{
	
	Log logger = LogFactory.getLog(RecordHandlerElixier.class);
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();

	Map<String,Object> toSafeList = new HashMap<>();
	
	public RecordHandlerElixier() {
	}
	
	public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {
		
		NodeList metadataNodeList = (NodeList)xpath.evaluate("metadata/*", nodeRecord, XPathConstants.NODESET);
		
		for(int i = 0; i < metadataNodeList.getLength();i++){
			Node child = metadataNodeList.item(i);
			String nodeName = child.getNodeName();
			String count = (String)xpath.evaluate("count(metadata/"+nodeName+")", nodeRecord, XPathConstants.STRING);
			String value = "";
			if(new Integer(count) > 1){
				NodeList multivalue = (NodeList)xpath.evaluate("metadata/"+nodeName, nodeRecord, XPathConstants.NODESET);
				
				for(int mvcount = 0; mvcount < multivalue.getLength(); mvcount++){
					Node actNode = multivalue.item(mvcount);
					value = value + "," +(String)xpath.evaluate(".", actNode, XPathConstants.STRING);
				}
			}else{
				value = (String)xpath.evaluate(".", child, XPathConstants.STRING);
			}
			
			if(!toSafeList.keySet().contains(nodeName)) toSafeList.put(nodeName, value);
			
		}
	}
	
	@Override
	public Map<String, Object> getProperties() {
		return toSafeList;
	}
	
}
