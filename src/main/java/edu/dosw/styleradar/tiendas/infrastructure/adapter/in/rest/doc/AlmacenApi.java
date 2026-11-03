package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;
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

@Tag(name = "Almacenes aliados", description = "Registro de almacenes con RUT en PDF (SR-RF-62), consulta pública, configuración y marcación de distintivos de catálogo (SR-RF-59, SR-RF-60) y fotografías de la tienda física (SR-RF-74)")
public interface AlmacenApi {

    @Operation(
            summary = "Consultar almacenes aliados verificados",
            description = "Público. Retorna de forma paginada los almacenes con estado de verificación APROBADO y coordenadas válidas. "
                    + "Permite filtrar opcionalmente por categoría comercial. Orden nombreComercial ASC, nit ASC.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de almacenes consultada exitosamente"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Categoría o paginación inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error inesperado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    ResponseEntity<PageResponseDTO<AlmacenResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute ConsultaAlmacenRequestDTO request);

    @Operation(
            summary = "Registrar solicitud de vinculación de almacén aliado con NIT y RUT en PDF (SR-RF-62)",
            description = "ROLE_ADMIN_ALMACEN o Público en proceso de vinculación. Registra la información comercial del almacén, NIT y URL del RUT adjunto en PDF "
                    + "en estado PENDIENTE para posterior revisión y aprobación por el Administrador StyleRadar (SR-RF-62).",
            security = {@SecurityRequirement(name = "bearerAuth")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegistrarAlmacenRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "SolicitudVinculacionAlmacen",
                                    value = "{\"nit\":\"900123456-1\",\"nombreComercial\":\"Atelier Urbano Chapinero\",\"descripcion\":\"Moda independiente y prendas urbanas en Chapinero\",\"telefono\":\"+57 601 312 4455\",\"correoContacto\":\"contacto@atelierurbano.co\",\"latitud\":4.6533,\"longitud\":-74.0602,\"categorias\":[\"CASUAL\",\"VINTAGE\"],\"urlRutPdf\":\"https://styleradarblob.blob.core.windows.net/documentos-rut/900123456-1/rut-atelier-urbano.pdf\"}"))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Solicitud de vinculación del almacén registrada en estado PENDIENTE de verificación",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VerificacionAlmacenResponseDTO.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos comerciales, NIT o URL del RUT inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "El NIT indicado ya se encuentra registrado en la plataforma",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    ResponseEntity<VerificacionAlmacenResponseDTO> registrarAlmacen(@Valid @RequestBody RegistrarAlmacenRequestDTO request);

    @Operation(
            summary = "Consultar detalle de un almacén aliado por NIT",
            description = "Público. Retorna la información comercial básica del almacén verificado identificado por su NIT.",
            security = {})
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle del almacén consultado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlmacenResponseDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Almacén inexistente o no aprobado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{nit}")
    ResponseEntity<AlmacenResponseDTO> obtenerPorNit(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit);

