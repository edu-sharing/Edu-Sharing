package org.edu_sharing.restservices.sharing.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.sharing.v1.model.SharingInfo;
import org.edu_sharing.service.search.model.SortDefinition;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@SecurityRequirements
@Path("/sharing/v1")
@Tag(name="SHARING v1")
@ApiService(value="SHARING", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class SharingApi {


	private static Logger logger = Logger.getLogger(SharingApi.class);
	  @GET
	    @Path("/sharing/{repository}/{node}/{share}")
	        
	    @Operation(summary = "Get general info of a share.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SharingInfo.class))),
		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		    })

	    public Response getInfo(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = "Share token",required=true ) @PathParam("share") String token,
			@Parameter(description = "Password to validate (optional)", required = false, schema = @Schema(defaultValue="")) @QueryParam("password") String password,
			@Context HttpServletRequest req) {

		  try {
			  return AuthenticationUtil.runAsSystem(()-> {
				  RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				  return Response.status(Response.Status.OK).entity(SharingDao.getInfo(repoDao, node, token, password)).build();
			  });
		  } catch (Throwable t) {
			  return ErrorResponse.createResponse(t);
		  }
	  }
	@GET
	@Path("/sharing/{repository}/{node}/{share}/children")

	@Operation(summary = "Get all children of this share.", description = "Only valid for shared folders")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntries.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getChildren(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = "Share token",required=true ) @PathParam("share") String token,
			@Parameter(description = "Password (required if share is locked)", required = false, schema = @Schema(defaultValue="")) @QueryParam("password") String password,
			@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="500") ) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0") ) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Context HttpServletRequest req) {

		try {
			return AuthenticationUtil.runAsSystem(()->{
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
				List<NodeRef> children=SharingDao.getChildren(repoDao, node, token, password);
				List<String> filter=new ArrayList<>();
				filter.add("files");
				children=NodeDao.sortApiNodeRefs(repoDao,children,filter,sortDefinition);
				NodeEntries response = NodeDao.convertToRest(repoDao, Filter.createShowAllFilter(),children,skipCount==null ? 0 : skipCount,maxItems==null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems);
				return Response.status(Response.Status.OK).entity(response).build();
			});
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
}

