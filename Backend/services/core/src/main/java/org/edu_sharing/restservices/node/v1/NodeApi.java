package org.edu_sharing.restservices.node.v1;

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
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.NodeRefVersion;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LRMITool;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.node.v1.model.SearchResult;
import org.edu_sharing.restservices.node.v1.model.*;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.clientutils.ClientUtilsService;
import org.edu_sharing.service.clientutils.WebsiteInformation;
import org.edu_sharing.service.editlock.EditLockServiceFactory;
import org.edu_sharing.service.editlock.LockedException;
import org.edu_sharing.service.nodeservice.AssocInfo;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.permission.HandleMode;
import org.edu_sharing.service.permission.HandleParam;
import org.edu_sharing.service.repoproxy.RepoProxy;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SharedToMeType;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.share.ShareService;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Path("/node/v1")
@Tag(name="NODE v1")
@ApiService(value="NODE", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class NodeApi  {

	
	private static Logger logger = Logger.getLogger(NodeApi.class);
	  @GET
	    @Path("/nodes/{repository}/{node}/workflow")
	        
	    @Operation(summary = "Get workflow history.", description = "Get workflow history of node.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = WorkflowHistory[].class))),        
		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		    })

	    public Response getWorkflowHistory(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		
	    		
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	
		    	return Response.status(Response.Status.OK).entity(nodeDao.getWorkflowHistory()).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);    		
	    	}

	    }

	@GET
	@Path("/nodes/{repository}/{node}/lrmi")
	@Operation(summary = "Get lrmi data.", description = "Get lrmi data of node.")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = JSONObject.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getLrmiData(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = "Version of the node" ) @QueryParam("version") String version,
			@Context HttpServletRequest req) {

		try {
			org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node);


			return Response.status(Response.Status.OK).entity(LRMITool.getLRMIJson(new NodeRefVersion(nodeRef,version)).toString()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	  @POST
	    @Path("/nodes/{repository}/{node}/report")
	        
	    @Operation(summary = "Report the node.", description = "Report a node to notify the admin about an issue)")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		    })

	    public Response reportNode(
			  @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			  @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			  @Parameter(description = "the mode of reporting (reporting or feedback)",required=false ) @QueryParam("mode")NotificationService.NotifyMode mode,
			  @Parameter(description = "the reason for the report",required=true ) @QueryParam("reason") String reason,
			  @Parameter(description = "mail of reporting user",required=true ) @QueryParam("userEmail") String userEmail,
			  @Parameter(description = "additional user comment",required=false ) @QueryParam("userComment") String userComment,
			  @Context HttpServletRequest req) {
	    	
	    	try {
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.reportNode(mode == null ? NotificationService.NotifyMode.ReportProblem : mode, reason,userEmail,userComment);
		    	
		    	return Response.status(Response.Status.OK).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);    		
	    	}

	    }
	  @PUT
	    @Path("/nodes/{repository}/{node}/workflow")
	        
	    @Operation(summary = "Add workflow.", description = "Add workflow entry to node.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		    })

	    public Response addWorkflowHistory(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@Parameter(description = "The history entry to put (editor and time can be null and will be filled automatically)", required = true) WorkflowHistory entry,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    			    		
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.addWorkflowHistory(entry, true);
		    	return Response.status(Response.Status.OK).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);    		
	    	}

	    }

	@POST
	@Path("/nodes/{repository}/{node}/publish")

	@Operation(summary = "Publish", description = "Create a published copy of the current node ")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response publishCopy(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = "handle mode, if a handle should be created. Skip this parameter if you don't want an handle",required=false ) @QueryParam("handleMode") HandleMode handleMode,
			@Parameter(description = "handle parameter, if a handle and/or doi should be created. Skip this parameter if you don't want a handle or doi,",required = false) HandleParam handleParam,
			@Context HttpServletRequest req) {

		try {
			if(handleParam == null){
				if(handleMode != null){
					handleParam = HandleParam.builder()
							.doiService(handleMode)
							.handleService(handleMode)
							.build();
				}
			}
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao published = nodeDao.publishCopy(handleParam);
			NodeEntry response = new NodeEntry();
			response.setNode(published.asNode());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	@DELETE
	@Path("/nodes/{repository}/{node}/publish/revoke")

	@Operation(summary = "Revoke published copy", description = "Revoke a previously published copy. The content of this copy will be irrevocable removed, only the metadata will remain")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Node.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response revokeCopy(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) RevokeDetails details,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node, Filter.createShowAllFilter());
			NodeDao response = nodeDao.revokeNode(details);
			return Response.status(Response.Status.OK).entity(response.asNode()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	@GET
	@Path("/nodes/{repository}/{node}/publish")

	@Operation(summary = "Publish", description = "Get all published copies of the current node")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntries.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getPublishedCopies(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao originalDao = NodeDao.getNode(repoDao, node);
			List<NodeDao> published = originalDao.getPublishedCopies();
			NodeEntries response = new NodeEntries();
			response.setNodes(published.stream().map(dao -> {
				try {
					return dao.asNode();
				} catch (DAOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList()));

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@PUT
	    @Path("/nodes/{repository}/{node}/aspects")
	        
	    @Operation(summary = "Add aspect to node.", description = "Add aspect to node.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),        
		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		    })

	    public Response addAspects(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@Parameter(description = "aspect name, e.g. ccm:lomreplication",required=true) List<String> aspects,
			@Context HttpServletRequest req) {
	    	
	    	try {
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.addAspects(aspects);
		    	NodeEntry response = new NodeEntry();
		    	response.setNode(nodeDao.asNode());
		    	
		    	return Response.status(Response.Status.OK).entity(response).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);    		
	    	}

	    }
	  
    @GET
    @Path("/nodes/{repository}/{node}/metadata")
        
    @Operation(summary = "Get metadata of node.", description = "Get metadata of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getMetadata(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-")) ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {
    	
    	try {
			RepoProxy.RemoteRepoDetails remote = RepoProxyFactory.getRepoProxy().myTurn(repository, node);
			if(remote != null) {
				return RepoProxyFactory.getRepoProxy().getMetadata(remote.getRepository(), remote.getNodeId(), propertyFilter, req);
			}
    		Filter filter = new Filter(propertyFilter);
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node, filter);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(nodeDao.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);    		
    	}

    }


    @GET
    @Path("/nodes/{repository}/{node}/lock/unlock")
        
    @Operation(summary = "unlock node.", description = "unlock node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response unlock(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Context HttpServletRequest req) {
    	
    	try{
    		EditLockServiceFactory.getEditLockService().unlock(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,node));
    	}catch(LockedException e){
    		return ErrorResponse.createResponse(e);    		
    	}
    	return Response.ok().build();
    }
    
    @GET
    @Path("/nodes/{repository}/{node}/lock/status")
        
    @Operation(summary = "locked status of a node.", description = "locked status of a node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeLocked.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response islocked(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Context HttpServletRequest req) {
		try {
			boolean isLocked = EditLockServiceFactory.getEditLockService().isLockedByAnotherUser(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node));
			NodeLocked response = new NodeLocked();
			response.setLocked(isLocked);

			return Response.ok().entity(response).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    
    
    @GET
    @Path("/nodes/{repository}/{node}/textContent")
        
    @Operation(summary = "Get the text content of a document.", description = "May fails with 500 if the node can not be read.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeText.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getTextContent(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
		@Context HttpServletRequest req) {
    	
    	try {
    		    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	
	    	NodeText response = new NodeText();
	    	response.setText(((MCAlfrescoAPIClient)repoDao.getBaseClient()).getNodeTextContent(node,MimetypeMap.MIMETYPE_TEXT_PLAIN));
	    	try{
	    		response.setHtml(((MCAlfrescoAPIClient)repoDao.getBaseClient()).getNodeTextContent(node,MimetypeMap.MIMETYPE_HTML));
	    	}catch(Throwable t){}
	    	try{
	    		InputStream is=((MCAlfrescoAPIClient)repoDao.getBaseClient()).getContent(node);
	    		if(is.available()<1024*1024*5)
	    			response.setRaw(IOUtils.toString(is));
	    	}catch(Throwable t){}
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);    		
    	}

    }
    @GET
    @Path("/nodes/{repository}/{node}/parents")
        
    @Operation(summary = "Get parents of node.", description = "Get all parents metadata + own metadata of node. Index 0 is always the current node")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ParentEntries.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getParents(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
    	@Parameter(description = "activate to return the full alfresco path, otherwise the path for the user home is resolved") @QueryParam("fullPath") Boolean fullPath,
		@Context HttpServletRequest req) {
    	
    	try {
    		
    		Filter filter = new Filter(propertyFilter);
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	if("-inbox-".equals(node)){
    			node = repoDao.getUserInbox(true);
    		}
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node, filter);
	    	
	    	ParentEntries response=new ParentEntries();
	    	List<Node> parents=new ArrayList<>();
	    	Node last=nodeDao.asNode();
	    	parents.add(last);
	    	String userHome=repoDao.getUserHome();
			if(last.getRef().getId().equals(userHome)) {
				response.setNodes(new ArrayList<>());
				response.setScope("MY_FILES");
			} else {
				List<NodeRef> shared = PersonDao.getPerson(repoDao, PersonDao.ME).asPerson().getSharedFolders();
				boolean collection = last.getMediatype().equals("collection");
				if (fullPath == null)
					fullPath = false;
				while (true) {
					if (last == null || last.getParent() == null || last.getParent().getId() == null)
						break;
					if (!fullPath) {
						if (last.getParent().getId().equals(userHome)) {
							response.setScope("MY_FILES");
							break;
						}
						if ((shared != null && shared.contains(last.getRef()))) {
							response.setScope("SHARED_FILES");
							break;
						}
					}
					if (collection && !fullPath) {
						Node finalLast = last;
						last = AuthenticationUtil.runAsSystem(() -> NodeDao.getNode(repoDao, finalLast.getParent().getId(), filter).asNode());
						if (!last.getMediatype().equals("collection")) {
							response.setScope("COLLECTION");
							break;
						}
					} else {
						last = NodeDao.getNode(repoDao, last.getParent().getId(), filter).asNode();
					}
					parents.add(last);
				}

				response.setNodes(parents);
			}
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);    		
    	}

    }
    
    @PUT
    @Path("/nodes/{repository}/{node}/metadata")    
    
    @Operation(summary = "Change metadata of node.", description = "Change metadata of node.")
    
    @ApiResponses(
    	value = { 
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		})

    public Response changeMetadata(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "properties" ,required=true ) HashMap<String, String[]> properties,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changeProperties(properties);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }

	@PUT
	@Path("/nodes/{repository}/{node}/metadata/copy/{from}")

	@Operation(summary = "Copy metadata from another node.", description = "Copies all common metadata from one note to another. Current user needs write access to the target node and read access to the source node.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response copyMetadata(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = "The node where to copy the metadata from",required=true ) @PathParam("from") String from,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao fromDao = NodeDao.getNode(repoDao, from);
			NodeDao newNode = nodeDao.copyProperties(fromDao);

			NodeEntry response = new NodeEntry();
			response.setNode(newNode.asNode());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}


	@GET
	@Path("/nodes/{repository}/{node}/metadata/template")

	@Operation(summary = "Get the metadata template + status for this folder.", description = "All the given metadata will be inherited to child nodes.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getTemplateMetadata(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);


			NodeTemplateEntry response = new NodeTemplateEntry();
			if(nodeDao.getTemplateNode()!=null)
				response.setNode(nodeDao.getTemplateNode().asNode());
			response.setEnabled(nodeDao.getTemplateStatus());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {

			return ErrorResponse.createResponse(t);
		}
	}
	@PUT
	@Path("/nodes/{repository}/{node}/metadata/template")

	@Operation(summary = "Set the metadata template for this folder.", description = "All the given metadata will be inherited to child nodes.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response changeTemplateMetadata(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = "Is the inherition currently enabled",required=true ) @QueryParam("enable") Boolean enable,
			@Parameter(description = "properties" ,required=true ) HashMap<String, String[]> properties,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);

			NodeDao templateNode = nodeDao.changeTemplateProperties(enable,properties);

			NodeTemplateEntry response = new NodeTemplateEntry();
			if(nodeDao.getTemplateNode()!=null)
				response.setNode(templateNode.asNode());
			response.setEnabled(nodeDao.getTemplateStatus());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {

			return ErrorResponse.createResponse(t);
		}
	}


	@POST
    @Path("/nodes/{repository}/{node}/metadata")    
    
    @Operation(summary = "Change metadata of node (new version).", description = "Change metadata of node (new version).")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),	        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    	})

    public Response changeMetadataWithVersioning(
	    @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "comment",required=true) @QueryParam("versionComment") String versionComment,
	    @Parameter(description = "properties" ,required=true ) HashMap<String, String[]> properties,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changePropertiesWithVersioning(properties, versionComment);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    
    @OPTIONS    
    @Path("/nodes/{repository}/{node}/metadata")
    @Hidden

    public Response options01() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, POST, PUT").build();
    }
    
    @DELETE
    @Path("/nodes/{repository}/{node}")    
    
    @Operation(summary = "Delete node.", description = "Delete node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response delete(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Parameter(description = "move the node to recycle",schema = @Schema(defaultValue="true"),required=false) @QueryParam("recycle") Boolean recycle,
    	@Parameter(description = "protocol",schema = @Schema(defaultValue=""),required=false) @QueryParam("protocol") String protocol,
    	@Parameter(description = "store",schema = @Schema(defaultValue=""),required=false) @QueryParam("store") String store,
		@Context HttpServletRequest req) {
    	
    	try {
    		
    		if(protocol != null && !protocol.trim().equals("") 
    				&& store != null && !store.trim().equals("")) {
    			
    			if(!ApplicationInfoList.getHomeRepository().getUsername().equals(AuthenticationUtil.getFullyAuthenticatedUser())){
    				throw new Exception("admin user required when trying to delete node of another store");
    			}
    			
    			NodeDao.delete(protocol, store, node);
    			return Response.status(Response.Status.OK).build();
    		
    		}
    		
    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	nodeDao.delete(recycle==null ? true : recycle);
	    	
	    	return Response.status(Response.Status.OK).build();

    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}

    }
    
    @OPTIONS    
    @Path("/nodes/{repository}/{node}")
    @Hidden

    public Response options02() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, DELETE").build();
    }

    @GET
    @Path("/nodes/{repository}/{node}/children")    
    
    @Operation(summary = "Get children of node.", description = "Get children of node.")
    
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
	    @Parameter(description = "ID of parent node (or \"-userhome-\" for home directory of current user, \"-shared_files-\" for shared folders, \"-to_me_shared_files\" for shared files for the user,\"-my_shared_files-\" for files shared by the user, \"-inbox-\" for the inbox, \"-workflow_receive-\" for files assigned by workflow, \"-saved_search-\" for saved searches of the user)",required=true ) @PathParam("node") String node,
	    @Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="500") ) @QueryParam("maxItems") Integer maxItems,
	    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0") ) @QueryParam("skipCount") Integer skipCount,
	    @Parameter(description = RestConstants.MESSAGE_FILTER) @QueryParam("filter") List<String> filter,
	    @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
	    @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
	    @Parameter(description = "Filter for a specific association. May be empty", required = false, schema = @Schema(defaultValue="")) @QueryParam("assocName") String assocName,
	    @Parameter(description = RestConstants.MESSAGE_PROPERTY_FILTER, array = @ArraySchema(schema = @Schema(defaultValue="-all-")) ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {

    	try {
			RepoProxy.RemoteRepoDetails remote = RepoProxyFactory.getRepoProxy().myTurn(repository, node);
			if(remote != null) {
				return RepoProxyFactory.getRepoProxy().getChildren(remote.getRepository(), remote.getNodeId(), maxItems, skipCount, filter, sortProperties, sortAscending, assocName, propertyFilter, req);
			}

    		Filter propFilter = new Filter(propertyFilter);
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	node=NodeDao.mapNodeConstants(repoDao,node, false);

			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);

			NodeEntries response=null;
			List<NodeRef> children=null;
			if(node == null) {
				response = new NodeEntries();
			} else if("-shared_files-".equals(node)){
		    	User person = PersonDao.getPerson(repoDao, PersonDao.ME).asPerson();
		    	children = person.getSharedFolders();
		    	List<org.alfresco.service.cmr.repository.NodeRef> converted=NodeDao.convertApiNodeRef(children);
                children=NodeDao.convertAlfrescoNodeRef(repoDao,NodeDao.sortAlfrescoRefs(converted,filter,sortDefinition));
            }
	    	else if("-my_shared_files-".equals(node)){
				response = searchResultToResponse(NodeDao.getFilesSharedByMe(repoDao, filter, propFilter, sortDefinition, skipCount, maxItems));
			}
	    	else if("-workflow_receive-".equals(node)){
				response = searchResultToResponse(NodeDao.getWorkflowReceive(repoDao,filter,sortDefinition, skipCount, maxItems));
	    	}
	    	else if("-to_me_shared_files-".equals(node)){
				response = searchResultToResponse(NodeDao.getFilesSharedToMe(repoDao, SharedToMeType.All, filter, propFilter,sortDefinition,skipCount,maxItems));
			}
			else if("-to_me_shared_files_personal-".equals(node)){
				response = searchResultToResponse(NodeDao.getFilesSharedToMe(repoDao, SharedToMeType.Private, filter, propFilter,sortDefinition,skipCount,maxItems));
			}
	    	else if("-frontpage-".equals(node)){
				response = searchResultToResponse(NodeDao.getFrontpageNodes(repoDao));
			}
	    	else{
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node, propFilter);
	    		children = nodeDao.getChildren(assocName,filter,sortDefinition);
            }
	    	if(response==null)
            	response=NodeDao.convertToRest(repoDao,propFilter,children,skipCount==null ? 0 : skipCount,maxItems==null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems);
			//List<Node> sorted=NodeDao.sortAndFilterByType(repoDao,children,sortDefinition,filter,propFilter);
	    	//Collections.sort(children);
			//NodeEntries response=createResponseFromNodeList(sorted,skipCount,maxItems);
	    
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }

	public NodeEntries searchResultToResponse(org.edu_sharing.restservices.shared.SearchResult<NodeDao> data) throws DAOException {
		NodeEntries response;
		response=new NodeEntries();
		response.setPagination(data.getPagination());
		List<Node> list = new ArrayList<>();
		for (NodeDao nodeDao : data.getNodes()) {
			Node asNode = nodeDao.asNode();
			list.add(asNode);
		}
		response.setNodes(list);
		return response;
	}

	@GET
	@Path("/nodes/{repository}/{node}/assocs")

	@Operation(summary = "Get related nodes.", description = "Get nodes related based on an assoc.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntries.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getAssocs(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="500") ) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0") ) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "Either where the given node should be the \"SOURCE\" or the \"TARGET\"",required = true) @QueryParam("direction") AssocInfo.Direction direction,
			@Parameter(description = "Association name (e.g. ccm:forkio).") @QueryParam("assocName") String assocName,
			@Parameter(description = RestConstants.MESSAGE_PROPERTY_FILTER, array = @ArraySchema(schema = @Schema(defaultValue="-all-")) ) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			Filter propFilter = new Filter(propertyFilter);

			NodeEntries response=new NodeEntries();
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);
			List<NodeRef> children;

            SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);

            NodeDao nodeDao = NodeDao.getNode(repoDao, node, propFilter);
			children = nodeDao.getAssocs(new AssocInfo(direction,assocName),null,sortDefinition);
            response=NodeDao.convertToRest(repoDao,propFilter,children,skipCount==null ? 0 : skipCount,maxItems==null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems);

			return Response.status(Response.Status.OK).entity(response).build();

		}
		catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
    @DELETE
    @Path("/nodes/{repository}/{node}/shares/{shareId}")    
    
    @Operation(summary = "Remove share of a node.", description = "Remove the specified share id")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response removeShare(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "share id",required=true ) @PathParam("shareId") String shareId,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao=NodeDao.getNode(repoDao, node);
	    	
	    	nodeDao.removeShare(shareId);
	    	return Response.status(Response.Status.OK).build();
    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }    
    @POST
    @Path("/nodes/{repository}/{node}/shares/{shareId}")    
    
    @Operation(summary = "update share of a node.", description = "update the specified share id")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeShare.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response updateShare(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "share id",required=true ) @PathParam("shareId") String shareId,
	    @Parameter(description = "expiry date for this share, leave empty or -1 for unlimited", required = false, schema = @Schema(defaultValue=""+ShareService.EXPIRY_DATE_UNLIMITED )) @QueryParam("expiryDate") Long expiryDate,
	    @Parameter(description = "new password for share, leave empty if you don't want to change it", required = false, schema = @Schema(defaultValue="")) @QueryParam("password") String password,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao=NodeDao.getNode(repoDao, node);
	    	
	    	NodeShare response=nodeDao.updateShare(shareId,expiryDate==null?ShareService.EXPIRY_DATE_UNLIMITED:expiryDate,password);
	    	return Response.status(Response.Status.OK).entity(response).build();
    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }  
    @POST
    @Path("/nodes/{repository}/{node}/import")    
    
    @Operation(summary = "Import node", description = "Import a node from a foreign repository to the local repository.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response importNode(
    	@Parameter(description = "The id of the foreign repository",required=true) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "Parent node where to store it locally, may also use -userhome- or -inbox-",required=true ) @QueryParam("parent") String parent,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	if(repoDao.isHomeRepo()){
	    		throw new InvalidArgumentException("Import can be only done for a foreign repository");
	    	}
	    	
	    	RepositoryDao repoDaoHome = RepositoryDao.getRepository(RepositoryDao.HOME);
			node=NodeDao.mapNodeConstants(repoDaoHome,node);
			parent=NodeDao.mapNodeConstants(repoDaoHome,parent);

			NodeDao nodeDao=NodeDao.getNode(repoDao, node).importNode(parent);
	    	NodeEntry response=new NodeEntry();
	    	response.setNode(nodeDao.asNode());
	    	return Response.status(Response.Status.OK).entity(response).build();

    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }    
    @GET
    @Path("/nodes/{repository}/{node}/shares")    
    
    @Operation(summary = "Get shares of node.", description = "Get list of shares (via mail/token) for a node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeShare[].class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getShares(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "Filter for a specific email or use "+ShareService.EMAIL_TYPE_LINK+" for link shares (Optional)",required=false) @QueryParam("email") String email,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao=NodeDao.getNode(repoDao, node);
	    	List<NodeShare> response=nodeDao.getShares(email);
	    	return Response.status(Response.Status.OK).entity(response).build();

    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }    
    @PUT
    @Path("/nodes/{repository}/{node}/shares")    
    
    @Operation(summary = "Create a share for a node.", description = "Create a new share for a node")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeShare.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response createShare(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "expiry date for this share, leave empty or -1 for unlimited", required = false, schema = @Schema(defaultValue=""+ShareService.EXPIRY_DATE_UNLIMITED )) @QueryParam("expiryDate") Long expiryDate,
	    @Parameter(description = "password for this share, use none to not use a password", required = false, schema = @Schema(defaultValue="")) @QueryParam("password") String password,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao=NodeDao.getNode(repoDao, node);
	    	NodeShare response=nodeDao.createShare(expiryDate==null ? ShareService.EXPIRY_DATE_UNLIMITED : expiryDate,password);
	    	return Response.status(Response.Status.OK).entity(response).build();

    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }    
    public static NodeEntries createResponseFromNodeList(List<Node> sorted, Integer skipCount, Integer maxItems) {
		NodeEntries response = new NodeEntries();
    	int min = (skipCount != null) ? Math.min(sorted.size(), skipCount) : 0;
    	int max = (maxItems != null) ? Math.min(sorted.size(), min + maxItems) : sorted.size();   
    		
    	List<Node> data = new ArrayList<>();
    	for (Node child : sorted.subList(min, max)) {
    		data.add(child);
    	}
    	
    	Pagination pagination = new Pagination();
    	pagination.setFrom(min);
    	pagination.setCount(data.size());
    	pagination.setTotal(sorted.size());
    	
    	response.setNodes(data);
    	response.setPagination(pagination);
    	return response;
	}


	

	@POST
    @Path("/nodes/{repository}/{node}/children")    
    
    @Operation(summary = "Create a new child.", description = "Create a new child.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response createChild(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_PARENT_NODE+" use -userhome- for userhome or -inbox- for inbox node",required=true ) @PathParam("node") String node,
	    @Parameter(description = "type of node",required=true ) @QueryParam("type") String type,
	    @Parameter(description = "aspects of node" ) @QueryParam("aspects") List<String> aspects,
	    @Parameter(description = "rename if the same node name exists", required = false, schema = @Schema(defaultValue="false")) @QueryParam("renameIfExists") Boolean renameIfExists,
	    @Parameter(description = "comment, leave empty = no inital version", required=false ) @QueryParam("versionComment")  String versionComment,
	    @Parameter(description = "properties, example: {\"{http://www.alfresco.org/model/content/1.0}name\": [\"test\"]}" , required=true ) HashMap<String, String[]> properties,	    
	    @Parameter(description = "Association type, can be empty" , required=false ) @QueryParam("assocType") String assocType,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	resolveURLTitle(properties);
	    	NodeDao child = nodeDao.createChild(type, aspects, properties,
	    			renameIfExists==null ? false : renameIfExists.booleanValue(),
					assocType!=null && !assocType.trim().isEmpty() ? assocType : null);

			if(versionComment!=null && !versionComment.isEmpty()){
				child.createVersion(versionComment);
			}
	    	
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(child.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }


	@POST
	@Path("/nodes/{repository}/{node}/xapi")

	@Operation(summary = "Store xApi-Conform data for a given node")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Object.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response storeXApiData(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = "xApi conform json data",required=true ) String xApi,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			String result = nodeDao.storeXApiData(xApi);
			return Response.status(Response.Status.OK).entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	public void resolveURLTitle(HashMap<String, String[]> properties) {
		String[] url=(String[])properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_WWWURL));
		if(url==null)
			return;
		// Don't resolve url if name is already given by client
		if(properties.get(CCConstants.getValidLocalName(CCConstants.CM_NAME))!=null) {
			properties.put(CCConstants.getValidLocalName(CCConstants.CM_NAME),
					new String[]{NodeServiceHelper.cleanupCmName(properties.get(CCConstants.getValidLocalName(CCConstants.CM_NAME))[0])});
			return;
		}
		 WebsiteInformation info=ClientUtilsService.getWebsiteInformation(url[0]);
		 if(info==null){
		     properties.put(CCConstants.getValidLocalName(CCConstants.CM_NAME), new String[]{NodeServiceHelper.cleanupCmName(url[0])});
		     properties.put(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_TITLE),url);
			 return;
		 }
		 String title=info.getTitle();
		 if(info.getTitle()==null) {
			 title = info.getPage();
		 }
		 if(title == null || title.trim().isEmpty()){
		 	title = url[0];
		 }
	    properties.put(CCConstants.getValidLocalName(CCConstants.CM_NAME), new String[]{NodeServiceHelper.cleanupCmName(title)});
	    properties.put(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_TITLE),new String[]{title});
	    if(info.getDescription()!=null)
	    	properties.put(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_DESCRIPTION),new String[]{info.getDescription()});
	    if(info.getKeywords()!=null)
	    	properties.put(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_KEYWORD),info.getKeywords());
	    if(info.getLicense()!=null){
	    	properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY),new String[]{info.getLicense().getName()});
	    	if(info.getLicense().getCcVersion()!=null)
	    		properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION),new String[]{info.getLicense().getCcVersion()});
		}
	    if(info.getLrmiProperties()!=null){
	    	properties.putAll(info.getLrmiProperties());
		}
	}
	@OPTIONS    
    @Path("/nodes/{repository}/{node}/children")
    @Hidden

    public Response options03() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, POST").build();
    }
	@POST
	@Path("/nodes/{repository}/{node}/children/_fork")

	@Operation(summary = "Create a copy of a node by creating a forked version (variant).")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response createForkOfNode(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_PARENT_NODE,required=true ) @PathParam("node") String node,
			@Parameter(description = RestConstants.MESSAGE_SOURCE_NODE,required=true) @QueryParam("source") String source,
			@Parameter(description = "flag for children",required=true) @QueryParam("withChildren") boolean withChildren,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			node=NodeDao.mapNodeConstants(repoDao,node);
			source=NodeDao.mapNodeConstants(repoDao,source);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao child = nodeDao.createFork(source);

			NodeEntry response = new NodeEntry();
			response.setNode(child.asNode());

			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@POST
    @Path("/nodes/{repository}/{node}/children/_copy")    
    
    @Operation(summary = "Create a new child by copying.", description = "Create a new child by copying.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response createChildByCopying(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_PARENT_NODE,required=true ) @PathParam("node") String node,
	    @Parameter(description = RestConstants.MESSAGE_SOURCE_NODE,required=true) @QueryParam("source") String source,
	    @Parameter(description = "flag for children",required=true) @QueryParam("withChildren") boolean withChildren,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			node=NodeDao.mapNodeConstants(repoDao,node);
			source=NodeDao.mapNodeConstants(repoDao,source);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao child = nodeDao.createChildByCopy(source, withChildren);
	    			
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(child.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {	
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    @OPTIONS    
    @Path("/nodes/{repository}/{node}/children/_copy")
    @Hidden

    public Response options04() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
    }
    
    @POST
    @Path("/nodes/{repository}/{node}/children/_move")    
    
    @Operation(summary = "Create a new child by moving.", description = "Create a new child by moving.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response createChildByMoving(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
		@Parameter(description = RestConstants.MESSAGE_PARENT_NODE,required=true ) @PathParam("node") String node,
		@Parameter(description = RestConstants.MESSAGE_SOURCE_NODE,required=true) @QueryParam("source") String source,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);
			source=NodeDao.mapNodeConstants(repoDao,source);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao child = nodeDao.createChildByMove(source);
	    			
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(child.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
        
    @OPTIONS    
    @Path("/nodes/{repository}/{node}/children/_move")
    @Hidden

    public Response options05() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
    }
    @POST
    @Path("/nodes/{repository}/{node}/preview")
    @Consumes({ "multipart/form-data" })
    
    @Operation(summary = "Change preview of node.", description = "Change preview of node.")
    
    @ApiResponses(
    	value = { 
        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),        
        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    })

    public Response changePreview(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @FormDataParam("image") InputStream inputStream,
	    @Parameter(description = "MIME-Type", required=true ) @QueryParam("mimetype")  String mimetype,
		@Parameter(description = "create a node version", required = false, schema = @Schema(defaultValue="true")) @QueryParam("createVersion") Boolean createVersion,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changePreview(inputStream,mimetype, createVersion==null || createVersion);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}

    }
	@DELETE
	@Path("/nodes/{repository}/{node}/preview")

	@Operation(summary = "Delete preview of node.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response deletePreview(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);

			NodeDao newNode = nodeDao.deletePreview();

			NodeEntry response = new NodeEntry();
			response.setNode(newNode.asNode());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
    @POST
    @Path("/nodes/{repository}/{node}/content")
    @Consumes({ "multipart/form-data" })
    
    @Operation(summary = "Change content of node.", description = "Change content of node.")
    
    @ApiResponses(
    	value = { 
        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),        
        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    })

    public Response changeContent(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "comment, leave empty = no new version, otherwise new version is generated", required=false ) @QueryParam("versionComment")  String versionComment,
	    @Parameter(description = "MIME-Type", required=true ) @QueryParam("mimetype")  String mimetype,
	    //@FormDataParam("file") InputStream inputStream,
		@Parameter(description = "file upload", schema = @Schema( name = "file", type = "string", format = "binary")) @FormDataParam("file") InputStream inputStream,
