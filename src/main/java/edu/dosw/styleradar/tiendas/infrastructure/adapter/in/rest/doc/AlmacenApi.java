package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import java.time.Instant;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarPeriodoRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarUmbralRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarNuevaLlegadaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarUltimasUnidadesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ReordenarFotografiasRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Almacenes aliados", description = "Registro de almacenes con RUT en PDF (SR-RF-62-A / SR-RF-62), consulta de mi almacén, configuración y evaluación de distintivos de catálogo (SR-RF-59, SR-RF-60) y fotografías de la tienda física (SR-RF-74)")
public interface AlmacenApi {

    @Operation(
            summary = "Consultar almacenes aliados verificados",
            description = "Público. Retorna de forma paginada los almacenes con estado de verificación APROBADO.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de almacenes consultada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Categoría o paginación inválida", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    ResponseEntity<PageResponseDTO<AlmacenResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute ConsultaAlmacenRequestDTO request);

    @Operation(
            summary = "Consultar mi almacén asociado al usuario autenticado (SR-RF-59 / SR-RF-62-A)",
            description = "ROLE_ADMIN_ALMACEN / TIENDA. Resuelve y retorna el almacén administrado por el usuario autenticado.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Almacén del usuario autenticado consultado exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario no administra ningún almacén", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/mi-almacen")
    ResponseEntity<VerificacionAlmacenResponseDTO> obtenerMiAlmacen(
            @Parameter(description = "ID del usuario autenticado", example = "10")
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(
            summary = "Subir documento RUT en PDF previo o asociado al registro de almacén (SR-RF-62-A)",
            description = "ROLE_ADMIN_ALMACEN / TIENDA. Recibe un archivo PDF de máximo 5 MB, lo almacena en el repositorio de objetos y devuelve su URL pública.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento RUT en PDF almacenado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo ausente, no es PDF o supera 5 MB", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Cuenta sin rol de tienda", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping(value = "/documentos/rut", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentoSubidoResponseDTO> subirDocumentoRut(
            @Parameter(description = "Archivo PDF del RUT (máximo 5 MB)") @RequestPart("archivo") MultipartFile archivo,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Registrar solicitud de vinculación de almacén aliado con NIT y RUT en PDF (SR-RF-62-A / SR-RF-62)",
            description = "ROLE_ADMIN_ALMACEN / TIENDA. Registra la información comercial del almacén, NIT y URL del RUT adjunto en PDF en estado PENDIENTE.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud de vinculación registrada en estado PENDIENTE"),
            @ApiResponse(responseCode = "400", description = "Datos comerciales, NIT o URL del RUT inválidos", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Solo una cuenta con rol de tienda puede registrar un almacén", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "El NIT ya está registrado o el usuario ya administra un almacén", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    ResponseEntity<VerificacionAlmacenResponseDTO> registrarAlmacen(
            @Valid @RequestBody RegistrarAlmacenRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(summary = "Consultar detalle de un almacén aliado por NIT", security = {})
    @GetMapping("/{nit}")
    ResponseEntity<AlmacenResponseDTO> obtenerPorNit(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit);

    @Operation(summary = "Consultar configuración de catálogo y umbrales (SR-RF-59 / SR-RF-60)", security = {})
    @GetMapping("/{nit}/configuracion-catalogo")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> obtenerConfiguracionCatalogo(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit);

    @Operation(
            summary = "Configurar umbrales de últimas unidades y días de nueva llegada (SR-RF-59 / SR-RF-60)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/{nit}/configuracion-catalogo")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> configurarCatalogo(
            @PathVariable String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ConfiguracionCatalogoAlmacenRequestDTO request);

    @Operation(
            summary = "Configurar exclusivamente el umbral de últimas unidades (SR-RF-59)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/{nit}/umbral-ultimas-unidades")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> actualizarUmbralUltimasUnidades(
            @PathVariable String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ActualizarUmbralRequestDTO request);

    @Operation(summary = "Consultar el umbral de últimas unidades (SR-RF-59)", security = {})
    @GetMapping("/{nit}/umbral-ultimas-unidades")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> consultarUmbralUltimasUnidades(
            @PathVariable String nit);

    @Operation(
            summary = "Configurar exclusivamente los días de nueva llegada (SR-RF-60)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/{nit}/dias-nueva-llegada")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> actualizarDiasNuevaLlegada(
            @PathVariable String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ActualizarPeriodoRequestDTO request);

    @Operation(summary = "Consultar el periodo de días de nueva llegada (SR-RF-60)", security = {})
    @GetMapping("/{nit}/dias-nueva-llegada")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> consultarDiasNuevaLlegada(
            @PathVariable String nit);

    @Operation(
            summary = "Evaluar y marcar prenda como últimas unidades según el umbral del almacén (SR-RF-59)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PatchMapping("/{nit}/prendas/{itemId}/ultimas-unidades")
    ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarUltimasUnidades(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @RequestParam(value = "stockActual", required = false) Integer stockActual,
            @Valid @RequestBody(required = false) EvaluarUltimasUnidadesRequestDTO body,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(
            summary = "Evaluar y marcar prenda como nueva llegada durante el periodo configurado del almacén (SR-RF-60)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PatchMapping("/{nit}/prendas/{itemId}/nueva-llegada")
    ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarNuevaLlegada(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @RequestParam(value = "fechaIngreso", required = false) Instant fechaIngreso,
            @Valid @RequestBody(required = false) EvaluarNuevaLlegadaRequestDTO body,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(
            summary = "Registrar fotografía de la tienda física (SR-RF-74)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PostMapping(value = "/{nit}/fotografias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FotografiaTiendaResponseDTO> registrarFotografia(
            @PathVariable String nit,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(summary = "Consultar fotografías de la tienda física (SR-RF-74)", security = {})
    @GetMapping("/{nit}/fotografias")
    ResponseEntity<List<FotografiaTiendaResponseDTO>> listarFotografias(
            @PathVariable String nit);

    @Operation(
            summary = "Reordenar fotografías de la tienda física y actualizar portada (SR-RF-74)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/{nit}/fotografias/orden")
    ResponseEntity<List<FotografiaTiendaResponseDTO>> reordenarFotografias(
            @PathVariable String nit,
            @Valid @RequestBody ReordenarFotografiasRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(
            summary = "Eliminar fotografía de la tienda física (SR-RF-74)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @DeleteMapping("/{nit}/fotografias/{fotoId}")
    ResponseEntity<Void> eliminarFotografia(
            @PathVariable String nit,
            @PathVariable Long fotoId,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);
}
