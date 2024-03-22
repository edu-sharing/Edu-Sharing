package org.edu_sharing.repository.server.tools.mailtemplates;

import jakarta.servlet.ServletContext;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MailTemplate {
	static XPathFactory pfactory = XPathFactory.newInstance();
	static XPath xpath = pfactory.newXPath();
	static Logger logger = Logger.getLogger(MailTemplate.class);
	public static String getSubject(String template,String locale) throws Exception{
	    return getChildContent(locale,template,"subject");
	}
	
	public static String getContent(String template,String locale,boolean addFooter) throws Exception{
		String data="<style>";
		data += getChildContent(getTemplates(null), "stylesheet", "style");
		data += "</style>";
	    data += getChildContent(locale,"header","message") + 
	    		"<div class='content'>"+getChildContent(locale,template,"message") + "</div>" + 
	    		(addFooter ? ("<div class='footer'>"+getChildContent(locale,"footer","message")+"</div>") : "");
	    return data;
	}

	public static void sendMail(String templateId, Map<String, String> replace) throws Exception {
		Mail mail = new Mail();

		String receiver = mail.getConfig().getString("report.receiver");
		if (receiver == null) {
			throw new IllegalArgumentException("no mail.report.receiver registered in ccmail.properties");
		}

		MailTemplate.sendMail(receiver, templateId, replace);
	}

	public static void sendMail(String receiver, String templateId, Map<String, String> replace) throws Exception {
		Mail mail = new Mail();
		ServletContext context;
		try {
			context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
		} catch(Throwable t) {
			context = Context.getGlobalContext();
		}
		String currentLocale = new AuthenticationToolAPI().getCurrentLocale();
		mail.sendMailHtml(
				context,
				receiver,
				MailTemplate.getSubject(templateId,currentLocale),
				MailTemplate.getContent(templateId,currentLocale, true),
				replace);
	}

	public static void sendMail(String senderName, String sendMail, String receiver, String templateId, Map<String, String> replace) throws Exception {
		Mail mail = new Mail();
		ServletContext context;
		try {
			context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
		} catch(Throwable t) {
			context = Context.getGlobalContext();
		}
		String currentLocale = new AuthenticationToolAPI().getCurrentLocale();
		mail.sendMailHtml(
				context,
				senderName,
				sendMail,
				receiver,
				MailTemplate.getSubject(templateId,currentLocale),
				MailTemplate.getContent(templateId,currentLocale, true),
				replace);
	}

	public static void addContentLinks(ApplicationInfo appInfo,String nodeId, Map<String, String> target, String keyName) throws Throwable{
		NodeService nodeService=NodeServiceFactory.getNodeService(appInfo.getAppId());
		String mime=MimeTypesV2.getMimeType(
				nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId),
				nodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId)
		);
		if(MimeTypesV2.MIME_DIRECTORY.equals(mime)){
			if(nodeService.hasAspect(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,CCConstants.CCM_ASPECT_COLLECTION)){
				target.put(keyName, URLTool.getNgComponentsUrl() + "collections/?id="+nodeId);
				target.put(keyName + ".static", URLTool.getNgComponentsUrl(false) + "collections/?id="+nodeId);
				return;
			}
			target.put(keyName, URLTool.getNgComponentsUrl() +  "workspace/?id="+nodeId);
			target.put(keyName + ".static", URLTool.getNgComponentsUrl(false) +  "workspace/?id="+nodeId);
			return;
		}
		target.put(keyName, URLTool.getNgComponentsUrl() + "render/"+nodeId+"?closeOnBack=true");
		target.put(keyName + ".static", URLTool.getNgComponentsUrl(false) + "render/"+nodeId+"?closeOnBack=true");
	}
	public static UserMail getUserMailData(String authorityName) {

		String fullName = null;
		String firstName = null, lastName = null, email = null;

		String user = new AuthenticationToolAPI().getCurrentUser();
		NodeRef nodeRef = AuthorityServiceHelper.getAuthorityNodeRef(authorityName);
		if(nodeRef != null) {
			firstName = (String) NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.CM_PROP_PERSON_FIRSTNAME);
			lastName = (String) NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.CM_PROP_PERSON_LASTNAME);
			email = (String) NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.PROP_USER_EMAIL);
			if(AuthorityType.GROUP.equals(AuthorityType.getAuthorityType(authorityName))) {
				firstName = (String) NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME);
				email = (String) NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPEMAIL);
			}
		}
		if (firstName != null && lastName != null) {
			fullName = (firstName + " " + lastName).trim();
		} else {
			fullName = user;
		}
		return new UserMail(fullName, firstName, lastName, email);
	}

	private static Map<TemplateDescription, Node> getTemplates(String locale) throws Exception {
		Document base=getXML(locale,false);
		NodeList templatesBase = (NodeList) xpath.evaluate("/templates/template", base, XPathConstants.NODESET);
		Map<TemplateDescription,Node> result = new HashMap<>();
		for(int i=0;i<templatesBase.getLength();i++){
			TemplateDescription desc = TemplateDescription.fromNode(templatesBase.item(i));
			if(desc!=null)
				result.put(desc,templatesBase.item(i));
		}
		try{
			Document override=getXML(locale,true);
			if(override!=null){
				NodeList templatesOverride = (NodeList) xpath.evaluate("/templates/template", override, XPathConstants.NODESET);

				for(int i=0;i<templatesOverride.getLength();i++) {
					TemplateDescription desc = TemplateDescription.fromNode(templatesOverride.item(i));
					if(desc!=null)
						result.put(desc,templatesOverride.item(i));
				}
			}
		}catch(Throwable t){
			logger.warn("Error overriding mail template for locale "+locale,t);
		}
		return result;

	}
	private static String getChildContent(String locale,String template, String name) throws Exception {
	    Map<TemplateDescription, Node> nodes = getTemplates(locale);
		String content=getChildContent(nodes,template,name);
		if(content!=null)
			return content;
		nodes = getTemplates(null);
		return getChildContent(nodes,template,name);
	}
	private static String getChildContent(Map<TemplateDescription, Node> nodes, String template, String name) throws XPathExpressionException {
		Node node = nodes.get(new TemplateDescription(template, ConfigServiceFactory.getCurrentContextId()));
		// no context specific template found, fallback to default
		if(node==null)
			node=nodes.get(new TemplateDescription(template,null));
		if(node==null){
			logger.info("No mail template found for id "+template);
			return null;
		}
		NodeList childs = node.getChildNodes();
		for(int j=0;j<childs.getLength();j++){
			if(childs.item(j).getNodeName().equals(name))
				return childs.item(j).getTextContent();
		}
	    return null;
	}

	private static Document getXML(String locale,boolean override) throws SAXException, IOException, ParserConfigurationException {
		InputStream in = null;
		String overridePostfix="";
		if(override)
			overridePostfix="_override";
		try{
			in = PropertiesHelper.Config.getInputStreamForFile(PropertiesHelper.Config.PATH_CONFIG + PropertiesHelper.Config.PathPrefix.DEFAULTS_MAILTEMPLATES + "/templates_"+locale + overridePostfix + ".xml");
		}
		catch(Throwable t){
			in = PropertiesHelper.Config.getInputStreamForFile(PropertiesHelper.Config.PATH_CONFIG + PropertiesHelper.Config.PathPrefix.DEFAULTS_MAILTEMPLATES + "/templates"+overridePostfix+".xml");
		}
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		return builder.parse(in);
	}

	public static void applyNodePropertiesToMap(String prefix, Map<String, Object> properties, Map<String, String> map) {
		properties.forEach((key, value) -> map.put(prefix + CCConstants.getValidLocalName(key), value instanceof Collection ?
				StringUtils.join((Collection)value, ", ") : value == null ? "" : value.toString()));
	}

	public static class UserMail {
		private final String fullName;
		private final String firstName;
		private final String lastName;
		private final String email;

		public UserMail(String fullName, String firstName, String lastName, String email) {
			this.fullName = fullName;
			this.firstName = firstName;
			this.lastName = lastName;
			this.email = email;
		}

		public String getFullName() {
			return fullName;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public String getEmail() {
			return email;
		}

		public void applyToMap(String prefix, Map<String, String> map) {
			map.put(prefix + "fullName", this.fullName);
			map.put(prefix + "firstName", this.firstName);
			map.put(prefix + "lastName", this.lastName);
		}
	}
}
