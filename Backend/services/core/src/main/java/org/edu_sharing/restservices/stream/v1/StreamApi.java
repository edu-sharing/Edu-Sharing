package org.edu_sharing.restservices.stream.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.StreamDao;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.stream.v1.model.StreamEntryInput;
import org.edu_sharing.restservices.stream.v1.model.StreamList;
import org.edu_sharing.service.search.model.SortDefinition;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;


@Path("/stream/v1")
@Tag(name="STREAM v1")
@ApiService(value="STREAM", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class StreamApi {

	private static Logger logger = Logger.getLogger(StreamApi.class);
	
	 @POST
     @Path("/search/{repository}")
	        
     @Operation(summary = "Get the stream content for the current user with the given status.")
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = StreamList.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	    public Response search(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "Stream object status to search for",required=false ) @QueryParam("status") String status,
	    	@Parameter(description = "generic text to search for (in title or description)",required=false ) @QueryParam("query") String query,
		    @Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS)) @QueryParam("maxItems") Integer maxItems,
		    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
		    @Parameter(description = "map with property + value to search") Map<String,String> properties,
		    @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES+", currently supported: created, priority, default: priority desc, created desc") @QueryParam("sortProperties") List<String> sortProperties,
		    @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamList response = StreamDao.search(repoDao,status,properties,query, skipCount,maxItems,new SortDefinition(sortProperties,sortAscending));
		    	return Response.status(Response.Status.OK).entity(response).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 
	 @GET
     @Path("/properties/{repository}/{property}")
	        
     @Operation(summary = "Get top values for a property")
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(type ="object", additionalPropertiesSchema = Number.class))),
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	    public Response getPropertyValues(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "The property to aggregate",required=true) @PathParam("property") String property,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		Map<String, Number> response = StreamDao.getTopValues(repoDao,property);
		    	return Response.status(Response.Status.OK).entity(response).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 @GET
     @Path("/access/{repository}/{node}")
	        
     @Operation(summary = "test")
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Map.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	    public Response canAccess(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "The property to aggregate",required=true) @PathParam("node") String node,
			@Context HttpServletRequest req) {
	    	try {
	    		boolean response = StreamDao.canAccessNode(node);
		    	return Response.status(Response.Status.OK).entity(response).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 
	 @PUT
     @Path("/add/{repository}")
	        
     @Operation(summary = "add a new stream object.", description = "will return the object and add the id to the object if creation succeeded")
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = StreamEntryInput.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	    public Response addEntry(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "Stream object to add",required=true ) StreamEntryInput entry,
			@Context HttpServletRequest req) {
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamEntryInput result = StreamDao.addEntry(repoDao, entry);
		    	return Response.status(Response.Status.OK).entity(result).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 @PUT
     @Path("/status/{repository}/{entry}")
	        
     @Operation(summary = "update status for a stream object and authority")
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	    public Response updateEntry(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "entry id to update",required=true ) @PathParam("entry") String entryId,
	    	@Parameter(description = "authority to set/change status",required=true ) @QueryParam("authority") String authority,
	    	@Parameter(description = "New status for this authority",required=true ) @QueryParam("status") String status,
			@Context HttpServletRequest req) {
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamDao.updateStatus(repoDao, entryId, authority, status);
		    	return Response.status(Response.Status.OK).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 @DELETE
     @Path("/delete/{repository}/{entry}")
	        
     @Operation(summary = "delete a stream object", description = "the current user must be author of the given stream object")
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
	    public Response deleteEntry(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "entry id to delete",required=true ) @PathParam("entry") String entryId,
			@Context HttpServletRequest req) {
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamDao.deleteEntry(repoDao, entryId);
		    	return Response.status(Response.Status.OK).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
}
