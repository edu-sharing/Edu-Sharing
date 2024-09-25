package org.edu_sharing.service.rendering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataTemplateRenderer;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.rendering.RenderingErrorServlet;
import org.edu_sharing.repository.server.rendering.RenderingException;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpException;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.PersonDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeUrls;
import org.edu_sharing.restservices.shared.SearchResult;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.connector.ConnectorServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.annotation.NodeManipulation;
import org.edu_sharing.service.nodeservice.annotation.NodeOriginal;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SortDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RenderingServiceImpl implements RenderingService{


	private final NodeService nodeService;
	private final PermissionService permissionService;
	private final GuestService guestService;
	ApplicationInfo appInfo;
	
	Map<String,String> authInfo;
	

	AuthenticationTool authTool;
	
	Logger logger = Logger.getLogger(RenderingServiceImpl.class);

	@Override
	public void setAppId(String appId) {

		try{
			this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			this.authTool = RepoFactory.getAuthenticationToolInstance(appId);

			//fix for running in runas user mode
			if((AuthenticationUtil.isRunAsUserTheSystemUser()
					|| "admin".equals(AuthenticationUtil.getRunAsUser()))
					|| Context.getCurrentInstance().getCurrentInstance() == null
					|| guestService.isGuestUser(AuthenticationUtil.getFullyAuthenticatedUser())) {
				logger.debug("starting in runas user mode");
				this.authInfo = new HashMap<>();
				this.authInfo.put(CCConstants.AUTH_USERNAME, AuthenticationUtil.getRunAsUser());
			}else {
				this.authInfo = this.authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
			}


		}catch(Throwable e){
			throw new RuntimeException(e);
		}
	}

	public RenderingVersionInfo getVersion() throws GeneralSecurityException {
		String url = new RenderingTool().getRenderServiceUrl(ApplicationInfoList.getHomeRepository(), null);
		url = url.replace("index.php", "version.php");
		return new Gson().fromJson(new HttpQueryTool().query(url), RenderingVersionInfo.class);
	}

	@Override
	public RenderingDetails getDetails(String nodeId,String nodeVersion,String displayMode,Map<String,String> parameters) throws InsufficientPermissionException, Exception{

		if(!this.permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.PERMISSION_READ)){
			throw new InsufficientPermissionException("no read permission");
		}
		String renderingServiceUrl = "";
		try {
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(this.appInfo.getAppId());
			// switch to home repo if the defined app is "local" (secondary home repo)
			if(appInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)){
				appInfo = ApplicationInfoList.getHomeRepository();
			}
			renderingServiceUrl = new RenderingTool().getRenderServiceUrl(appInfo,nodeId,parameters,displayMode);
			// base url for dynamic context routing of domains
			renderingServiceUrl = UrlTool.setParam(renderingServiceUrl, "baseUrl",URLEncoder.encode(URLHelper.getBaseUrl(true)));
			logger.debug(renderingServiceUrl);
			RenderingServiceOptions options = new RenderingServiceOptions();
			options.displayMode = displayMode;
			options.parameters = parameters;
			RenderingServiceData data = getData(appInfo, nodeId, nodeVersion, AuthenticationUtil.getFullyAuthenticatedUser(), options);
            return new RenderingDetails(getDetails(renderingServiceUrl, data), data);
		}catch(Throwable t) {
			logger.warn(t.getMessage(),t);
			RenderingException exception = new RenderingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage(), RenderingException.I18N.unknown, t);
			RenderingDetails details = new RenderingDetails(RenderingErrorServlet.errorToHTML(null, exception), null);
			details.setException(exception);
			return details;
			/*
			String repository=VersionService.getVersionNoException(VersionService.Type.REPOSITORY);
			String rs=VersionService.getVersionNoException(VersionService.Type.RENDERSERVICE);
			String info="Repository version "+repository+", Renderservice version "+rs;
			logger.info("called url:" + renderingServiceUrl);
			if(repository.equals(rs)) {
				logger.info(info);
				throw t;
			}
			else {
				info+=" do not match";
				logger.warn(info);
				throw t;
			}
			*/
		}

	}

	@Override
	public String getDetails(String renderingServiceUrl, RenderingServiceData data) throws JsonProcessingException, UnsupportedEncodingException {
		RequestBuilder post = RequestBuilder.post(renderingServiceUrl);
		if (Context.getCurrentInstance() != null) {
			Context.getCurrentInstance().getB3().addToRequest(post);
		}
		/*
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(data);
		*/
		String json=new GsonBuilder().serializeNulls().create().toJson(data);
		/*
		Encryption encryption=new Encryption("RSA");
		try {
			json=new String(encryption.encrypt(json, encryption.getPemPublicKey(ApplicationInfoList.getRenderService().getPublicKey())));
		} catch (Exception e) {
			logger.warn(e);
			return "";
		}
		*/

		post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		try {
			return new HttpQueryTool().query(post);
		}catch(HttpException e){
			return RenderingErrorServlet.errorToHTML(null,new RenderingException(e));
		}
	}
	@Override
	public RenderingServiceData getData(ApplicationInfo appInfo, String nodeId, String nodeVersion, String user, RenderingServiceOptions options) throws Throwable {
		long time=System.currentTimeMillis();
		NodeService nodeService = NodeServiceFactory.getNodeService(appInfo.getAppId());
		RenderingServiceData data=new RenderingServiceData();
		if(appInfo.ishomeNode()) {
			data.setEditors(getAvailableEditors(nodeId, nodeVersion, user));
		}
		RepositoryDao repoDao = RepositoryDao.getRepository(this.appInfo.getAppId());
		NodeDao nodeDao = NodeDao.getNodeWithVersion(repoDao, nodeId, nodeVersion);

		// child object: inherit all props from parent
		nodeDao.setNativeProperties(nodeDao.getInheritedPropertiesFromParent());

		Node node = nodeDao.asNode();

		if(appInfo.ishomeNode()) {
			if (nodeService.hasAspect(StoreRef.PROTOCOL_WORKSPACE,
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
					nodeId,
					CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
				String original = nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
						nodeId, CCConstants.CCM_PROP_IO_ORIGINAL);
				AuthenticationUtil.runAsSystem(() -> {
					try {
						NodeDao originalNodeDao = NodeDao.getNode(repoDao, original);
						node.setContent(originalNodeDao.asNode().getContent());
					} catch (DAOException e) {
						logger.error(e.getMessage());
					}
					return null;
				});
			}
		}
		ApplicationInfo remoteApp=ApplicationInfoList.getRepositoryInfoById(nodeDao.getRepositoryDao().getId());
		// remove any javascript (important for title)
		node.setProperties(new HashMap<>(new MetadataTemplateRenderer(
				MetadataHelper.getMetadataset(
					remoteApp,node.getMetadataset()==null ? CCConstants.metadatasetdefault_id : node.getMetadataset()),
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),
				user,
				nodeDao.getNativeType(),
				nodeDao.getAspectsNative(),
				nodeDao.getNativeProperties())
			.getProcessedProperties()));
		data.setNode(node);
		if(CCConstants.CCM_TYPE_SAVED_SEARCH.equals(nodeService.getType(nodeId))){
			SearchResult<Node> search = nodeDao.runSavedSearch(0,
					//options.savedSearch.getMaxItems(),
                    Integer.parseInt(options.parameters.get("maxItems")),
					SearchService.ContentType.FILES,
					new SortDefinition(Collections.singletonList(options.parameters.get("sortBy")),
							Collections.singletonList(Boolean.valueOf(options.parameters.get("sortAscending")))),
					null);
			data.setChildren(search.getNodes());
		}else{
			data.setChildren(
					NodeDao.convertToRest(repoDao, Filter.createShowAllFilter(), nodeDao.getChildrenSubobjects(), 0, Integer.MAX_VALUE).getNodes()
			);
		}
		// template
		// switch to the remote appInfo (for shadow objects) so the mds is the right one
		data.setMetadataHTML(new MetadataTemplateRenderer(
				MetadataHelper.getMetadataset(
						remoteApp,node.getMetadataset()==null ? CCConstants.metadatasetdefault_id : node.getMetadataset()),
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),
				user,
				nodeDao.getNativeType(),
				nodeDao.getAspectsNative(),
				nodeDao.getNativeProperties()).render(
						options.parameters != null && options.parameters.containsKey("metadataGroup") ?
								options.parameters.get("metadataGroup") :
						RenderingTool.DISPLAY_INLINE.equals(options.displayMode) ? "io_render_inline" : "io_render"
		));

		// user
		if(!AuthenticationUtil.isRunAsUserTheSystemUser()) {
			data.setUser(PersonDao.getPerson(RepositoryDao.getHomeRepository(), user).asPersonRender());
		}

		// context/config
		data.setConfigValues(ConfigServiceFactory.getCurrentConfig().values);
		data.setNodeUrls(new NodeUrls(node, nodeVersion));

		logger.info("Preparing rendering data took "+(System.currentTimeMillis()-time)+" ms");
		return data;
	}

	@NodeManipulation
	List<RenderingServiceData.Editor> getAvailableEditors(@NodeOriginal String nodeId, String nodeVersion, String user) {
		if(nodeVersion != null && !nodeVersion.equals("-1")) {
			return Collections.emptyList();
		}
		return ConnectorServiceFactory.getConnectorList().getConnectors().stream().filter(
				connector -> {
					if (!connector.isHasViewMode() && !permissionService.hasPermission(
							StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
							nodeId, user, CCConstants.PERMISSION_WRITE
					)
					) {
						return false;
					}
					String mimetype = nodeService.getContentMimetype(
							StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId
					);
					return connector.getFiletypes().stream().anyMatch(type -> {
						if(!type.isEditable()) {
							return false;
						}
						if(type.getMimetype().equals("application/zip") && Objects.equals(mimetype, type.getMimetype())) {
							String ccRessourcetype = nodeService.getProperty(
									StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.CCM_PROP_CCRESSOURCETYPE
							);
							String ccRessourceVersion = nodeService.getProperty(
									StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.CCM_PROP_CCRESSOURCEVERSION
							);
							String ccresourcesubtype = nodeService.getProperty(
									StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.CCM_PROP_CCRESSOURCESUBTYPE
							);
							return
									(type.getCcressourcetype() == null || type.getCcressourcetype().equals(ccRessourcetype)) &&
									(type.getCcressourceversion() == null || type.getCcressourceversion().equals(ccRessourceVersion)) &&
									(type.getCcresourcesubtype() == null || type.getCcresourcesubtype().equals(ccresourcesubtype));
						}
						return Objects.equals(mimetype, type.getMimetype());
					});
				}
		).map(connector -> {
			RenderingServiceData.Editor editor = new RenderingServiceData.Editor();
			editor.setId(connector.getId());
			editor.setOnlyDesktop(connector.isOnlyDesktop());
			return editor;
		}).collect(Collectors.toList());
	}

	@Override
	public boolean renderingSupported() {
		return true;
	}
}
