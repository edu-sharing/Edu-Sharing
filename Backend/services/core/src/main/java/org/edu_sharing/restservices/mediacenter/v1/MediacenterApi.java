package org.edu_sharing.restservices.mediacenter.v1;

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
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.mediacenter.v1.model.McOrgConnectResult;
import org.edu_sharing.restservices.mediacenter.v1.model.MediacentersImportResult;
import org.edu_sharing.restservices.mediacenter.v1.model.OrganisationsImportResult;
import org.edu_sharing.restservices.node.v1.model.SearchResult;
import org.edu_sharing.restservices.search.v1.SearchApi;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.edu_sharing.service.mediacenter.MediacenterServiceImpl;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Path("/mediacenter/v1")
@Tag(name="MEDIACENTER v1")
@ApiService(value="MEDIACENTER", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class MediacenterApi {

	private static Logger logger = Logger.getLogger(MediacenterApi.class);


	@GET
	@Path("/mediacenter/{repository}")

	@Operation(summary = "get mediacenters in the repository.", description = "Only shows the one available/managing the current user (only admin can access all)"
	)

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Mediacenter[].class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getMediacenters(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<Mediacenter> mediacenters = MediacenterDao.getAll(repoDao).stream().map(MediacenterDao::asMediacenter).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(mediacenters).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
    @POST
    @Path("/mediacenter/{repository}/{mediacenter}")

    @Operation(summary = "create new mediacenter in repository.", description = "admin rights are required.")

    @ApiResponses(
    	value = {
    		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Mediacenter.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    	})

    public Response createMediacenter(
        	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "mediacenter name",required=true) @PathParam("mediacenter") String mediacenter,
    		Mediacenter.Profile profile,
    		@Context HttpServletRequest req) {

    	try {

	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	Mediacenter group = MediacenterDao.create(repoDao,mediacenter,profile).asMediacenter();
	    	return Response.status(Response.Status.OK).entity(group).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	@PUT
	@Path("/mediacenter/{repository}/{mediacenter}")

	@Operation(summary = "edit a mediacenter in repository.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Mediacenter.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response editMediacenter(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "mediacenter name",required=true) @PathParam("mediacenter") String mediacenter,
			Mediacenter.Profile profile,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MediacenterDao group = MediacenterDao.get(repoDao,mediacenter);
			group.changeProfile(profile);
			return Response.status(Response.Status.OK).entity(group.asMediacenter()).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/mediacenter/{repository}/{mediacenter}/manages")

	@Operation(summary = "get groups that are managed by the given mediacenter")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Group[].class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getMediacenterGroups(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "authorityName of the mediacenter that should manage the group",required=true) @PathParam("mediacenter") String mediacenter,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MediacenterDao dao = MediacenterDao.get(repoDao, mediacenter);
			List<Group> groups = dao.getManagedGroups().stream().map(GroupDao::asGroup).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(groups).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	
	
	@POST
	@Path("/mediacenter/{repository}/{mediacenter}/licenses")

	@Operation(summary = "get nodes that are licensed by the given mediacenter")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Group[].class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getMediacenterLicensedNodes(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
		    @Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="10")) @QueryParam("maxItems") Integer maxItems,
		    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
		    @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
		    @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
		    @Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-")) ) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Parameter(description = "authorityName of the mediacenter that licenses nodes",required=true) @PathParam("mediacenter") String mediacenter,
			@Parameter(description = "searchword of licensed nodes",required=true) @QueryParam("searchword") String searchword,
			@Parameter(description = "search parameters", required = true) SearchParameters parameters,
			@Context HttpServletRequest req) {

		try {
			
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			Filter filter= new Filter(propertyFilter);
			SearchToken searchToken=new SearchToken();
			searchToken.setFrom(skipCount != null ? skipCount : 0);
			searchToken.setMaxResult(maxItems!= null ? maxItems : 10);

			checkMediacenterSortProperty(sortProperties, mediacenter);

			searchToken.setSortDefinition(new SortDefinition(sortProperties, sortAscending));

			String authorityScope = MediacenterServiceImpl.getAuthorityScope(mediacenter);

			searchToken.setAuthorityScope(Arrays.asList(new String[] {authorityScope}));
			MdsDao mdsDao = MdsDao.getMds(repoDao, MdsDao.DEFAULT);

			searchToken.setFacets(new ArrayList<>());
			NodeSearch search = NodeDao.search(repoDao,mdsDao,"mediacenter_filter", parameters.getCriteria() ,searchToken,Filter.createShowAllFilter(), SearchApi.parametersToDaoTransformer(parameters));

			List<Node> data = null;
			if(search.getNodes().size() < search.getResult().size()){
				//searched repo deliveres only nodeRefs by query time
				data = NodeDao.convertToRest(repoDao, search.getResult(), filter, null);
			}else{
				//searched repo delivered properties by query time
				data = search.getNodes();
				// @TODO: we may need to still call convertToRest to make sure we've latest data from remote repos
			}

			for(Node node : data){
				String newValue = null;
				String[] mediacenters = node.getProperties().get("ccm:mediacenter");
				if(mediacenters != null) {
					for (String mzStatus : mediacenters) {
						try {
							JSONObject o = (JSONObject) new JSONParser().parse(mzStatus.trim());
							String mzName = (String) o.get("name");
							if (mzName.contains(mediacenter)) {
								newValue = (String) o.get("activated");
							}
						}catch(Exception e){
							logger.error(e.getMessage());
						}
					}
				}
				node.getProperties().put("ccm:mediacenter",new String[]{newValue});

				MetadataSet mds = null;
				try{mds = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), node.getProperties().get("cm:edu_metadataset")[0]);}catch(Exception e){};
				String[] displayNames = MetadataHelper.getDisplayNames(mds, "ccm:mediacenter", newValue);
				if(displayNames != null){
					node.getProperties().put("ccm:mediacenter"+ CCConstants.DISPLAYNAME_SUFFIX,displayNames);
				}
			}

	    	Pagination pagination = new Pagination();
	    	pagination.setFrom(search.getSkip());
	    	pagination.setCount(data.size());
	    	pagination.setTotal(search.getCount());
	    	
	    	
	    	SearchResult response = new SearchResult();
	    	response.setNodes(data);
	    	response.setPagination(pagination);	    	
	    	response.setFacets(search.getFacets());
	    	return Response.status(Response.Status.OK).entity(response).build();
			//MediacenterDao dao = MediacenterDao.get(repoDao, mediacenter);
	    	//List<Node> result = dao.getLicensedNodes();
	    	//return Response.status(Response.Status.OK).entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/mediacenter/{repository}/{mediacenter}/licenses/export")
	@Operation(summary = "get nodes that are licensed by the given mediacenter",  description = "e.g. cm:name")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Map[].class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})
	public Response exportMediacenterLicensedNodes(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "properties to fetch, use parent::<property> to include parent property values") @QueryParam("properties") List<String> properties,
			@Parameter(description = "authorityName of the mediacenter that licenses nodes",required=true) @PathParam("mediacenter") String mediacenter,
			@Parameter(description = "search parameters", required = true) SearchParameters parameters,
			@Context HttpServletRequest req) {

		try {

			checkMediacenterSortProperty(sortProperties, mediacenter);

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MdsDao mdsDao = MdsDao.getMds(repoDao, MdsDao.DEFAULT);
			List<Map<String, List<Serializable>>> data = new ArrayList<>();

			List<NodeRef> nodes = MediacenterServiceFactory.getLocalService().getAllLicensedNodes(mediacenter, MetadataSearchHelper.convertCriterias(parameters.getCriteria()), new SortDefinition(sortProperties, sortAscending));
			for (NodeRef ref : nodes) {
				Map<String, List<Serializable>> props=new HashMap<>();
				for(String prop : properties){
					Object value;
					if(prop.startsWith("parent::")){
						String parentId = NodeServiceFactory.getLocalService().getPrimaryParent(ref.getNodeId());
						String realProp=prop.substring("parent::".length());
						value = NodeServiceHelper.getPropertyNative(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId),CCConstants.getValidGlobalName(realProp));
					}
					else{
						//props.put(prop, NodeServiceHelper.getPropertyNative(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ref.getRef().getId()),CCConstants.getValidGlobalName(prop)));
						value = ref.getProperties().get(CCConstants.getValidGlobalName(prop));
					}
					if(value instanceof Collection) {
						props.put(prop, (List<Serializable>) value);
					} else {
						props.put(prop, Collections.singletonList((Serializable) value));
					}
				}
				data.add(props);
			}
			logger.info("result:" + data.size());
			return Response.status(Response.Status.OK).entity(data).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	private static void checkMediacenterSortProperty(List<String> sortProperties, String mediacenter) {
		if(!sortProperties.isEmpty() && sortProperties.get(0).equals("ccm:mediacenter")){
			sortProperties.set(0,"ccm:mediacenter_sort."+ mediacenter +".activated.keyword");
		}
	}

	@PUT
	@Path("/mediacenter/{repository}/{mediacenter}/manages/{group}")

	@Operation(summary = "add a group that is managed by the given mediacenter", description = "although not restricted, it is recommended that the group is an edu-sharing organization (admin rights are required)")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Group[].class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response addMediacenterGroup(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "authorityName of the mediacenter that should manage the group",required=true) @PathParam("mediacenter") String mediacenter,
			@Parameter(description = "authorityName of the group that should be managed by that mediacenter",required=true) @PathParam("group") String group,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MediacenterDao dao = MediacenterDao.get(repoDao, mediacenter);
			dao.addManagedGroup(group);
			List<Group> groups = dao.getManagedGroups().stream().map(GroupDao::asGroup).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(groups).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@DELETE
	@Path("/mediacenter/{repository}/{mediacenter}")
	@Operation(summary = "delete a mediacenter group and it's admin group and proxy group", description = "admin rights are required.")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})
	public Response deleteMediacenter(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "authorityName of the mediacenter that should manage the group",required=true) @PathParam("mediacenter") String authorityName,
			@Context HttpServletRequest req
			){
		try {
			RepositoryDao repoDao  = RepositoryDao.getRepository(repository);
			MediacenterDao.delete(repoDao,authorityName);
			return Response.status(Response.Status.OK).build();
		} catch (DAOException e) {
			return ErrorResponse.createResponse(e);
		}
	}

	@DELETE
	@Path("/mediacenter/{repository}/{mediacenter}/manages/{group}")

	@Operation(summary = "delete a group that is managed by the given mediacenter", description = "admin rights are required.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Group[].class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response removeMediacenterGroup(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "authorityName of the mediacenter that should manage the group",required=true) @PathParam("mediacenter") String mediacenter,
			@Parameter(description = "authorityName of the group that should not longer be managed by that mediacenter",required=true) @PathParam("group") String group,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MediacenterDao dao = MediacenterDao.get(repoDao, mediacenter);
			dao.removeManagedGroup(group);
			List<Group> groups = dao.getManagedGroups().stream().map(GroupDao::asGroup).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(groups).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	
	@POST
	@Path("/import/mediacenters")
	@Consumes({ "multipart/form-data" })
	@Operation(summary = "Import mediacenters", description = "Import mediacenters.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = MediacentersImportResult.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response importMediacenters(@Parameter(description = "Mediacenters csv to import", required = true) @FormDataParam("mediacenters") InputStream is,
			@Context HttpServletRequest req) {
		try {

			org.edu_sharing.service.authority.AuthorityService eduAuthorityService = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(!eduAuthorityService.isGlobalAdmin()){
				throw new Exception("Admin rights are required for this endpoint");
			}

			int count = MediacenterServiceFactory.getInstance().importMediacenters(is);
			MediacentersImportResult result = new MediacentersImportResult();
			result.setRows(count);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Consumes({ "multipart/form-data" })
	@Path("/import/organisations")

	@Operation(summary = "Import Organisations", description = "Import Organisations.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = OrganisationsImportResult.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response importOrganisations(@Parameter(description = "Organisations csv to import", required = true) @FormDataParam("organisations") InputStream is,
									   @Context HttpServletRequest req) {
		try {

			org.edu_sharing.service.authority.AuthorityService eduAuthorityService = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(!eduAuthorityService.isGlobalAdmin()){
				throw new Exception("Admin rights are required for this endpoint");
			}

			int count = MediacenterServiceFactory.getInstance().importOrganisations(is);
			OrganisationsImportResult result = new OrganisationsImportResult();
			result.setRows(count);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/import/mc_org")
	@Consumes({ "multipart/form-data" })
	@Operation(summary = "Import Mediacenter Organisation Connection", description = "Import Mediacenter Organisation Connection.")
	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = McOrgConnectResult.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response importMcOrgConnections(@Parameter(description = "Mediacenter Organisation Connection csv to import", required = true) @FormDataParam("mcOrgs") InputStream is,
										@Parameter(description = "removeSchoolsFromMC" , schema = @Schema(defaultValue="false")) @QueryParam("removeSchoolsFromMC") boolean removeSchoolsFromMC,
										@Context HttpServletRequest req) {
		try {

			org.edu_sharing.service.authority.AuthorityService eduAuthorityService = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(!eduAuthorityService.isGlobalAdmin()){
				throw new Exception("Admin rights are required for this endpoint");
			}

			int count = MediacenterServiceFactory.getInstance().importOrgMcConnections(is, removeSchoolsFromMC);
			McOrgConnectResult result = new McOrgConnectResult();
			result.setRows(count);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

}

