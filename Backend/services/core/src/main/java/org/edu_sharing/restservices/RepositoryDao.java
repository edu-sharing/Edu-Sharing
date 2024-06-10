package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.restservices.shared.Repo;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.mediacenter.MediacenterService;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

public class RepositoryDao {

	public static final String HOME = "-home-";

	static Logger logger = Logger.getLogger(RepositoryDao.class);
	
	public static RepositoryDao getRepository(String repId) throws DAOException {

		try {
			ApplicationInfo appInfo = 
					HOME.equals(repId) 
				  ? ApplicationInfoList.getHomeRepositoryObeyConfig(ConfigServiceFactory.getCurrentConfig().getValue("availableRepositories", (String[]) null))
				  : ApplicationInfoList.getRepositoryInfoById(repId); 
			
			if (appInfo == null) {
				
				throw new DAOMissingException(
						new IllegalArgumentException(repId));
			}
				  
			if (appInfo == null || !(ApplicationInfo.TYPE_REPOSITORY.equals(appInfo.getType()))) {
				
				throw new DAOValidationException(
						new IllegalArgumentException("unsupported repository type."));
			}

			
			//WSClient is deprecated always use ApiClient, WS Impl of Service Tier is used
			MCAlfrescoBaseClient baseClient = null;

			/*
			//prevent when there is a runas user that authenticationService.validate (AuthenticationUtil) overwrites the runas user
			if((AuthenticationUtil.isRunAsUserTheSystemUser() || "admin".equals(AuthenticationUtil.getRunAsUser())) 
					&& ApplicationInfoList.getHomeRepository().getAppId().equals(appInfo.getAppId())){
				baseClient = new MCAlfrescoAPIClient();
			}else {
				baseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(
						appInfo.getAppId(), 
						//ApplicationInfoList.getHomeRepository().getAppId(),
						Context.getCurrentInstance().getRequest().getSession());
			}
			*/
			// 5.1: there is no other client anymore
            baseClient = new MCAlfrescoAPIClient();

            CollectionService collectionClient = CollectionServiceFactory.getCollectionService(appInfo.getAppId());
			
			RenderingService renderingClient = RenderingServiceFactory.getRenderingService(appInfo.getAppId());
			
			return new RepositoryDao(appInfo, baseClient, collectionClient, renderingClient);
			
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public static List<RepositoryDao> getRepositories() throws DAOException {

		try {
			
			List<RepositoryDao> result = new ArrayList<>();
			for (ApplicationInfo appInfo : ApplicationInfoList.getRepositoryInfosOrdered()) {
				ToolPermissionService tp = ToolPermissionServiceFactory.getInstance();
				if (ApplicationInfo.TYPE_REPOSITORY.equals(appInfo.getType()) && appInfo.getSearchable()) {
					if(tp.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX+appInfo.getAppId())) {
						result.add(getRepository(appInfo.getAppId()));
					}
				}
			}
			
			return result;
			
		} catch (Throwable t) {
	
			throw DAOException.mapping(t);
		}
	}
	
	private final ApplicationInfo appInfo;
	private final MCAlfrescoBaseClient baseClient;
	private final CollectionService collectionClient;
	
	private final RenderingService renderingClient;
	private NodeService nodeService;
	
	private RepositoryDao(ApplicationInfo appInfo, MCAlfrescoBaseClient baseClient, CollectionService collectionClient, RenderingService renderingClient) {
		
		this.appInfo = appInfo;
		this.baseClient = baseClient;
		this.collectionClient = collectionClient;
		this.renderingClient = renderingClient;
		this.nodeService=NodeServiceFactory.getNodeService(appInfo.getAppId());
	}
	
	public String getId() {
		return this.appInfo.getAppId();
	}
	
	public String getCaption() {
		
		return this.appInfo.getAppCaption();
	}
	public String getIcon() {
		if(this.appInfo.getIcon()!=null && !this.appInfo.getIcon().isEmpty())
			return URLTool.getBaseUrl()+"/"+this.appInfo.getIcon();
		return null;
	}
	public String getLogo() {
		if(this.appInfo.getLogo()!=null && !this.appInfo.getLogo().isEmpty())
			return URLTool.getBaseUrl()+"/"+this.appInfo.getLogo();
		return null;
	}
	public boolean isHomeRepo() {
		return this.appInfo.ishomeNode() || ApplicationInfo.REPOSITORY_TYPE_LOCAL.equals(getRepositoryType());
	}
	
	ApplicationInfo getApplicationInfo() {
		
		return appInfo;
	}
	
	public MCAlfrescoBaseClient getBaseClient() {
		
		return baseClient;
	}
	
	CollectionService getCollectionClient() {
		
		return collectionClient;
	}
	
	RenderingService getRenderingServiceClient(){
		return renderingClient;
	}
	
	public String getUserName(){
		return new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession()).get(CCConstants.AUTH_USERNAME);
	}
	
	public String getUserHome() throws Exception{
	
		return baseClient.getHomeFolderID(getUserName());
		
	}
	public String getUserInbox(boolean createIfNotExists) {
		return nodeService.getUserInbox(createIfNotExists);
	}

	public String getRepositoryType() {
		return appInfo.getRepositoryType();
	}

	public String getUserSavedSearch(boolean createIfNotExists) {
		return nodeService.getUserSavedSearch(createIfNotExists);

	}

	public static RepositoryDao getHomeRepository() throws DAOException {
		return RepositoryDao.getRepository(ApplicationInfoList.getHomeRepository().getAppId());
	}

	public Repo asRepo(){
		Repo repo = new Repo();

		repo.setId(getId());
		//if(repository.isHomeRepo())
		//	repo.setId(RepositoryDao.HOME);
		repo.setTitle(getCaption());
		repo.setIcon(getIcon());
		repo.setLogo(getLogo());
		repo.setHomeRepo(isHomeRepo());
		repo.setRepositoryType(getRepositoryType());
		repo.setRenderingSupported(getRenderingSupported());

		return repo;
	}

	private boolean getRenderingSupported() {
		return RenderingServiceFactory.getRenderingService(getId()).renderingSupported();
	}

	AuthorityService getAuthorityService(){
		return AuthorityServiceFactory.getAuthorityService(getId());
	}
	NodeService getNodeService(){
		return NodeServiceFactory.getNodeService(getId());
	}
	SearchService getSearchService() {
		return SearchServiceFactory.getSearchService(getId());
	}
	MediacenterService getMediacenterService(){
		return MediacenterServiceFactory.getMediacenterService(getId());
	}

}
