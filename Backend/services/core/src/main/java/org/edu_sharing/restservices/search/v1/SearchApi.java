package org.edu_sharing.restservices.search.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.edu_sharing.repository.server.NodeRefVersion;
import org.edu_sharing.repository.server.tools.LRMITool;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.restservices.search.v1.model.SearchParametersFacets;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.CombineMode;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SearchVCard;
import org.edu_sharing.service.search.model.SortDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Path("/search/v1")
@Tag(name= "SEARCH v1" )
@ApiService(value = "SEARCH", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class SearchApi {

	private static Logger logger = Logger.getLogger(SearchApi.class);

	@POST
	@Path("/queries/{repository}/{metadataset}/{query}")
	@Consumes({ "application/json" })

	@Operation(operationId = "search", summary = "Perform queries based on metadata sets.", description = "Perform queries based on metadata sets.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SearchResultNode.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response search(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, schema = @Schema(defaultValue="-default-")) @PathParam("metadataset") String mdsId,
			@Parameter(description = "ID of query", required = true) @PathParam("query") String query,
			@Parameter(description = "Type of element", required = false) @QueryParam("contentType") SearchService.ContentType contentType,
			@Parameter(description = "maximum items per page", schema = @Schema(defaultValue="10")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = "skip a number of items", schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "search parameters", required = true) SearchParameters parameters,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-"))) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {


			if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
				return RepoProxyFactory.getRepoProxy().searchV2(repository, mdsId, query, contentType, maxItems, skipCount, sortProperties, sortAscending, parameters, propertyFilter, req);
			}

			Filter filter = new Filter(propertyFilter);

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MdsDao mdsDao = MdsDao.getMds(repoDao, mdsId);

			SearchToken token = new SearchToken();
			token.setFacets(parameters.getFacets());
			token.setFacetLimit((parameters.getFacetLimit() != null && parameters.getFacetLimit() > 0)
					? parameters.getFacetLimit() : 10);
			token.setFacetsMinCount((parameters.getFacetMinCount() != null && parameters.getFacetMinCount() >= 0 )
					? parameters.getFacetMinCount(): 5);
			token.setQueryString(parameters.getFacetSuggest());
			token.setPermissions(parameters.getPermissions());
			token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
			token.setFrom(skipCount != null ? skipCount : 0);
			token.setMaxResult(maxItems != null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
			token.setContentType(contentType);
			token.setResolveCollections(parameters.isResolveCollections());
			token.setReturnSuggestion(parameters.isReturnSuggestions());
			token.setExcludes(parameters.getExcludes());
			NodeSearch search = NodeDao.search(repoDao, mdsDao, query, parameters.getCriteria(), token, filter, parametersToDaoTransformer(parameters));

			List<Node> data = null;//new ArrayList<>();
			if(search.getNodes().isEmpty()){
				//searched repo deliveres only nodeRefs by query time
				data = NodeDao.convertToRest(repoDao, search.getResult(), filter, parametersToDaoTransformer(parameters));
			}else{
				//searched repo delivered properties by query time
				data = search.getNodes();
				// @TODO: we may need to still call convertToRest to make sure we've latest data from remote repos
			}


			Pagination pagination = new Pagination();
			pagination.setFrom(search.getSkip());
			pagination.setCount(data.size());
			pagination.setTotal(search.getCount());


			SearchResultNode response = new SearchResultNode();
			response.setNodes(data);
			response.setIgnored(search.getIgnored());
			response.setPagination(pagination);
			response.setFacets(search.getFacets());
			response.setSuggests(search.getSuggests());

			return Response.status(Response.Status.OK).entity(response).build();

		}  catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	public static Function<NodeDao, NodeDao> parametersToDaoTransformer(SearchParameters parameters) {
		return (dao) -> {
			dao.resolveUsernames = parameters.isResolveUsernames();
			return dao;
		};
	}

	@POST
	@Path("/queries/{repository}/{metadataset}/{query}/lrmi")
	@Consumes({ "application/json" })

	@Operation(operationId = "search-lrmi", summary = "Perform queries based on metadata sets.", description = "Perform queries based on metadata sets.")
	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SearchResultLrmi.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response searchLrmi(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, schema = @Schema(defaultValue="-default-")) @PathParam("metadataset") String mdsId,
			@Parameter(description = "ID of query", required = true) @PathParam("query") String query,
			@Parameter(description = "Type of element", required = false) @QueryParam("contentType") SearchService.ContentType contentType,
			@Parameter(description = "maximum items per page", schema = @Schema(defaultValue="10")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = "skip a number of items", schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "search parameters", required = true) SearchParameters parameters,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-"))) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {


			if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
				return RepoProxyFactory.getRepoProxy().searchV2(repository, mdsId, query, contentType, maxItems, skipCount, sortProperties, sortAscending, parameters, propertyFilter, req);
			}

			Filter filter = new Filter(propertyFilter);

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MdsDao mdsDao = MdsDao.getMds(repoDao, mdsId);

			SearchToken token = new SearchToken();
			token.setFacets(parameters.getFacets());
			token.setFacetLimit((parameters.getFacetLimit() != null && parameters.getFacetLimit() > 0)
					? parameters.getFacetLimit() : 10);
			token.setFacetsMinCount((parameters.getFacetMinCount() != null && parameters.getFacetMinCount() >= 0 )
					? parameters.getFacetMinCount(): 5);
			token.setQueryString(parameters.getFacetSuggest());
			token.setPermissions(parameters.getPermissions());
			token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
			token.setFrom(skipCount != null ? skipCount : 0);
			token.setMaxResult(maxItems != null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
			token.setContentType(contentType);
			token.setResolveCollections(parameters.isResolveCollections());
			token.setReturnSuggestion(parameters.isReturnSuggestions());
			token.setExcludes(parameters.getExcludes());
			NodeSearch search = NodeDao.search(repoDao, mdsDao, query, parameters.getCriteria(), token, filter, parametersToDaoTransformer(parameters));

			List<Node> nodes = null;//new ArrayList<>();
			if(search.getNodes().size() == 0){
				//searched repo deliveres only nodeRefs by query time
				nodes = NodeDao.convertToRest(repoDao, search.getResult(), filter, null);
			}else{
				//searched repo delivered properties by query time
				nodes = search.getNodes();
				// @TODO: we may need to still call convertToRest to make sure we've latest data from remote repos
			}


			List<String> data = new ArrayList<>(nodes.size());
			for (Node node: nodes) {
				org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(
						node.getRef().isArchived()
								? StoreRef.STORE_REF_ARCHIVE_SPACESSTORE
								: StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
						node.getRef().getId());

				data.add(LRMITool.getLRMIJson(new NodeRefVersion(nodeRef, null)).toString());
			}


			Pagination pagination = new Pagination();
			pagination.setFrom(search.getSkip());
			pagination.setCount(nodes.size());
			pagination.setTotal(search.getCount());


			SearchResultLrmi response = new SearchResultLrmi();
			response.setNodes(data);
			response.setIgnored(search.getIgnored());
			response.setPagination(pagination);
			response.setFacets(search.getFacets());
			response.setSuggests(search.getSuggests());

			return Response.status(Response.Status.OK).entity(response).build();

		}  catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}


	@POST
	@Path("/queries/{repository}/{metadataset}/{query}/facets")
	@Consumes({ "application/json" })

	@Operation(summary = "Search in facets.", description = "Perform queries based on metadata sets.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SearchResultNode.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response searchFacets(

			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, schema = @Schema(defaultValue="-default-")) @PathParam("metadataset") String mdsId,
			@Parameter(description = "ID of query", required = true) @PathParam("query") String query,
			@Parameter(description = "facet parameters", required = true) SearchParametersFacets parameters,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MdsDao mdsDao = MdsDao.getMds(repoDao, mdsId);

			SearchToken token = new SearchToken();
			token.setFacets(parameters.getFacets());
			token.setFrom(0);
			token.setMaxResult(0);
			token.setFacetLimit((parameters.getFacetLimit() != null && parameters.getFacetLimit() > 0)
					? parameters.getFacetLimit() : 10);
			token.setFacetsMinCount((parameters.getFacetMinCount()  != null && parameters.getFacetMinCount() >= 0 )
					? parameters.getFacetMinCount() : 5);
			token.setQueryString(parameters.getFacetSuggest());

			NodeSearch search = NodeDao.searchFacets(repoDao, mdsDao, query, parameters.getCriteria(), token);
			SearchResultNode response = new SearchResultNode();
			response.setNodes(new ArrayList<>());
			response.setIgnored(search.getIgnored());
			response.setFacets(search.getFacets());
			return Response.status(Response.Status.OK).entity(search).build();

		}  catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@POST
	@Path("/queries/{repository}/{metadataset}/{query}/save")
	@Consumes({ "application/json" })

	@Operation(summary = "Save a search query.", description = "Save a search query.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response saveSearch(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, schema = @Schema(defaultValue="-default-")) @PathParam("metadataset") String mdsId,
			@Parameter(description = "ID of query", required = true) @PathParam("query") String query,
			@Parameter(description = "Name of the new search item", required = true) @QueryParam("name") String name,
			@Parameter(description = "Replace if search with the same name exists", required = false, schema = @Schema(defaultValue="false")) @QueryParam("replace") Boolean replace,
			@Parameter(description = "search parameters", required = true) List<MdsQueryCriteria> parameters,
			@Context HttpServletRequest req) {

		try {

			// RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			// MdsDao mdsDao = MdsDao.getMds(repoDao, mdsId);

			NodeDao nodeDao = NodeDao.saveSearch(repository, mdsId, query, name, parameters,
					replace != null ? replace : false);
			NodeEntry result = new NodeEntry();
			result.setNode(nodeDao.asNode());
			return Response.status(Response.Status.OK).entity(result).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/queries/load/{nodeId}")
	@Consumes({ "application/json" })

	@Operation(summary = "Load a saved search query.", description = "Load a saved search query.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Node.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response loadSaveSearch(
			@Parameter(description = "Node id of the search item", required = true) @PathParam("nodeId") String nodeId,
			@Parameter(description = "Type of element", required = false) @QueryParam("contentType") SearchService.ContentType contentType,
			@Parameter(description = "maximum items per page", schema = @Schema(defaultValue="10")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = "skip a number of items", schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "facets", required = false) List<String> facets,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-"))) @QueryParam("propertyFilter") List<String> propertyFilter,

			@Context HttpServletRequest req) {

		try {

			Filter filter = new Filter(propertyFilter);

			SearchResult<Node> result = NodeDao.getNode(RepositoryDao.getHomeRepository(), nodeId, filter).
					runSavedSearch(skipCount != null ? skipCount : 0,
							maxItems != null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
							contentType,
							new SortDefinition(sortProperties,sortAscending),
							facets
					);
			return Response.status(Response.Status.OK).entity(result).build();
		}  catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@POST
	@Path("/queries/{repository}/fingerprint/{nodeid}")
	@Consumes({ "application/json" })

	@Operation(summary = "Perform queries based on metadata sets.", description = "Perform queries based on metadata sets.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SearchResultNode.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response searchFingerprint(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "nodeid", required = true) @PathParam("nodeid") String nodeId,
			@Parameter(description = "maximum items per page", schema = @Schema(defaultValue="10")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = "skip a number of items", schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-"))) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			Filter filter = new Filter(propertyFilter);
			NodeSearch nodeSearch = NodeDao.searchFingerprint(repoDao, nodeId, filter);
			SearchResultNode response = new SearchResultNode();

			List<Node> data = new ArrayList<>();
			for (org.edu_sharing.restservices.shared.NodeRef ref : nodeSearch.getResult()) {
				data.add(NodeDao.getNode(repoDao, ref.getId(), filter).asNode());
			}
			response.setNodes(data);


			Pagination pagination = new Pagination();
			pagination.setFrom(nodeSearch.getSkip());
			pagination.setCount(data.size());
			pagination.setTotal(nodeSearch.getCount());

			response.setPagination(pagination);

			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}


	@GET
	@Path("/custom/{repository}")
	@Consumes({ "application/json" })

	@Operation(summary = "Search for custom properties with custom values", description = "e.g. property=cm:name, value:*Test*")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SearchResultNode.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response searchByProperty(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "Type of element", required = false) @QueryParam("contentType") SearchService.ContentType contentType,
			@Parameter(description = "Combine mode, AND or OR, defaults to AND", required = false) @QueryParam("combineMode") SearchService.CombineMode combineMode,
			@Parameter(description = "One (or more) properties to search for, will be combined by specified combine mode", required = false) @QueryParam("property") List<String> property,
			@Parameter(description = "One (or more) values to search for, matching the properties defined before", required = false) @QueryParam("value") List<String> value,
			@Parameter(description = "(Optional) comparator, only relevant for date or numerical fields, currently allowed =, <=, >=", required = false) @QueryParam("comparator") List<String> comparator,
			@Parameter(description = "maximum items per page", schema = @Schema(defaultValue="10")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = "skip a number of items", schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-"))) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			Filter filter = new Filter(propertyFilter);

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			SearchToken token = new SearchToken();
			token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
			token.setFrom(skipCount != null ? skipCount : 0);
			token.setMaxResult(maxItems != null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
			token.setContentType(contentType);
			if (property.size() != value.size()) {
				throw new InvalidArgumentException("Number of properties must be equal to number of values");
			}
			if (comparator != null && comparator.size() == 0)
				comparator = null;
			if (comparator != null && property.size() != comparator.size()) {
				throw new InvalidArgumentException("Number of properties must be equal to number of comparators");
			}
			String query = "";
			for (int i = 0; i < property.size(); i++) {
				String comp = "=";
				if (comparator != null)
					comp = comparator.get(i);
				if (!query.isEmpty()) {
					if (combineMode == null || combineMode.equals(CombineMode.AND)) {
						query += " AND ";
					} else {
						query += " OR ";
					}
				}
				query += "@" + property.get(i).replace(":", "\\:") + ":";
				if (comp.equals("<=")) {
					query += "[MIN TO ";
				}
				if (comp.equals(">=")) {
					query += "[";
				}
				query += QueryParser.escape(value.get(i));
				if (comp.equals("<=")) {
					query += "]";
				}
				if (comp.equals(">=")) {
					query += " TO MAX]";
				}
				query += "";
			}
			token.setLuceneString(query);
			NodeSearch search = NodeDao.search(repoDao, token);

			List<Node> data = new ArrayList<>();
			for (NodeRef ref : search.getResult()) {
				data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
			}

			Pagination pagination = new Pagination();
			pagination.setFrom(search.getSkip());
			pagination.setCount(data.size());
			pagination.setTotal(search.getCount());


			SearchResultNode response = new SearchResultNode();
			response.setNodes(data);
			response.setPagination(pagination);
			response.setFacets(search.getFacets());
			return Response.status(Response.Status.OK).entity(response).build();

		}  catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/relevant/{repository}")
	@Consumes({ "application/json" })
	@Operation(summary = "Get relevant nodes for the current user")
	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SearchResultNode.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getRelevantNodes(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-")) ) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Parameter(description = "maximum items per page", schema = @Schema(defaultValue="10")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = "skip a number of items", schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Context HttpServletRequest req) {
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			Filter filter = new Filter(propertyFilter);
			NodeSearch nodeSearch = NodeDao.getRelevantNodes(repoDao,skipCount!=null ? skipCount : 0,maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
			SearchResultNode response = new SearchResultNode();

			List<Node> data = new ArrayList<>();
			for (org.edu_sharing.restservices.shared.NodeRef ref : nodeSearch.getResult()) {
				data.add(NodeDao.getNode(repoDao, ref.getId(), filter).asNode());
			}
			response.setNodes(data);
			Pagination pagination = new Pagination();
			pagination.setFrom(nodeSearch.getSkip());
			pagination.setCount(data.size());
			pagination.setTotal(nodeSearch.getCount());
			response.setPagination(pagination);
			return Response.status(Response.Status.OK).entity(response).build();

		}   catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/custom/{repository}")
	@Hidden

	public Response options02() {

		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}


	@GET
	@Path("/queries/{repository}/contributor")
	@Consumes({ "application/json" })

	@Operation(summary = "Search for contributors", description = "")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SearchVCard[].class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response searchContributor(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "search word", required = true) @QueryParam("searchWord") String searchWord,
			@Parameter(description = "contributor kind", required = true, schema = @Schema(defaultValue="PERSON")) @QueryParam("contributorKind") SearchService.ContributorKind contributorKind,
			@Parameter(description = "define which authority fields should be searched: ['firstname', 'lastname', 'email', 'uuid', 'url']") @QueryParam("fields") List<String> fields,
			@Parameter(description = "define which contributor props should be searched: ['ccm:lifecyclecontributer_author', 'ccm:lifecyclecontributer_publisher', ..., 'ccm:metadatacontributer_creator', 'ccm:metadatacontributer_validator']") @QueryParam("contributorProperties") List<String> contributorProperties,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			Set<SearchVCard> result = SearchServiceFactory.getSearchService(repoDao.getId()).searchContributors(searchWord, fields, contributorProperties, contributorKind);
			return Response.status(Response.Status.OK).entity(result).build();

		}  catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/metadata/{repository}")
	@Consumes({ "application/json" })
	@Operation(summary = "get nodes with metadata and collections")
	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntries.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getMetdata(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "nodeIds") @QueryParam("nodeIds") List<String> nodeIds,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-"))) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			Filter filter = new Filter(propertyFilter);
			NodeSearch search = NodeDao.getMetadata(repoDao, nodeIds, filter);

			List<Node> data = null;//new ArrayList<>();
			if(search.getNodes().size() < search.getResult().size()){
				//searched repo deliveres only nodeRefs by query time
				data = NodeDao.convertToRest(repoDao, search.getResult(), null, null);
			}else{
				//searched repo delivered properties by query time
				data = search.getNodes();
				// @TODO: we may need to still call convertToRest to make sure we've latest data from remote repos
			}
			NodeEntries response = new NodeEntries();
			response.setNodes(data);

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (DAOException e) {
			return ErrorResponse.createResponse(e);
		}

	}

}