//	    @FormDataParam("file") FormDataContentDisposition fileDetail,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changeContent(inputStream, mimetype, versionComment);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
    }
    
    @POST
    @Path("/nodes/{repository}/{node}/textContent")
    @Consumes({ "multipart/form-data" })
    
    @Operation(summary = "Change content of node as text.", description = "Change content of node as text.")
    
    @ApiResponses(
    	value = { 
        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),        
        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    })

    public Response changeContentAsText(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Parameter(description = "comment, leave empty = no new version, otherwise new version is generated", required=false ) @QueryParam("versionComment")  String versionComment,
	    @Parameter(description = "MIME-Type", required=true ) @QueryParam("mimetype")  String mimetype,
	    @Parameter(description = "The content data to write (text)", required=true )String text,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changeContent(new ByteArrayInputStream(text.getBytes()), mimetype, versionComment);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/content")
    @Hidden

	public Response options06() {

		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}

    @GET
    @Path("/nodes/{repository}/{node}/versions")    
    
    @Operation(summary = "Get all versions of node.", description = "Get all versions of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeVersionRefEntries.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getVersions(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
		@Context HttpServletRequest req) {
    
    	try {

	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node, Filter.createShowAllFilter());

			NodeVersionRefEntries response=new NodeVersionRefEntries();
	    	response.setVersions(nodeDao.getNodeRefHistory());
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	  }
    
    
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/versions")
    @Hidden

	public Response options07() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}


	@GET
	@Path("/nodes/{repository}/{node}/versions/metadata")

	@Operation(summary = "Get all versions of node, including it's metadata.", description = "Get all versions of node, including it's metadata.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeVersionEntries.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getVersions(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-"))) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			Filter filter = new Filter(propertyFilter);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node, filter);

			NodeVersionEntries response=new NodeVersionEntries();
			response.setVersions(nodeDao.getHistory());
			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

    @GET
    @Path("/nodes/{repository}/{node}/versions/{major}/{minor}/metadata")    
    
    @Operation(summary = "Get metadata of node version.", description = "Get metadata of node version.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeVersionEntry.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getVersionMetadata(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Parameter(description = "major version",required=true ) @PathParam("major") int major,
    	@Parameter(description = "minor version",required=true ) @PathParam("minor") int minor,
    	@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-"))) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {
    
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	Filter filter = new Filter(propertyFilter);
	    	
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node, filter);
	    	
	    	NodeVersionEntry response = new NodeVersionEntry(); 
	    	response.setVersion(nodeDao.getVersion(major, minor));
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/versions/{major}/{minor}/metadata")
    @Hidden

	public Response options08() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}

    @PUT
    @Path("/nodes/{repository}/{node}/versions/{major}/{minor}/_revert")    
    
    @Operation(summary = "Revert to node version.", description = "Revert to node version.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response revertVersion(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Parameter(description = "major version",required=true ) @PathParam("major") int major,
    	@Parameter(description = "minor version",required=true ) @PathParam("minor") int minor,
		@Context HttpServletRequest req) {
    
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao revert = nodeDao.revertHistory(major, minor);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(revert.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/versions/{major}/{minor}/_revert")    
	@Hidden

	public Response options09() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
	}

    @POST
    @Path("/nodes/{repository}")    
    
    @Operation(summary = "Searching nodes.", description = "Searching nodes.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SearchResult.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getNodes(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    @Parameter(description = "lucene query",required=true) @QueryParam("query") String query,
	    @Parameter(description = "facets") @QueryParam("facets") List<String> facets,
	    @Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="10")) @QueryParam("maxItems") Integer maxItems,
	    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
	    @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
	    @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
	    @Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)", array = @ArraySchema(schema = @Schema(defaultValue="-all-")) ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {
		    
    	try {
    		
    		Filter filter= new Filter(propertyFilter);
  
    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
    		
    		SearchToken searchToken=new SearchToken();
			searchToken.setLuceneString(query);
			searchToken.setFrom(skipCount != null ? skipCount : 0);
			searchToken.setMaxResult(maxItems!= null ? maxItems : 10);
			searchToken.setFacets(facets);
			searchToken.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
    		NodeSearch search = NodeDao.search(repoDao,searchToken);
    		List<Node> data = new ArrayList<>();
	    	for (NodeRef ref : search.getResult()) {
	    		data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
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
    		
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }

    
	@OPTIONS    
	@Path("/nodes/{repository}")
    @Hidden

	public Response options10() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
    
	@GET
    @Path("/nodes/{repository}/{node}/permissions/{user}")    
    
    @Operation(summary = "Which permissions has user/group for node.", description = "Check for actual permissions (also when user is in groups) for a specific node")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = String[].class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
 
    public Response hasPermission(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Parameter(description = "Authority (user/group) to check (use \"-me-\" for current user",required=true ) @PathParam("user") String authority,
		@Context HttpServletRequest req) {
    
    	try {
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	if(authority.equals("-me-"))
	    		authority=AuthenticationUtil.getFullyAuthenticatedUser();
	    	List<String> response = nodeDao.getPermissions(authority);
	    
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	
    @GET
    @Path("/nodes/{repository}/{node}/permissions")    
    
    @Operation(summary = "Get all permission of node.", description = "Get all permission of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodePermissionEntry.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
 
    public Response getPermission(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
		@Context HttpServletRequest req) {
    
    	try {
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodePermissionEntry response = new NodePermissionEntry(); 
	    	
	    	response.setPermissions(nodeDao.getPermissions());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }


	@GET
	@Path("/nodes/{repository}/{node}/permissions/raw")
	@Operation(summary = "Get all permission of node (not grouped, requires admin).", description = "Get all permission of node (not grouped, requires admin).")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = org.edu_sharing.repository.client.rpc.ACL.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getRawPermission(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			return Response.status(Response.Status.OK).entity(nodeDao.getRawPermissions()).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
    @GET
    @Path("/nodes/{repository}/{node}/notifys")    
    
    @Operation(summary = "Get notifys (sharing history) of the node.", description = "Ordered by the time of each notify")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NotifyEntry[].class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
 
    public Response getNotifyList(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
		@Context HttpServletRequest req) {
    
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	List<NotifyEntry> notifys = nodeDao.getNotifys();
	    	return Response.status(Response.Status.OK).entity(notifys).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    @POST
    @Path("/nodes/{repository}/{node}/permissions")    
    
    @Operation(summary = "Set local permissions of node.", description = "Set local permissions of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response setPermission(
    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Parameter(description = "permissions",required=true ) ACL permissions,
    	@Parameter(description = "mailtext",required=false ) @QueryParam("mailtext")  String mailText,
    	@Parameter(description = "sendMail",required=true ) @QueryParam("sendMail") Boolean sendMail,
    	@Parameter(description = "sendCopy",required=true ) @QueryParam("sendCopy") Boolean sendCopy,
		@Context HttpServletRequest req) {
    
    	try {
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	nodeDao.setPermissions(permissions,mailText,sendMail,sendCopy);
	    	
	    	return Response.status(Response.Status.OK).build();
	
    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}

    }
    
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/permissions")
    @Hidden

	public Response options11() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, @POST").build();
	}
		
	
	  @POST
	    @Path("/nodes/{repository}/{node}/prepareUsage")    
	    
	    @Operation(summary = "create remote object and get properties.", description = "create remote object and get properties.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeRemote.class))),        
		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		    })

	    public Response prepareUsage(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Context HttpServletRequest req) {
	    
	    	try {
	    		
	    		if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
	    			return RepoProxyFactory.getRepoProxy().prepareUsage(repository, node, req);
	    		}
	    		
			
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeRemote nodeRemote = NodeDao.prepareUsage(repoDao.getId(), node);
		    	return Response.status(Response.Status.OK).entity(nodeRemote).build();
		
	    	} catch (DAOValidationException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOSecurityException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOMissingException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (Throwable t) {
	    		
	    		logger.error(t.getMessage(), t);
	    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
	    	}

	    }
	    
		@OPTIONS    
		@Path("/nodes/{repository}/{node}/prepareUsage")
	    @Hidden

		public Response options12() {
			
			return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, @POST").build();
		}
		
		
		@POST
	    @Path("/nodes/{repository}/{node}/owner")    
	    
	    @Operation(summary = "Set owner of node.", description = "Set owner of node.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		    })

	    public Response setOwner(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@Parameter(description = "username",required=false ) @QueryParam("username")  String username,
			@Context HttpServletRequest req) {
	    
	    	try {
			
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.setOwner(username);   	
		    	return Response.status(Response.Status.OK).build();
		
	    	} catch (DAOValidationException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOSecurityException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOMissingException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (Throwable t) {
	    		
	    		logger.error(t.getMessage(), t);
	    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
	    	}

	    }
		
		@POST
	    @Path("/nodes/{repository}/{node}/property")    
	    
	    @Operation(summary = "Set single property of node.", description = "When the property is unset (null), it will be removed")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
		    })

	    public Response setProperty(
	    	@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@Parameter(description = "property",required=true ) @QueryParam("property")  String property,
			@Parameter(description = "keepModifiedDate",required=false, schema = @Schema(defaultValue="false")) @QueryParam("keepModifiedDate") Boolean keepModifiedDate,
			@Parameter(description = "value",required=false ) @QueryParam("value")  List<String> value,
			@Context HttpServletRequest req) {
	    
	    	try {
			
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeDao.setProperty(
						repoDao,
						node,
						property,
						value == null || value.size() != 1? (Serializable) value : value.get(0),
						keepModifiedDate != null && keepModifiedDate
				);
		    	return Response.status(Response.Status.OK).build();
		
	    	} catch (DAOValidationException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOSecurityException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOMissingException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (Throwable t) {
	    		
	    		logger.error(t.getMessage(), t);
	    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
	    	}

	    }

	@GET
	@Path("/nodes/{repository}/{node}/stats")
	@Operation(summary = "Get statistics of node.", description = "Get statistics (views, downloads) of node. Requires " + PermissionService.CHANGE_PERMISSIONS + " permission on node")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeStatsEntry.NodeStats.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getStats(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			return Response.status(Response.Status.OK).entity(NodeDao.getStats(nodeDao)).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}


	@GET
	@Path("/nodes/{repository}/{node}/permissions/jwt")
	@Operation(summary = "Generate a jwt access token of the node.", description = "Generate a jwt access token of the node")
	@Produces({"application/text"})
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = String.class,  format = "byte"))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})
	public Response getJWT(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
//			node=NodeDao.mapNodeConstants(repoDao,node);
//
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			return Response.status(Response.Status.OK).entity(nodeDao.getJWT()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
}

