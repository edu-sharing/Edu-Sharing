package org.edu_sharing.service.rendering;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigCache;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.config.ConfigServiceFactory;

public class RenderingTool {

	public static String DISPLAY_PRERENDER = "prerender";
	public static String DISPLAY_DYNAMIC = "dynamic";
	public static String DISPLAY_EMBED = "embed";
	// only content (e.g. video player), no license data
	public static String DISPLAY_CONTENT = "content";
	public static String DISPLAY_INLINE = "inline";
	
	public static String COM_INTERNAL = "internal";
	
	static Logger logger = Logger.getLogger(RenderingTool.class);

	// thread tasks for processing requests of pre-rendering objects
	static ExecutorService prepareExecutor = Executors.newFixedThreadPool(1);
	/**
	 * this only works for alfresco repositories
	 * 
	 * @param repInfo
	 * @return
	 * @throws GeneralSecurityException
	 */
	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId,Map<String,String> parameters) throws GeneralSecurityException{

		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
		
		String renderingService = homeRepo.getContentUrl();

		//renderServiceUrl = UrlTool.setParam(renderServiceUrl, "proxyRepId", ApplicationInfoList.getHomeRepository().getAppId());
		
		long timestamp = System.currentTimeMillis();

		if(parameters!=null){
			for(Entry<String, String> param : parameters.entrySet()){
				renderingService = UrlTool.setParam(renderingService, param.getKey(),param.getValue());
			}
		}
		String appId = repInfo.getAppId();
		if(nodeId == null) {
			appId = "";
			nodeId = UUID.randomUUID().toString();
			renderingService = UrlTool.setParam(renderingService, "sig_token", nodeId);
		}
		renderingService = UrlTool.setParam(renderingService, "ts",""+timestamp);
		try{
			renderingService = UrlTool.setParam(renderingService, "language",new AuthenticationToolAPI().getCurrentLanguage());
		}catch(Throwable t){}

		renderingService = UrlTool.setParam(renderingService, "sig", getSignatureSigned(appId,nodeId,timestamp));
		return renderingService;

	}
	
	/**
	 * Just an override with few parameters!
	 */
	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId) throws GeneralSecurityException{
		return getRenderServiceUrl(repInfo,nodeId,null,null);
	}

	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId,Map<String,String> parameters,String displayType) throws GeneralSecurityException {
		
		String baseUrl = getRenderServiceUrl(repInfo,nodeId,parameters);
		return UrlTool.setParam(baseUrl,"display",displayType);
	}
	public static String getSignatureSigned(String repId, String nodeId, long timestamp) throws GeneralSecurityException {
		String data = getSignatureContent(repId, nodeId, timestamp);
		Signing sig = new Signing();
		//take the homeRepository keys for signature
		String privateKey = ApplicationInfoList.getHomeRepository().getPrivateKey();


		if(privateKey == null){
			logger.error("no privateKey available");
			throw new GeneralSecurityException("no privateKey available");
		}
		byte[] signature = sig.sign(sig.getPemPrivateKey(privateKey, CCConstants.SECURITY_KEY_ALGORITHM), data.getBytes(StandardCharsets.UTF_8), CCConstants.SECURITY_SIGN_ALGORITHM);
		return URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(signature));

	}

	public static String getSignatureContent(String repId, String nodeId, Object timestamp) {
		return repId+nodeId+timestamp;
	}

	public static String getRenderServiceUrl(ApplicationInfo repInfo, String nodeId,String version,boolean displayMetadata, boolean backendCall) throws GeneralSecurityException{

		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
		
		String renderingProxy = (backendCall) ? homeRepo.getWebServerUrl() + "/" + homeRepo.getWebappname() +"/renderingproxy" 
											  : homeRepo.getClientBaseUrl() +"/renderingproxy";
		//renderServiceUrl = UrlTool.setParam(renderServiceUrl, "proxyRepId", ApplicationInfoList.getHomeRepository().getAppId());
		
		long timestamp = System.currentTimeMillis();

		renderingProxy = UrlTool.setParam(renderingProxy, "obj_id", nodeId);
		renderingProxy = UrlTool.setParam(renderingProxy, "rep_id",repInfo.getAppId());
		if(version!=null)
			renderingProxy = UrlTool.setParam(renderingProxy, "version",version);
		renderingProxy = UrlTool.setParam(renderingProxy, "metadata",""+displayMetadata);
		renderingProxy = UrlTool.setParam(renderingProxy, "ts",""+timestamp);
		

		renderingProxy = UrlTool.setParam(renderingProxy, "sig", getSignatureSigned(repInfo.getAppId(),nodeId,timestamp));
		
		if(repInfo.ishomeNode()){
			renderingProxy = UrlTool.setParam(renderingProxy, "app_id",repInfo.getAppId());
		}else{
			renderingProxy = UrlTool.setParam(renderingProxy, "proxyRepId",homeRepo.getAppId());
		}
		
		renderingProxy = URLTool.addOAuthAccessToken(renderingProxy);
			
		return renderingProxy;
		
	}
	
	private String getUsernameEncrypted(String username) {
		ApplicationInfo appInfoRender = ApplicationInfoList.getHomeRepository();
		String usernameEncrypted = null;
		try {
			Encryption encryptionTool = new Encryption("RSA");
			byte[] userEncryptedBytes = encryptionTool.encrypt(username.getBytes(), encryptionTool.getPemPublicKey(appInfoRender.getPublicKey()));
			usernameEncrypted = java.util.Base64.getEncoder().encodeToString(userEncryptedBytes);
			usernameEncrypted = URLEncoder.encode(usernameEncrypted, "UTF-8");
			return usernameEncrypted;
		}catch(Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	public static void buildRenderingCache(String nodeId) {
		final Context context = Context.getCurrentInstance();
		prepareExecutor.execute(()->{
			AuthenticationUtil.runAsSystem(()-> {
				try {
					Context.setInstance(context);
					// Deprecated, use the Lightbend config!
					if(!ConfigServiceFactory.getCurrentConfig().getValue("rendering.prerender",true)) {
						return null;
					}
					if(!LightbendConfigCache.getBoolean("rendering.prerender")) {
						return null;
					}
					// @TODO: May we need to build up caches just for particular file types?
					RenderingService service = RenderingServiceFactory.getLocalService();
					return service.getDetails(nodeId, null, DISPLAY_PRERENDER, null);
				} catch (Exception e) {
					logger.warn("Error building rendering cache for node " + nodeId + ": " + e.getMessage(), e);
					return e;
				} finally {
					Context.release();
				}
			});
		});
	}


}
