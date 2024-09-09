package org.edu_sharing.restservices.rendering.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tracking.TrackingTool;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.rendering.v1.model.RenderingDetailsEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.rendering.RenderingDetails;
import org.edu_sharing.service.rendering.RenderingTool;
import org.edu_sharing.service.repoproxy.RepoProxy;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.tracking.NodeTrackingDetails;
import org.edu_sharing.service.tracking.TrackingService;

import java.util.Arrays;
import java.util.Map;


@Path("/rendering/v1")
@Tag(name="RENDERING v1")
@ApiService(value="RENDERING", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class RenderingApi {

	private static Logger logger = Logger.getLogger(RenderingApi.class);
	
	@GET
    @Path("/details/{repository}/{node}")
    
	
	 @Operation(summary = "Get metadata of node.", description = "Get metadata of node.")
		    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = RenderingDetailsEntry.class))),
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })


	/**
	 * @Deprecated
	 * use getDetailsSnippetWithParameters instead
	 */
	public Response getDetailsSnippet(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "ID of node",required=true ) @PathParam("node") String node,
	    	@Parameter(description = "version of node",required=false) @QueryParam("version") String nodeVersion,
	    	@Parameter(description = "Rendering displayMode", required=false) @QueryParam("displayMode") String displayMode,
			@Context HttpServletRequest req){
		return getDetailsSnippetWithParameters(repository, node, nodeVersion, displayMode, null, req);
	}
	
	
	@POST
    @Path("/details/{repository}/{node}")
    
	
	 @Operation(summary = "Get metadata of node.", description = "Get metadata of node.")
		    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = RenderingDetailsEntry.class))),
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
	
	public Response getDetailsSnippetWithParameters(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "ID of node",required=true ) @PathParam("node") String node,
	    	@Parameter(description = "version of node",required=false) @QueryParam("version") String nodeVersion,
			@Parameter(description = "Rendering displayMode", required=false) @QueryParam("displayMode") String displayMode,
			// options include: showDownloadButton, showDownloadAdvice, metadataGroup
			@Parameter(description = "additional parameters to send to the rendering service",required=false) Map<String,String> parameters,
			@Context HttpServletRequest req){

		try {
			RepoProxy.RemoteRepoDetails remote = RepoProxyFactory.getRepoProxy().myTurn(repository, node);


			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			if (repoDao == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			if(remote != null) {
				org.edu_sharing.generated.repository.backend.services.rest.client.model.RenderingDetailsEntry entity = (org.edu_sharing.generated.repository.backend.services.rest.client.model.RenderingDetailsEntry) RepoProxyFactory.getRepoProxy().getDetailsSnippetWithParameters(remote.getRepository(), remote.getNodeId(), nodeVersion, displayMode, parameters, req).getEntity();
				return Response.status(Response.Status.OK).entity(entity).build();
			} else {
				RenderingDetails detailsSnippet = new RenderingDao(repoDao).getDetails(node, nodeVersion, displayMode, parameters);
				if(detailsSnippet.getException() != null) {
					if(detailsSnippet.getException().getNested() != null)  {
						DAOException mapped = DAOException.mapping(detailsSnippet.getException().getNested());
						if(mapped instanceof DAOMissingException) {
							throw mapped;
						}
					}
				}
				if (repoDao.isHomeRepo()) {
					NodeTrackingDetails details = (NodeTrackingDetails) org.edu_sharing.alfresco.repository.server.authentication.
							Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.SESSION_RENDERING_DETAILS);
					if (details == null || !details.getNodeId().equals(node)) {
						details = new NodeTrackingDetails(node, nodeVersion);
					} else {
						details.setNodeVersion(nodeVersion);
						org.edu_sharing.alfresco.repository.server.authentication.
								Context.getCurrentInstance().getRequest().getSession().removeAttribute(CCConstants.SESSION_RENDERING_DETAILS);
					}
					if (Arrays.asList(RenderingTool.DISPLAY_DYNAMIC, RenderingTool.DISPLAY_CONTENT).contains(displayMode) || displayMode == null) {
						TrackingTool.trackActivityOnNode(node, details, TrackingService.EventType.VIEW_MATERIAL);
					} else if (RenderingTool.DISPLAY_INLINE.equals(displayMode)) {
						TrackingTool.trackActivityOnNode(node, details, TrackingService.EventType.VIEW_MATERIAL_EMBEDDED);
					}
				}

				RenderingDetailsEntry response = new RenderingDetailsEntry();
				response.setDetailsSnippet(detailsSnippet.getDetails());
				if(detailsSnippet.getRenderingServiceData() != null) {
					String mimeType = detailsSnippet.getRenderingServiceData().getNode().getMimetype();
					response.setMimeType(mimeType);
					response.setNode(detailsSnippet.getRenderingServiceData().getNode());
				}

				return Response.status(Response.Status.OK).entity(response).build();
			}
		}catch (Throwable t) {
			logger.error(t.getMessage(), t);
			return ErrorResponse.createResponse(t);
		}

	}
}
