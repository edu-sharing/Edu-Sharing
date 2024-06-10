package org.edu_sharing.repository.server;

import com.typesafe.config.Config;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LRMITool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class NgServlet extends HttpServlet {
	public static final String COMPONENTS_RENDER = "components/render";
	public static final String COMPONENTS_COLLECTIONS = "components/collections";
	public static final String COMPONENTS_ERROR = "components/error";
	// max length for the html title value
	private static final int MAX_TITLE_LENGTH = 45;
	private static final int MAX_DESCRIPTION_LENGTH = 160;
	private static Logger logger = Logger.getLogger(NgServlet.class);

	public static final String PREVIOUS_ANGULAR_URL = "PREVIOUS_ANGULAR_URL";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			ApplicationInfo home = ApplicationInfoList.getHomeRepository();
			String head=home.getCustomHtmlHeaders();
			// store url for shibboleth/sso, regardless if login is present or not (for guest redirects)
			URL url = new URL(req.getRequestURL().toString()+"?"+req.getQueryString());
			if(!url.getPath().contains(COMPONENTS_ERROR)) {
				logger.debug("Persist previous frontend url: " + req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : ""));
				logger.debug("Previous frontend url path: " + url.getPath());
				req.getSession().setAttribute(PREVIOUS_ANGULAR_URL,
						req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : ""));
			}
			File index=new File(req.getSession().getServletContext().getRealPath("index.html"));
			String html=FileUtils.readFileToString(index);

			if(head!=null) {
				html = addToHead(head, html);
			}
			html = html.replace("{{ngCspNonce}}", SecurityHeadersFilter.ngCspNonce.get());
			if(url.getPath().contains(COMPONENTS_RENDER)){
				html = addLicenseMetadata(html,url);
				html = addLRMI(html,url);
				html = addEmbed(html,url);
			}
			if(url.getPath().contains(COMPONENTS_RENDER) || url.getPath().contains(COMPONENTS_COLLECTIONS)){
				html = addSEO(html,url);
			}
			if(url.getPath().contains(COMPONENTS_ERROR)){
				resp.setStatus(getErrorCode(url.getPath()));
			}
			resp.setHeader("Content-Type","text/html");
			resp.getOutputStream().write(html.getBytes("UTF-8"));
		}catch(Throwable t) {
			t.printStackTrace();
			resp.sendError(500, "Fatal error preparing index.html: "+t.getMessage());
		}
	}

	private static String addSEO(String html, URL url) {
		try {
			NodeRefVersion node = getNodeFromURL(url);
			Map<String, Object> props = NodeServiceHelper.getPropertiesVersion(node.getNodeRef(), node.getVersion());

			Document doc = Jsoup.parse(html);
			String title = (String) (props.get(CCConstants.LOM_PROP_GENERAL_TITLE));
			if(title==null || title.trim().isEmpty()){
				title= (String) props.get(CCConstants.CM_PROP_TITLE);
			}
			if(title==null || title.trim().isEmpty()){
				title= (String) props.get(CCConstants.CM_NAME);
			}
			// truncate the title to a reasonable size
			if(title.length()>MAX_TITLE_LENGTH) title = title.substring(0, MAX_TITLE_LENGTH-3) + "...";
			if(ConfigServiceFactory.getCurrentConfig().values.branding) {
				title += " – edu-sharing";
			}
			doc.title(title);
			String description = (String) props.get(CCConstants.LOM_PROP_GENERAL_DESCRIPTION);
			if(description==null || description.trim().isEmpty()){
				description= (String) props.get(CCConstants.CM_PROP_DESCRIPTION);
			}
			if(description!=null && !description.trim().isEmpty()) {
				// truncate the description to a reasonable size
				if(description.length()>MAX_DESCRIPTION_LENGTH) {
					description = description.substring(0, MAX_DESCRIPTION_LENGTH-3) + "...";
				}
				doc.head().appendElement("meta").attr("name","description").attr("content", description);
			}
			return doc.outerHtml();
		}
		catch(Throwable t){
			logger.warn("Could not add any SEO data: "+t.getMessage());
			return html;
		}
	}

	private int getErrorCode(String path) {
		try{
			String[] components=path.split("/");
			return Integer.parseInt(components[components.length-1]);
		}
		catch(Throwable t){
			return 500;
		}
	}

	private static String addLicenseMetadata(String html, URL url) {
		try {
			NodeRefVersion node = getNodeFromURL(url);
			NodeRef ref = node.getNodeRef();
			String licenseUrl=new LicenseService().getLicenseUrl(
					NodeServiceHelper.getProperty(ref,CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY),
					NodeServiceHelper.getProperty(ref,CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE),
					NodeServiceHelper.getProperty(ref,CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION)
			);
			if(licenseUrl!=null) {
				String data = "<link rel=\"license\" href=\"" + licenseUrl + "\">";
				return addToHead(data, html);
			}
		}catch(Throwable t){
			logger.error("Failed to load node license for attaching to head:",t);
		}
		return html;	}

	private static String addEmbed(String html, URL url) throws UnsupportedEncodingException {
		html=addToHead("<link rel=\"alternate\" type=\"application/json+oembed\" href=\""+URLTool.getEduservletUrl(true)+"oembed?format=json&url="+URLEncoder.encode(url.toString(),"UTF-8")+"\"/>",html);
		html=addToHead("<link rel=\"alternate\" type=\"text/xml+oembed\" href=\""+URLTool.getEduservletUrl(true)+"oembed?format=xml&url="+URLEncoder.encode(url.toString(),"UTF-8")+"\"/>",html);
		return html;
	}

	private static String addLRMI(String html, URL url) {
		try {
			NodeRefVersion node = getNodeFromURL(url);
			JSONObject lrmi = LRMITool.getLRMIJson(node);
			String data = "<script type=\"application/ld+json\">";
			data += lrmi.toString(2);
			data += "</script>";
			return addToHead(data, html);
		}catch(Throwable t){
			logger.error("Failed to load node properties for attaching LRMI data:",t);
		}
		return html;
	}

	private static NodeRefVersion getNodeFromURL(URL url) {
		if(url.toString().contains(COMPONENTS_RENDER)) {
			String[] path = url.getPath().split("/");
			int i = 0;
			for(String p: path){
				if(p.equals("render")){
					if(path.length> i + 2){
						return new NodeRefVersion(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, path[i + 1]), path[i + 2]);
					}else if(path.length > i + 1) {
						return new NodeRefVersion(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, path[i + 1]), null);
					}
				}
				i++;
			}
			return null;
		}
		if(url.toString().contains(COMPONENTS_COLLECTIONS)){
			if(!url.getQuery().contains("id="))
				return null;
			String param=url.getQuery().substring(url.getQuery().indexOf("id=")+3);
			if(param.contains("&"))
				param=param.substring(0,param.indexOf("&"));
			return new NodeRefVersion(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, param), null);
		}
		return null;
	}

	private static String addToHead(String head, String html) {
		int pos=html.indexOf("</head>");
		html=html.substring(0,pos)+head+html.substring(pos);
		return html;
	}
}
