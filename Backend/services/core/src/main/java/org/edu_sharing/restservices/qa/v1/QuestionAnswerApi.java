package org.edu_sharing.restservices.qa.v1;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.qa.v1.domain.CreateQANodeRequestDTO;
import org.edu_sharing.restservices.qa.v1.domain.UpdateQAEntriesRequestDTO;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.qa.QAService;
import org.edu_sharing.service.qa.domain.QAEntry;
import org.edu_sharing.service.qa.domain.QANode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Path("/qa/v1")
@Tag(name = "QUESTION ANSWER v1", description = "Question answers storage endpoint")
@ApiService(value = "QUESTION ANSWER", major = 1)
@Consumes({"application/json"})
@Produces({"application/json"})
public class QuestionAnswerApi {

    @Autowired
    private QAService qaService;

    @POST
    @Path("/{sourceId}/{nodeId}")
    @Operation(summary = "Create a new QANode",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))

            })
    public Response createQANode(@PathParam("sourceId") String sourceId, @PathParam("nodeId") String nodeId, CreateQANodeRequestDTO requestData) {
        qaService.createQANode(sourceId, nodeId, requestData);
        return Response.ok().build();
    }


    @PUT
    @Path("/{sourceId}/{nodeId}")
    @Operation(summary = "Update QA Entries of a specific sourceId and nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<Void> updateQAEntries(@PathParam("sourceId") String sourceId, @PathParam("nodeId") String nodeId, UpdateQAEntriesRequestDTO requestData) {
        qaService.updateQANode(sourceId, nodeId, requestData);
        return ResponseEntity.ok().build();
    }

    @GET
    @Path("/{sourceId}/{nodeId}/node")
    @Operation(summary = "Get QA Node of a specific sourceId and nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = QANode.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<QANode> getQANodes(@PathParam("sourceId") String sourceId, @PathParam("nodeId") String nodeId) {
        return ResponseEntity.ok(qaService.getQANode(sourceId, nodeId));
    }

    @GET
    @Path("/nodes/{nodeId}")
    @Operation(summary = "Get all QA Nodes of a specific nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = QANode[].class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<List<QANode>> getAllQANodes(@PathParam("nodeId") String nodeId) {
        return ResponseEntity.ok(qaService.getAllQANode(nodeId));
    }


    @GET
    @Path("/{sourceId}/{nodeId}")
    @Operation(summary = "Get QA Entries of a specific sourceId and nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = QAEntry[].class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<List<QAEntry>> getQAEntries(@PathParam("sourceId") String sourceId, @PathParam("nodeId") String nodeId) {
        return ResponseEntity.ok(qaService.getAllQAEntriesOf(sourceId, nodeId));
    }


    @GET
    @Path("/{nodeId}")
    @Operation(summary = "Get all QA Entries of a specific nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = QAEntry[].class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<List<QAEntry>> getQAEntriesOf(@PathParam("nodeId") String nodeId) {
        return ResponseEntity.ok(qaService.getAllQAEntriesOf(nodeId));
    }

    @DELETE
    @Path("/{nodeId}/node")
    @Operation(summary = "Delete all QA Nodes by nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<Void> deleteAllQANodes(@PathParam("nodeId") String nodeId) {
        qaService.delete(nodeId);
        return ResponseEntity.ok().build();
    }

    @DELETE
    @Path("/{sourceId}/{nodeId}")
    @Operation(summary = "Delete QA Node by sourceId and nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<Void> deleteQANodes(@PathParam("sourceId") String sourceId, @PathParam("nodeId") String nodeId) {
        qaService.delete(sourceId, nodeId);
        return ResponseEntity.ok().build();
    }

}
