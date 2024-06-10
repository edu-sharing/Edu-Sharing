package org.edu_sharing.service.repoproxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.restservices.mds.v1.model.SuggestionParam;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.service.search.SearchService;

public interface RepoProxy {

	Response searchV2(String repository, String mdsId, String query, SearchService.ContentType contentType,
			Integer maxItems, Integer skipCount, List<String> sortProperties, List<Boolean> sortAscending,
			SearchParameters parameters, List<String> propertyFilter, HttpServletRequest req) throws Throwable;

	Response getDetailsSnippetWithParameters(String repository, String node, String nodeVersion,
			String displayMode, Map<String, String> parameters, HttpServletRequest req) throws Throwable;

	Response getChildren(String repository, String node, Integer maxItems, Integer skipCount, List<String> filter,
			List<String> sortProperties, List<Boolean> sortAscending, String assocName, List<String> propertyFilter,
			HttpServletRequest req) throws Throwable;

	Response getMetadataSets(String repository, HttpServletRequest req) throws Throwable;

	Response getMetadataSet(String repository, String mdsId, HttpServletRequest req) throws Throwable;

	Response getValues(String repository, String mdsId, SuggestionParam suggestionParam, HttpServletRequest req) throws Throwable;

	/**
	 * Whenever possible, prefer to use the method with nodeId to also find out if it is a local node pointing to a remote
	 * @param repoId
	 * @return
	 */
	@Deprecated
	boolean myTurn(String repoId);
	RemoteRepoDetails myTurn(String repoId, String nodeId);

	Map<String, String> remoteAuth(ApplicationInfo repoInfo, String username, boolean validate) throws Throwable;

	Response prepareUsage(String repository, String node, HttpServletRequest req) throws Throwable;

	Response getMetadata(String repository, String node, List<String> propertyFilter, HttpServletRequest req) throws Throwable;

	public static class RemoteRepoDetails {
		private String repository,nodeId;

		public RemoteRepoDetails(String repository, String nodeId) {
			this.repository = repository;
			this.nodeId = nodeId;
		}

		public String getRepository() {
			return repository;
		}

		public void setRepository(String repository) {
			this.repository = repository;
		}

		public String getNodeId() {
			return nodeId;
		}

		public void setNodeId(String nodeId) {
			this.nodeId = nodeId;
		}
	}
}
