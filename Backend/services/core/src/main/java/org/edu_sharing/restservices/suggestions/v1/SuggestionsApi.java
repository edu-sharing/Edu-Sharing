package org.edu_sharing.restservices.suggestions.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;
import org.edu_sharing.restservices.suggestions.v1.dto.NodeSuggestionResponseDTO;
import org.edu_sharing.restservices.suggestions.v1.dto.SuggestionResponseDTO;
import org.edu_sharing.service.suggestion.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/suggestions/v1")
@Tag(name = "SUGGESTIONS v1")
@ApiService(value = "SUGGESTIONS", major = 1, minor = 0)
@Consumes({"application/json"})
@Produces({"application/json"})
public class SuggestionsApi {

    private static final Logger log = LoggerFactory.getLogger(SuggestionsApi.class);
    @Autowired
    private SuggestionServiceFactory suggestionServiceFactory;

    @POST
    @Path("/{repository}/{node}")
    @Operation(summary = "Create suggestions")
    @ApiResponse(responseCode = "200",
            description = "Store suggestions for the given node.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SuggestionResponseDTO.class))))
    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response createSuggestions(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @Parameter(description = "Type of the suggestion", required = true) @QueryParam("type") SuggestionType type,
            @Parameter(description = "Version of the suggestion", required = true) @QueryParam("version") String version,
            List<CreateSuggestionRequestDTO> suggestionsDto) {
        Mapper mapper = new Mapper(RepositoryDao.getRepository(repository));
        SuggestionService suggestionService = suggestionServiceFactory.getServiceByAppId(repository);
        List<Suggestion> suggestions = suggestionService.createSuggestion(node, type, version, suggestionsDto);
        return Response.ok(suggestions.stream().map(mapper::map).toArray(SuggestionResponseDTO[]::new)).build();
    }

    @DELETE
    @Path("/{repository}/{node}")
    @Operation(summary = "Delete suggestions")
    @ApiResponse(responseCode = "200", description = "Delete all suggestions of a given node.")
    public Response deleteSuggestions(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @Parameter(description = "delete only specified versions. If not set, it deletes all versions") @QueryParam("version") List<String> versions) {

        SuggestionService suggestionService = suggestionServiceFactory.getServiceByAppId(repository);
        suggestionService.deleteSuggestions(node, versions);
        return Response.ok().build();
    }

    @PATCH
    @Path("/{repository}/{node}")
    @Operation(summary = "Update suggestion status")
    @ApiResponse(responseCode = "200",
            description = "Updates the status of all suggestions by the given suggestion ids",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SuggestionResponseDTO.class))))
    public Response updateStatus(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @QueryParam("id") List<String> ids,
            @QueryParam("status") SuggestionStatus status) {

        Mapper mapper = new Mapper(RepositoryDao.getRepository(repository));
        SuggestionService suggestionService = suggestionServiceFactory.getServiceByAppId(repository);
        List<Suggestion> suggestions = suggestionService.updateStatus(node, ids, status);
        return Response.ok(suggestions.stream().map(mapper::map).toArray(SuggestionResponseDTO[]::new)).build();

    }

    @GET
    @Path("/{repository}/{node}")
    @Operation(summary = "Retrieve stored suggestion for the given nodeId")
    @ApiResponse(
            responseCode = "200",
            description = "get all suggestions notifications",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = NodeSuggestionResponseDTO.class)))
    public Response getSuggestionsByNodeId(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @Parameter(description = "Filter option") @QueryParam("status") List<SuggestionStatus> status) {

        Mapper mapper = new Mapper(RepositoryDao.getRepository(repository));
        SuggestionService suggestionService = suggestionServiceFactory.getServiceByAppId(repository);
        Map<String, List<Suggestion>> nodeSuggestions = suggestionService.getSuggestionsByNodeId(node, status);
        return Response.ok(mapper.map(node, nodeSuggestions)).build();
    }

    @RequiredArgsConstructor
    private static class Mapper {
        private final RepositoryDao repositoryDao;

        public NodeSuggestionResponseDTO map(String node, Map<String, List<Suggestion>> nodeSuggestions) {
            return new NodeSuggestionResponseDTO(
                    node,
                    map(nodeSuggestions)
            );
        }

        public Map<String, List<SuggestionResponseDTO>> map(Map<String, List<Suggestion>> nodeSuggestions) {
            return nodeSuggestions
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, y -> y.getValue().stream().map(this::map).collect(Collectors.toList())));
        }

        public SuggestionResponseDTO map(Suggestion suggestion) {

            UserSimple createBy = getPerson(suggestion.getCreatedBy());
            UserSimple modifiedBy = getPerson(suggestion.getModifiedBy());

            return new SuggestionResponseDTO(
                    suggestion.getId(),
                    suggestion.getNodeId(),
                    suggestion.getVersion(),
                    suggestion.getPropertyId(),
                    suggestion.getValue(),
                    suggestion.getType(),
                    suggestion.getStatus(),
                    suggestion.getDescription(),
                    suggestion.getConfidence(),
                    suggestion.getCreated(),
                    createBy,
                    suggestion.getModified(),
                    modifiedBy);
        }

        private UserSimple getPerson(String user) {
            try {
                return PersonDao.getPerson(repositoryDao, user).asPersonSimple(false);
            } catch (DAOException daoException) {
                log.error(daoException.getMessage());
                return null;
            }
        }
    }
}
