package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
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

@Tag(name = "Fundaciones aliadas", description = "Registro, edición y desactivación administrativa de fundaciones aliadas (SR-RF-63, SR-RF-64, SR-RF-65) y consulta por cercanía para donantes (SR-RF-43)")
@SecurityRequirement(name = "bearerAuth")
public interface FundacionApi {

    @Operation(
            summary = "Registrar fundación aliada (SR-RF-63)",
            description = "ROLE_ADMIN_STYLERADAR. Registra una fundación aliada en la plataforma con certificado de personería jurídica, causas apoyadas y cobertura geográfica, asignando al usuario indicado en representanteId el rol REPRESENTANTE_FUNDACION.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FundacionRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "RegistrarFundacion",
                                    value = "{\"nombre\":\"Fundación Abrigo Solidario\",\"descripcion\":\"Recolección de prendas en buen estado para comunidades vulnerables de Bogotá\",\"direccion\":\"Calle 85 # 15-32, Chapinero, Bogotá\",\"telefono\":\"+57 601 742 8899\",\"email\":\"contacto@abrigosolidario.org\",\"latitud\":4.6712,\"longitud\":-74.0531,\"categoriasAceptadas\":[\"SUPERIOR\",\"INFERIOR\",\"CALZADO\"],\"representanteId\":24,\"urlCertificadoPersoneriaJuridica\":\"https://styleradarblob.blob.core.windows.net/fundaciones/certificado-abrigo-solidario.pdf\",\"causasApoyadas\":\"Niñez vulnerable y adultos mayores\",\"horarioAtencion\":\"Lunes a Sábado de 08:00 a 17:00\",\"politicasDonacion\":\"Se reciben prendas limpias en estado Bueno, Muy bueno o Como nuevo\"}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fundación registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Representante inexistente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado o representante ya asignado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Representante con cuenta no activa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/api/v1/admin/fundaciones")
    ResponseEntity<FundacionResponseDTO> registrar(@Valid @RequestBody FundacionRequestDTO request);

    @Operation(
            summary = "Listar todas las fundaciones aliadas (SR-RF-63)",
            description = "ROLE_ADMIN_STYLERADAR. Retorna todas las fundaciones registradas en la plataforma, incluyendo activas y desactivadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fundaciones consultadas exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/api/v1/admin/fundaciones")
    ResponseEntity<List<FundacionResponseDTO>> listarTodas();

    @Operation(
            summary = "Editar fundación aliada (SR-RF-64)",
            description = "ROLE_ADMIN_STYLERADAR. Actualiza la información institucional de una fundación aliada (representante legal, teléfono, horarios y políticas de donación). Si cambia el representante, transfiere el rol REPRESENTANTE_FUNDACION.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fundación actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Fundación o representante inexistente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado o representante ya asignado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Representante con cuenta no activa", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/api/v1/admin/fundaciones/{id}")
    ResponseEntity<FundacionResponseDTO> editar(
            @Parameter(description = "Identificador de la fundación", example = "3") @PathVariable Long id,
            @Valid @RequestBody FundacionRequestDTO request);

    @Operation(
            summary = "Desactivar fundación aliada (SR-RF-65)",
            description = "ROLE_ADMIN_STYLERADAR. Desactiva lógicamente una fundación para impedir nuevas solicitudes de donación sin eliminar su historial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fundación desactivada exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Fundación inexistente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "La fundación ya estaba desactivada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/api/v1/admin/fundaciones/{id}/desactivar")
    ResponseEntity<FundacionResponseDTO> desactivar(
            @Parameter(description = "Identificador de la fundación a desactivar", example = "3") @PathVariable Long id);

    @Operation(
            summary = "Consultar fundaciones aliadas cercanas (SR-RF-43)",
            description = "ROLE_COMPRADOR (rol donante). Retorna únicamente las fundaciones aliadas activas ordenadas por cercanía a la ubicación indicada, con filtro opcional por categoría de prenda aceptada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fundaciones activas ordenadas por cercanía"),
            @ApiResponse(responseCode = "400", description = "Coordenadas ausentes o fuera de rango", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/api/v1/fundaciones")
    ResponseEntity<List<FundacionResponseDTO>> consultarCercanas(
            @Valid @ParameterObject @ModelAttribute ConsultaFundacionesRequestDTO request);
}
