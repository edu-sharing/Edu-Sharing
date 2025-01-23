package org.edu_sharing.restservices.usage.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.UsageDao;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.usage.v1.model.CreateUsage;
import org.edu_sharing.restservices.usage.v1.model.Usages;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

@Path("/usage/v1")
@Tag(name= "USAGE v1" )
@ApiService(value = "USAGE", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class UsageApi {

	private static Logger logger = Logger.getLogger(UsageApi.class);

	@GET
	@Path("/usages/{appId}")

	@Operation(summary = "Get all usages of an application.", description = "Get all usages of an application.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Usages.class))),
			@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response getUsages(@Parameter(description = "ID of application (or \"-home-\" for home repository)", required = true) @PathParam("appId") String appId) {
		try {
			
			
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			
			if (appInfo == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			
			
			return Response.status(Response.Status.OK).entity(new Usages(new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsages(appId))).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/usages/{appId}")
	@Hidden
	public Response options1() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS,  GET").build();
	}

	@GET
	@Path("/usages/course/{appId}/{courseId}")

	@Operation(summary = "Get all usages of an course.", description = "Get all usages of an course.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Usages.class))),
			@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response getUsagesByCourse(
			@Parameter(description = "ID of application (or \"-home-\" for home repository)", required = true) @PathParam("appId") String appId,
			@Parameter(description = "ID of course", required = true) @PathParam("courseId") String courseId,
			@Context HttpServletRequest req) {
		try {
			
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			
			if (appInfo == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			
			return Response.status(Response.Status.OK).entity(new Usages(new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsagesByCourse(appId, courseId)))
					.build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/usages/course/{appId}/{courseId}")
	@Hidden
	public Response options2() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS,  GET").build();
	}
	
	
	@GET
	@Path("/usages/node/{nodeId}")

	@Operation(summary = "Get all usages of an node.", description = "Get all usages of an node.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Usages.class))),
			@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description="Resources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description="Fatal error occurred.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response getUsagesByNode(
			@Parameter(description = "ID of node", required = true) @PathParam("nodeId") String nodeId,
			@Context HttpServletRequest req) {
		try {
			return Response.status(Response.Status.OK).entity(new Usages(new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsagesByNode(nodeId)))
					.build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/usages/node/{nodeId}")
	@Hidden
	public Response options3() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS,  GET").build();
	}
	
	@GET
	@Path("/usages/node/{nodeId}/collections")

	@Operation(summary = "Get all collections where this node is used.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Collection[].class))),
			@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description="Resources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description="Fatal error occurred.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response getUsagesByNodeCollections(
			@Parameter(description = "ID of node", required = true) @PathParam("nodeId") String nodeId,
			@Context HttpServletRequest req) {
		try {
			java.util.Collection<Usages.CollectionUsage> collections = new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsagesByNodeCollection(nodeId);
			return Response.status(Response.Status.OK).entity(collections).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@DELETE
	@Path("/usages/node/{nodeId}/{usageId}")

	@Operation(summary = "Delete an usage of a node.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Usages.class))),
			@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description="Resources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description="Fatal error occurred.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response deleteUsage(
			@Parameter(description = "ID of node", required = true) @PathParam("nodeId") String nodeId,
			@Parameter(description = "ID of usage", required = true) @PathParam("usageId") String usageId,
			@Context HttpServletRequest req) {
		try {
			new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).deleteUsage(nodeId,usageId);
			return Response.status(Response.Status.OK).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@GET
	@Path("/usages/repository/{repositoryId}/{nodeId}")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Usages.NodeUsage[].class))),
			@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description="Resources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description="Fatal error occurred.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response getUsages(@Parameter(description = "ID of repository", required = true, schema = @Schema(defaultValue=RepositoryDao.HOME))  @PathParam("repositoryId") String repositoryId,
			@Parameter(description = "ID of node. Use -all- for getting usages of all nodes", required = true, schema = @Schema(defaultValue="-all-")) @PathParam("nodeId") String nodeId,
			@Parameter(description = "from date", required = false) @QueryParam("from") Long from,
			@Parameter(description = "to date", required = false) @QueryParam("to") Long to,
			@Context HttpServletRequest req) {
		
		try {
			RepositoryDao homeRepo = RepositoryDao.getRepository(RepositoryDao.HOME);
			if(RepositoryDao.HOME.equals(repositoryId)) {
				repositoryId = homeRepo.getId();
			}
			
			if("-all-".equals(nodeId)) {
				nodeId = null;
			}
			ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_USAGE_STATISTIC);
			
			List<Usages.NodeUsage> usages = new UsageDao(homeRepo).
					getUsages(repositoryId, nodeId, from, to);
			return Response.status(Response.Status.OK).entity(usages).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
		
	}

	@POST
	@Path("/usages/repository/{repositoryId}")

	@Operation(summary = "Set a usage for a node. app signature headers and authenticated user required.", description = "headers must be set: X-Edu-App-Id, X-Edu-App-Sig, X-Edu-App-Signed, X-Edu-App-Ts")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode = "200", description=RestConstants.HTTP_200,content = @Content(schema = @Schema(implementation = Usages.Usage.class)) ),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})
	public Response setUsage(@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repositoryId") String repository,
							 @Parameter(description = " usage date",required = true) CreateUsage usage){
		try {
			RepositoryDao homeRepo = RepositoryDao.getRepository(RepositoryDao.HOME);
			if(RepositoryDao.HOME.equals(repository)) {
				repository = homeRepo.getId();
			}

			Usages.Usage result = new UsageDao(homeRepo).setUsage(repository,usage);
			return Response.status(Response.Status.OK).entity(result).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	
}
