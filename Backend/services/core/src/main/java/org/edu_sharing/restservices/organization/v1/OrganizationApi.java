package org.edu_sharing.restservices.organization.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.OrganizationDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.organization.v1.model.OrganizationEntries;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.Organization;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SortDefinition;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/organization/v1")
@Tag(name="ORGANIZATION v1")
@ApiService(value="ORGANIZATION", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class OrganizationApi  {

	private static Logger logger = Logger.getLogger(OrganizationApi.class);

    @GET
    @Path("/organizations/{repository}")
        
    @Operation(summary = "Get organizations of repository.", description = "Get organizations of repository the current user is member. May returns an empty list.")
    
    @ApiResponses(
        	value = { 
    	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = OrganizationEntries.class))),
    	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    	    })
    
    public Response getOrganizations(
        	@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
        	@Parameter(description = "pattern",required=false) @QueryParam("pattern") String pattern,
    		@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS)) @QueryParam("maxItems") Integer maxItems,
    	    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
    	    @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    	    @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
    	    @Parameter(description = "search only in memberships, false can only be done by admin",schema = @Schema(defaultValue="true")) @QueryParam("onlyMemberships") boolean onlyMemberships,
    		@Context HttpServletRequest req) {
    	
    	try {
    		
    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);

	    	OrganizationEntries response = new OrganizationEntries();
	    	response.setCanCreate(AuthorityServiceFactory.getAuthorityService(repoDao.getId()).isGlobalAdmin());
	    	List<Organization> data = new ArrayList<>();
	    	SearchResult<EduGroup> result=SearchServiceFactory.getSearchService(repoDao.getId()).searchOrganizations( 
	    			pattern, 
	    			skipCount!=null ? skipCount : 0, 
	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS, 
	    			new SortDefinition(sortProperties,sortAscending),
	    			false,
	    			onlyMemberships);
	    	for (EduGroup eduGroup : result.getData()) {
				try {
					data.add(new OrganizationDao(repoDao,eduGroup).asOrganization());
				} catch(NullPointerException e) {
					logger.warn("Group " + eduGroup.getGroupname() + " as provided by search was causing NullPointerException", e);
					Organization org = new Organization();
					org.setAuthorityName(eduGroup.getGroupname());
					org.setRef(new NodeRef(RepositoryDao.getHomeRepository(), eduGroup.getGroupId()));
					org.setAdministrationAccess(AuthorityServiceHelper.isAdmin());
					data.add(org);
				}
	    	}
	    	response.setList(data);
	    	response.setPagination(new Pagination(result));
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }

	@GET
	@Path("/organizations/{repository}/{organization}")

	@Operation(summary = "Get organization by id.", description = "Get organization by id.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Organization.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getOrganization(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of organization",required=true) @PathParam("organization") String organization,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			return Response.status(Response.Status.OK).entity(OrganizationDao.getInstant(repoDao,organization).asOrganization()).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

    @OPTIONS    
    @Path("/organizations/{repository}")
    @Hidden

    public Response options01() {
    		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
    @PUT
    @Path("/organizations/{repository}/{organization}")
        
    @Operation(summary = "create organization in repository.", description = "create organization in repository.")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Organization.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    	})

    public Response createOrganizations(
        	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "organization name",required=true) @PathParam("organization") String organization,
    		@Parameter(description = "eduscope (may be null)",schema = @Schema(defaultValue=""),required=false) @QueryParam("eduscope") String scope,
    		@Context HttpServletRequest req) {
    	
    	try {


    		Organization group = (Organization)new MCAlfrescoAPIClient().doInTransaction(new RetryingTransactionCallback<Organization>() {

				@Override
				public Organization execute() throws Throwable {
					RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			    	Organization group = OrganizationDao.create(repoDao, organization,scope).asOrganization();
			    	return group;
				}
			});

	    	return Response.status(Response.Status.OK).entity(group).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    @DELETE
    @Path("/organizations/{repository}/{organization}")
        
    @Operation(summary = "Delete organization of repository.", description = "Delete organization of repository.")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    	})

    public Response deleteOrganizations(
        	@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "groupname",required=true) @PathParam("organization") String organization,
    		@Context HttpServletRequest req) {
    	
    	try {

	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	OrganizationDao organizationDao = OrganizationDao.getInstant(repoDao, organization);
	    	
	    	organizationDao.delete();
	    	
	    	return Response.status(Response.Status.OK).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    @DELETE
    @Path("/organizations/{repository}/{organization}/member/{member}")
        
    @Operation(summary = "Remove member from organization.", description = "Remove member from organization.")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    	})
    public Response removeFromOrganization(
    		@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "groupname",required=true) @PathParam("organization") String organization,
    		@Parameter(description = "authorityName of member",required=true ) @PathParam("member") String member,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	OrganizationDao organizationDao = OrganizationDao.getInstant(repoDao, organization);
	    	organizationDao.removeMember(member);
	    	return Response.status(Response.Status.OK).build();
	    	
    	}  catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }
    @OPTIONS    
    @Path("/organizations/{repository}/{organization}")
    @Hidden

    public Response options02() {
    		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT, DELETE").build();
    }
}