    @Operation(
            summary = "Configurar umbrales de últimas unidades y días de nueva llegada (SR-RF-59 / SR-RF-60)",
            description = "ROLE_ADMIN_ALMACEN propietario del NIT. Configura el umbral personalizado de stock bajo para marcar prendas como 'Últimas unidades' "
                    + "(por defecto <= 3 unidades, SR-RF-59) y la ventana temporal en días para marcar publicaciones recientes como 'Nueva llegada' (por defecto 7 días, SR-RF-60).",
            security = {@SecurityRequirement(name = "bearerAuth")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracionCatalogoAlmacenRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "ConfiguracionDistintivos",
                                    value = "{\"umbralUltimasUnidades\":3,\"diasNuevaLlegada\":7}"))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuración del almacén actualizada correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracionCatalogoAlmacenResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ConfiguracionActualizada",
                                    value = "{\"nit\":\"900123456-1\",\"umbralUltimasUnidades\":3,\"diasNuevaLlegada\":7,\"fechaActualizacion\":\"2026-11-03T01:40:00Z\"}"))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Umbral o ventana de días fuera del rango permitido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT ausente, inválido o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene rol ADMIN_ALMACEN o no está asociado al NIT indicado (prevención BOLA/IDOR)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Almacén inexistente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{nit}/configuracion-catalogo")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> configurarCatalogo(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit,
            @Valid @RequestBody ConfiguracionCatalogoAlmacenRequestDTO request);

    @Operation(
            summary = "Marcar prenda como últimas unidades según el umbral del almacén (SR-RF-59)",
            description = "ROLE_ADMIN_ALMACEN propietario del NIT. Identifica y marca una prenda del almacén como 'Últimas unidades' (ULTIMAS_UNIDADES) "
                    + "cuando su inventario alcanza el umbral personalizado definido para el almacén (por defecto <= 3 unidades).",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Prenda marcada como últimas unidades según el umbral del almacén",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DistintivoPrendaAlmacenResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PrendaUltimasUnidades",
                                    value = "{\"nit\":\"900123456-1\",\"itemId\":104,\"ultimasUnidades\":true,\"umbralUltimasUnidades\":3,\"nuevaLlegada\":false,\"diasNuevaLlegada\":7,\"distintivoActivo\":\"ULTIMAS_UNIDADES\",\"fechaActualizacion\":\"2026-11-03T01:45:00Z\"}"))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT ausente, inválido o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "El administrador autenticado no administra el almacén del NIT indicado (prevención BOLA/IDOR)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Almacén o prenda inexistente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "El inventario actual de la prenda supera el umbral de últimas unidades configurado para el almacén",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{nit}/prendas/{itemId}/ultimas-unidades")
    ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarUltimasUnidades(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit,
            @Parameter(description = "Identificador de la prenda o ítem de catálogo", example = "104") @PathVariable Long itemId);

    @Operation(
            summary = "Marcar prenda como nueva llegada durante el periodo configurado del almacén (SR-RF-60)",
            description = "ROLE_ADMIN_ALMACEN propietario del NIT. Identifica y marca una prenda del almacén como 'Nueva llegada' (NUEVA_PRENDA) "
                    + "durante el periodo temporal configurado para ese distintivo en la tienda (por defecto 7 días).",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Prenda marcada como nueva llegada dentro del periodo configurado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DistintivoPrendaAlmacenResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PrendaNuevaLlegada",
                                    value = "{\"nit\":\"900123456-1\",\"itemId\":104,\"ultimasUnidades\":false,\"umbralUltimasUnidades\":3,\"nuevaLlegada\":true,\"diasNuevaLlegada\":7,\"distintivoActivo\":\"NUEVA_PRENDA\",\"fechaActualizacion\":\"2026-11-03T01:45:00Z\"}"))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT ausente, inválido o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "El administrador autenticado no administra el almacén del NIT indicado (prevención BOLA/IDOR)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Almacén o prenda inexistente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "La fecha de publicación de la prenda supera el periodo de nueva llegada configurado para el almacén",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{nit}/prendas/{itemId}/nueva-llegada")
    ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarNuevaLlegada(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit,
            @Parameter(description = "Identificador de la prenda o ítem de catálogo", example = "104") @PathVariable Long itemId);

    @Operation(
            summary = "Registrar fotografía de la tienda física (SR-RF-74)",
            description = "ROLE_ADMIN_ALMACEN propietario del NIT. Permite cargar hasta 6 fotografías de la tienda física en formato multipart. "
                    + "La primera fotografía registrada se asigna automáticamente como portada de la página del almacén.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Fotografía de la tienda física registrada correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FotografiaTiendaResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "FotografiaCreada",
                                    value = "{\"id\":1,\"almacenNit\":\"900123456-1\",\"url\":\"https://styleradar.blob.core.windows.net/tiendas/900123456-1/foto-1.webp\",\"orden\":1,\"portada\":true,\"fechaRegistro\":\"2026-11-03T01:45:00Z\"}"))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parte multipart ausente o inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT ausente, inválido o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "El administrador autenticado no es propietario del NIT indicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Almacén inexistente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Se alcanzó el límite máximo de 6 fotografías por tienda física o el tipo MIME no es válido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "LimiteFotografias",
                                    value = "{\"timestamp\":\"2026-11-03T01:45:00\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"El almacén ya cuenta con el máximo permitido de 6 fotografías de tienda física\",\"path\":\"/api/v1/almacenes/900123456-1/fotografias\",\"validationErrors\":{}}"))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error al almacenar la fotografía",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping(value = "/{nit}/fotografias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FotografiaTiendaResponseDTO> registrarFotografia(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit,
            @Parameter(description = "Archivo de imagen de la tienda física (JPEG, PNG o WebP)") @RequestPart("archivo") MultipartFile archivo);

    @Operation(
            summary = "Consultar fotografías de la tienda física (SR-RF-74)",
            description = "Público. Retorna hasta 6 fotografías registradas de la tienda física ordenadas por posición, identificando la imagen de portada.",
            security = {})
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Galería de fotografías del almacén consultada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FotografiaTiendaResponseDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Almacén inexistente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{nit}/fotografias")
    ResponseEntity<List<FotografiaTiendaResponseDTO>> listarFotografias(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit);

    @Operation(
            summary = "Eliminar fotografía de la tienda física (SR-RF-74)",
            description = "ROLE_ADMIN_ALMACEN propietario del NIT. Elimina una fotografía de la tienda física y reasigna la portada si la primera imagen fue eliminada.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fotografía eliminada correctamente"),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT ausente, inválido o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "El administrador autenticado no es propietario del NIT indicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Almacén o fotografía inexistente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{nit}/fotografias/{fotoId}")
    ResponseEntity<Void> eliminarFotografia(
            @Parameter(description = "NIT del almacén aliado", example = "900123456-1") @PathVariable String nit,
            @Parameter(description = "Identificador de la fotografía", example = "1") @PathVariable Long fotoId);
}
