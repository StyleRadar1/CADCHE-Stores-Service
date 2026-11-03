package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.VerificacionConsultaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
        @ApiResponse(responseCode = "400", description = "Request inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o cuenta bloqueada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_STYLERADAR", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Almacén inexistente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "422", description = "Transición inválida o revisión repetida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Fallo técnico", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
})
public interface VerificacionAlmacenApi {

    @Operation(
            summary = "Consultar almacenes para verificación (SR-RF-62)",
            description = "ROLE_ADMIN_STYLERADAR. Filtro de estado opcional; sin filtro incluye NULL legado sin aprobación. "
                    + "page=0, size=20; máximo 100. Orden nombreComercial ASC, nit ASC; incluye almacenes sin coordenadas.")
    @GetMapping("/verificacion")
    ResponseEntity<PageResponseDTO<VerificacionAlmacenResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute VerificacionConsultaRequestDTO request);

    @Operation(
            summary = "Revisar datos de un almacén por NIT (SR-RF-62)",
            description = "ROLE_ADMIN_STYLERADAR. Retorna los datos de registro, contacto y la última decisión de verificación.")
    @GetMapping("/{nit}/verificacion")
    ResponseEntity<VerificacionAlmacenResponseDTO> consultarUno(
            @Parameter(description = "NIT del almacén a revisar", example = "900123456-1") @PathVariable String nit);

    @Operation(
            summary = "Aprobar o rechazar vinculación pendiente (SR-RF-62)",
            description = "ROLE_ADMIN_STYLERADAR. Solo aplica cuando el almacén está en estado PENDIENTE. "
                    + "El rechazo exige motivo no vacío de máximo 1000 caracteres; la aprobación permite omitirlo.")
    @PatchMapping("/{nit}/verificacion")
    ResponseEntity<VerificacionAlmacenResponseDTO> decidir(
            @Parameter(description = "NIT del almacén pendiente de decisión", example = "900123456-1") @PathVariable String nit,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "APROBADO", value = "{\"decision\":\"APROBADO\",\"motivo\":\"Datos revisados\"}"),
                            @ExampleObject(name = "RECHAZADO", value = "{\"decision\":\"RECHAZADO\",\"motivo\":\"Información insuficiente\"}")
                    }))
            @Valid @RequestBody DecisionVerificacionRequestDTO request);

    @Operation(
            summary = "Iniciar o reabrir revisión de un almacén (SR-RF-62)",
            description = "ROLE_ADMIN_STYLERADAR. Pasa un almacén con estado NULL, APROBADO o RECHAZADO a PENDIENTE. "
                    + "Si ya se encuentra en PENDIENTE devuelve 422. Conserva metadatos de la última decisión hasta finalizar la nueva evaluación.")
    @PostMapping("/{nit}/verificacion/solicitar-revision")
    ResponseEntity<VerificacionAlmacenResponseDTO> solicitarRevision(
            @Parameter(description = "NIT del almacén para iniciar revisión", example = "900123456-1") @PathVariable String nit);
}

