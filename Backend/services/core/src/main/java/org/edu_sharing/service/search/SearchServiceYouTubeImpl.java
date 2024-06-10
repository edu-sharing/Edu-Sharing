package org.edu_sharing.service.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.SearchCriterias;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.nodeservice.NodeServiceYouTube;
import org.edu_sharing.service.search.model.SearchToken;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

public class SearchServiceYouTubeImpl extends SearchServiceAdapter{
	
	Logger logger = Logger.getLogger(SearchServiceYouTubeImpl.class);
	
	String repositoryId = null;

	String googleAPIKey = null;
	
	public SearchServiceYouTubeImpl(String appId) {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		googleAPIKey = appInfo.getApiKey(); 
	}

	private SearchResultNodeRef searchInternal(SearchCriterias rc, String query, List<MdsQueryCriteria> criterias, SearchToken token)
			throws Throwable {
		
		if (!MetadataSet.DEFAULT_CLIENT_QUERY.equals(query)) {
			throw new Exception("Only ngsearch query is supported for this repository type, requested "+query);
		}
		
		MdsQueryCriteria searchWordCriteria = null;
		
		for(MdsQueryCriteria criteria : criterias){
			if (MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA.equals(criteria.getProperty())) {
				searchWordCriteria = criteria;
			}	
		}
		
		if (searchWordCriteria == null) {
			throw new Exception("Only supported criteria found for query, please use ngsearchword");
		}
		
		String searchWord = searchWordCriteria.getValues().get(0);
		
		try {
			// This object is used to make YouTube Data API requests.
			// The last argument is required, but since we don't need anything
			// initialized when the HttpRequest is initialized, we
			// override the interface and provide a no-op function.

			// Prompt the user to enter a query term.
			String queryTerm = searchWord;

			// Define the API request for retrieving search results.
			YouTube.Search.List search = NodeServiceYouTube.getYoutube().search().list("id,snippet");
			
			// Set your developer key from the Google Developers Console
			// for non-authenticated requests. See:
			// https://console.developers.google.com/

			// String apiKey = properties.getProperty("youtube.apikey");
			search.setKey(googleAPIKey);
			search.setQ(queryTerm);

			// Restrict the search results to only include videos. See:
			// https://developers.google.com/youtube/v3/docs/search/list#type
			search.setType("video");

			// To increase efficiency, only retrieve the fields that the application uses.
			search.setFields("items(id/kind,id/videoId,snippet/title,snippet/description,snippet/publishedAt,snippet/thumbnails/default/url,snippet/channelTitle)");
			// youtube api only supports max. 50
			search.setMaxResults(Math.min((long) token.getFrom()+token.getMaxResult(),50));
			
			search.setVideoLicense("creativecommon");
			//search.setPageToken(arg0)

			// Call the API and print results.
			
			SearchListResponse searchResponse = search.execute();
			
			SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
			List<SearchResult> searchResultList = searchResponse.getItems();
			if (searchResultList != null) {
				
				// prettyPrint(searchResultList.iterator(), queryTerm);

				
				searchResultNodeRef.setSearchCriterias(rc);
				
				
				Map<String, Map<String, Object>> resultData = new HashMap<>();
				searchResultNodeRef.setStartIDX(token.getFrom());
				searchResultNodeRef.setNodeCount(searchResponse.getPageInfo()==null ? 0 : searchResponse.getPageInfo().getTotalResults());
				Iterator<SearchResult> iteratorSearchResults = searchResultList.iterator();
				int i = 0;
				
			    List<org.edu_sharing.service.model.NodeRef> nodeRefs = new ArrayList<org.edu_sharing.service.model.NodeRef>();
				while (iteratorSearchResults.hasNext()) {
					
					SearchResult singleVideo = iteratorSearchResults.next();
					if(i >= token.getFrom()){
						
						ResourceId rId = singleVideo.getId();

						// Confirm that the result represents a video.
						// Otherwise, the item will not contain a video ID.
						if (rId.getKind().equals("youtube#video")) {
							Map<String, Object> esResult = NodeServiceYouTube.getPropsByVideoEntry(repositoryId,singleVideo);
							//resultData.put((String) esResult.get(CCConstants.SYS_PROP_NODE_UID), esResult);
							
							org.edu_sharing.service.model.NodeRef enr = new org.edu_sharing.service.model.NodeRefImpl(this.repositoryId, 
									StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
									StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), esResult);
							nodeRefs.add(enr);
						}
						
					}
					i++;

				}
				searchResultNodeRef.setData(nodeRefs);
				
				
				return searchResultNodeRef;

			}
		} catch (GoogleJsonResponseException e) {
			System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
			throw e;
		} catch (IOException e) {
			System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
			throw e;
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}

		return null;
	}
	
	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
									  SearchToken searchToken) throws Throwable {
		SearchCriterias rc = new SearchCriterias();
		rc.setMetadataSetId(mds.getId());
		rc.setMetadataSetQuery(query);
		rc.setRepositoryId(this.repositoryId);
		
		//recommend search
		if(criterias == null || criterias.size() == 0) {
			String searchword = ApplicationInfoList.getRepositoryInfoById(this.repositoryId).getRecommend_objects_query();
			if (searchword == null) {
				searchword = "Mathematik";
			}
			criterias.put(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA, new String[] {searchword});
			query = MetadataSet.DEFAULT_CLIENT_QUERY;
		}
		
		List<MdsQueryCriteria> criterasConverted = MdsQueryCriteria.fromMap(criterias);
		return searchInternal(rc,query,criterasConverted,searchToken);
	}
	
}
