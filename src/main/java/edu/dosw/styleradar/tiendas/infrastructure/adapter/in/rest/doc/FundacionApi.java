package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Partner foundations", description = "Registration, lookup, edition, deactivation and reactivation of partner foundations and nearby lookup for donors")
@SecurityRequirement(name = "bearerAuth")
public interface FundacionApi {

    @Operation(
            summary = "Upload the legal-entity certificate in PDF of a foundation",
            description = "ROLE_STYLERADAR_ADMIN. Receives a PDF of at most 5 MB and returns its public URL in object storage.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "PDF certificate stored successfully"),
            @ApiResponse(responseCode = "400", description = "File is not a PDF or larger than 5 MB", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Requires ROLE_STYLERADAR_ADMIN", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping(value = "/api/v1/admin/foundations/certificates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentoSubidoResponseDTO> subirCertificado(
            @Parameter(description = "Legal-entity certificate PDF file (max 5 MB)") @RequestPart("file") MultipartFile archivo,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Register a partner foundation",
            description = "ROLE_STYLERADAR_ADMIN. Registers a partner foundation with NIT, neighborhood, legal-entity certificate, supported causes and coverage.")
    @PostMapping("/api/v1/admin/foundations")
    ResponseEntity<FundacionResponseDTO> registrar(
            @Valid @RequestBody FundacionRequestDTO request,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "List every partner foundation",
            description = "ROLE_STYLERADAR_ADMIN. Returns every foundation registered on the platform.")
    @GetMapping("/api/v1/admin/foundations")
    ResponseEntity<List<FundacionResponseDTO>> listarTodas(
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Get a partner foundation by ID",
            description = "ROLE_STYLERADAR_ADMIN. Returns the full foundation data to prefill the edit form.")
    @GetMapping("/api/v1/admin/foundations/{id}")
    ResponseEntity<FundacionResponseDTO> obtenerPorId(
            @Parameter(description = "Foundation identifier", example = "3") @PathVariable Long id,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Edit a partner foundation",
            description = "ROLE_STYLERADAR_ADMIN. Updates the institutional information of a partner foundation.")
    @PutMapping("/api/v1/admin/foundations/{id}")
    ResponseEntity<FundacionResponseDTO> editar(
            @Parameter(description = "Foundation identifier", example = "3") @PathVariable Long id,
            @Valid @RequestBody FundacionRequestDTO request,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Deactivate a partner foundation",
            description = "ROLE_STYLERADAR_ADMIN. Logically deactivates a foundation without deleting its history and publishes foundation.deactivated.")
    @PatchMapping("/api/v1/admin/foundations/{id}/deactivate")
    ResponseEntity<FundacionResponseDTO> desactivar(
            @Parameter(description = "Identifier of the foundation to deactivate", example = "3") @PathVariable Long id,
            @Parameter(description = "Reason for the deactivation (max 500 characters)", example = "Cierre temporal de operaciones por reestructuración")
            @RequestParam(value = "reason", required = false) String motivo,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Reactivate a partner foundation",
            description = "ROLE_STYLERADAR_ADMIN. Reactivates a deactivated foundation, makes it visible again in the donation directory and publishes foundation.reactivated.")
    @PatchMapping("/api/v1/admin/foundations/{id}/activate")
    ResponseEntity<FundacionResponseDTO> activar(
            @Parameter(description = "Identifier of the foundation to reactivate", example = "3") @PathVariable Long id,
            @Parameter(description = "Reason for the reactivation (max 500 characters)", example = "Reapertura de recepción de donaciones")
            @RequestParam(value = "reason", required = false) String motivo,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "List active partner foundations",
            description = "Public. Returns only active partner foundations. With latitude and longitude they are sorted by distance; without them, by name. Optional type filter.")
    @GetMapping("/api/v1/foundations")
    ResponseEntity<List<FundacionResponseDTO>> consultarCercanas(
            @Valid @ParameterObject @ModelAttribute ConsultaFundacionesRequestDTO request);
}
