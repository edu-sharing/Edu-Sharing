package org.edu_sharing.service.search;

import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.search.model.*;

import java.io.IOException;
import java.util.*;

public class SearchServiceAdapter implements SearchService {

    @Override
	public List<String> getAllMediacenters(boolean membershipsOnly) throws Exception {
        return null;
    }
	@Override
	public SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort, boolean scoped, boolean onlyMemberships) throws Throwable {
		return null;
	}

	@Override
	public SearchResultNodeRef getFilesSharedByMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		return null;
	}

	@Override
	public SearchResultNodeRef getFilesSharedToMe(SharedToMeType type, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		return null;
	}

	@Override
	public SearchResult<String> searchGroupMembers(String groupName, String pattern, String authorityType,
			int skipCount, int maxValues, SortDefinition sort) {
		return null;
	}

	@Override
	public SearchResultNodeRef search(SearchToken searchToken, boolean scoped) {
		return null;
	}

	@Override
	public SearchResultNodeRef search(SearchToken searchToken) {
		return null;
	}

	@Override
	public SearchResult<EduGroup> getAllOrganizations(boolean scoped) throws Exception {
		return null;
	}

	@Override
	public SearchResultNodeRef getRelevantNodes(int skipCount, int maxItems) throws Throwable {
		return null;
	}

	@Override
	public SearchResultNodeRef getWorkflowReceive(String fullyAuthenticatedUser, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) {
		return null;
	}

	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
									  SearchToken searchToken) throws Throwable {
		return null;
	}

	@Override
	public SearchResult<String> searchUsers(String _pattern, boolean globalSearch, int _skipCount, int _maxValues,
			SortDefinition sort, Map<String, String> customProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResult<String> findAuthorities(AuthorityType user, String searchWord, boolean globalContext, int from,
			int nrOfResults, SortDefinition sort, Map<String, String> customProperties)
			throws InsufficientPermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<ContentType,SearchToken> getLastSearchTokens() throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResult<String> searchPersonGroups(String authorityName, String pattern, int skipCount, int maxValues,
			SortDefinition sort) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SearchResultNodeRef searchFingerPrint(String nodeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SearchVCard> searchContributors(String suggest, List<String> fields, List<String> contributorProperties, ContributorKind contributorKind) throws IOException {
		return new HashSet<>();
	}

	@Override
	public SearchResultNodeRef getMetadata(List<String> nodeIds) throws IOException {
		return null;
	}
}
