package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Stores", description = "Store registration with RUT in PDF (SR-RF-62-A / SR-RF-62), store lookup, catalog badge settings and evaluation (SR-RF-59, SR-RF-60) and physical-store photos (SR-RF-74)")
public interface UsuarioAlmacenApi {

    @Operation(
            summary = "Get the store administered by a user (SR-RF-59 / SR-RF-62-A)",
            description = "The user is named in the path. Only that same user (store) or a STYLERADAR_ADMIN can read it; any other identity gets 403.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store administered by the user"),
            @ApiResponse(responseCode = "401", description = "JWT missing or invalid", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "The user in the path is not the caller", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "The user does not administer any store", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{userId}/store")
    ResponseEntity<VerificacionAlmacenResponseDTO> obtenerAlmacenDelUsuario(
            @Parameter(description = "Identifier of the user who owns the store", example = "10") @PathVariable("userId") Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);
}
