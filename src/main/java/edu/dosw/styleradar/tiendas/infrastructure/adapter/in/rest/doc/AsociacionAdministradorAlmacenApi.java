package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Store administrators", description = "Association and lookup of STORE_ADMIN users authorized per NIT to prevent BOLA/IDOR (SR-RF-62 / SR-RNF-01)")
@SecurityRequirement(name = "bearerAuth")
public interface AsociacionAdministradorAlmacenApi {

    @Operation(
            summary = "Associate an authorized administrator to an existing NIT",
            description = "Only ROLE_STYLERADAR_ADMIN with an active account. Idempotently links an existing STORE_ADMIN user to an existing store identified by its NIT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Association stored or already existing"),
            @ApiResponse(responseCode = "400", description = "Invalid identifier", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT missing, invalid or account suspended", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Requires ROLE_STYLERADAR_ADMIN", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or store not found", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Concurrency or integrity conflict", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Target user does not have the STORE_ADMIN role", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Technical failure", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{nit}/administrators/{userId}")
    ResponseEntity<AdministradorAlmacenResponseDTO> asociar(
            @Parameter(description = "Store NIT", example = "900123456-1") @PathVariable String nit,
            @Parameter(description = "Identifier of the STORE_ADMIN user", example = "15") @PathVariable("userId") Long usuarioId,
            @Parameter(in = ParameterIn.HEADER, description = "StyleRadar admin ID", example = "1")
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "List the authorized administrators of a store by NIT",
            description = "ROLE_STYLERADAR_ADMIN or the owning ROLE_STORE_ADMIN. Returns the administrators linked to the NIT for ownership checks (BOLA/IDOR prevention).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorized administrators of the NIT"),
            @ApiResponse(responseCode = "401", description = "JWT missing, invalid or expired", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "No permission over the requested NIT", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{nit}/administrators")
    ResponseEntity<List<AdministradorAlmacenResponseDTO>> listarAdministradores(
            @Parameter(description = "Store NIT", example = "900123456-1") @PathVariable String nit,
            @Parameter(in = ParameterIn.HEADER, description = "Authenticated user ID", example = "15")
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Parameter(in = ParameterIn.HEADER, description = "User roles", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);
}
