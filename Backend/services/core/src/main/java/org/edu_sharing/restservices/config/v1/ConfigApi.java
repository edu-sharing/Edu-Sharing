package org.edu_sharing.restservices.config.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.config.model.Context;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.config.v1.model.Config;
import org.edu_sharing.restservices.config.v1.model.Language;
import org.edu_sharing.restservices.config.v1.model.Variables;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.config.ConfigService;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.config.DynamicConfig;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/config/v1")
@Tag(name = "CONFIG v1")
@ApiService(value = "CONFIG", major = 1, minor = 0)
@Consumes({"application/json"})
@Produces({"application/json"})
public class ConfigApi {
    private static Logger logger = Logger.getLogger(ConfigApi.class);

    @GET
    @Path("/values")
    @Operation(summary = "get repository config values", description = "Current is the actual (context-based) active config. Global is the default global config if no context is active (may be identical to the current)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Config.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getConfig() {
        try {
            Config config = new Config();
            ConfigService configService = ConfigServiceFactory.getConfigService();
            config.setGlobal(configService.getConfig().values);
            try {
                Context context = configService.getContext(ConfigServiceFactory.getCurrentDomain());
                if (context != null) {
                    config.setContextId(context.id);
                    config.setCurrent(configService.getConfigByContext(context).values);
                } else {
                    config.setCurrent(config.getGlobal());
                }
            } catch (IllegalArgumentException e) {
                logger.debug(e.getMessage());
                // context for domain does not exist -> use default
                config.setCurrent(config.getGlobal());
            }
            return Response.status(Response.Status.OK).entity(config).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }

    @GET
    @Path("/context/")
    @Operation(summary = "get all available repository context objects", description = "Retrieves all available context objects")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Context[].class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getAvailableContext() {
        try {
            ConfigService configService = ConfigServiceFactory.getConfigService();
            List<Context> context = configService.getAvailableContext();
            return Response.status(Response.Status.OK).entity(context).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }

    @PUT
    @Path("/context/")
    @Operation(summary = "Set a context entry (admin rights required)", requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = Context.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Context.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createOrUpdateContext(Context context) {
        try {
            Context response = ConfigServiceFactory.getConfigService().createOrUpdateContext(context);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }

    @DELETE
    @Path("/context/{id}")
    @Operation(summary = "Delete a context entry by id (admin rights required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Context.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteContext(@Parameter(description = "Id of the context that should be deleted", required = true) @PathParam("id")String id) {
        try {
            ConfigServiceFactory.getConfigService().deleteContext(id);
            return Response.status(Response.Status.OK).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }


    @GET
    @Path("/language/defaults")
    @Operation(summary = "get all inital language strings for angular")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getLanguageDefaults() {
        try {
            return Response.status(Response.Status.OK).entity(I18nAngular.getLanguageStrings().toString()).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }

    @GET
    @Path("/language")
    @Operation(summary = "get override strings for the current language", description = "Language strings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Language.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getLanguage() {
        try {
            Language language = new Language();
            language.setCurrentLanguage(new AuthenticationToolAPI().getCurrentLanguage());

            language.setGlobal(getActiveLanguage(ConfigServiceFactory.getConfigService().getConfig().language));
            try {
                language.setCurrent(getActiveLanguage(ConfigServiceFactory.getConfigService().getConfigByDomain(ConfigServiceFactory.getCurrentDomain()).language));
            } catch (IllegalArgumentException e) {
                logger.debug(e.getMessage());
                language.setCurrent(language.getGlobal());

            }
            return Response.status(Response.Status.OK).entity(language).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }

    @GET
    @Path("/variables")
    @Operation(summary = "get global config variables", description = "global config variables")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Variables.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getVariables() {
        try {
            Variables variables = new Variables();

            variables.setGlobal(convertVariables(ConfigServiceFactory.getConfigService().getConfig().variables));
            try {
                variables.setCurrent(convertVariables(ConfigServiceFactory.getConfigService().getConfigByDomain(ConfigServiceFactory.getCurrentDomain()).variables));
            } catch (IllegalArgumentException e) {
                logger.debug(e.getMessage());
                variables.setCurrent(variables.getGlobal());

            }
            return Response.status(Response.Status.OK).entity(variables).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }

    @GET
    @Path("/dynamic/{key}")
    @Operation(summary = "Get a config entry (appropriate rights for the entry are required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = DynamicConfig.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getDynamicValue(@Parameter(description = "Key of the config value that should be fetched", required = true) @PathParam("key") String key) {
        try {
            DynamicConfig config = ConfigServiceFactory.getConfigService().getDynamicValue(key);
            return Response.status(Response.Status.OK).entity(config).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }

    @POST
    @Path("/dynamic/{key}")
    @Operation(summary = "Set a config entry (admin rights required)", description = "the body must be a json encapsulated string")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = DynamicConfig.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response setDynamicValue(@Parameter(description = "Key of the config value that should be fetched", required = true) @PathParam("key") String key,
                                    @Parameter(description = "Is everyone allowed to read the value", required = true) @QueryParam("public") Boolean readPublic,
                                    @Parameter(description = "Must be a json-encapsulated string", required = true) String value) {
        try {
            DynamicConfig config = ConfigServiceFactory.getConfigService().setDynamicValue(key, readPublic == null ? false : readPublic.booleanValue(), new JSONObject(value));
            return Response.status(Response.Status.OK).entity(config).build();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
        }
    }

    private Map<String, String> convertVariables(org.edu_sharing.alfresco.service.config.model.Variables variables) {
        if (variables == null || variables.variable == null)
            return null;
        return convertKeyValue(variables.variable);
    }

    private Map<String, String> convertKeyValue(List<org.edu_sharing.alfresco.service.config.model.KeyValuePair> pairs) {
        if (pairs == null)
            return null;
        Map<String, String> map = new HashMap<>();
        for (org.edu_sharing.alfresco.service.config.model.KeyValuePair pair : pairs) {
            map.put(pair.key, pair.value);
        }
        return map;
    }

    private Map<String, String> getActiveLanguage(List<org.edu_sharing.alfresco.service.config.model.Language> languages) {
        return convertKeyValue(ConfigServiceFactory.getLanguageData(languages));
    }
}
