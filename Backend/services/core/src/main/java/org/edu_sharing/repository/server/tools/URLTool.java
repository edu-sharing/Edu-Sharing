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
package org.edu_sharing.repository.server.tools;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.springframework.extensions.surf.util.URLEncoder;


public class URLTool{

	public static String propertyfile = CCConstants.REPOSITORY_FILE_HOME;

	public static String hostProp = "host";

	public static String portProp = "port";

	public static String afrescocontextProp = "alfrescocontext";

	

	private static Log logger = LogFactory.getLog(URLTool.class);

	public static String getBrowserURL(NodeRef node) {
		return getBrowserURL(node, null);
	}
	
	public static String getBrowserURL(NodeRef node, String property) {
		String previewURL = null;
		try {
			
			if(property == null){
				property = CCConstants.CM_PROP_CONTENT;
			}
			
			ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
			
			if(property.equals(CCConstants.CM_PROP_CONTENT)){
				/**
				 * Collection change nodeRef to original
				 */
				if(serviceRegistry.getNodeService().hasAspect(node, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
					String nodeId = (String)serviceRegistry.getNodeService().getProperty(node, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
					node = new NodeRef(MCAlfrescoAPIClient.storeRef,nodeId);
				}
			}
			
			
			
			String contentUrl = null;
			try{
				logger.debug("propertyNAme:"+property);
				QName propQname = QName.createQName(property); 
				contentUrl = serviceRegistry.getContentService().getReader(node,propQname).getContentUrl();
			}catch(NullPointerException e){
				
			}
			if(contentUrl != null){
				NodeService nodeService = serviceRegistry.getNodeService();
				String name = (String) nodeService.getProperty(node, QName.createQName(CCConstants.CM_NAME));
				previewURL = generateBrowserURL(node, name);
				
				
				ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
				String hostOrDomain = (homeRepository.getDomain() == null || homeRepository.getDomain().trim().equals(""))? homeRepository.getHost() : homeRepository.getDomain();
				
				String host = hostOrDomain;
				logger.debug("host:"+host);
				String port = homeRepository.getClientport();
				logger.debug("port:"+port);
				String alfctx = homeRepository.getAlfrescocontext();
				logger.debug("alfctx:"+alfctx);
				
				String protocol = homeRepository.getClientprotocol();
				
				if(port.equals("80") || port.equals("443")){
					previewURL = protocol+"://" + host + alfctx + previewURL;
				}else{
					previewURL = protocol+"://" + host + ":" + port + alfctx + previewURL;
				}
				
				if(property != null){
					if(previewURL.contains("?")){
						previewURL = previewURL + "&property="+property;
					}else{
						previewURL = previewURL + "?property="+property;
					}
				}
			}
		} catch (Exception e) {
			
			logger.error(e);
		}
		return previewURL;
	}

	public static String getBaseUrl(){
		return URLHelper.getBaseUrl(false);
	}

	public static String getNgMessageUrl(String messageId){
		return URLHelper.getNgComponentsUrl()+"messages/"+messageId;
	}
	public static String getNgErrorUrl(String errorId){
		return URLHelper.getNgComponentsUrl()+"error/"+errorId;
	}

	public static String getNgAssetsUrl(){
        return URLHelper.getBaseUrl(false)+"/assets/";
    }

	public static String getPreviewServletUrl(String node, String storeProtocol,String storeId,String baseUrl) {
		ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
		NodeService alfNodeService = serviceRegistry.getNodeService();
		NodeRef nodeRef = new NodeRef(new StoreRef(storeProtocol,storeId),node);
		QName type = serviceRegistry.getNodeService().getType(nodeRef);
		if(type.equals(QName.createQName(CCConstants.CCM_TYPE_REMOTEOBJECT))) {
			String repoId = (String)alfNodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID));
			String remoteNodeId = (String)alfNodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID));
			try {
				Map<String, Object> props = NodeServiceFactory.getNodeService(repoId).getProperties(null, null, remoteNodeId);
				return  (String)props.get(CCConstants.CM_ASSOC_THUMBNAILS);
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}

		}else {
			String previewURL = baseUrl;
			previewURL += "/preview?nodeId="+node+"&storeProtocol="+storeProtocol+"&storeId="+storeId+"&dontcache="+System.currentTimeMillis();
			previewURL =  addOAuthAccessToken(previewURL);
			return previewURL;
		}
	}
	public static String getPreviewServletUrl(String node, String storeProtocol,String storeId){
		return getPreviewServletUrl(node,storeProtocol,storeId, URLHelper.getBaseUrl(true));
	}
	public static String getPreviewServletUrl(NodeRef node){
		return getPreviewServletUrl(node.getId(), node.getStoreRef().getProtocol(), node.getStoreRef().getIdentifier());
	}
	public static String getPreviewServletUrl(org.edu_sharing.service.model.NodeRef node) {
		return getPreviewServletUrl(node.getNodeId(), node.getStoreProtocol(), node.getStoreId());
	}




	public static String getShareServletUrl(NodeRef node, String token){
		String shareUrl = URLHelper.getBaseUrl(true);
		shareUrl += "/share?nodeId="+node.getId()+"&token="+token;
		return shareUrl;
	}
	

	
	/**
	 * returns null when no RenderServiceUrl is set
	 * @param nodeID
	 * @param preview
	 * @return
	 */
	public static String getRenderServiceURL(String nodeID, boolean preview){
		
		String CONTENTURLKEY = "contenturl";
		String PREVIEWURLKEY = "previewurl";

		String url = null;
		
		String nodeIdKey = "obj_id";
		ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
		try{
			if(preview){
				url = appInfo.getPreviewUrl();
			}else{
				url = appInfo.getContentUrl();
			}
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			return null;
		}
		
		if(url != null && !url.trim().equals("")){
			if(url.contains("?")) url = url + "&"+nodeIdKey+"="+nodeID;
			else url = url + "?"+nodeIdKey+"="+nodeID;
			return url;
		}else{
			return null;
		}
	}

	/**
	 * code from alfresco DownloadContentServlet (no longer available in 5.0.d)
	 * @return
	 */
	public static String generateBrowserURL(NodeRef nodeRef, String name){
		 String URL_DIRECT        = "d";
		 String BROWSER_URL   = "/d/" + URL_DIRECT + "/{0}/{1}/{2}/{3}";
		 
		return  MessageFormat.format(BROWSER_URL, new Object[] {
				 nodeRef.getStoreRef().getProtocol(),
				 nodeRef.getStoreRef().getIdentifier(),
				 nodeRef.getId(),
	              URLEncoder.encode(name) } );
	}
	/**
	 * adds the accessToken to the url if the user is authenticated via oauth
	 * @param url
	 * @return
	 */
	public static String addOAuthAccessToken(String url) {
		if(Context.getCurrentInstance() != null){
			String accessToken = (String) Context.getCurrentInstance().getAccessToken();
			if(accessToken!=null)
				url=UrlTool.setParam(url, CCConstants.REQUEST_PARAM_ACCESSTOKEN, accessToken);
			}
		return url;
		
	}

	/**
	 * Get the url to a angular collection
	 * @param nodeId
	 * @return
	 */
	public static String getNgCollectionUrl(String nodeId) {
		return URLHelper.getNgComponentsUrl()+"collections?id="+nodeId;
	}
	
	public static String getRedirectServletLink(String repId, String nodeId){
		String url = URLHelper.getBaseUrl(true) + "/" + CCConstants.EDU_SHARING_SERVLET_PATH_REDIRECT;
		//if no cookies are allowed render jsessionid in url. Attention: the host or domain in appinfo must match the client ones
		Context context = Context.getCurrentInstance();
		//context can be null when not accessing true ContextManagementFilter (i.i by calling nativealfrsco webservice)
		if(context != null){
			url = context.getResponse().encodeURL(url);
		}
		
		url = UrlTool.setParam(url, "APP_ID",  repId);
		url =  UrlTool.setParam(url,"NODE_ID", nodeId);
		
		return url;
	}

    public static String getRestServiceUrl() {
        return getBaseUrl()+"/rest/";
    }

	public static String getEduservletUrl() {
		return getEduservletUrl(false);
	}
	public static String getEduservletUrl(boolean dynamic) {
		return URLHelper.getBaseUrl(dynamic)+"/eduservlet/";
	}

	public static String getDownloadServletUrl(String id,String version, boolean dynamic, String repositoryId) {
		String download=getEduservletUrl(dynamic)+"download?nodeId="+URLEncoder.encodeUriComponent(id);
		if(version!=null){
			download += "&version=" + URLEncoder.encodeUriComponent(version);
		}
		if(repositoryId!=null){
			download += "&repositoryId=" + URLEncoder.encodeUriComponent(repositoryId);
		}
		return download;
	}
	public static String getDownloadServletUrl(String id,String version, boolean dynamic) {
		return getDownloadServletUrl(id,version,true, null);
	}
}

