package org.edu_sharing.restservices.register.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RegisterDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.register.v1.model.RegisterExists;
import org.edu_sharing.restservices.register.v1.model.RegisterInformation;
import org.edu_sharing.restservices.shared.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@SecurityRequirements
@Path("/register/v1")
@Tag(name="REGISTER v1")
@ApiService(value="REGISTER", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class RegisterApi {

	@POST
	@Path("/register")
    @Operation(summary = "Register a new user")
    @ApiResponses(
    	value = { 
    		@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
		})

    public Response register(@Context HttpServletRequest req,RegisterInformation info) {
		try{
			RegisterDao.register(info);
			return Response.ok().build();
		}
    	catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
	@POST
	@Path("/resend/{mail}")
	@Operation(summary = "Resend a registration mail for a given mail address", description = "The method will return false if there is no pending registration for the given mail")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response resendMail(@Context HttpServletRequest req,
							 @Parameter(description = "The mail a registration is pending for and should be resend to",required=true ) @PathParam("mail") String mail
	) {
		try{
			RegisterDao.resendMail(mail);
			return Response.ok().build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/activate/{key}")
	@Operation(summary = "Activate a new user (by using a supplied key)")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response activate(@Context HttpServletRequest req,
							 @Parameter(description = "The key for the user to activate",required=true ) @PathParam("key") String key
							 ) {
		try{
			RegisterDao.activate(key);
			return Response.ok().build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
	@GET
	@Path("/exists/{mail}")
	@Operation(summary = "Check if the given mail is already successfully registered")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = RegisterExists.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response mailExists(@Context HttpServletRequest req,
									@Parameter(description = "The mail (authority) of the user to check",required=true ) @PathParam("mail") String mail
	) {
		try{
			RegisterExists result = RegisterDao.mailExists(mail);
			return Response.ok().entity(result).build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/recover/{mail}")
	@Operation(summary = "Send a mail to recover/reset password")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response recoverPassword(@Context HttpServletRequest req,
							 @Parameter(description = "The mail (authority) of the user to recover",required=true ) @PathParam("mail") String mail
	) {
		try{
			RegisterDao.recoverPassword(mail);
			return Response.ok().build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/reset/{key}/{password}")
	@Operation(summary = "Send a mail to recover/reset password")
	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response resetPassword(@Context HttpServletRequest req,
									@Parameter(description = "The key for the password reset request",required=true ) @PathParam("key") String key,
									@Parameter(description = "The new password for the user",required=true ) @PathParam("password") String password
	) {
		try{
			RegisterDao.resetPassword(key,password);
			return Response.ok().build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
}

