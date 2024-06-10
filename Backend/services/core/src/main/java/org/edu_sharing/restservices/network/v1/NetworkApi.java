package org.edu_sharing.restservices.network.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.NetworkDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.network.v1.model.RepoEntries;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Repo;
import org.edu_sharing.service.network.model.Service;
import org.edu_sharing.service.network.model.StoredService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Path("/network/v1")
@Tag(name="NETWORK v1")
@ApiService(value="NETWORK", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class NetworkApi {

    private static Logger logger = Logger.getLogger(NetworkApi.class);

    @GET
    @Path("/repositories")
    @Operation(summary = "Get repositories.", description = "Get repositories.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = RepoEntries.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })

    public Response getRepositories(
            @Context HttpServletRequest req) {

        try {

            List<Repo> repos = new ArrayList<>();
            for (RepositoryDao repository : RepositoryDao.getRepositories()) {
                repos.add(repository.asRepo());
            }

            RepoEntries response = new RepoEntries();
            response.setList(repos);

            return Response.status(Response.Status.OK).entity(response).build();

        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }

    }

    @OPTIONS
    @Path("/repositories")
    @Hidden

    public Response options() {

        return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    @GET
    @Path("/service")
    @Operation(summary = "Get own service.", description = "Get the servic entry from the current repository.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = StoredService.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getService(
            @Context HttpServletRequest req
    ) {
        try {
            StoredService response = NetworkDao.getOwnService();
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
    @GET
    @Path("/services")
    @Operation(summary = "Get services.", description = "Get registerted services.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = StoredService[].class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getServices(
            @Context HttpServletRequest req,
            @Parameter(description = "search or filter for services", schema = @Schema(defaultValue=""), required = false) @QueryParam("query") String query
            ) {
        try {
            Collection<StoredService> response = NetworkDao.getServices();
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @POST
    @Path("/services")
    @Operation(summary = "Register service.", description = "Register a new service.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = StoredService.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response addService(
            @Context HttpServletRequest req,
            @Parameter(description = "Service data object") Service service
    ) {
        try {
            StoredService response = NetworkDao.addService(service);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
    @PUT
    @Path("/services/{id}")
    @Operation(summary = "Update a service.", description = "Update an existing service.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = StoredService.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response updateService(
            @Context HttpServletRequest req,
            @Parameter(description = "Service id", required=true ) @PathParam("id") String id,
            @Parameter(description = "Service data object") Service service
    ) {
        try {
            StoredService response = NetworkDao.updateService(id,service);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
}
