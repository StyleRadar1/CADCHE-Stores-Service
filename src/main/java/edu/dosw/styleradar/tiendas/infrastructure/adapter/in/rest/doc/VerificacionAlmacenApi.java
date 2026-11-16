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

@Tag(name = "Verificación de almacenes", description = "Revisión y aprobación de almacenes aliados (SR-RF-62). Requiere ROLE_ADMIN_STYLERADAR")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación correcta"),
        @ApiResponse(responseCode = "400", description = "Request inválido o motivo de rechazo ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o cuenta bloqueada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_STYLERADAR (Control de acceso BOLA/IDOR)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Almacén inexistente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "409", description = "Almacén ya decidido previamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "422", description = "Transición inválida o almacén sin documento RUT en PDF", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Fallo técnico", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
})
public interface VerificacionAlmacenApi {

    @Operation(
            summary = "Consultar almacenes para verificación (SR-RF-62)",
            description = "ROLE_ADMIN_STYLERADAR. Filtro de estado opcional; sin filtro incluye almacenes pendientes y decididos. "
                    + "page=0, size=20; máximo 100. Orden nombreComercial ASC, nit ASC.")
    @GetMapping("/verificacion")
    ResponseEntity<PageResponseDTO<VerificacionAlmacenResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute VerificacionConsultaRequestDTO request,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Revisar datos de un almacén por NIT (SR-RF-62)",
            description = "ROLE_ADMIN_STYLERADAR. Retorna los datos de registro, contacto, personería jurídica (NIT y RUT PDF) y la última decisión de verificación.")
    @GetMapping("/{nit}/verificacion")
    ResponseEntity<VerificacionAlmacenResponseDTO> consultarUno(
            @Parameter(description = "NIT del almacén a revisar", example = "900123456-1") @PathVariable String nit,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Aprobar o rechazar vinculación pendiente (SR-RF-62)",
            description = "ROLE_ADMIN_STYLERADAR. Solo aplica cuando el almacén está en estado PENDIENTE. "
                    + "Para APROBAR exige que el almacén cuente con RUT en PDF (de lo contrario retorna 422). "
                    + "Si ya fue decidido previamente retorna 409. El rechazo exige motivo explicativo.")
    @PatchMapping("/{nit}/verificacion")
    ResponseEntity<VerificacionAlmacenResponseDTO> decidir(
            @Parameter(description = "NIT del almacén pendiente de decisión", example = "900123456-1") @PathVariable String nit,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "APROBADO", value = "{\"decision\":\"APROBADO\",\"motivo\":\"Datos y RUT en PDF verificados correctamente\"}"),
                            @ExampleObject(name = "RECHAZADO", value = "{\"decision\":\"RECHAZADO\",\"motivo\":\"El RUT presentado no coincide con la actividad económica\"}")
                    }))
            @Valid @RequestBody DecisionVerificacionRequestDTO request,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @Parameter(in = ParameterIn.HEADER, description = "ID del administrador que realiza la decisión", example = "1")
            @RequestHeader(value = "X-User-Id", required = false) Long adminId);

    @Operation(
            summary = "Iniciar o reabrir revisión de un almacén (SR-RF-62)",
            description = "ROLE_ADMIN_STYLERADAR. Pasa un almacén con estado APROBADO o RECHAZADO a PENDIENTE. "
                    + "Si ya se encuentra en PENDIENTE devuelve 422.")
    @PostMapping("/{nit}/verificacion/solicitar-revision")
    ResponseEntity<VerificacionAlmacenResponseDTO> solicitarRevision(
            @Parameter(description = "NIT del almacén para iniciar revisión", example = "900123456-1") @PathVariable String nit,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @Parameter(in = ParameterIn.HEADER, description = "ID del administrador que solicita la revisión", example = "1")
            @RequestHeader(value = "X-User-Id", required = false) Long adminId);
}
