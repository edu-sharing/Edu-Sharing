package org.edu_sharing.restservices.rating.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.rating.Rating;
import org.edu_sharing.service.rating.RatingDetails;
import org.edu_sharing.service.rating.RatingHistory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

@Path("/rating/v1")
@Tag(name= "RATING v1" )
@ApiService(value = "RATING", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class RatingApi {
	private static Logger logger = Logger.getLogger(RatingApi.class);
	@PUT
	@Path("/ratings/{repository}/{node}")
	@Operation(summary = "create or update a rating", description = "Adds the rating. If the current user already rated that element, the rating will be altered")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
	public Response addOrUpdateRating(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@Parameter(description = "The rating (usually in range 1-5)",required=true) @QueryParam("rating") double rating,
	    	@Parameter(description = "Text content of rating",required=true) String comment,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	new RatingDao(repoDao).addOrUpdateRating(node, rating, comment);
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
	@DELETE
	@Path("/ratings/{repository}/{node}")
	@Operation(summary = "delete a comment", description = "Delete the comment with the given id")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
	public Response deleteRating(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	new RatingDao(repoDao).deleteRating(node);
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }

	@GET
	@Path("/ratings/{repository}/{node}/history")
	@Operation(summary = "get the range of nodes which had tracked actions since a given timestamp", description = "requires admin")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = RatingHistory[].class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getAccumulatedRatings(@Context HttpServletRequest req,
							  @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
							  @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
							  @Parameter(description = "date range from", required = false) @QueryParam("dateFrom") Long dateFrom
	) {
		try {
			if (!AuthorityServiceHelper.isAdmin()) {
				throw new NotAnAdminException();
			}
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<RatingHistory> ratings = new RatingDao(repoDao).getAccumulatedRatingHistory(node, dateFrom == null ? null : new Date(dateFrom));
			return Response.ok().entity(ratings).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/ratings/{repository}/nodes/altered")
	@Operation(summary = "get the range of nodes which had tracked actions since a given timestamp", description = "requires admin")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = String[].class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getNodesAlteredInRange(@Context HttpServletRequest req,
										   @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
										   @Parameter(description = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom
	) {
		try {
			if (!AuthorityServiceHelper.isAdmin()) {
				throw new NotAnAdminException();
			}
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<String> ratings = new RatingDao(repoDao).getAlteredNodes(new Date(dateFrom));
			return Response.ok().entity(ratings).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

}
