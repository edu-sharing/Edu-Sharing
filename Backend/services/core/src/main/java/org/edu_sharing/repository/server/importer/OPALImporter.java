package org.edu_sharing.repository.server.importer;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.htmlparser.Parser;
import org.htmlparser.visitors.TextExtractingVisitor;

public class OPALImporter {

	private static final String SOURCE_PREFIX = "opal";
	private static final String HEADER_TOKEN = "X-OLAT-TOKEN";
	
	Logger logger = Logger.getLogger(OPALImporter.class); 
		
	public OPALImporter(			
			String scheme, 
			String host,
			int port, 
			String context, 
			String user,
			String password,
			PersistentHandlerInterface persistentHandler) {
				
		try {
			
			String source = SOURCE_PREFIX + "_" + host;
			
			SimpleDateFormat sdfEdu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss");

			CloseableHttpClient httpclient = HttpClients.createDefault();
			
			URI auth = new URIBuilder()
		        .setScheme(scheme)
		        .setHost(host)
		        .setPort(port)
		        .setPath(context + "/restapi/auth/" + user)
		        .setParameter("password", password)
		        .build();
			
			CloseableHttpResponse authResponse = httpclient.execute(new HttpGet(auth));
			String token = authResponse.getFirstHeader(HEADER_TOKEN).getValue();	

			URI uri = new URIBuilder()
	        .setScheme(scheme)
	        .setHost(host)
	        .setPort(port)
	        .setPath(context + "/restapi/enterprise/catalog/search/courses")
	        .build();
			
			HttpGet httpget = new HttpGet(uri);
			
			httpget.setHeader(HEADER_TOKEN, token);
			httpget.setHeader("Content-Type", "application/x-www-form-urlencoded");
			
			CloseableHttpResponse response = httpclient.execute(httpget);

			String webappname = ApplicationInfoList.getHomeRepository().getWebappname();
			try {
				
				ObjectMapper mapper = new ObjectMapper(); 
				Response data = mapper.readValue(EntityUtils.toString(response.getEntity(), "UTF-8"), Response.class);
				
				for (Response.RepositoryResource course : data.repositoryResource) {
					
					Map<String,Object> eduProps = new HashMap<>();
					
					eduProps.put(
							CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,
							source);

					String id = course.key;
					
					eduProps.put(
							CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, 
							course.key);

					eduProps.put(
							CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP,  
							sdfEdu.format(new Date()));

					String name = course.displayname + " (" + id + ")";
					
					eduProps.put(
							CCConstants.CM_NAME, 
							name.replaceAll(
									ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), 
									"_"));

					Parser parser = new Parser();
					parser.setInputHTML(course.description);
					TextExtractingVisitor visitor = new TextExtractingVisitor(); 
					parser.visitAllNodesWith(visitor); 					
					
					eduProps.put(
							CCConstants.LOM_PROP_GENERAL_DESCRIPTION, 
							visitor.getExtractedText());

					URI link = new URIBuilder()
				        .setScheme(scheme)
				        .setHost(host)
				        .setPort(port)
				        .setPath(context + "/auth/RepositoryEntry/" + course.key)
				        .build();
					
					eduProps.put(
							CCConstants.CCM_PROP_IO_WWWURL, 
							link.toString());					
					
					eduProps.put(
							CCConstants.CCM_PROP_IO_THUMBNAILURL, 
							"/" + webappname + "/images/logos/opal.png");
					
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
		
	private static class Response {
		
		public RepositoryResource[] repositoryResource;
		
		@JsonIgnore
		public Object pagination;
		
		public static class RepositoryResource {
			
			public String key;
			public String displayname;
			public String description;
			
			@JsonIgnore
			public Object softkey;
			@JsonIgnore
			public Object resourcename;
			@JsonIgnore
			public Object resourceableId;
			@JsonIgnore
			public Object resourceableTypeName;
			@JsonIgnore
			public Object nodeTitle;
			@JsonIgnore
			public Object nodeLongTitle;
			@JsonIgnore
			public Object nodeDescription;
			@JsonIgnore
			public Object nodeIdent;
			@JsonIgnore
			public Object status;
			@JsonIgnore
			public Object metadata;
			@JsonIgnore
			public Object relevantChildren;
			@JsonIgnore
			public Object userRelatedData;
			
		}
	}
}
