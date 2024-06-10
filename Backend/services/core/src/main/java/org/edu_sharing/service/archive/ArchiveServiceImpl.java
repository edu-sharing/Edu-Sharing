package org.edu_sharing.service.archive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;

import org.edu_sharing.service.archive.model.RestoreResult;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.springframework.context.ApplicationContext;

public class ArchiveServiceImpl implements ArchiveService  {
	
	
	public static final String RESTORESTATUS_FALLBACK_PARENT_NOT_EXISTS = "FALLBACK_PARENT_NOT_EXISTS";
	
	public static final String RESTORESTATUS_FALLBACK_PARENT_NO_PERMISSION = "FALLBACK_PARENT_NO_PERMISSION";
	
	public static final String RESTORESTATUS_DUPLICATENAME = "DUPLICATENAME";
	
	public static final String RESTORESTATUS_FINE = "FINE";
	
	
	
	ApplicationInfo appInfo;
	MCAlfrescoAPIClient client;
	
	SearchService searchService;
	
	Logger logger = Logger.getLogger(ArchiveServiceImpl.class);
	
	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService nodeService = serviceRegistry.getNodeService();

	private AuthorityService authorityService;
	
	public ArchiveServiceImpl() {
		try{
			this.appInfo = ApplicationInfoList.getHomeRepository();
			this.client = new MCAlfrescoAPIClient();
			this.searchService = SearchServiceFactory.getSearchService(this.appInfo.getAppId());
			this.authorityService = AuthorityServiceFactory.getAuthorityService(this.appInfo.getAppId());
			
		}catch(Throwable e){
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public void purge(List<String> archivedNodeIds) {
		for(String archivedNodeId : archivedNodeIds){
			this.client.removeNode(MCAlfrescoAPIClient.archiveStoreRef.getProtocol(), MCAlfrescoAPIClient.archiveStoreRef.getIdentifier(), archivedNodeId);
			// clear cache so that primary parent etc. gets newly resolved
			new RepositoryCache().remove(archivedNodeId);
		}
	}
	
	@Override
	public List<RestoreResult> restore(List<String> archivedNodeIds, String toFolder) {
		List<RestoreResult> result = new ArrayList<>();
		for(String archivedNodeId : archivedNodeIds){
			result.add(this.restoreNode(archivedNodeId, toFolder));
		}
		return result;
	}
	
	@Override
	public SearchResultNodeRef search(String searchWord, int from, int maxResults,SortDefinition sortDefinition) {
		if(!authorityService.isGlobalAdmin()){
			return search(searchWord,AuthenticationUtil.getFullyAuthenticatedUser(),from,maxResults,sortDefinition);
		}
		
		try{

			SearchToken searchToken = getSearchToken(searchWord,null, from, maxResults, sortDefinition);
			
			return this.searchService.search(searchToken);
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			return null;
		}
		
	}

	private SearchToken getSearchToken(String searchWord,String user, int from, int maxResults, SortDefinition sortDefinition) throws Exception {
		SearchToken searchToken = new SearchToken();
		searchToken.setFrom(from);
		searchToken.setMaxResult(maxResults);
		searchToken.setStoreName(MCAlfrescoAPIClient.archiveStoreRef.getIdentifier());
		searchToken.setStoreProtocol(MCAlfrescoAPIClient.archiveStoreRef.getProtocol());
		//searchToken.setQueryString("@cm\\:name:\""+QueryParser.escape(searchWord) + "*\""+" AND ASPECT:\"sys:archived\"");
		MetadataQueries queries = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id).getQueries(MetadataReader.QUERY_SYNTAX_LUCENE);
		Map<String, String[]> params=new HashMap<>();
		params.put(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA,new String[]{searchWord});
		if(user!=null && !user.isEmpty()) {
			params.put("user", new String[]{user});
		}
		searchToken.setMetadataQuery(queries,"archive",params);
		searchToken.setSortDefinition(sortDefinition);
		searchToken.setContentType(ContentType.FILES_AND_FOLDERS);
		return searchToken;
	}

	@Override
	public SearchResultNodeRef search(String searchWord, String user, int from, int maxResults, SortDefinition sortDefinition) {
		try{
			SearchToken searchToken = getSearchToken(searchWord,user,from,maxResults,sortDefinition);
			return this.searchService.search(searchToken);
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			return null;
		}
	}
	
	private RestoreResult restoreNode(String archivedNodeId, String destinationParentId){
		
		NodeRef archivedNodeRef = new NodeRef(MCAlfrescoAPIClient.archiveStoreRef,archivedNodeId);
		String name = client.getProperty(MCAlfrescoAPIClient.archiveStoreRef,archivedNodeId,CCConstants.CM_NAME);
		
		RestoreResult restoreResult = new RestoreResult();
		restoreResult.setArchiveNodeId(archivedNodeId);
		
		
		//try to use original
		if(destinationParentId == null || destinationParentId.trim().equals("")){
			ChildAssociationRef childRef = (ChildAssociationRef)nodeService.getProperty(archivedNodeRef, QName.createQName(CCConstants.SYS_PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC));
			if(childRef != null){
				destinationParentId = childRef.getParentRef().getId();
			}else{
				try{
					destinationParentId = new UserEnvironmentTool(null,client.getAuthenticationInfo()).getDefaultUserDataFolder();
					restoreResult.setRestoreStatus(RESTORESTATUS_FALLBACK_PARENT_NOT_EXISTS);
				}catch(Throwable e){
					logger.error(e.getMessage(), e);
				}
			}
		}
		
		if(!client.exists(destinationParentId)){
			try{
				destinationParentId = new UserEnvironmentTool(null,client.getAuthenticationInfo()).getDefaultUserDataFolder();
				restoreResult.setRestoreStatus(RESTORESTATUS_FALLBACK_PARENT_NOT_EXISTS);
			}catch(Throwable e){
				logger.error(e.getMessage(), e);
			}
		}
		
		if(!client.hasPermissions(destinationParentId, new String[]{CCConstants.PERMISSION_ADD_CHILDREN})){
			try{
				destinationParentId = new UserEnvironmentTool(null,client.getAuthenticationInfo()).getDefaultUserDataFolder();
				restoreResult.setRestoreStatus(RESTORESTATUS_FALLBACK_PARENT_NO_PERMISSION);
			}catch(Throwable e){
				logger.error(e.getMessage(), e);
			}
		}
		
		if(destinationParentId == null){
			throw new RuntimeException("can not restore, no target available");
		}
		
		
		
		try{
			String newName = new DuplicateFinder().getUniqueValue(destinationParentId, CCConstants.CM_NAME, name);
			if(!newName.equals(name)){
				name = newName;
				nodeService.setProperty(archivedNodeRef, QName.createQName(CCConstants.CM_NAME), name);
				QName type = nodeService.getType(archivedNodeRef);
		
				if(QName.createQName(CCConstants.CCM_TYPE_IO).equals(type)){
					nodeService.setProperty(archivedNodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE), name);
				}
				restoreResult.setRestoreStatus(RESTORESTATUS_DUPLICATENAME);
			}
			
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
		}
		
		String assocName = QName.createValidLocalName(name);
		assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;
		
		
		
		NodeRef restoredNode = nodeService.restoreNode(archivedNodeRef, 
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, destinationParentId),
				QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS), 
				QName.createQName(assocName));
		// clear cache so that primary parent etc. gets newly resolved
		new RepositoryCache().remove(restoredNode.getId());
		
		restoreResult.setNodeId(restoredNode.getId());
		restoreResult.setParent(destinationParentId);
		restoreResult.setPath(client.getPath(restoredNode.getId()));
		if(restoreResult.getRestoreStatus() == null){
			restoreResult.setRestoreStatus(RESTORESTATUS_FINE);
		}
		restoreResult.setName(name);

		new RepositoryCache().remove(restoredNode.getId());
		
		return restoreResult;
	}

}
