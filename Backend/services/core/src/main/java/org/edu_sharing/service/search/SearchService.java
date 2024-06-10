
package org.edu_sharing.service.search;

import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.search.model.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SearchService {
	public static enum ContentType{
		FILES,
		FOLDERS,
		FILES_AND_FOLDERS,
		COLLECTIONS,
		TOOLPERMISSIONS,
		COLLECTION_PROPOSALS,
		ALL
	};
	public static enum CombineMode{
		AND,
		OR
	};

	public static enum ContributorKind {
		PERSON,
		ORGANIZATION
	}

	SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort,boolean scoped,boolean onlyMemberShips) throws Throwable;

	default List<String> getAllMediacenters() throws Exception {
		return getAllMediacenters(false);
	}

	List<String> getAllMediacenters(boolean membershipsOnly) throws Exception;


	SearchResultNodeRef getFilesSharedByMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception;

	SearchResultNodeRef getFilesSharedToMe(SharedToMeType type, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception;
	
	SearchResultNodeRef getWorkflowReceive(String fullyAuthenticatedUser, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception;

	SearchResult<String> searchGroupMembers(String groupName, String pattern,
			String authorityType, int skipCount, int maxValues, SortDefinition sort);

	SearchResultNodeRef search(SearchToken searchToken, boolean scoped);
	SearchResultNodeRef search(SearchToken searchToken);



	SearchResult<EduGroup> getAllOrganizations(boolean scoped) throws Exception;

	SearchResultNodeRef getRelevantNodes(int skipCount, int maxItems) throws Throwable;

	SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
							   SearchToken searchToken) throws Throwable;


 	/**
      * find Authorities and Users plain solr
 	 * @param user
      * @param searchWord
      * @param globalContext
      * @param from
      * @param nrOfResults
 	 * @param customProperties
 	 * @param sort
      * @return
 	 * @throws InsufficientPermissionException 
      */
	SearchResult<String> findAuthorities(AuthorityType user, String searchWord, boolean globalContext, int from, int nrOfResults, SortDefinition sort, Map<String, String> customProperties) throws Exception;

	SearchResult<String> searchUsers(String _pattern, boolean globalSearch, int _skipCount, int _maxValues,
			SortDefinition sort, Map<String, String> customProperties) throws Exception;

	SearchResult<String> searchPersonGroups(String authorityName, String pattern, int skipCount, int maxValues,
			SortDefinition sort);

	Map<ContentType,SearchToken> getLastSearchTokens() throws Throwable;
	
	public default List<? extends  Suggestion> getSuggestions(MetadataSet mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criterias) {
		return null;	
	}

	SearchResultNodeRef searchFingerPrint(String nodeId);

	public Set<SearchVCard> searchContributors(String suggest, List<String> fields, List<String> contributorProperties, ContributorKind kind) throws IOException;

	public SearchResultNodeRef getMetadata(List<String> nodeIds) throws IOException;
}
