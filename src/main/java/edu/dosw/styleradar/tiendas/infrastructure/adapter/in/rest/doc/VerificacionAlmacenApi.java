package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.VerificacionConsultaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Store verification", description = "Review and approval of partner stores (SR-RF-62). Requires ROLE_STYLERADAR_ADMIN")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful operation"),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing rejection reason", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "JWT missing, invalid or account blocked", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Requires ROLE_STYLERADAR_ADMIN (BOLA/IDOR access control)", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Store not found", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "409", description = "Store already decided", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "422", description = "Invalid transition or store without RUT in PDF", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Technical failure", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
})
public interface VerificacionAlmacenApi {

    @Operation(
            summary = "List stores for verification (SR-RF-62)",
            description = "ROLE_STYLERADAR_ADMIN. Optional status filter; without it, pending and decided stores are included. page=0, size=20, max 100. Sorted by tradeName ASC, nit ASC.")
    @GetMapping("/verifications")
    ResponseEntity<PageResponseDTO<VerificacionAlmacenResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute VerificacionConsultaRequestDTO request,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Review a store by NIT (SR-RF-62)",
            description = "ROLE_STYLERADAR_ADMIN. Returns the registration, contact and legal-entity data (NIT and RUT PDF) and the last verification decision.")
    @GetMapping("/{nit}/verification")
    ResponseEntity<VerificacionAlmacenResponseDTO> consultarUno(
            @Parameter(description = "NIT of the store to review", example = "900123456-1") @PathVariable String nit,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Approve or reject a pending store (SR-RF-62)",
            description = "ROLE_STYLERADAR_ADMIN. Only applies to PENDING stores. APPROVED requires a RUT in PDF (otherwise 422). An already decided store returns 409. REJECTED requires a reason.")
    @PatchMapping("/{nit}/verification")
    ResponseEntity<VerificacionAlmacenResponseDTO> decidir(
            @Parameter(description = "NIT of the store pending decision", example = "900123456-1") @PathVariable String nit,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "APPROVED", value = "{\"decision\":\"APPROVED\",\"motivo\":\"Datos y RUT en PDF verificados correctamente\"}"),
                            @ExampleObject(name = "REJECTED", value = "{\"decision\":\"REJECTED\",\"motivo\":\"El RUT presentado no coincide con la actividad económica\"}")
                    }))
            @Valid @RequestBody DecisionVerificacionRequestDTO request,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @Parameter(in = ParameterIn.HEADER, description = "ID of the admin making the decision", example = "1")
            @RequestHeader(value = "X-User-Id", required = false) Long adminId);

    @Operation(
            summary = "Start or reopen the review of a store (SR-RF-62)",
            description = "ROLE_STYLERADAR_ADMIN. Moves an APPROVED or REJECTED store back to PENDING. If it is already PENDING it returns 422.")
    @PostMapping("/{nit}/verification/review-request")
    ResponseEntity<VerificacionAlmacenResponseDTO> solicitarRevision(
            @Parameter(description = "NIT of the store to review again", example = "900123456-1") @PathVariable String nit,
            @Parameter(in = ParameterIn.HEADER, description = "User roles (requires ROLE_STYLERADAR_ADMIN)", example = "ROLE_STYLERADAR_ADMIN")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @Parameter(in = ParameterIn.HEADER, description = "ID of the admin requesting the review", example = "1")
            @RequestHeader(value = "X-User-Id", required = false) Long adminId);
}
