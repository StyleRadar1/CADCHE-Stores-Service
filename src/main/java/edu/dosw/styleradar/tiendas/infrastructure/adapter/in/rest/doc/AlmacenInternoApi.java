package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Internal - Stores",
        description = "Service-to-service lookups for internal StyleRadar services. Authorization: Basic with the internal service credentials")
public interface AlmacenInternoApi {

    @Operation(summary = "Store of an owner/admin user (internal)",
            description = "Resolves the store managed by the given ownerUserId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store found"),
            @ApiResponse(responseCode = "400", description = "ownerUserId missing or not numeric", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service credentials", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "The user does not administer any store", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "503", description = "Internal service access is not configured", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    ResponseEntity<AlmacenResponseDTO> porAdministrador(
            @Parameter(description = "User ID of the store administrator", example = "15")
            @RequestParam(value = "ownerUserId", required = false) Long ownerUserId);
}
