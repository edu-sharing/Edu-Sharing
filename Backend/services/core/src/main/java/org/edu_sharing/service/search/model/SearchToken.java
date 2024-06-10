package org.edu_sharing.service.search.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.edu_sharing.metadataset.v2.SearchCriterias;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.SearchService.ContentType;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class SearchToken implements Serializable {
	static Logger logger = Logger.getLogger(SearchToken.class);

	@Getter
	@Setter
	SortDefinition sortDefinition;
	
	@Setter
	String luceneString;
	
	@Setter
	List<String> facets =null;

	@Getter
	@Setter
	String storeProtocol = "workspace";
	
	@Getter
	@Setter
	String storeName = "SpacesStore";
	
	@Getter
	@Setter
	int from;
	
	@Getter
	@Setter
	int maxResult;
	
	
	/**
	 * search in scope of authorities
	 * -- SETTER --
	 *  set the scope of authorities
	 *  security problem (only when admin permissions)
	 *
	 * @param authorities

	 */
	@Getter
	@Setter
	List<String> authorityScope;

	private ContentType contentType;
	private MetadataQueries queries;
	/**
	 * -- SETTER --
	 *  Filter for permissions by the current user
	 *  Only materials with ALL permissions provided here will be shown for the current user
	 *  Note: Only supported via ElasticSearch!
	 */
	@Getter
	@Setter
	private List<String> permissions;
	@Getter
	@Setter
	private boolean resolveCollections = false;
	@Getter
	@Setter
	private boolean returnSuggestion = false;

	@Getter
	@Setter
	List<String> excludes = new ArrayList<>();

	public ContentType getContentType(){
		if(contentType==null)
			return ContentType.FILES;
		return contentType;
	}
	public void setContentType(ContentType contentType){
		this.contentType=contentType;
		updateSearchCriterias(true);
	}


	@Getter
	@Setter
	private int facetLimit =50;
	@Setter
	@Getter
	private int facetsMinCount =4;

	@Getter
	private SearchCriterias searchCriterias;

	/**
	 * -- SETTER --
	 *  Get and set query string (for ui purposes/info)
	 *
	 * @param queryString
	 */
	@Setter
	private String queryString;

	@Getter
	private MetadataQuery query;

	@Getter
	private Map<String, String[]> parameters;

	public String getLuceneString() throws IllegalArgumentException {
		if(query!=null){
			return MetadataSearchHelper.getLuceneString(queries,query,searchCriterias,parameters);
		}
		if(searchCriterias!=null){
			logger.debug("Using lucene string only search");
			/*QueryBuilder queryBuilder = new QueryBuilder();
			queryBuilder.setSearchCriterias(searchCriterias);
			if(luceneString==null || luceneString.trim().isEmpty())
				return queryBuilder.getSearchString();
			return "("+queryBuilder.getSearchString()+") AND ("+luceneString+")";
			*/
			return MetadataSearchHelper.convertSearchCriteriasToLucene(luceneString,searchCriterias);
		}
		return luceneString;
	}

	/**
	 * Nulls any existing search criterias
	 */
	public void disableSearchCriterias() {
		this.searchCriterias=null;
	}

	public void setMetadataQuery(MetadataQueries queries, String queryId, Map<String, String[]> parameters) {
		this.queries = queries;
		this.query = queries.findQuery(queryId);
		this.parameters = parameters;
	}

	public List<String> getFacets() {
		if(this.query != null && facets != null) {
			List<String> combined = new ArrayList<>();
			this.facets.forEach((facet) -> {
				List<String> sublist = this.query.findParameterByName(facet).getFacets();
				if(sublist != null) {
					combined.addAll(sublist);
				} else {
					combined.add(facet);
				}
			});
			return combined;
		}
		return facets;
	}

	/**
	 *
	 * re aggregates splitted facettes from the current mds query to a single list for the client
	 */
	public Map<String, Map<String, Integer>> aggregateFacetes(Map<String, Map<String, Integer>> propsMap) {
		Map<String, Map<String, Integer>> combined = new HashMap<>();
		if(this.query != null && facets != null) {
			List<MetadataQueryParameter> facetParams = this.query.getParameters().stream().filter((p) -> p.getFacets() != null).collect(Collectors.toList());
			for(Map.Entry<String, Map<String, Integer>> entry : propsMap.entrySet()) {
				Optional<MetadataQueryParameter> param = facetParams.stream().filter((p) -> p.getFacets().contains(entry.getKey())).findFirst();
				String key = entry.getKey();
				if(param.isPresent()) {
					key = param.get().getName();
				}
				if(combined.containsKey(key)) {
					Map<String, Integer> current = combined.get(key);
					current.putAll(entry.getValue());
					combined.put(key, current);
				} else {
					combined.put(key, entry.getValue());
				}
			}
			return combined;
		}
		return propsMap;
	}

	public void setSearchCriterias(SearchCriterias searchCriterias) {
		this.searchCriterias=searchCriterias;
		updateSearchCriterias(false);
	}

	private void updateSearchCriterias(boolean rebuild) {
		if(searchCriterias==null || rebuild)
			searchCriterias=new SearchCriterias();
		if(getContentType()==null || getContentType().equals(ContentType.ALL)){
			//searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_MAP});
			return;
		}
		if(getContentType().equals(ContentType.FILES)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_IO});
		}
		if(getContentType().equals(ContentType.FOLDERS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_MAP});
		}
		if(getContentType().equals(ContentType.FILES_AND_FOLDERS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_MAP});
		}
		if(getContentType().equals(ContentType.COLLECTIONS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_MAP});
			searchCriterias.setAspects(new String[]{CCConstants.CCM_ASPECT_COLLECTION});
		}
		if(getContentType().equals(ContentType.TOOLPERMISSIONS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_TOOLPERMISSION});
		}
		if(getContentType().equals(ContentType.COLLECTION_PROPOSALS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_COLLECTION_PROPOSAL});
		}
	}

	public String getQueryString() throws IllegalArgumentException {
		return queryString;
	}

}
