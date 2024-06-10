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

package org.edu_sharing.webservices.crud;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.CCForms;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CrudSoapBindingImpl implements org.edu_sharing.webservices.crud.Crud{
   
	public static final String iconFormEleName = "{http://www.alfresco.org/model/content/1.0}thumbnail#{http://www.alfresco.org/model/content/1.0}content";

	public static Logger logger = Logger.getLogger(CrudSoapBindingImpl.class);
	
	public java.lang.String create(java.lang.String username, java.lang.String ticket, java.lang.String nodeType, java.lang.String repositoryId, java.util.HashMap properties, byte[] content, byte[] icon) throws java.rmi.RemoteException {
		logger.info("params username:" + username+" ticket:"+ticket+" nodeType:"+nodeType+" repositoryId:"+repositoryId +" content:"+content +" icon:"+icon);
		
		if(content != null){
			logger.info("content size:"+content.length);
		}
		if(icon != null)  logger.info("icon size:"+icon.length);
		
		HttpClient client = new HttpClient();
		
		//allow null as repositoryId cause some clients do not know it and just want to safe things to the home repository of the service
		repositoryId = (repositoryId == null || repositoryId.trim().equals(""))? ApplicationInfoList.getHomeRepository().getAppId() : repositoryId;
		
		
		//if(repositoryId == null) throw new RemoteException("repository id is null!");
		
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
		
		if(appInfo == null) throw new RemoteException("unknown repository id" + repositoryId);
		
		
		
		
		String servletUrl = appInfo.getProtocol() + "://" + appInfo.getHost() + ":" + appInfo.getPort() + "/"+appInfo.getWebappname() + "/" + CCConstants.EDU_SHARING_SERVLET_PATH_CREATE;

		
		PostMethod method = new PostMethod(servletUrl);

		ArrayList<Part> partsList = new ArrayList<>();
		partsList.add(new StringPart(CCConstants.AUTH_USERNAME, username));
		partsList.add(new StringPart(CCConstants.AUTH_TICKET, ticket));
		partsList.add(new StringPart(CCConstants.NODETYPE, nodeType));
		partsList.add(new StringPart(CCConstants.REPOSITORY_ID, repositoryId));
		//partsList.add(new StringPart(CCConstants.CM_PROP_C_TITLE, (String) properties.get(CCConstants.CCM_PROP_IO_FILENAME)));
		
		if(content != null && content.length > 0){
			ByteArrayPartSource baps = new ByteArrayPartSource((String) properties.get(CCConstants.CCM_PROP_IO_FILENAME), content);
			
			
			//partsList.add(new FilePart("{http://www.campuscontent.de/model/1.0}io#{http://www.alfresco.org/model/content/1.0}content", baps, null, null));
			
			partsList.add(new FilePart(CCForms.getFormEleNameByProp(nodeType, CCConstants.CM_PROP_CONTENT), baps, null, null));
		}
		
		if(icon != null && icon.length > 0){
			ByteArrayPartSource bapsIcon = new ByteArrayPartSource("preview"+System.currentTimeMillis(), icon);
			partsList.add(new FilePart(iconFormEleName, bapsIcon, null, null));
		}

		for (Object key : properties.keySet()) {
			String value = (String) properties.get(key);
			//encoding angeben sonst sind die umlaute futsch
			partsList.add(new StringPart((String) key, value,"UTF-8"));
		}

		Part[] parts = partsList.toArray(new Part[partsList.size()]);

		// new FilePart(CCConstants.CM_PROP_CONTENT, baps, null, null);

		method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
		 method.getParams().setContentCharset("utf-8");

		try {
			
			//we have to login first to get a cookie
			String loginUrl = appInfo.getProtocol() + "://" + appInfo.getHost() + ":" + appInfo.getPort() + "/"+appInfo.getWebappname() +"?ticket="+ticket;
			GetMethod getMethodLogin = new GetMethod(loginUrl);
			client.executeMethod(getMethodLogin);
			
			client.executeMethod(method);
			String result = method.getResponseBodyAsString();
			//result =  result.replaceAll("(.*&lt;nodeid&gt;|&lt;/nodeid&gt;.*|.*&lt;nodeid>|&lt;/nodeid>.*|.*<nodeid>|</nodeid>.*)", "");
			
			result = parseNodeId(result);
			
			return result;
		} catch (HttpException e) {
			e.printStackTrace();
			throw new RemoteException(e.getMessage(), e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RemoteException(e.getMessage(), e);
		}
		
		finally {
			method.releaseConnection();
		}
    }

    public java.lang.String update(java.lang.String username, java.lang.String ticket, java.lang.String nodeType, java.lang.String repositoryId, java.lang.String nodeId, java.util.HashMap properties, byte[] content, byte[] icon) throws java.rmi.RemoteException {
    	HttpClient client = new HttpClient();

		if (repositoryId == null)
			throw new RemoteException("repository id is null!");
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
		if (appInfo == null)
			throw new RemoteException("unknown repository id" + repositoryId);

		String servletUrl = appInfo.getProtocol() + "://" + appInfo.getHost() + ":" + appInfo.getPort() +"/"+appInfo.getWebappname()+ "/" + CCConstants.EDU_SHARING_SERVLET_PATH_UPDATE;

		PostMethod method = new PostMethod(servletUrl);

		ArrayList<Part> partsList = new ArrayList<>();

		partsList.add(new StringPart(CCConstants.AUTH_USERNAME, username));
		partsList.add(new StringPart(CCConstants.AUTH_TICKET, ticket));
		partsList.add(new StringPart(CCConstants.NODETYPE, nodeType));
		partsList.add(new StringPart(CCConstants.REPOSITORY_ID, repositoryId));
		partsList.add(new StringPart(CCConstants.NODEID, nodeId));

		if (content != null && content.length > 0) {
			ByteArrayPartSource baps = new ByteArrayPartSource((String) properties.get(CCConstants.CCM_PROP_IO_FILENAME), content);
			partsList.add(new FilePart(CCForms.getFormEleNameByProp(nodeType, CCConstants.CM_PROP_CONTENT), baps, null, null));
		}

		if (icon != null && icon.length > 0) {
			ByteArrayPartSource bapsIcon = new ByteArrayPartSource("preview" + System.currentTimeMillis(), icon);
			partsList.add(new FilePart(iconFormEleName, bapsIcon, null, null));
		}

		for (Object key : properties.keySet()) {
			//encoding angeben sonst sind die umlaute futsch
			String value = (String) properties.get(key);
			//encoding angeben sonst sind die umlaute futsch
			partsList.add(new StringPart((String) key, value,"UTF-8"));
		}

		Part[] parts = partsList.toArray(new Part[partsList.size()]);

		// new FilePart(CCConstants.CM_PROP_CONTENT, baps, null, null);

		method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));

		try {
			
			//we have to login first to get a cookie
			String loginUrl = appInfo.getProtocol() + "://" + appInfo.getHost() + ":" + appInfo.getPort() + "/"+appInfo.getWebappname() +"?ticket="+ticket;
			GetMethod getMethodLogin = new GetMethod(loginUrl);
			client.executeMethod(getMethodLogin);
			
			client.executeMethod(method);
			String result = method.getResponseBodyAsString();
			//result = result.replaceAll("(.*&lt;nodeid&gt;|&lt;/nodeid&gt;.*|.*&lt;nodeid>|&lt;/nodeid>.*|.*<nodeid>|</nodeid>.*)", "");
			result = parseNodeId(result);
			return result;
		} catch (HttpException e) {
			e.printStackTrace();
			throw new RemoteException(e.getMessage(), e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RemoteException(e.getMessage(), e);
		} finally {
			method.releaseConnection();
		}
    }

    private String parseNodeId(String result) {
		logger.info("result:" + result);

		result = URLDecoder.decode(result);
		logger.info("result decoded:" + result);

		XPathFactory pfactory = XPathFactory.newInstance();
		XPath xpath = pfactory.newXPath();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(result)));

			return (String) xpath.evaluate("/result/nodeid", doc, XPathConstants.STRING);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}
}
