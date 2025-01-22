package org.edu_sharing.repository.server.authentication;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.QueryUtils;
import org.edu_sharing.repository.TrackingApplicationInfo;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.service.authentication.ScopeAuthenticationServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.alfresco.service.config.model.AvailableMds;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.edu_sharing.service.usage.Usage2Exception;
import org.edu_sharing.webservices.util.AuthenticationUtils;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.springframework.context.ApplicationContext;


public class ContextManagementFilter implements jakarta.servlet.Filter {
	// stores the currently accessing tool type, e.g. CONNECTOR
	public static ThreadLocal<TrackingApplicationInfo> accessTool = new ThreadLocal<>();

	Logger logger = Logger.getLogger(ContextManagementFilter.class);

	ServletContext context;

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	AuthenticationService authservice = serviceRegistry.getAuthenticationService();
	AuthenticationComponent authenticationComponent = (AuthenticationComponent)applicationContext.getBean("authenticationComponent");

	@Override
	public void destroy() {
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		this.context=config.getServletContext();
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		logger.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+" starting");

		try {
			final HttpServletRequest http = (HttpServletRequest) req;

			Context.newInstance(http , (HttpServletResponse)res, context);

			ScopeAuthenticationServiceFactory.getScopeAuthenticationService().setScopeForCurrentThread();

			try{
				// Run as System because there is yet no session opened
				Map<String, Serializable> info = AuthenticationUtil.runAsSystem(() ->
						AuthorityServiceFactory.getLocalService().getUserInfo(
								(String) ((HttpServletRequest) req).getSession().getAttribute(CCConstants.AUTH_USERNAME))
				);
				QueryUtils.setUserInfo(info);
			}catch(Exception e){
				logger.info("Could not set user info: "+e.getMessage());
			}

			((HttpServletResponse)res).setHeader("Access-Control-Expose-Headers","X-Edu-Scope");
			((HttpServletResponse)res).setHeader("X-Edu-Scope", NodeServiceInterceptor.getEduSharingScope());
			SubsystemChainingAuthenticationService.setCurrentPath(((HttpServletRequest) req).getContextPath() + ((HttpServletRequest) req).getServletPath());

			try {
				NodeCustomizationPolicies.setEduSharingContext(ConfigServiceFactory.getCurrentContextId((HttpServletRequest) req));
				AvailableMds[] availableMdss = ConfigServiceFactory.getCurrentConfig().values.availableMds;
				if(availableMdss != null) {
					for(AvailableMds availableMds : availableMdss) {
						if(RepositoryDao.HOME.equals(availableMds.repository)
								|| ApplicationInfoList.getHomeRepository().getAppId().equals(availableMds.repository)){
							HttpContext.setCurrentMetadataSet(availableMds.mds[0]);
						}
					}
				}

			}catch(Throwable e) {
				logger.debug(e.getMessage());
			}

			handleAppSignature((HttpServletRequest)req, (HttpServletResponse)res);

			chain.doFilter(req,res);

		} finally {

			logger.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+" cleaning up");

			NodeServiceInterceptor.setEduSharingScope((String)null);
			NodeCustomizationPolicies.setEduSharingContext(null);
			QueryUtils.setUserInfo(null);
			NodeDao.setIsGlobalAdmin(null);
			SubsystemChainingAuthenticationService.setSuccessFullAuthenticationMethod((String)null);
			SubsystemChainingAuthenticationService.setCurrentPath((String)null);

			HttpContext.setCurrentMetadataSet(null);

			/**
			 * OAuth kill Session
			 */
			HttpServletRequest request = (HttpServletRequest)req;


			Context.getCurrentInstance().release();

			//clean up alfresco security context

			//for native API

			try{
				//its not really necessary cause AuthenticationFilter -> AuthenticationTool calls alfresco authenticationservice.validate which
				//also calls clearCurrentSecurityContext()
				authservice.clearCurrentSecurityContext();
			}catch(AuthenticationCredentialsNotFoundException e){
				logger.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+ " there was nothing to clean up in native api");
			}

			//for soap api
			AuthenticationUtils.setAuthenticationDetails(null);

		}

	}

	/**
	 * Checks if app headers and signature are present and sets the header accordingly
	 */
	private void handleAppSignature(HttpServletRequest httpReq, HttpServletResponse httpRes) throws IOException {
		accessTool.set(null);

		String appId = SignatureVerifier.getHeaderOrParam("X-Edu-App-Id",httpReq);
		if(appId != null) {
			SignatureVerifier.Result result = new SignatureVerifier().verifyAppSignature(httpReq);
			if (result.getStatuscode() != 200) {
				String msg = "application request could not be verified:" + appId + " " + result.getMessage();
				logger.warn(msg);
				httpRes.sendError(result.getStatuscode(), result.getMessage());
			} else {
				String userId = SignatureVerifier.getHeaderOrParam("X-Edu-User-Id",httpReq);
				if (userId != null) {
					userId = SSOAuthorityMapper.mapAdminAuthority(userId, appId);
					accessTool.set(new TrackingApplicationInfo(result.getAppInfo(), userId));
				} else {
					accessTool.set(new TrackingApplicationInfo(result.getAppInfo(), null));
				}
				String courseId = SignatureVerifier.getHeaderOrParam("X-Edu-Usage-Course-Id",httpReq);
				String nodeId = SignatureVerifier.getHeaderOrParam("X-Edu-Usage-Node-Id",httpReq);
				String resourceId = SignatureVerifier.getHeaderOrParam("X-Edu-Usage-Resource-Id",httpReq);
				if (courseId != null && nodeId != null && resourceId != null) {
					Usage2Service u2 = new Usage2Service();
					try {
						Usage usage = u2.getUsage(appId, courseId, nodeId, resourceId);
						if (usage != null) {
							Context.getCurrentInstance().addSingleUseNode(nodeId);
							authenticationComponent.setCurrentUser(CCConstants.PROXY_USER);
							AuthenticationToolAPI authTool = new AuthenticationToolAPI();
							authTool.storeAuthInfoInSession(CCConstants.PROXY_USER, authservice.getCurrentTicket(), CCConstants.AUTH_TYPE_SIGNATURE, httpReq.getSession());
						}

					} catch (Usage2Exception e) {
					}
				}
			}
		}
	}

}
