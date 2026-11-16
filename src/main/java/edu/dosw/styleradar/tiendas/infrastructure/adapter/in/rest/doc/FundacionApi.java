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

@Tag(name = "Fundaciones aliadas", description = "Registro, consulta individual, edición, desactivación y reactivación administrativa de fundaciones aliadas (SR-RF-63, SR-RF-64, SR-RF-65) y consulta por cercanía para donantes (SR-RF-43)")
@SecurityRequirement(name = "bearerAuth")
public interface FundacionApi {

    @Operation(
            summary = "Subir certificado de personería jurídica en PDF para una fundación (SR-RF-63)",
            description = "ROLE_ADMIN_STYLERADAR. Recibe un archivo PDF de máximo 5 MB y devuelve su URL pública en el repositorio de objetos.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Certificado PDF almacenado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo no es PDF o supera 5 MB", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_STYLERADAR", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping(value = "/api/v1/admin/fundaciones/certificados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentoSubidoResponseDTO> subirCertificado(
            @Parameter(description = "Archivo PDF del certificado de personería jurídica (máximo 5 MB)") @RequestPart("archivo") MultipartFile archivo,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Registrar fundación aliada (SR-RF-63)",
            description = "ROLE_ADMIN_STYLERADAR. Registra una fundación aliada con NIT, barrio, certificado de personería jurídica, causas apoyadas y cobertura geográfica.")
    @PostMapping("/api/v1/admin/fundaciones")
    ResponseEntity<FundacionResponseDTO> registrar(
            @Valid @RequestBody FundacionRequestDTO request,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Listar todas las fundaciones aliadas (SR-RF-63)",
            description = "ROLE_ADMIN_STYLERADAR. Retorna todas las fundaciones registradas en la plataforma.")
    @GetMapping("/api/v1/admin/fundaciones")
    ResponseEntity<List<FundacionResponseDTO>> listarTodas(
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Consultar detalle de una fundación aliada por ID (SR-RF-64)",
            description = "ROLE_ADMIN_STYLERADAR. Retorna los datos completos de la fundación aliada para precargar el formulario de edición.")
    @GetMapping("/api/v1/admin/fundaciones/{id}")
    ResponseEntity<FundacionResponseDTO> obtenerPorId(
            @Parameter(description = "Identificador de la fundación", example = "3") @PathVariable Long id,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Editar fundación aliada (SR-RF-64)",
            description = "ROLE_ADMIN_STYLERADAR. Actualiza la información institucional de una fundación aliada.")
    @PutMapping("/api/v1/admin/fundaciones/{id}")
    ResponseEntity<FundacionResponseDTO> editar(
            @Parameter(description = "Identificador de la fundación", example = "3") @PathVariable Long id,
            @Valid @RequestBody FundacionRequestDTO request,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Desactivar fundación aliada (SR-RF-65)",
            description = "ROLE_ADMIN_STYLERADAR. Desactiva lógicamente una fundación sin eliminar su historial y publica FundacionDesactivadaEvent.")
    @PatchMapping("/api/v1/admin/fundaciones/{id}/desactivar")
    ResponseEntity<FundacionResponseDTO> desactivar(
            @Parameter(description = "Identificador de la fundación a desactivar", example = "3") @PathVariable Long id,
            @Parameter(description = "Motivo de la desactivación", example = "Cierre temporal de operaciones por reestructuración")
            @RequestParam(value = "motivo", required = false) String motivo,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Reactivar fundación aliada (SR-RF-65)",
            description = "ROLE_ADMIN_STYLERADAR. Reactiva una fundación previamente desactivada, restableciendo su visibilidad en el directorio de donaciones y publicando FundacionReactivadaEvent.")
    @PatchMapping("/api/v1/admin/fundaciones/{id}/activar")
    ResponseEntity<FundacionResponseDTO> activar(
            @Parameter(description = "Identificador de la fundación a reactivar", example = "3") @PathVariable Long id,
            @Parameter(description = "Motivo de la reactivación", example = "Reapertura de recepción de donaciones")
            @RequestParam(value = "motivo", required = false) String motivo,
            @Parameter(in = ParameterIn.HEADER, description = "Roles del usuario (Requiere ROLE_ADMIN_STYLERADAR)", example = "ROLE_ADMIN_STYLERADAR")
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Parameter(in = ParameterIn.HEADER, hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Consultar fundaciones aliadas cercanas (SR-RF-43)",
            description = "ROLE_COMPRADOR (rol donante). Retorna únicamente las fundaciones aliadas activas ordenadas por cercanía.")
    @GetMapping("/api/v1/fundaciones")
    ResponseEntity<List<FundacionResponseDTO>> consultarCercanas(
            @Valid @ParameterObject @ModelAttribute ConsultaFundacionesRequestDTO request);
}
