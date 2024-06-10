package org.edu_sharing.service.statistic;

import java.util.*;

import org.alfresco.repo.search.impl.solr.ESSearchParameters;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacetMethod;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacetSort;
import org.alfresco.util.Pair;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.context.ApplicationContext;

public class StatisticServiceImpl implements StatisticService {

	ApplicationInfo appInfo;
	Map<String, String> authInfo;
	MCAlfrescoBaseClient client;
	AuthenticationTool authTool;

	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
	org.alfresco.service.cmr.search.SearchService searchService = (org.alfresco.service.cmr.search.SearchService) alfApplicationContext
			.getBean("scopedSearchService");
	
	public StatisticServiceImpl(String appId) {
		try {
			this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			this.authTool = RepoFactory.getAuthenticationToolInstance(appId);
			this.authInfo = this.authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
			this.client = (MCAlfrescoBaseClient) RepoFactory.getInstance(appId, this.authInfo);
		} catch (Throwable e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public long countForQuery(String mdsId,String queryId,String type,String customLucene) throws Throwable {
		String lucene = getLucene(mdsId,queryId,type,customLucene);
		SearchParameters searchParameters = new ESSearchParameters();
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		searchParameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(lucene);
		searchParameters.setSkipCount(0);
		searchParameters.setMaxItems(0);
		ResultSet result = searchService.query(searchParameters);
		return result.getNumberFound();
	}

	

	@Override
	public Statistics get(String context, List<String> properties, Filter filter) throws Throwable {
			String query = "";

			if (context != null) {

				if (context.equals("-root-")) {
					context = client.getHomeFolderID((String) client.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME));
				}
				
				String pathParent = (context != null) ? client.getPath(context) : "";

				query = "PATH:\""+pathParent+"//.\"";
			}

			for (FilterEntry entry : filter.getEntries()) {
				for (String val : entry.getValues()) {
					
					String prop = entry.getProperty();
					String shortProp = CCConstants.getValidLocalName(prop);
					if(shortProp != null) prop = shortProp;
					
					prop = "@" + prop.replaceFirst(":", "\\\\:");
					query += (query.length() > 0) ? " AND " : "";
					query += prop + ":\"" + val + "\"";
				}
			}

			SearchResult result = this.client.searchSolr(query, 0, 0, properties, 1, -1);
			Map<String, Map<String, Integer>> facettes = result.getCountedProps();

			Statistics stats = new Statistics();
			for (Map.Entry<String, Map<String, Integer>> entry : facettes.entrySet()) {
				StatisticEntry statEntry = new StatisticEntry();
				statEntry.setProperty(entry.getKey());
				statEntry.setStatistic(entry.getValue());
				stats.getEntries().add(statEntry);
			}
			return stats;

	}
	private String getLucene(String mdsId, String queryId, String type, String customLucene) throws Exception {
		MetadataSet set = MetadataHelper.getMetadataset(appInfo, mdsId);
		String lucene=set.getQueries(MetadataReader.QUERY_SYNTAX_LUCENE).findBasequery(null);
		String basequery=set.findQuery(queryId, MetadataReader.QUERY_SYNTAX_LUCENE).findBasequery(null);
		if(basequery!=null && !basequery.trim().isEmpty()) {
			lucene+=" AND ("+basequery+")";
		}
		lucene+=" AND TYPE:\""+ QueryParser.escape(type)+"\"";
		if(customLucene!=null)
			lucene+=" AND ("+customLucene+")";
		return lucene;
	}
	@Override
	public List<Map<String, Integer>> countFacetsForQuery(String mdsId, String queryId, String type, String customLucene,
                                                          Collection<String> facets) throws Throwable {
		String lucene=getLucene(mdsId,queryId,type,customLucene);
		SearchParameters searchParameters = new ESSearchParameters();
		for(String field : facets) {
			FieldFacet facette = new FieldFacet(field);
			facette.setMinCount(1);
			facette.setMethod(FieldFacetMethod.ENUM);
			facette.setSort(FieldFacetSort.COUNT);
			searchParameters.addFieldFacet(facette);
		}
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		searchParameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(lucene);
		searchParameters.setSkipCount(0);
		searchParameters.setMaxItems(0);
		ResultSet result = searchService.query(searchParameters);
		List<Map<String, Integer>> list=new ArrayList<Map<String, Integer>>();
		for(String field : facets) {
			List<Pair<String, Integer>> data = result.getFieldFacet(field);
			Map<String, Integer> map = new HashMap<>();
			for(Pair<String, Integer> d : data) {
				if(d.getSecond()>0)
					map.put(d.getFirst(), d.getSecond());
			}
			list.add(map);
		}
		return list;
	}
}
