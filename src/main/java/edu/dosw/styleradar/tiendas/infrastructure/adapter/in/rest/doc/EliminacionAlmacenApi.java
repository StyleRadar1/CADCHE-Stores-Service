package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Store verification", description = "Review and approval of partner stores. Requires ROLE_STYLERADAR_ADMIN")
public interface EliminacionAlmacenApi {

    @Operation(
            summary = "Delete a partner store",
            description = "ROLE_STYLERADAR_ADMIN. Permanently deletes the store with its photos, catalog settings and administrators and publishes store.deleted. To suspend a store without losing data use the review request.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Store deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Requires ROLE_STYLERADAR_ADMIN", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{nit}")
    ResponseEntity<Void> eliminar(
            @Parameter(description = "Store NIT", example = "900123456-1") @PathVariable String nit,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);
}
