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
package org.edu_sharing.repository.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.rendering.RenderingServiceFactory;

import java.io.IOException;

public class RedirectServlet extends HttpServlet {

	private static Log logger = LogFactory.getLog(RedirectServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String appId = req.getParameter("APP_ID");
		String nodeId = req.getParameter("NODE_ID");
		if(!RenderingServiceFactory.getRenderingService(appId).renderingSupported()){
			String wwwurl = NodeServiceFactory.getNodeService(appId).getProperty(StoreRef.PROTOCOL_WORKSPACE,
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
					nodeId,
					CCConstants.LOM_PROP_TECHNICAL_LOCATION);
			if(wwwurl != null) {
				resp.sendRedirect(wwwurl);
				return;
			}
			throw new ServletException("Repository "+appId+" does not support rendering and didn't provide a "+CCConstants.LOM_PROP_TECHNICAL_LOCATION+" property! Please make sure that a property is provided");
		}
		String url= URLHelper.getNgRenderNodeUrl(nodeId,req.getParameter("version"), false, appId);
		String params = req.getParameter("params");
		if (params != null && !params.trim().equals("")) {
			url=UrlTool.setParamEncode(url,"params",params);
		}
		resp.sendRedirect(url);
		/*
		// gets appID und NodeId
		// checks if app id is an repository and if there is a node with the given nodid
		// if true, creates AlfrescoRemoteNode object
		// after this, go to renderservice with created nodeid
		
		String appId = req.getParameter("APP_ID");
		String nodeId = req.getParameter("NODE_ID");
		String version = req.getParameter("version");

		String username = (String)req.getSession().getAttribute(CCConstants.AUTH_USERNAME);
		String ticket = (String)req.getSession().getAttribute(CCConstants.AUTH_TICKET);
		
		// LMS_URL: if set publish in LMS handling else renderservice Handling
		String toLmsUrl = req.getParameter("LMS_URL");

		ApplicationInfo repInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		if (null == repInfo) {
			logger.error("ApplicationInfoList.getRepositoryInfoById(appId) did not return repository information.");
			resp.getOutputStream().print("ApplicationInfoList.getRepositoryInfoById(appId) did not return repository information.");
			return;
		}
		
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		if (null == homeRepository) {
			logger.error("ApplicationInfoList.getHomeRepository() did not return home-repository information.");
			resp.getOutputStream().print("ApplicationInfoList.getHomeRepository() did not return home-repository information.");
			return;
		}

		String renderUrlNodeId = null;

		// Final Use Remote Objects only by none Alfresco Repositories cause
		// they can not put Usage Objects. Only when redirect url
		if(toLmsUrl != null && !toLmsUrl.trim().equals("")) {
			renderUrlNodeId = new RemoteObjectService().getRemoteObject(repInfo.getAppId(), nodeId);
		}else {
			renderUrlNodeId = nodeId;
		}
		
		String redirectUrl = null;
		
		// renderservice
		if (toLmsUrl == null || toLmsUrl.trim().equals("")) {

			String renderServiceUrl = null;
			if(repInfo.getContentUrl() != null 
					//remote alfresco repo we don't need a contenturl
					|| (!homeRepository.getAppId().equals(repInfo.getAppId()) && repInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_ALFRESCO))){
				try{
					String paramCom = req.getParameter("com");
					if(paramCom != null && paramCom.trim().equals(RenderingTool.COM_INTERNAL)) {
						renderServiceUrl = new RenderingTool().getRenderServiceUrl(repInfo,renderUrlNodeId,username,null,false,true);
						renderServiceUrl = UrlTool.setParam(renderServiceUrl,"com",RenderingTool.COM_INTERNAL);
					}else {
						renderServiceUrl = new RenderingSe().getRenderServiceUrl(repInfo,renderUrlNodeId, username);
					}
					
				}catch(GeneralSecurityException e){
					logger.error(e.getMessage(), e);
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}

			}
			
			logger.info("renderServiceUrl:"+renderServiceUrl);
		
			if (renderServiceUrl != null) {

				// put additional params that come with the encoded params param
				String params = req.getParameter("params");
				if (params != null && !params.trim().equals("")) {
					params = URLDecoder.decode(params);
					System.out.println("adding params to render url:" + params);
					renderServiceUrl = (renderServiceUrl.contains("?")) ? renderServiceUrl + "&" + params : renderServiceUrl + "?" + params;
                    try {
                        List<NameValuePair> parsed = URLEncodedUtils.parse(params, Charset.defaultCharset());
                        for(NameValuePair pair : parsed){
                            if(pair.getName().equals("display") && pair.getValue().equals("download")){
                                // Track download action for node
								TrackingTool.trackActivityOnNode(nodeId,new NodeTrackingDetails(version),TrackingService.EventType.DOWNLOAD_MATERIAL);
                                break;
                            }
                        }
                        logger.info(parsed);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

				if(version != null && !version.trim().equals("")){
					renderServiceUrl = UrlTool.setParam(renderServiceUrl,"version",version);
				}
				
				if(ApplicationInfo.REPOSITORY_TYPE_EDUNEX.equals(repInfo.getRepositoryType())){
					String arixContextPath = (String)Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.SESSION_ARIX_CONTEXT_PREFIX + repInfo.getAppId());
					
					if(arixContextPath == null){
						arixContextPath = "/" +repInfo.getWebappname()+"/"+ repInfo.getPath();
					}
					renderServiceUrl = UrlTool.setParam(renderServiceUrl,"context",arixContextPath);
				}
			}

			// no render service url is configured. take the standard alfresco
			// contenturl
			if ((renderServiceUrl == null || renderServiceUrl.trim().equals(""))) {
				
				Map<String, String> authInfo = new HashMap<>();
				authInfo.put(CCConstants.AUTH_USERNAME, username);
				authInfo.put(CCConstants.AUTH_TICKET, ticket);
				try {
					if(repInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_ALFRESCO)){
	
							MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(repInfo.getAppId(), authInfo);
							// remoteObjectNodeId wenn remoteRepository
							renderServiceUrl = mcAlfrescoBaseClient.getAlfrescoContentUrl(renderUrlNodeId);

							if(version != null && !version.trim().equals("")){
								Map<String,Map<String,Object>> versHist = mcAlfrescoBaseClient.getVersionHistory(renderUrlNodeId);
								
								for(Map.Entry<String,Map<String,Object>> entry : versHist.entrySet()){
									
									
									String currentVers = (String)entry.getValue().get(CCConstants.CM_PROP_VERSIONABLELABEL);
									
									if(version.equals(currentVers)){
										String versionStoreNodeId = (String)entry.getValue().get(CCConstants.VERSION_STORE_NODEID);
										renderServiceUrl = URLTool.getBrowserURL(new NodeRef("workspace","version2Store",versionStoreNodeId));
									}
									
								}
								//getBrowserURL
							}
							
							System.out.println("CONTENTHASH:"+((MCAlfrescoAPIClient)mcAlfrescoBaseClient).getContentHash(renderUrlNodeId,CCConstants.CM_PROP_CONTENT));
							
							renderServiceUrl = UrlTool.setParam(renderServiceUrl, "ticket", ticket);
							
							if(renderServiceUrl != null){
								renderServiceUrl = URIUtil.encodeQuery(renderServiceUrl);
							}
							
							//fallback to wwwurl
							if(renderServiceUrl == null){
								 HashMap props = mcAlfrescoBaseClient.getProperties(renderUrlNodeId);
								 if(props != null){
									String wwwurl = (String)props.get(CCConstants.CCM_PROP_IO_WWWURL);
									if(wwwurl != null && !wwwurl.trim().equals("")){
										renderServiceUrl = wwwurl;
									}
									//fallback to technical location
									if(renderServiceUrl == null){
										String technicalLocation = (String)props.get(CCConstants.LOM_PROP_TECHNICAL_LOCATION);
										if(technicalLocation != null && !technicalLocation.trim().equals("")){
											renderServiceUrl = technicalLocation;
										}
									}
								 }
							}
							
						
					}else{
						
						Map<String, Object> props = NodeServiceFactory.getNodeService(appId).getProperties(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId);

						 if(props != null){
							 String technicalLocation = (String)props.get(CCConstants.LOM_PROP_TECHNICAL_LOCATION);
							if(technicalLocation != null && !technicalLocation.trim().equals("")){
								renderServiceUrl = technicalLocation;
							}
						 }
						 if(renderServiceUrl==null){
							 resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Remote repository did not provide "+CCConstants.LOM_PROP_TECHNICAL_LOCATION);
							 return;
						 }
					}
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
					return;
				}
			}
			
			redirectUrl = renderServiceUrl;
			
		}
		// publish in LMS
		else {
			logger.info("toLMSUrl bevore:" + toLmsUrl);
			toLmsUrl = URLDecoder.decode(toLmsUrl);
			logger.info("toLMSUrl decoded:" + toLmsUrl);

			String repositoryId = repInfo.getAppId();
			if (!repInfo.ishomeNode() && repInfo.getRepositoryType() != null && !repInfo.getRepositoryType().trim().equals("")
					&& !repInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_ALFRESCO)) {
				// take homerepository cause there are the remote Objects
				repositoryId = homeRepository.getAppId();
			}

			String repUrl = "ccrep://" + repositoryId + "/" + renderUrlNodeId;

			redirectUrl = UrlTool.setParam(toLmsUrl, "nodeId", repUrl);

			try {
				redirectUrl = setUrlParameters(appId, nodeId, repInfo, redirectUrl);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
				resp.getOutputStream().print(e.getMessage());
				return;
			}
		}

		logger.info("redirectUrl:" + redirectUrl);

		resp.sendRedirect(redirectUrl);

	}
	
	private String setUrlParameters(String appId, String nodeId, ApplicationInfo repInfo, String redirectUrl)
			throws Throwable {
		NodeService nodeService = NodeServiceFactory.getNodeService(appId);
		Map<String, Object> props = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId);
		
		if (props != null) {
			String title = (String) props.get(CCConstants.LOM_PROP_GENERAL_TITLE);
			if(title==null || title.isEmpty()){
				title = (String) props.get(CCConstants.CM_NAME);
			}
			if (title != null) {
				if (title.length() > 150)
					title = title.substring(0, 149) + "...";
				redirectUrl = UrlTool.setParamEncode(redirectUrl, "title", title);
			}
			
			String mimeType = (String) props.get(CCConstants.ALFRESCO_MIMETYPE);
			if(mimeType != null && !mimeType.trim().equals("")){
				redirectUrl = UrlTool.setParamEncode(redirectUrl, "mimeType", mimeType);
			}
			
			String resourceType = (String) props.get(CCConstants.CCM_PROP_CCRESSOURCETYPE);
			if(resourceType != null && !resourceType.trim().equals("")){
				redirectUrl = UrlTool.setParamEncode(redirectUrl, "resourceType", resourceType);
			}
			
			String resourceVersion = (String) props.get(CCConstants.CCM_PROP_CCRESSOURCEVERSION);
			if(resourceVersion != null && !resourceVersion.trim().equals("")){
				redirectUrl = UrlTool.setParamEncode(redirectUrl, "resourceVersion", resourceVersion);
			}
			
			String width = (String) props.get(CCConstants.CCM_PROP_IO_WIDTH);
			if(width != null && !width.trim().equals("")){
				try{
					width=""+Math.round(Double.parseDouble(width));
				}
				catch(Throwable t){}
				redirectUrl = UrlTool.setParamEncode(redirectUrl, "w", width);
			}
			String height =  (String) props.get(CCConstants.CCM_PROP_IO_HEIGHT);
			if(height != null && !height.trim().equals("")){
				try{
					height=""+Math.round(Double.parseDouble(height));
				}
				catch(Throwable t){}
				redirectUrl = UrlTool.setParamEncode(redirectUrl, "h", height);
			}
			String version = (String) props.get(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
			if(version != null && !version.trim().equals("")){
				redirectUrl = UrlTool.setParamEncode(redirectUrl, "v", version);
			}

			boolean isDirectory = MimeTypesV2.isDirectory(props);
			redirectUrl = UrlTool.setParamEncode(redirectUrl, "isDirectory", isDirectory+"");

			String type = CCConstants.getValidLocalName(nodeService.getType(nodeId));
			redirectUrl = UrlTool.setParamEncode(redirectUrl, "nodeType", type);

			String iconUrl = new MimeTypesV2(ApplicationInfoList.getHomeRepository()).getIcon(nodeService.getType(nodeId),props,Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId)));
			redirectUrl = UrlTool.setParamEncode(redirectUrl, "iconUrl", iconUrl);

			//if it's a remoteObject(no alfresco) make it possible for the lms to show some type icons, license info a.s.o
			String repoType = repInfo.getRepositoryType();
			if(repoType != null && !repoType.trim().equals("")){
				redirectUrl = UrlTool.setParamEncode(redirectUrl, "repoType", repoType);
			}
		}
		return redirectUrl;
		*/
	}
	
}
