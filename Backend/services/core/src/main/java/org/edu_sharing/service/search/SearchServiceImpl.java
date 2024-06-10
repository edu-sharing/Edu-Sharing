package org.edu_sharing.service.search;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.solr.ESSearchParameters;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.*;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.alfresco.util.Pair;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.edu_sharing.alfresco.policy.Helper;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.rpc.Authority;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LogTime;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.model.*;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.service.util.AlfrescoDaoHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.util.URLEncoder;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class SearchServiceImpl implements SearchService {

	MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();

	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	Logger logger = Logger.getLogger(SearchServiceImpl.class);

	private String applicationId;

	org.alfresco.service.cmr.search.SearchService searchService = (org.alfresco.service.cmr.search.SearchService) alfApplicationContext
			.getBean("scopedSearchService");

	private MCAlfrescoAPIClient baseClient;

	private ToolPermissionService toolPermissionService;

	public SearchServiceImpl(String applicationId) {
		this.applicationId = applicationId;
		this.baseClient = new MCAlfrescoAPIClient();
		this.toolPermissionService = ToolPermissionServiceFactory.getInstance();
	}

	@Override
	public SearchResultNodeRef getFilesSharedByMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		throw new NotImplementedException("Method not supported for solr");
	}


	@Override
	public SearchResultNodeRef getFilesSharedToMe(SharedToMeType type, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		throw new NotImplementedException("Method not supported for solr");
	}

	public static String getFilesSharedToMeLucene(SharedToMeType type) throws Exception{
		ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
		String username = AuthenticationUtil.getFullyAuthenticatedUser();
		Set<String> memberships = new HashSet<>();
		memberships.add(username);
		memberships.addAll(serviceRegistry.getAuthorityService().getAuthorities());
		memberships.remove(CCConstants.AUTHORITY_GROUP_EVERYONE);
		String mdsQuery = MetadataSearchHelper.getLuceneString(
				"sharedToMe",
				null
		);
		StringBuilder query= new StringBuilder(mdsQuery + " AND ("
				+ "NOT (@ccm\\:ph_users:\"" + QueryParser.escape(username) + "\""
				+ 		"OR @cm\\:creator:\"" + QueryParser.escape(username) + "\")"
				+ " AND (");
		int i=0;
		if(type.equals(SharedToMeType.All)) {
			for (String m : memberships) {
				if (i++ > 0)
					query.append(" OR ");
				query.append("@ccm\\:ph_invited:\"").append(QueryParser.escape(m)).append("\"");
			}
		} else if (type.equals(SharedToMeType.Private)){
			query.append("@ccm\\:ph_invited:\"").append(QueryParser.escape(username)).append("\"");
		}
		query.append(")");
		query.append(")");
		return query.toString();
	}

	public static String getFilesSharedByMeLucene() throws Exception{
		String username = AuthenticationUtil.getFullyAuthenticatedUser();
		String mdsQuery = MetadataSearchHelper.getLuceneString(
				"sharedByMe",
				null
		);
		return mdsQuery + " AND @ccm\\:ph_users:\"" + QueryParser.escape(username) + "\"";
	}

	@Override
	public SearchResult<EduGroup> getAllOrganizations(boolean scoped) throws Exception {
		return searchOrganizations("", 0, Integer.MAX_VALUE, null,scoped,true);
	}

	/**
	 *
	 * @param membershipsOnly (only for admin/system) when true, behave like regular users and show only groups
	 *                        false will show all mz of the system
	 * @return
	 * @throws Exception
	 */
	@Override
	public List<String> getAllMediacenters(boolean membershipsOnly) throws Exception {


		Set<String> memberships = serviceRegistry.getAuthorityService().getAuthorities();
		boolean isSystemUser = AuthenticationUtil.isRunAsUserTheSystemUser();
		boolean isAdmin = ((memberships != null && memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS))
				|| "admin".equals(AuthenticationUtil.getFullAuthentication().getName())
				|| isSystemUser) ? true : false;

		if(isAdmin && !membershipsOnly) {
			SearchParameters parameters = new SearchParameters();
			parameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
			parameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_LUCENE);
			parameters.addAllAttribute(org.edu_sharing.alfresco.service.AuthorityService.MEDIA_CENTER_GROUP_TYPE);
			parameters.addSort(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME,true);
			parameters.setQuery("@ccm\\:groupType:\"" + org.edu_sharing.alfresco.service.AuthorityService.MEDIA_CENTER_GROUP_TYPE + "\"");
			return SearchServiceHelper.queryAll(parameters,0).stream().map((ref) ->
					NodeServiceFactory.getNodeService(applicationId).getProperty(ref.getStoreRef().getProtocol(), ref.getStoreRef().getIdentifier(), ref.getId(), CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME)
			).collect(Collectors.toList());
		}else {
			List<String> result = new ArrayList<>();
			for(String memberShip : memberships) {
				NodeRef nodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(memberShip);
				if(nodeRef != null && serviceRegistry.getNodeService().hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_MEDIACENTER))) {
					result.add(memberShip);
				}
			}
			return result;
		}
	}
	@Override
	public SearchResultNodeRef getRelevantNodes(int skipCount, int maxItems) throws Throwable {
		String query=SearchRelevancyTool.getLuceneQuery();
		if(query.isEmpty()){
			return new SearchResultNodeRef();
		}
		SearchToken token = new SearchToken();
		token.setLuceneString(query);
		token.setFrom(skipCount);
		token.setMaxResult(maxItems);
		return search(token);
	}

	@Override
	public SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort,boolean scoped, boolean onlyMemberShips)
			throws Exception {
		try {
			
			
			Set<String> memberships = serviceRegistry.getAuthorityService().getAuthorities();
			boolean isAdmin = ((memberships != null && memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)) 
					|| "admin".equals(AuthenticationUtil.getFullAuthentication().getName())) ? true : false;

			return AuthenticationUtil.runAsSystem(new RunAsWork<SearchResult<EduGroup>>() {

				@Override
				public SearchResult<EduGroup> doWork() throws Exception {
					try {
						List<EduGroup> result = new ArrayList<>();
						org.alfresco.service.cmr.search.SearchService searchService = serviceRegistry
								.getSearchService();
						SearchParameters parameters = new SearchParameters();
						parameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
						parameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_LUCENE);
						parameters.setSkipCount(skipCount);
						parameters.setMaxItems(maxValues);
						parameters.addAllAttribute(CCConstants.CCM_PROP_AUTHORITYCONTAINER_EDUHOMEDIR);
						if (sort != null)
							sort.applyToSearchParameters(parameters);
						String param = QueryParser.escape(pattern == null ? "" : pattern);
						
						//only search organisations the curren user is in,except: its adminuser and onlyMemberShips == true
						StringBuilder additionalQuery=null;
						if(onlyMemberShips) {
							List<String> memberShibsOrg = new ArrayList<>();
							if(memberships != null && memberships.size() > 0) {
								for(String membershib : memberships) {
									NodeRef authorityNodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(membershib);
									if(authorityNodeRef != null) {
										if(serviceRegistry.getNodeService().hasAspect(authorityNodeRef,
												QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {
											memberShibsOrg.add(membershib);
										}
									}
								}
								if(memberShibsOrg.size() > 0) {
									additionalQuery = new StringBuilder();
									additionalQuery.append(" AND (");
									int i = 0;
									for(String membershibOrg : memberShibsOrg) {
										if(i > 0) {
											additionalQuery.append(" OR ");
										}
										additionalQuery.append("@cm\\:authorityName:\"" + QueryParser.escape(membershibOrg) + "\"");
										i++;
									}
									additionalQuery.append(")");
								}else {
									return new SearchResult<EduGroup>();
								}
								
							}
						} else if(!isAdmin) {
							additionalQuery = new StringBuilder();
							// seems not necessary since we filter by user groups anyway
							// + this will also hide any groups in the user manager for org admins
							// additionalQuery.append(" AND NOT ISNULL:\"ccm:group_signup_method\"");
						}
						
						parameters
								.setQuery(
										"(@cm\\:authorityName:\"*" + param + "*\"" + 
										" OR @cm\\:authorityDisplayName:\"*" + param + "*\"" + 
										") AND @ccm\\:edu_homedir:\"workspace://*\"" + 
										((additionalQuery != null) ? " " + additionalQuery.toString() : "") );
						logger.info("query:" +parameters.getQuery());
						
						ResultSet edugroups = searchService.query(parameters);

						for (ResultSetRow row : edugroups) {
							Map<String, Object> entry = apiClient.getProperties(row.getNodeRef().getId());
							String nodeRef = (String) entry.get(CCConstants.CCM_PROP_AUTHORITYCONTAINER_EDUHOMEDIR);
							// when a group folder relation is removed the noderef can be null cause of async solr refresh
							if (nodeRef != null) {
								String nodeId = nodeRef.replace("workspace://SpacesStore/", "");
								Map<String, Object> folderProps = apiClient.getProperties(nodeId);
								EduGroup eduGroup = new EduGroup();
								eduGroup.setFolderId((String) folderProps.get(CCConstants.SYS_PROP_NODE_UID));
								eduGroup.setFolderName((String) folderProps.get(CCConstants.CM_NAME));
								eduGroup.setGroupId((String) entry.get(CCConstants.SYS_PROP_NODE_UID));
								eduGroup.setGroupname((String) entry.get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
								eduGroup.setGroupDisplayName(
										(String) entry.get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
								eduGroup.setFolderPath(
										apiClient.getPath((String) folderProps.get(CCConstants.SYS_PROP_NODE_UID)));
								eduGroup.setScope((String) folderProps.get(CCConstants.CCM_PROP_EDUSCOPE_NAME));
								boolean add = false;
								for (String group : memberships) {
									if (group.equals(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)
											|| group.equals(eduGroup.getGroupname())) {
										add = true;
										break;
									}
								}
								if(scoped) {
									String currentScope = NodeServiceInterceptor.getEduSharingScope();
									if(eduGroup.getScope()==null && currentScope!=null)
										add=false;
									if(eduGroup.getScope()!=null && !eduGroup.getScope().equals(currentScope))
										add=false;
								}
								if (add)
									result.add(eduGroup);
							}
						}
						int count = result.size();
						return new SearchResult<EduGroup>(result, skipCount, count);
					} catch (Throwable t) {
						throw new Exception(t);
					}
				}
			});
		} catch (Throwable t) {
			throw t;
		}
	}
	
	
	

	private List limitList(List list, int skipCount, int maxValues) {
		return list.subList(Math.min(skipCount, list.size()), Math.min(list.size(), skipCount + maxValues));
	}
	
	/**
	 * find all parent groups where the given authority is a member
	 */
	@Override
	public SearchResult<String> searchPersonGroups(String authorityName, String pattern, int skipCount, int maxValues, SortDefinition sort) {
		AuthorityService authorityService = serviceRegistry.getAuthorityService();

		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

				new RetryingTransactionCallback<SearchResult<String>>() {

					public SearchResult<String> execute() throws Throwable {
						String key = authorityName;
						String[] data = authorityService.getContainingAuthorities(null, key, true)
								.toArray(new String[0]);
						return filterAndSortAuthorities(data,pattern,null,skipCount,maxValues,sort);
					}

				}, true);
	}
	
	@Override
	public SearchResult<String> searchGroupMembers(String groupName, String pattern, String authorityType,
			int skipCount, int maxValues, SortDefinition sort) {
		AuthorityService authorityService = serviceRegistry.getAuthorityService();

		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

				new RetryingTransactionCallback<SearchResult<String>>() {

					public SearchResult<String> execute() throws Throwable {
						String key = groupName;
						String[] data = authorityService.getContainedAuthorities(null, key, true)
								.toArray(new String[0]);
						return filterAndSortAuthorities(data,pattern,authorityType,skipCount,maxValues,sort);
					}

				}, true);
	}
	private SearchResult<String> filterAndSortAuthorities(String[] data,String pattern,String authorityType,int skipCount, int maxValues,SortDefinition sort) throws Exception {
		List<String> list2 = new ArrayList<>();
		if (authorityType != null && !authorityType.isEmpty()) {
			for (String authority : data) {
				if (authorityType.equals("GROUP")
						&& !authority.startsWith(PermissionService.GROUP_PREFIX))
					continue;
				if (authorityType.equals("USER")
						&& authority.startsWith(PermissionService.GROUP_PREFIX))
					continue;
				list2.add(authority);
			}
		} else {
			list2 = Arrays.asList(data);
		}

		List<String> list = new ArrayList<>();
		if (pattern != null && !pattern.isEmpty()) {
			for (String authority : list2) {
				
				NodeRef authorityNodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(authority);
				
				String name = authority;

				String toCompare = "" + name;
				
				if (name.startsWith(PermissionService.GROUP_PREFIX)) {
					name = name.substring(PermissionService.GROUP_PREFIX.length());
					
					if(authorityNodeRef != null) {
						String displayName = (String)serviceRegistry.getNodeService().getProperty(authorityNodeRef, ContentModel.PROP_AUTHORITY_DISPLAY_NAME);
						if(displayName != null) {
							toCompare += displayName;
						}
					}
						
					
				}else {
					if(authorityNodeRef != null) {
						String firstName = (String)serviceRegistry.getNodeService().getProperty(authorityNodeRef, ContentModel.PROP_FIRSTNAME);
						String lastName = (String)serviceRegistry.getNodeService().getProperty(authorityNodeRef, ContentModel.PROP_LASTNAME);
						if(firstName != null) {
							toCompare+=firstName;
						}
						if(lastName != null) {
							toCompare+=lastName;
						}
					}
				}
				
				
				if (toCompare.toLowerCase().contains(pattern.toLowerCase()))
					list.add(authority);
			}
		} else {
			list = list2;
		}

		if (sort.hasContent()) {
			if (!sort.getFirstSortBy().equals("authorityName")) {
				throw new Exception("Group Members can only be sorted by authorityName, requested: "
						+ sort.getFirstSortBy());
			}
			Collections.sort(list, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					if (o1.startsWith(PermissionService.GROUP_PREFIX))
						o1 = o1.substring(PermissionService.GROUP_PREFIX.length());
					if (o2.startsWith(PermissionService.GROUP_PREFIX))
						o2 = o2.substring(PermissionService.GROUP_PREFIX.length());
					return o1.compareToIgnoreCase(o2) * (sort.getFirstSortAscending() ? 1 : -1);
				}
			});
		}
		int count = list.size();
		list = limitList(list, skipCount, maxValues);
		return new SearchResult<String>(list, skipCount, count);
	}
	@Override
	public SearchResult<String> searchUsers(String _pattern, boolean globalSearch, int _skipCount, int _maxValues,
			SortDefinition sort,Map<String,String> customProperties) throws Exception {
			return findAuthorities(AuthorityType.USER,_pattern, globalSearch, _skipCount, _maxValues, sort, customProperties);
		/*
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

				new RetryingTransactionCallback<SearchResult<String>>() {
					public SearchResult<String> execute() throws Throwable {
						
						String pattern = _pattern;
						int skipCount = _skipCount;
						int maxValues = _maxValues;
						
						if(restrictAuthoritySearch()) {
							if(pattern != null 
									&& pattern.contains("*") 
									&& pattern.trim().replaceAll(" ", "").replaceAll("\\*", "").length() == 0 ) {
								pattern = pattern.replaceAll("\\*", "");
							}
							skipCount = 0;
							maxValues = 10;
						}
						
						List<String> result = new ArrayList<>();
						if (globalSearch) {
							checkGlobalSearchPermission();
							PersonService personService = serviceRegistry.getPersonService();
							List<QName> filters = new ArrayList<>();
							filters.add(ContentModel.PROP_FIRSTNAME);
							filters.add(ContentModel.PROP_LASTNAME);
							filters.add(ContentModel.PROP_EMAIL);
							PagingRequest paging = new PagingRequest(skipCount, maxValues);
							paging.setRequestTotalCountMax(Integer.MAX_VALUE);
							PagingResults<PersonInfo> peopleReq = personService.getPeople(pattern, filters,
									sort.asSortProperties(), paging);

							try {
								for (PersonInfo personInfo : peopleReq.getPage()) {
									result.add(personInfo.getUserName());
								}
							} catch (IllegalStateException e) {
								// No results found
							}
							return new SearchResult<String>(result, skipCount, peopleReq.getTotalResultCount());

						} else {
							return searchAuthoritiesSolr(pattern, skipCount, maxValues, sort, AuthorityType.USER,false,customProperties);
						}

					}

				}, true);
				*/
	}

	boolean restrictAuthoritySearch() {
		
		if(AuthenticationUtil.isRunAsUserTheSystemUser() 
				|| ApplicationInfoList.getHomeRepository().getUsername().equals(AuthenticationUtil.getRunAsUser())) {
			return false;
		}
		
		/**
		 * find if user is in one ORG_ADMINISTRATORS Group
		 */
		Set<String> authoritiesCurrentUser = serviceRegistry.getAuthorityService().getAuthorities();
		
		try {
			SearchResult<EduGroup> sr = getAllOrganizations(true);
			for(EduGroup eg : sr.getData()) {
				
				if(!authoritiesCurrentUser.contains(eg.getGroupname())) {
					continue;
				}
				
				Set<AccessPermission> aps = serviceRegistry.getPermissionService().getPermissions(new NodeRef(MCAlfrescoAPIClient.storeRef,eg.getFolderId()));
				for(AccessPermission ap : aps) {
					NodeRef authorityNodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(ap.getAuthority());
					String groupType = (String)serviceRegistry.getNodeService().getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
					if(groupType != null
							&& groupType.equals(CCConstants.ADMINISTRATORS_GROUP_TYPE)) {
						return false;
					}
				}
			}
			
			return true;
		}catch(Exception e) {
			return true;
		}
		
	}

	protected void checkGlobalSearchPermission() throws InsufficientPermissionException {
		AuthenticationToolAPI authTool = new AuthenticationToolAPI();

		if (authTool.getScope() == null) {
			if ((toolPermissionService.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH)
					|| toolPermissionService
							.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE)))
				return;
			throw new InsufficientPermissionException(
					"Toolpermission " + CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH + " or "
							+ CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE + " are missing");
		} else {
			if ((toolPermissionService
					.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE)
					|| toolPermissionService.hasToolPermission(
							CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE)))
				return;
			throw new InsufficientPermissionException(
					"Toolpermission " + CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE + " or "
							+ CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE + " are missing");
		}
	}

	private SearchResult<String> searchAuthoritiesSolr(String pattern, int skipCount, int maxValues,
			SortDefinition sort, AuthorityType authorityType,boolean globalContext,Map<String,String> customProperties) throws Throwable {
		List<String> result = new ArrayList<>();
		NodeService nodeService = serviceRegistry.getNodeService();
		SearchToken token = new SearchToken();
		String query = "TYPE:cm\\:";
		if (authorityType.equals(AuthorityType.USER))
			query += "person";
		else
			query += "authorityContainer";
		if(customProperties!=null){
			for(Entry<String, String> entry : customProperties.entrySet()){
				query+=" AND @"+entry.getKey().replace(":", "\\:")+":\""+QueryParser.escape(entry.getValue())+"\"";
			}
		}
		query += " AND (@cm\\:authorityName:\"*" + QueryParser.escape(pattern) + "*\" "+
				 "OR @cm\\:userName:\"*" + QueryParser.escape(pattern) + "*\" "+
				 "OR @cm\\:firstName:\"*" + QueryParser.escape(pattern) + "*\" "+
				 "OR @cm\\:lastName:\"*" + QueryParser.escape(pattern) + "*\" "+
				 "OR @cm\\:email:\"*" + QueryParser.escape(pattern) + "*\")";

		if(globalContext){
			checkGlobalSearchPermission();
		}
		else{
			List<EduGroup> organisations = getAllOrganizations(true).getData();
			if (organisations != null && organisations.size() > 0) {
				query += " AND (";
	
				int i = 0;
				for (EduGroup entry : organisations) {
					if (i > 0)
						query += " OR ";
					String ref = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE + "/" + entry.getGroupId();
					// query+="PARENT:"+QueryParser.escape(ref);
					query += "PATH:\""
							+ QueryParser.escape("sys:system/sys:authorities/cm:" + ISO9075.encode(entry.getGroupname()))
							+ "//.\"";
					query += " OR ID:" + QueryParser.escape(ref);
					i++;
				}
				query += ")";
			}
		}
		token.setLuceneString(query);
		token.setFrom(skipCount);
		token.setMaxResult(maxValues);
		token.setSortDefinition(sort);
		token.setContentType(ContentType.ALL);
		token.disableSearchCriterias();
		SearchResultNodeRef data = search(token,false);
		
		for(org.edu_sharing.service.model.NodeRef enr : data.getData()){
			
			NodeRef entry = new NodeRef(new StoreRef(enr.getStoreProtocol(),enr.getStoreId()),enr.getNodeId());
			String name=(String) nodeService.getProperty(entry, ContentModel.PROP_AUTHORITY_NAME);
			if(name==null)
				name=(String)nodeService.getProperty(entry, ContentModel.PROP_USERNAME);
			result.add(name);
		}
		return new SearchResult<String>(result, skipCount, data.getNodeCount());
	}
	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String,String[]> criterias,
									  SearchToken searchToken) throws Throwable {
		MetadataQueries queries = mds.getQueries(MetadataReader.QUERY_SYNTAX_LUCENE);
		searchToken.setMetadataQuery(queries,query,criterias);
		SearchCriterias scParam = new SearchCriterias();
		scParam.setRepositoryId(mds.getRepositoryId());
		scParam.setMetadataSetId(mds.getId());
		scParam.setMetadataSetQuery(query);
		searchToken.setSearchCriterias(scParam);

		Map<ContentType, SearchToken> lastTokens = getLastSearchTokens();
		lastTokens.put(searchToken.getContentType(),searchToken);
		Context.getCurrentInstance().getRequest().getSession().setAttribute(CCConstants.SESSION_LAST_SEARCH_TOKENS,lastTokens);
		List<String> facets = searchToken.getFacets();
		SearchResultNodeRef search = search(searchToken,true);
		return search;
	}
	@Override
	public Map<ContentType,SearchToken> getLastSearchTokens() throws Throwable {
		if(Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.SESSION_LAST_SEARCH_TOKENS)!=null) {
			return (Map<ContentType, SearchToken>) Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.SESSION_LAST_SEARCH_TOKENS);
		}
		return new HashMap<>();

	}
	public SearchResultNodeRef search(SearchToken searchToken) {
		return search(searchToken, true);
	}

	@Override
	public SearchResultNodeRef search(SearchToken searchToken, boolean scoped) {
		try {
			StoreRef storeRef = new StoreRef(searchToken.getStoreProtocol(), searchToken.getStoreName());

			SearchParameters searchParameters = new ESSearchParameters();
			searchParameters.addStore(storeRef);

			searchParameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_LUCENE);

			if(searchToken.getQuery() != null && !searchToken.getQuery().getSpecialFilter().isEmpty()) {
				throw new RuntimeException("specialFilter is not supported for query syntax " + MetadataReader.QUERY_SYNTAX_LUCENE);
			}
			searchParameters.setQuery(searchToken.getLuceneString());
			logger.info("query: "+searchParameters.getQuery());
			searchParameters.setSkipCount(searchToken.getFrom());
			searchParameters.setMaxItems(searchToken.getMaxResult());
			if (searchToken.getSortDefinition() != null)
				searchToken.getSortDefinition().applyToSearchParameters(searchParameters);
			if (searchToken.getContentType().equals(ContentType.FILES)) {
				((ESSearchParameters) searchParameters).setGroupBy("text@sd___@" + CCConstants.CCM_PROP_IO_ORIGINAL);
				// group.truncate = If true, facet counts are based on the most
				// relevant document of each group matching the query. The
				// default value is false.
				// -> only 1 keyword when the file is also in a collection
				// https://cwiki.apache.org/confluence/display/solr/Result+Grouping
				String sort = URLEncoder.encodeUriComponent("datetime@sd@" + CCConstants.CM_PROP_C_CREATED) + "+asc";
				((ESSearchParameters) searchParameters).setGroupConfig(
						"&group=true&group.limit=1&group.sort=" + sort + "&group.ngroups=true&group.truncate=true");
			}
			
			if(searchToken.getAuthorityScope() != null && searchToken.getAuthorityScope().size() > 0) {
				
				if(new Helper(serviceRegistry.getAuthorityService()).isAdmin(serviceRegistry.getAuthenticationService().getCurrentUserName())) {
					((ESSearchParameters) searchParameters).setAuthorities(searchToken.getAuthorityScope().toArray(new String[searchToken.getAuthorityScope().size()]));
				}else {
					logger.error("only admins are allowed to change authority scope of search");
				}
			}

			List<String> facets = searchToken.getFacets();

			if (facets != null && facets.size() > 0) {
				for (String facetProp : facets) {
					String fieldFacetStr = facetProp;
					if(!fieldFacetStr.startsWith("@")) {
						fieldFacetStr = "@" + facetProp;
					}
					FieldFacet fieldFacet = new FieldFacet(fieldFacetStr);
					fieldFacet.setLimit(searchToken.getFacetLimit());
					fieldFacet.setMinCount(searchToken.getFacetsMinCount());
					searchParameters.addFieldFacet(fieldFacet);
				}
			}

			List<FieldHighlightParameters> fieldHighlightParameters = new ArrayList<>();
			fieldHighlightParameters.add( new FieldHighlightParameters("cm:name",255,100,false,"",""));
			fieldHighlightParameters.add( new FieldHighlightParameters("cm:title",255,100,false,"",""));
			fieldHighlightParameters.add( new FieldHighlightParameters("cm:description",255,100,false,"",""));
			fieldHighlightParameters.add( new FieldHighlightParameters("cclom:description",255,100,false,"",""));
			fieldHighlightParameters.add( new FieldHighlightParameters("content",255,100,false,"",""));
			fieldHighlightParameters.add( new FieldHighlightParameters("ia:descriptionEvent",255,100,false,"",""));
			fieldHighlightParameters.add( new FieldHighlightParameters("ia:whatEvent",255,100,false,"",""));
			fieldHighlightParameters.add( new FieldHighlightParameters("lnk:title",255,100,false,"",""));
			GeneralHighlightParameters ghp = new GeneralHighlightParameters(255,100,false,"","",null,true,fieldHighlightParameters);
			searchParameters.setHighlight(ghp);

			ResultSet resultSet;
			logger.info(searchParameters.getQuery());
			resultSet=LogTime.log("Searching Solr",()-> {
				if (scoped)
					return searchService.query(searchParameters);
				else
					return serviceRegistry.getSearchService().query(searchParameters);
			});
			SearchResultNodeRef sr = new SearchResultNodeRef();
			sr.setData(AlfrescoDaoHelper.unmarshall(resultSet.getNodeRefs(), ApplicationInfoList.getHomeRepository().getAppId()));
			sr.setStartIDX(searchToken.getFrom());
			sr.setNodeCount(searchToken.getMaxResult());
			sr.setNodeCount((int) resultSet.getNumberFound());

			// process facette
			if (facets != null && facets.size() > 0) {
				Map<String, NodeSearch.Facet> newCountPropsMap = new HashMap<>();
				for (FieldFacet facetProp : searchParameters.getFieldFacets()) {
					NodeSearch.Facet facetPair = newCountPropsMap.get(facetProp.getField());
					if (facetPair == null) {
						facetPair = new NodeSearch.Facet();
					}
					String facetField = facetProp.getField();
					if(facetField.startsWith("@")) {
						facetField = facetField.substring(1);
					}
					facetPair.setProperty(facetField);
					String fieldFacet = facetProp.getField();

					List<Pair<String, Integer>> facetPairs = resultSet.getFieldFacet(fieldFacet);

					// plain solr
					logger.info("found " + facetPairs.size() + " facet pairs for" + fieldFacet);
					for (Pair<String, Integer> pair : facetPairs) {

						// value contains language information i.e. {de}
						String first = pair.getFirst().replaceAll("\\{[a-z]*\\}", "");

						/**
						 * solr4 problem: delivers facetes that have count 0 and
						 * should not occur in the searchresult
						 *
						 * http://stackoverflow.com/questions/10069868/getting-facet-count-0-in-solr
						 * --> pair.getSecond() > 0
						 */
						if (first != null && !first.trim().equals("") && pair.getSecond() > 0) {
							NodeSearch.Facet.Value value = new NodeSearch.Facet.Value();
							value.setValue(first);
							value.setCount(pair.getSecond());
							facetPair.getValues().add(value);
						}
					}

					if (facetPair.getValues().size() > 0) {
						newCountPropsMap.put(facetProp.getField(), facetPair);
					}
					sr.setFacets(new ArrayList<>(newCountPropsMap.values()));
				}
			}
			SearchLogger.logSearch(searchToken,sr);
			return sr;
		} catch (Throwable e) {
			if(e.getCause() != null) e.getCause().printStackTrace();
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public SearchResultNodeRef searchFingerPrint(String nodeId) {

		int skipCount = 0;
		int maxItems = 100;
		StoreRef storeRef = new StoreRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier());

		SearchParameters searchParameters = new ESSearchParameters();
		searchParameters.addStore(storeRef);

		searchParameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_FTS_ALFRESCO);
		searchParameters.setQuery("FINGERPRINT:" + nodeId + "_30_90 AND NOT ID:\"workspace://SpacesStore/"+ nodeId + "\"");
		searchParameters.setSkipCount(skipCount);
		searchParameters.setMaxItems(maxItems);

		ResultSet resultSet = serviceRegistry.getSearchService().query(searchParameters);

		SearchResultNodeRef sr = new SearchResultNodeRef();
		sr.setData(AlfrescoDaoHelper.unmarshall(resultSet.getNodeRefs(), ApplicationInfoList.getHomeRepository().getAppId()));
		sr.setStartIDX(skipCount);
		sr.setNodeCount(maxItems);
		sr.setNodeCount((int) resultSet.getNumberFound());

		return sr;
	}


	@Override
	public SearchResultNodeRef getWorkflowReceive(String user, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		throw new NotImplementedException("Method not supported for solr");
	}

	@Override
	public SearchResult<String> findAuthorities(AuthorityType type,String searchWord, boolean globalContext, int from, int nrOfResults,SortDefinition sort,Map<String,String> customProperties) throws Exception {
		String signupMethod = customProperties == null ? null : customProperties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD));
		boolean searchingSignupGroups = ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_SIGNUP_GROUP) &&
										AuthorityType.GROUP.equals(type) &&
										signupMethod != null &&
										!signupMethod.isEmpty();
		if(globalContext && !searchingSignupGroups) {
			checkGlobalSearchPermission();
		}

		// fields to search in - also using username as admin (6.0 or later)

		org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(null);

		StringBuffer findUsersQuery =  permissionService.getFindUsersSearchString(searchWord,AuthorityServiceHelper.getDefaultAuthoritySearchFields(), globalContext);
		// we're skipping TP checks when the search requested signup groups -> it's possible to see them even without GLOBAL_AUTHORITY_SEARCH permissions
		StringBuffer findGroupsQuery = permissionService.getFindGroupsSearchString(searchWord, globalContext, searchingSignupGroups);
		

		if(findUsersQuery == null && findGroupsQuery == null) {
			return new SearchResult<String>(new ArrayList<>(), 0, 0);
		}
		
		/**
		 * don't find groups of scopes when no scope is provided
		 */
		if (NodeServiceInterceptor.getEduSharingScope() == null && findGroupsQuery!=null) {

			/**
			 * groups arent initialized with eduscope aspect and eduscopename
			 * null
			 */
			findGroupsQuery.append(" AND NOT @ccm\\:eduscopename:\"*\"");	
			
		}

		String finalQuery;
		if(type==null) {
			finalQuery="";
			if(findUsersQuery!=null)
				finalQuery += "("+findUsersQuery+")";
			if(findGroupsQuery!=null) {
				if(findUsersQuery != null){
					finalQuery += " OR ";
				}
				finalQuery += "(" + findGroupsQuery + ")";
			}
		}
		else if(type.equals(AuthorityType.USER)) {
			finalQuery=findUsersQuery.toString();
		}
		else if(type.equals(AuthorityType.GROUP)) {
			if(findGroupsQuery==null)
				finalQuery="";
			else
				finalQuery=findGroupsQuery.toString();
		}
		else {
			throw new IllegalArgumentException("Unsupported authority type "+type);
		}
		if(finalQuery.isEmpty())
			return new SearchResult<String>();
		if(customProperties!=null){
			for(Map.Entry<String, String> entry : customProperties.entrySet()){
				finalQuery+=(" AND @"+entry.getKey().replace(":", "\\:")+":\""+QueryParser.escape(entry.getValue())+"\"");
			}
		}

		logger.debug("finalQuery:" + finalQuery);

		List<Authority> data = new ArrayList<>();

		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		searchParameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_FTS_ALFRESCO);
		searchParameters.setQuery(finalQuery.toString());
		searchParameters.setSkipCount(from);
		searchParameters.setMaxItems(nrOfResults);
		if(sort==null || !sort.hasContent()) {
			searchParameters.addSort("score", false);
		}
		else {
			sort.applyToSearchParameters(searchParameters);
		}
		// dont use scopeed search service
		org.alfresco.service.cmr.search.SearchService searchService = serviceRegistry.getSearchService();
		ResultSet resultSet = searchService.query(searchParameters);

		List<String> result = new ArrayList<>();
		for (NodeRef nodeRef : resultSet.getNodeRefs()) {
			String authorityName = (String) serviceRegistry.getNodeService().getProperty(nodeRef,
					ContentModel.PROP_AUTHORITY_NAME);
			if (authorityName == null) {
				authorityName = (String) serviceRegistry.getNodeService().getProperty(nodeRef,
						ContentModel.PROP_USERNAME);
			}

			result.add(authorityName);
		}

		return new SearchResult<String>(result, from, (int) resultSet.getNumberFound());

	}

	private static String getLuceneSuggestionQuery(MetadataQueryParameter parameter,String value){
		//return "("+queries.getBasequery()+") AND ("+parameter.getStatement().replace("${value}","*"+QueryParser.escape(value)+"*")+")";
		return parameter.getStatement(value).replace("${value}","*"+QueryParser.escape(value)+"*");
	}
	@Override
	public List<? extends Suggestion> getSuggestions(MetadataSet mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criterias) {
			List<Suggestion> result = new ArrayList<>();
			ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
			org.alfresco.service.cmr.search.SearchService searchService = (org.alfresco.service.cmr.search.SearchService)applicationContext.getBean("scopedSearchService");

			SearchParameters searchParameters = new SearchParameters();
			searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
			searchParameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_LUCENE);

			searchParameters.setSkipCount(0);
			searchParameters.setMaxItems(1);
			MetadataQueryParameter parameter = mds.findQuery(queryId, MetadataReader.QUERY_SYNTAX_LUCENE).findParameterByName(parameterId);
			if(parameter == null){
				logger.warn("no parameter found for queryId:"+queryId+ " parameterId:" + parameterId);
				return result;
			}
			if(value == null){
				logger.warn("value is null queryId:"+queryId+ " parameterId:" + parameterId);
				return result;
			}
			String luceneQuery = "(TYPE:\"" + CCConstants.CCM_TYPE_IO + "\"" +") AND ("+getLuceneSuggestionQuery(parameter, value)+")";
			if(criterias != null && criterias.size() > 0 ) {

				Map<String,String[]> criteriasMap=new HashMap<>();
				for(MdsQueryCriteria criteria : criterias){
					criteriasMap.put(criteria.getProperty(),criteria.getValues().toArray(new String[0]));
				}
				MetadataQueries queries = mds.getQueries(MetadataReader.QUERY_SYNTAX_LUCENE);
				MetadataQuery queryObj = queries.findQuery(queryId).copy();
				queryObj.setApplyBasequery(false);
				queryObj.setBasequery(null);

				SearchCriterias scParam = new SearchCriterias();
				scParam.setRepositoryId(mds.getRepositoryId());
				scParam.setMetadataSetId(mds.getId());
				scParam.setMetadataSetQuery(queryId);
				try {
					luceneQuery = "(" + luceneQuery + ") AND " +  MetadataSearchHelper.getLuceneString(queries,queryObj,scParam, criteriasMap);
					//System.out.println("MetadataSearchHelper lucenequery suggest:" +luceneQuery);
				} catch (IllegalArgumentException e) {
					logger.error(e.getMessage(), e);
				}

			}
			searchParameters.setQuery(luceneQuery);

			String facetName = "@" + parameter.getName();
			List<String> facets = parameter.getFacets() == null ? Arrays.asList(new String[]{facetName}) : parameter.getFacets();
			for(String facet : facets){
				FieldFacet fieldFacet = new FieldFacet(facet);
				fieldFacet.setLimit(100);
				fieldFacet.setMinCount(1);
				searchParameters.addFieldFacet(fieldFacet);
			}

			ResultSet rs = searchService.query(searchParameters);
			Map<String, MetadataKey> captions = mds.findWidget(parameterId).getValuesAsMap();

			for(String facet : facets) {
				List<Pair<String, Integer>> facettPairs = rs.getFieldFacet(facet);

				for (Pair<String, Integer> pair : facettPairs) {

					//solr 4 bug: leave out zero values
					if (pair.getSecond() == 0) {
						continue;
					}

					String hit = pair.getFirst(); // new String(pair.getFirst().getBytes(), "UTF-8");

					if (hit.toLowerCase().contains(value.toLowerCase())) {

						Suggestion dto = new Suggestion();
						dto.setKey(hit);
						dto.setDisplayString(captions.containsKey(hit) ? captions.get(hit).getCaption() : null);

						result.add(dto);
					}
				}
			}
			return result;


	}

	@Override
	public Set<SearchVCard> searchContributors(String suggest, List<String> fields, List<String> contributorProperties, ContributorKind contributorKind) throws IOException {
		throw new NotImplementedException("searchContributors not supported via Solr");
	}

	@Override
	public SearchResultNodeRef getMetadata(List<String> nodeIds) throws IOException {
		throw new NotImplementedException("getMetadata not supported via Solr");
	}
}
