package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Internal - Foundations",
        description = "Service-to-service lookups for ms-segunda-vida-donaciones. Authorization: Basic with the internal "
                + "service credentials; not exposed through the API Gateway")
public interface FundacionInternaApi {

    @Operation(summary = "Foundation by ID (internal)",
            description = "Full foundation data, including representativeId and active, for an authenticated service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foundation found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service credentials", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Foundation not found", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "503", description = "Internal service access is not configured", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    ResponseEntity<FundacionResponseDTO> porId(
            @Parameter(description = "Foundation ID", example = "7") @PathVariable Long id);

    @Operation(summary = "Foundation of a representative (internal)",
            description = "Resolves the foundation whose representative is the given user. Each representative has one foundation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foundation found"),
            @ApiResponse(responseCode = "400", description = "representativeId missing or not numeric", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service credentials", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "The user does not represent any foundation", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    ResponseEntity<FundacionResponseDTO> porRepresentante(
            @Parameter(description = "User ID of the foundation representative", example = "24")
            @RequestParam(value = "representativeId", required = false) Long representanteId);
}
