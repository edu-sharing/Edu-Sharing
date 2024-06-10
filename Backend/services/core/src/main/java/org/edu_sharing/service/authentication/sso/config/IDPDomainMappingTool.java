package org.edu_sharing.service.authentication.sso.config;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class IDPDomainMappingTool {
	Log logger = LogFactory.getLog(IDPDomainMappingTool.class);
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	String file = "/config/cluster/idp.xml";

	Document docMdSets = null;
	
	Map<String,String> mapper = new HashMap<>();

	public IDPDomainMappingTool() {
		URL url = IDPDomainMappingTool.class.getResource(file);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			docMdSets = builder.parse(url.openStream());
			initMap();
		} catch (ParserConfigurationException e) {
			logger.error(e.getMessage(), e);
		} catch (SAXException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	public String getDomain(String idpUrlPart) {
		return get(idpUrlPart, "domain");
	}

	public String getCaption(String idpUrlPart) {
		return get(idpUrlPart, "caption");
	}

	private String get(String idpUrlPart, String tag) {
		try {

			xpath.reset();

			NodeList idps = (NodeList) xpath.evaluate("/idp/idp-item", docMdSets, XPathConstants.NODESET);
			for (int i = 0; i < idps.getLength(); i++) {
				Node item = idps.item(i);
				String idpUrl = (String) xpath.evaluate("idp", item, XPathConstants.STRING);
				if (idpUrl.contains(idpUrlPart)) {
					String value = (String) xpath.evaluate(tag, item, XPathConstants.STRING);
					if (value != null && !value.trim().equals("")) {
						return value.trim();
					}
				}
			}
			return null;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}
	
	private void initMap(){
		try {

			xpath.reset();

			NodeList idps = (NodeList) xpath.evaluate("/idp/idp-item", docMdSets, XPathConstants.NODESET);
			for (int i = 0; i < idps.getLength(); i++) {
				Node item = idps.item(i);
				String domain = (String) xpath.evaluate("domain", item, XPathConstants.STRING);
				String caption = (String) xpath.evaluate("caption", item, XPathConstants.STRING);
				mapper.put(domain, caption);
			}
	
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		
		}
	}
	
	public Map<String, String> getMapper() {
		return mapper;
	}
}
