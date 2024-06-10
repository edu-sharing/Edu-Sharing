
package org.edu_sharing.restservices.iam.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.iam.v1.model.*;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.organization.v1.model.GroupSignupDetails;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SortDefinition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/iam/v1")
@Tag(name="IAM v1")
@ApiService(value="IAM", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class IamApi  {

	private static Logger logger = Logger.getLogger(IamApi.class);

    @GET

    @Path("/people/{repository}")    
    
    @Operation(summary = "Search users.", description = "Search users. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = UserEntries.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response searchUser(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "pattern",required=true) @QueryParam("pattern") String pattern,
			@Parameter(description = "global search context, defaults to true, otherwise just searches for users within the organizations", required = false, schema = @Schema(defaultValue="true")) @QueryParam("global") Boolean global,
			@Parameter(description = "the user status (e.g. active), if not set, all users are returned",required=false) @QueryParam("status") PersonLifecycleService.PersonStatus status,
			@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS)) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			Map<String, String> filter=new HashMap<>();
			if(status!=null)
				filter.put(CCConstants.getValidLocalName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS),status.name());

			SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).searchUsers(
	    					pattern,
	    					global==null ? true : global,
	    					skipCount!=null ? skipCount : 0,
	    	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
	    					new SortDefinition(CCConstants.NAMESPACE_CM,sortProperties,sortAscending),
	    					filter
	    			);

	    	List<UserSimple> result = new ArrayList<>();
	    	for (String user: search.getData()) {
	    		result.add(new PersonDao(repoDao,user).asPersonSimple(true));
	    	}	
	    	UserEntries response = new UserEntries();
	    	response.setList(result);
	    	response.setPagination(new Pagination(search));
	    		    	
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
   
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
  }

    
    @OPTIONS        
    @Path("/people/{repository}")
    @Hidden

    public Response options01() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
    @GET
    @Path("/people/{repository}/{person}")
    @Operation(summary = "Get the user.", description = "Get the user. (Not all information are feteched for foreign profiles if current user is not an admin)")
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = UserEntry.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	    })
    public Response getUser(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	
	    	UserEntry response = new UserEntry();
	    	response.setPerson(personDao.asPerson());

	    	org.edu_sharing.alfresco.repository.server.authentication.Context context =  org.edu_sharing.alfresco.repository.server.authentication.Context.getCurrentInstance();
	    String username = context.getSessionAttribute(CCConstants.AUTH_USERNAME);
	    	String authType = context.getAuthType();
		if(person.equals("-me-") || person.equals(username)) {
		 	if(authType != null && !authType.equals(CCConstants.AUTH_TYPE_DEFAULT)) {
		 		response.setEditProfile(false);
			}else {
				response.setEditProfile(true);
			}
		}


	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t, ErrorResponse.ErrorResponseLogging.relaxed);
    	}
    }

	@GET
	@Path("/people/{repository}/{person}/stats")
	@Operation(summary = "Get the user stats.", description = "Get the user stats (e.g. publicly created material count)")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = UserStats.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})
	public Response getUserStats(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
			@Context HttpServletRequest req) {
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			PersonDao personDao = PersonDao.getPerson(repoDao, person);
			return Response.status(Response.Status.OK).entity(personDao.getStats()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t, ErrorResponse.ErrorResponseLogging.relaxed);
		}
	}
    @GET       
    @Path("/people/{repository}/{person}/nodeList/{list}")    
    @Operation(summary = "Get a specific node list for a user", description = "For guest users, the list will be temporary stored in the current session")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntries.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    		    })

    public Response getNodeList(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
			@Parameter(description = "list name",required=true) @PathParam("list") String list,
			@Parameter(description = RestConstants.MESSAGE_PROPERTY_FILTER, array = @ArraySchema(schema = @Schema(defaultValue="-all-")))  @QueryParam("propertyFilter") List<String> propertyFilter,
    		@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    		@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
    		@Context HttpServletRequest req) {
		try{
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
    		Filter propFilter = new Filter(propertyFilter);
	    	NodeEntries result=new NodeEntries();
	    	List<NodeRef> refList = personDao.getNodeList(list);
	    	if(refList!=null){
                refList=NodeDao.sortApiNodeRefs(repoDao,refList,null,sortDefinition);
	    	    result=NodeDao.convertToRest(repoDao,propFilter,refList,0,Integer.MAX_VALUE);
	    	}
	    	return Response.status(Response.Status.OK).entity(result).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    @PUT       
    @Path("/people/{repository}/{person}/nodeList/{list}/{node}")    
    @Operation(summary = "Add a node to node a list of a user", description = "For guest users, the list will be temporary stored in the current session")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    		    })

    public Response addNodeList(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    		@Parameter(description = "list name. If this list does not exist, it will be created",required=true) @PathParam("list") String list,    		
    		@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true) @PathParam("node") String node,    		
    		@Context HttpServletRequest req) {
		try{
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	personDao.addNodeList(list, node);
	    	return Response.status(Response.Status.OK).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    @DELETE       
    @Path("/people/{repository}/{person}/nodeList/{list}/{node}")    
    @Operation(summary = "Delete a node of a node list of a user", description = "For guest users, the list will be temporary stored in the current session")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    		    })

    public Response removeNodeList(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    		@Parameter(description = "list name",required=true) @PathParam("list") String list,    		
    		@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true) @PathParam("node") String node,    		
    		@Context HttpServletRequest req) {
		try{
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	personDao.removeNodeList(list, node);
	    	return Response.status(Response.Status.OK).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    @GET       
    @Path("/people/{repository}/{person}/preferences")    
    @Operation(summary = "Get preferences stored for user", description = "Will fail for guest")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Preferences.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    		    })

    public Response getPreferences(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    		@Context HttpServletRequest req) {
		try{
			org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(service.isGuest())
				throw new Exception("Not allowed for guest user");
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	String preferences=personDao.getPreferences();
	    	Preferences pref=new Preferences();
	    	pref.setPreferences(preferences);
	    	return Response.status(Response.Status.OK).entity(pref).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    @PUT       
    @Path("/people/{repository}/{person}/preferences")    
    @Operation(summary = "Set preferences for user", description = "Will fail for guest")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    		    })

    public Response setPreferences(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    		@Parameter(description = "preferences (json string)", required = true, schema = @Schema(defaultValue="-me-" )) String content,
    		@Context HttpServletRequest req) {
		try{
			org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(service.isGuest())
				throw new Exception("Not allowed for guest user");
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	personDao.setPreferences(content);
	    	return Response.status(Response.Status.OK).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }

	@GET
	@Path("/people/{repository}/{person}/profileSettings")
	@Operation(summary = "Get profileSettings configuration", description = "Will fail for guest")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ProfileSettings.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getProfileSettings(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-")) @PathParam("person") String person,
			@Context HttpServletRequest req) {
		try {
			org.edu_sharing.service.authority.AuthorityService service = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if (service.isGuest())
				throw new Exception("Not allowed for guest user");
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			PersonDao personDao = PersonDao.getPerson(repoDao, person);
			ProfileSettings personDaoProfileSettings = personDao.getProfileSettings();
			return Response.status(Response.Status.OK).entity(personDaoProfileSettings).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}


	@PUT
	@Path("/people/{repository}/{person}/profileSettings")
	@Operation(summary = "Set profileSettings Configuration", description = "Will fail for guest")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response setProfileSettings(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-")) @PathParam("person") String person,
			@Parameter(description = "ProfileSetting Object", required = true) ProfileSettings profileSettings,
			@Context HttpServletRequest req) {
		try {
			org.edu_sharing.service.authority.AuthorityService service = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if (service.isGuest())
				throw new Exception("Not allowed for guest user");
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			PersonDao personDao = PersonDao.getPerson(repoDao, person);
			personDao.setProfileSettings(profileSettings);
			return Response.status(Response.Status.OK).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}



    @POST

    @Path("/people/{repository}/{person}")    
    
    @Operation(summary = "Create a new user.", description = "Create a new user. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = User.class))),
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response createUser(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username",required=true) @PathParam("person") String person,
    	    @Parameter(description = "profile" ,required=true ) UserProfileEdit profile,
    	    @Parameter(description = "Password, leave empty if you don't want to set any" ,required=false )@QueryParam("password") String password,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	User result = PersonDao.createPerson(repoDao, person,password, profile).asPerson();
	    	
	    	return Response.status(Response.Status.OK).entity(result).build();
	    	
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

    @PUT
    @Path("/people/{repository}/{person}/status/{status}")
    @Operation(summary = "update the user status.", description = "update the user status. (admin rights are required.)")

    @ApiResponses(
    	value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})

    public Response updateUserStatus(
    		@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username",required=true) @PathParam("person") String person,
    		@Parameter(description = "the new status to set",required=true) @PathParam("status") PersonLifecycleService.PersonStatus status,
    		@Parameter(description = "notify the user via mail", required = true, schema = @Schema(defaultValue="true")) @QueryParam("notify") Boolean notifyMail,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);

	    	personDao.setStatus(status,notifyMail==null ? true : notifyMail);
	    	
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

	@DELETE

	@Path("/people/{repository}/{person}")

	@Operation(summary = "Delete the user.", description = "Delete the user. (admin rights are required.)")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Void.class))),
					@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response deleteUser(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "username",required=true) @PathParam("person") String person,
			@Parameter(description = "force the deletion (if false then only persons which are previously marked for deletion are getting deleted)", required = false, schema = @Schema(defaultValue="false")) @QueryParam("force") Boolean force,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			PersonDao personDao = PersonDao.getPerson(repoDao, person);

			personDao.delete(force != null && force);

			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

    @OPTIONS        
    @Path("/people/{repository}/{person}")
    @Hidden

    public Response options02() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, POST, DELETE").build();
    }

    @PUT

    @Path("/people/{repository}/{person}/profile")    
    
    @Operation(summary = "Set profile of the user.", description = "Set profile of the user. (To set foreign profiles, admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	    })

    public Response changeUserProfile(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    	    @Parameter(description = "properties" ,required=true ) UserProfileEdit profile,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	
	    	personDao.changeProfile(profile);
	    		    	
	    	return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }
    @PUT
    @Path("/people/{repository}/{person}/avatar")
    @Operation(summary = "Set avatar of the user.", description = "Set avatar of the user. (To set foreign avatars, admin rights are required.)")
	@Consumes({ "multipart/form-data" })
	@ApiResponses(
    	value = {
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	    })

    public Response changeUserAvatar(
    		@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    	    @Parameter(description = "avatar image" ,required=true ) @FormDataParam("avatar") InputStream avatar,
    		@Context HttpServletRequest req) {

    	try {

	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);

	    	personDao.changeAvatar(avatar);

	    	return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {

    		return ErrorResponse.createResponse(t);
    	}
    }
    @DELETE
    @Path("/people/{repository}/{person}/avatar")
    @Operation(summary = "Remove avatar of the user.", description = "Remove avatar of the user. (To Remove foreign avatars, admin rights are required.)")
    @ApiResponses(
    	value = {
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	    })

    public Response removeUserAvatar(
    		@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    		@Context HttpServletRequest req) {

    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	personDao.removeAvatar();
	    	return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }

    @OPTIONS        
    @Path("/people/{repository}/{person}/profile")
    @Hidden

    public Response options03() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
    }

    @PUT

    @Path("/people/{repository}/{person}/credential")    
    
    @Operation(summary = "Change/Set password of the user.", description = "Change/Set password of the user. (To change foreign passwords or set passwords, admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response changeUserPassword(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "username (or \"-me-\" for current user)", required = true, schema = @Schema(defaultValue="-me-" )) @PathParam("person") String person,
    	    @Parameter(description = "credential" ,required=true ) UserCredential credential,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	
    		personDao.changePassword(credential.getOldPassword(), credential.getNewPassword());
	    		    	
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
    @Path("/people/{repository}/{person}/credential")
    @Hidden

    public Response options04() {	
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
    }
    
    @GET

    @Path("/groups/{repository}")    
    
    @Operation(summary = "Search groups.", description = "Search groups. (admin rights are required.)")

    @ApiResponses(
        	value = { 
    	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = GroupEntries.class))),
    	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    	    })

        public Response searchGroups(
        		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
        		@Parameter(description = "pattern",required=true) @QueryParam("pattern") String pattern,
        		@Parameter(description = "find a specific groupType",required=false) @QueryParam("groupType") String groupType,
				@Parameter(description = "find a specific signupMethod for groups (or asterisk for all including one)",required=false) @QueryParam("signupMethod") String signupMethod,
				@Parameter(description = "global search context, defaults to true, otherwise just searches for groups within the organizations", required = false, schema = @Schema(defaultValue="true")) @QueryParam("global") Boolean global,
        		@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS)) @QueryParam("maxItems") Integer maxItems,
        	    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
        	    @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
        	    @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
        		@Context HttpServletRequest req) {

        	try {
        		Map<String, String> props = new HashMap<>();
        		if(groupType!=null && !groupType.isEmpty()){
        			props.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), groupType);
        		}
				if(signupMethod!=null && !signupMethod.isEmpty()){
					props.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD), signupMethod);
				}
    	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
    	    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).findAuthorities(
    	    					AuthorityType.GROUP,
    	    					pattern,
    	    					global==null ? true : global, 
    	    					skipCount!=null ? skipCount : 0,
    	    					maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
    	    					new SortDefinition(sortProperties,sortAscending),
    	    					props
    	    			);

    	    	List<Group> result = new ArrayList<>();
    	    	for (String group: search.getData()) {
					try {
						result.add(new GroupDao(repoDao, group).asGroup());
					} catch(DAOMissingException e) {
						logger.warn("Group " + group + " as provided by search was not found", e);
						Group groupObj = new Group();
						groupObj.setAuthorityName(group);
						result.add(groupObj);
					}
    	    	}	
    	    	GroupEntries response = new GroupEntries();
    	    	response.setList(result);
    	    	response.setPagination(new Pagination(search));		    	
    	    	
    	    	return Response.status(Response.Status.OK).entity(response).build();
    	    	
        	 
        	} catch (Throwable t) {
        		return ErrorResponse.createResponse(t);
        	}
    }

        
    @OPTIONS        
    @Path("/groups/{repository}")
    @Hidden

    public Response options05() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
    @GET

    @Path("/groups/{repository}/{group}")    
    
    @Operation(summary = "Get the group.", description = "Get the group. (To get foreign profiles, admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = GroupEntry.class))),        
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getGroup(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "groupname",required=true) @PathParam("group") String group,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	GroupDao groupDao = GroupDao.getGroup(repoDao, group);
	    	
	    	GroupEntry response = new GroupEntry();
	    	response.setGroup(groupDao.asGroup());
	    		    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t, ErrorResponse.ErrorResponseLogging.relaxed);
    	}
    }
    
    @POST

    @Path("/groups/{repository}/{group}")    
    
    @Operation(summary = "Create a new group.", description = "Create a new group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Group.class))),
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response createGroup(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "groupname",required=true) @PathParam("group") String group,
    		@Parameter(description = "parent (will be added to this parent, also for name hashing), may be null",required=false) @QueryParam("parent") String parent,
    		@Parameter(description = "properties" ,required=true ) GroupProfile profile,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	Group groupResult = GroupDao.createGroup(repoDao, group, profile,parent).asGroup();
	    	
	    	return Response.status(Response.Status.OK).entity(groupResult).build();
	    	
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

    @Path("/groups/{repository}/{group}")    
    
    @Operation(summary = "Delete the group.", description = "Delete the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response deleteGroup(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "groupname",required=true) @PathParam("group") String group,
    		@Context HttpServletRequest req) {

    	try {
			new MCAlfrescoAPIClient().doInTransaction(()-> {
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				GroupDao groupDao = GroupDao.getGroup(repoDao, group);

				groupDao.delete();
				return groupDao;
			});
	    	return Response.status(Response.Status.OK).build();
	    	
    	} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
    }

    @OPTIONS        
    @Path("/groups/{repository}/{group}")
    @Hidden

    public Response options06() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, POST, DELETE").build();
    }

    @PUT

    @Path("/groups/{repository}/{group}/profile")    
    
    @Operation(summary = "Set profile of the group.", description = "Set profile of the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response changeGroupProfile(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "groupname",required=true ) @PathParam("group") String group,
    	    @Parameter(description = "properties" ,required=true ) GroupProfile profile,
    		@Context HttpServletRequest req) {

    	try {
			new MCAlfrescoAPIClient().doInTransaction(()->{
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				GroupDao groupDao = GroupDao.getGroup(repoDao, group);
				groupDao.changeProfile(profile);
				return groupDao;
			});
			return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
    	}
    }

    @OPTIONS        
    @Path("/groups/{repository}/{group}/profile")
    @Hidden

    public Response options07() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
    }

	@POST
	@Path("/groups/{repository}/{group}/signup/config")

	@Operation(
			description = "set group signup options",
			summary =" requires admin rights"
	)

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response signupGroupDetails(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of group",required=true) @PathParam("group") String group,
			@Parameter(description = "Details to edit",required=true) GroupSignupDetails details,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			GroupDao.getGroup(repoDao,group).setSignup(details);
			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@POST
	@Path("/groups/{repository}/{group}/signup")

	@Operation(summary = "let the current user signup to the given group")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = GroupSignupResult.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response signupGroup(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of group",required=true) @PathParam("group") String group,
			@Parameter(description = "Password for signup (only required if signupMethod == password)",required=false) @QueryParam("password") String password,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			return Response.status(Response.Status.OK).entity(GroupDao.getGroup(repoDao,group).signupUser(password)).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/groups/{repository}/{group}/signup/list")

	@Operation(summary = "list pending users that want to join this group", description = "Requires admin rights or org administrator on this group")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = User[].class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response signupGroupList(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of group",required=true) @PathParam("group") String group,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<User> persons = GroupDao.getGroup(repoDao, group).signupUserList().stream().map(personDao -> {
				try {
					return personDao.asPerson();
				} catch (DAOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(persons).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@PUT
	@Path("/groups/{repository}/{group}/signup/list/{user}")

	@Operation(summary = "put the pending user into the group", description = "Requires admin rights or org administrator on this group")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = void.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response confirmSignup(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of group",required=true) @PathParam("group") String group,
			@Parameter(description = "ID of user",required=true) @PathParam("user") String user,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			GroupDao.getGroup(repoDao, group).confirmSignup(user);
			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@DELETE
	@Path("/groups/{repository}/{group}/signup/list/{user}")

	@Operation(summary = "reject the pending user", description = "Requires admin rights or org administrator on this group")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = void.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response rejectSignup(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of group",required=true) @PathParam("group") String group,
			@Parameter(description = "ID of user",required=true) @PathParam("user") String user,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			GroupDao.getGroup(repoDao, group).rejectSignup(user);
			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET

    @Path("/people/{repository}/{person}/memberships")

    @Operation(summary = "Get all groups the given user is member of.")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = GroupEntries.class))),
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getUserGroups(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "authority name",required=true ) @PathParam("person") String person,
       		@Parameter(description = "pattern",required=false) @QueryParam("pattern") String pattern,
    		@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS)) @QueryParam("maxItems") Integer maxItems,
    	    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
    	    @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    	    @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,

    		@Context HttpServletRequest req) {

    	try {

    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			GroupEntries response = PersonDao.getPerson(repoDao, person).getMemberships(
    				pattern, 
    				skipCount!=null ? skipCount : 0,
	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
					new SortDefinition(sortProperties,sortAscending)
			);

	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }


    @GET

    @Path("/groups/{repository}/{group}/members")    
    
    @Operation(summary = "Get all members of the group.", description = "Get all members of the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = AuthorityEntries.class))),        
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getMembership(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "authority name (begins with GROUP_)",required=true ) @PathParam("group") String group,
       		@Parameter(description = "pattern",required=false) @QueryParam("pattern") String pattern,
       		@Parameter(description = "authorityType either GROUP or USER, empty to show all",required=false) @QueryParam("authorityType") String authorityType,
    		@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS)) @QueryParam("maxItems") Integer maxItems,
    	    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
    	    @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    	    @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
 
    		@Context HttpServletRequest req) {

    	try {
    		
    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
    		GroupDao.getGroup(repoDao, group).checkAdminAccess();
	    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).searchGroupMembers(
	    					group,
	    					pattern,
	    					authorityType,
	    					skipCount!=null ? skipCount : 0,
	    	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
	    					new SortDefinition(sortProperties,sortAscending)
	    			);
	    	AuthorityEntries response = new AuthorityEntries();
    		List<Authority> result = new ArrayList<>();
	    	for (String member: search.getData()) {
	    		result.add(
	    				member.startsWith(PermissionService.GROUP_PREFIX) ?
	    							new GroupDao(repoDao,member).asGroup() :
	    							new PersonDao(repoDao, member).asPerson());	
	    	}	
	    	response.setList(result);
	    	response.setPagination(new Pagination(search));
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }


	@GET

	@Path("/groups/{repository}/{group}/type/{type}")

	@Operation(summary = "Get a subgroup by the specified type", description = "Get a subgroup by the specified type")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = AuthorityEntries.class))),
					@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getSubgroupByType(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "authority name of the parent/primary group (begins with GROUP_)",required=true ) @PathParam("group") String group,
			@Parameter(description = "group type to filter for, e.g. " + CCConstants.ADMINISTRATORS_GROUP_TYPE,required=true) @PathParam("type") String type,

			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			GroupEntry response = new GroupEntry();
			response.setGroup(GroupDao.getGroup(repoDao, group).getSubgroupByType(type).asGroup());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@PUT

    @Path("/groups/{repository}/{group}/members/{member}")    
    
    @Operation(summary = "Add member to the group.", description = "Add member to the group. (admin rights are required.)")

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

    public Response addMembership(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "groupname",required=true ) @PathParam("group") String group,
    		@Parameter(description = "authorityName of member",required=true ) @PathParam("member") String member,
    		@Context HttpServletRequest req) {

    	try {
			new MCAlfrescoAPIClient().doInTransaction(()-> {
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				GroupDao groupDao = GroupDao.getGroup(repoDao, group);
				groupDao.addMember(member);
				return groupDao;
			});
	    	return Response.status(Response.Status.OK).build();
	    	
    	} catch (Throwable t) {
    		return  ErrorResponse.createResponse(t);
    	}
    }
    
    @DELETE

    @Path("/groups/{repository}/{group}/members/{member}")    
    
    @Operation(summary = "Delete member from the group.", description = "Delete member from the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Void.class))),        
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response deleteMembership(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "groupname",required=true ) @PathParam("group") String group,
    		@Parameter(description = "authorityName of member",required=true ) @PathParam("member") String member,
    		@Context HttpServletRequest req) {

    	try {
			new MCAlfrescoAPIClient().doInTransaction(()-> {
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				GroupDao groupDao = GroupDao.getGroup(repoDao, group);
				groupDao.deleteMember(member);
				return groupDao;
			});
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return  ErrorResponse.createResponse(t);
		}
    }

    @OPTIONS        
    @Path("/groups/{repository}/{group}/members/{member}")
    @Hidden

    public Response options09() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT, DELETE").build();
    }
    
    
    @GET

    @Path("/authorities/{repository}")    
    
    @Operation(summary = "Search authorities.", description = "Search authorities.")

    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = AuthorityEntries.class))),        
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response searchAuthorities(
    		@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
    		@Parameter(description = "pattern",required=true) @QueryParam("pattern") String pattern,
    		@Parameter(description = "global search context, defaults to true, otherwise just searches for users within the organizations", required = false, schema = @Schema(defaultValue="true")) @QueryParam("global") Boolean global,
			@Parameter(description = "find a specific groupType (does nothing for persons)",required=false) @QueryParam("groupType") String groupType,
			@Parameter(description = "find a specific signupMethod for groups (or asterisk for all including one) (does nothing for persons)",required=false) @QueryParam("signupMethod") String signupMethod,
			@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS)) @QueryParam("maxItems") Integer maxItems,
    	    @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
    		@Context HttpServletRequest req) {

    	try {
			Map<String, String> props = new HashMap<>();
			if(groupType!=null && !groupType.isEmpty()){
				props.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), groupType);
			}
			if(signupMethod!=null && !signupMethod.isEmpty()){
				props.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD), signupMethod);
			}
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).findAuthorities(
	    					null,
	    					pattern,
	    					global==null ? true : global,
	    					skipCount!=null ? skipCount : 0,
	    	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
	    					null,
	    					props
	    					
	    			);

	    	List<Authority> result = new ArrayList<>();
	    	for (String user: search.getData()) {
	    		result.add(getUserOrGroup(repoDao, user));
	    	}	
	    	AuthorityEntries response = new AuthorityEntries();
	    	response.setList(result);
	    	response.setPagination(new Pagination(search));
	    		    	
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	 
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
  }
	@GET

	@Path("/authorities/{repository}/recent")

	@Operation(summary = "Get recently invited authorities.", description = "Get the authorities the current user has recently invited.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = AuthorityEntries.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getRecentlyInvited(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<String> recent = PermissionServiceFactory.getPermissionService(repoDao.getId()).getRecentlyInvited();
			List<Authority> result = new ArrayList<>();
			for (String user: recent) {
				result.add(getUserOrGroup(repoDao, user));
			}
			AuthorityEntries response = new AuthorityEntries();
			response.setList(result);
			return Response.status(Response.Status.OK).entity(response).build();


		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	private Authority getUserOrGroup(RepositoryDao repoDao, String authority) throws DAOException {

		if(authority.startsWith(AuthorityType.GROUP.getPrefixString())){
			return new GroupDao(repoDao,authority).asGroup();
		}else{
			return new PersonDao(repoDao,authority).asPersonSimple(true);
		}
	}
}

