package org.edu_sharing.repository.server.importer;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.htmlparser.Parser;
import org.htmlparser.visitors.TextExtractingVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MoodleImporter {

	private static final String SOURCE_PREFIX = "moodle";
	
	Logger logger = Logger.getLogger(MoodleImporter.class); 
		
	public MoodleImporter(			
			String scheme, 
			String host,
			int port, 
			String context, 
			String wstoken,
			PersistentHandlerInterface persistentHandler) {
				
		try {
			
			String source = SOURCE_PREFIX + "_" + host;
			
			CloseableHttpClient httpclient = HttpClients.createDefault();
			
			URI uri = new URIBuilder()
		        .setScheme(scheme)
		        .setHost(host)
		        .setPort(port)
		        .setPath(context + "/webservice/rest/server.php")
		        .setParameter("wsfunction", "core_course_get_courses")
		        .setParameter("wstoken", wstoken)
		        .build();
			
			HttpGet httpget = new HttpGet(uri);
			
			CloseableHttpResponse response = httpclient.execute(httpget);
			
			String webappname = ApplicationInfoList.getHomeRepository().getWebappname();
			try {
			    
				DocumentBuilderFactory builderFactory =
				        DocumentBuilderFactory.newInstance();
				
				DocumentBuilder builder = builderFactory.newDocumentBuilder();
				
				Document xmlDocument = builder.parse(response.getEntity().getContent());
				
				XPath xPath =  XPathFactory.newInstance().newXPath();
				SimpleDateFormat sdfEdu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss");

				NodeList nodeList = (NodeList) xPath.compile("/RESPONSE/MULTIPLE/SINGLE").evaluate(xmlDocument, XPathConstants.NODESET);
				
				for (int i = 0, c = nodeList.getLength(); i < c; ++i) {
					
					Node node = nodeList.item(i);
					Map<String,Object> eduProps = new HashMap<>();
					
					String id = xPath.compile("KEY[@name='id']/VALUE/text()").evaluate(node);
					
					eduProps.put(
							CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,
							source);

					eduProps.put(
							CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, 
							id);

					long epochTime = 
							Long.parseLong(xPath.compile("KEY[@name='timemodified']/VALUE/text()").evaluate(node));
							
					eduProps.put(
							CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP,  
							sdfEdu.format(new Date(epochTime * 1000)));

					String name = xPath.compile("KEY[@name='fullname']/VALUE/text()").evaluate(node) + " (" + id + ")";
					
					eduProps.put(
							CCConstants.CM_NAME, 
							name.replaceAll(
									ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), 
									"_"));

					Parser parser = new Parser();
					parser.setInputHTML(xPath.compile("KEY[@name='summary']/VALUE/text()").evaluate(node)); 
					TextExtractingVisitor visitor = new TextExtractingVisitor(); 
					parser.visitAllNodesWith(visitor); 					
					
					eduProps.put(
							CCConstants.LOM_PROP_GENERAL_DESCRIPTION, 
							visitor.getExtractedText());

					URI link = new URIBuilder()
				        .setScheme(scheme)
				        .setHost(host)
				        .setPort(port)
				        .setPath(context + "/course/view.php")
				        .setParameter("id", id)
				        .build();
					
					eduProps.put(
							CCConstants.CCM_PROP_IO_WWWURL, 
							link.toString());					
					
					eduProps.put(
							CCConstants.CCM_PROP_IO_THUMBNAILURL, 
							"/" + webappname + "/images/logos/moodle.png");
					
					eduProps.put(
							CCConstants.LOM_PROP_GENERAL_KEYWORD, 
							SOURCE_PREFIX);
					

					persistentHandler.safe(new RecordHandlerStatic(eduProps), null, source);
					
				}
				
			} finally {
			    response.close();
			    httpclient.close();
			}
			
		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
		}
		
	}
		
}
