package org.edu_sharing.alfresco.action;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RessourceInfoTool {
	
	
	private static Log logger = LogFactory.getLog(RessourceInfoTool.class);
	
	public class QTIInfo{
		String type = null;
		String version = null;
		//Aufgabe oder Test
		String subtype = null;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public String getSubtype() {
			return subtype;
		}
		public void setSubtype(String subtype) {
			this.subtype = subtype;
		}
		
		
	}
	
	/**
	 * 
	 * @param doc
	 * @param xpath
	 * @return List of qti info objects when it's an qti, else it returns an empty list
	 */
	public ArrayList<QTIInfo> isQti(Document doc, XPath xpath){
		
		ArrayList<QTIInfo> qtiInfos = new ArrayList<>();
		try{
			String ressourceTypeExp = "/manifest/resources/resource[@type]";
			NodeList nodeList = (NodeList) xpath.evaluate(ressourceTypeExp, doc, XPathConstants.NODESET);
			if(nodeList != null){
				for(int i = 0; i < nodeList.getLength(); i++){
					Node node = nodeList.item(i);
					if(node != null){
						NamedNodeMap nnm = node.getAttributes();
						if(nnm != null){
							Node typAtt = nnm.getNamedItem("type");
							if(typAtt != null){
								String type = typAtt.getNodeValue();
								//imsqti_xmlv1p0 imsqti_item_xmlv2p1 imsqti_test_xmlv2p1
								if(type.contains("imsqti")){
									String tmpversion = null;
									String tmptype = null;
									String tmpsubtype = null;
									String[] splitted = type.split("_");
									if(splitted != null && splitted.length == 3){
										tmptype = splitted[0];
										tmpsubtype = splitted[1];
										tmpversion = splitted[2];
									}else if(splitted != null && splitted.length == 2){
										tmptype = splitted[0];
										tmpversion = splitted[1];
									}else{
										logger.info("unknown qti type splitted by \"_\" is" + splitted.length );
										continue;
									}
									
									if(tmptype != null && !tmptype.trim().equals("")){
										QTIInfo qtiInfo = new QTIInfo();
										qtiInfo.setSubtype(tmpsubtype);
										qtiInfo.setType(tmptype);
										qtiInfo.setVersion(tmpversion);
										qtiInfos.add(qtiInfo);
									}
								}
							}
						}
					}
					
				}
				
				
			}
			
			
			if(qtiInfos.size() > 0){
				logger.info("its an qti. qtiInfo(0):" + qtiInfos.get(0).getType() +" "+qtiInfos.get(0).getVersion()+" "+qtiInfos.get(0).getSubtype());
			}
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return qtiInfos;
	}
	
	/**
	 * @deprecated
	 * @param doc
	 * @param xpath
	 * @return if qti it returns String{type,version} else null
	 */
	public String[] isQti_old(Document doc, XPath xpath){
		boolean everythingIsQTI = true;
		String qtiType = null;
		String qtiVersion = null;
		try{
			String ressourceTypeExp = "/manifest/resources/resource[@type]";
			NodeList nodeList = (NodeList) xpath.evaluate(ressourceTypeExp, doc, XPathConstants.NODESET);
			if(nodeList != null){
				
				
				for(int i = 0; i < nodeList.getLength(); i++){
					Node node = nodeList.item(i);
					if(node != null){
						NamedNodeMap nnm = node.getAttributes();
						if(nnm != null){
							Node typAtt = nnm.getNamedItem("type");
							if(typAtt != null){
								String type = typAtt.getNodeValue();
								//imsqti_xmlv1p0 imsqti_item_xmlv2p1 imsqti_test_xmlv2p1
								if(type.contains("imsqti")){
									String tmpversion = null;
									String tmptype = null;
									String[] splitted = type.split("_");
									if(splitted != null && splitted.length == 3){
										tmptype = splitted[0];
										tmpversion = splitted[2];
									}else if(splitted != null && splitted.length == 2){
										tmptype = splitted[0];
										tmpversion = splitted[1];
									}else{
										logger.info("unknown qti");
										everythingIsQTI = false;
									}
									if((qtiType != null && !qtiType.equals(tmptype)) && (qtiVersion != null && !qtiVersion.equals(tmpversion))){
										everythingIsQTI = false;
									}
									
									qtiVersion = tmpversion;
									qtiType = tmptype;
									
								}else{
									everythingIsQTI = false;
								}
							}
						}
					}
					
				}
				
				if(qtiVersion == null || qtiType == null){
					everythingIsQTI = false;
				}
			}
			
			if(qtiVersion != null && qtiType != null && everythingIsQTI){
				return new String[]{qtiType, qtiVersion};
			}
			
			System.out.println("everythingIsQTI:"+everythingIsQTI+ " qtiType:"+ qtiType+" qtiVersion:"+qtiVersion);
			
			
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return null;
	}
	
	public Document loadFromStream(InputStream is) {
		Document result = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			result = builder.parse(is);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public Document loadFromFile(String filename) {
		Document result = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			result = builder.parse(new File(filename));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
