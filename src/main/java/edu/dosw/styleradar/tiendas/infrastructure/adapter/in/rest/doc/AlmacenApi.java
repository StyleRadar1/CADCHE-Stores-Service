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

@Tag(name = "Stores", description = "Store registration with RUT in PDF (SR-RF-62-A / SR-RF-62), store lookup, catalog badge settings and evaluation (SR-RF-59, SR-RF-60) and physical-store photos (SR-RF-74)")
public interface AlmacenApi {

    @Operation(
            summary = "Consultar almacenes aliados verificados",
            description = "Public. Returns the APPROVED stores, paginated.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of stores returned"),
            @ApiResponse(responseCode = "400", description = "Invalid category or pagination", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    ResponseEntity<PageResponseDTO<AlmacenResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute ConsultaAlmacenRequestDTO request);

    @Operation(
            summary = "Upload the RUT in PDF before or during store registration (SR-RF-62-A)",
            description = "ROLE_STORE_ADMIN. Receives a PDF of at most 5 MB, stores it in object storage and returns its public URL.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "RUT in PDF stored"),
            @ApiResponse(responseCode = "400", description = "File missing, not a PDF or larger than 5 MB", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT missing or invalid", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Account without the store role", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping(value = "/documents/rut", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentoSubidoResponseDTO> subirDocumentoRut(
            @Parameter(description = "RUT PDF file (max 5 MB)") @RequestPart("file") MultipartFile archivo,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(
            summary = "Register a store for verification with NIT and RUT in PDF (SR-RF-62-A / SR-RF-62)",
            description = "ROLE_STORE_ADMIN. Registers the store business data, NIT and RUT URL; the store starts as PENDING.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Store registered as PENDING"),
            @ApiResponse(responseCode = "400", description = "Invalid business data, NIT or RUT URL", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Only a store account can register a store", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "The NIT is already registered or the user already administers a store", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    ResponseEntity<VerificacionAlmacenResponseDTO> registrarAlmacen(
            @Valid @RequestBody RegistrarAlmacenRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);

    @Operation(summary = "Get a partner store by NIT", security = {})
    @GetMapping("/{nit}")
    ResponseEntity<AlmacenResponseDTO> obtenerPorNit(
            @Parameter(description = "NIT of the partner store", example = "900123456-1") @PathVariable String nit);

    @Operation(summary = "Get the catalog settings and thresholds (SR-RF-59 / SR-RF-60)", security = {})
    @GetMapping("/{nit}/catalog-settings")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> obtenerConfiguracionCatalogo(
            @Parameter(description = "NIT of the partner store", example = "900123456-1") @PathVariable String nit);

    @Operation(
            summary = "Set the low-stock threshold and new-arrival days (SR-RF-59 / SR-RF-60)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/{nit}/catalog-settings")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> configurarCatalogo(
            @PathVariable String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ConfiguracionCatalogoAlmacenRequestDTO request);

    @Operation(
            summary = "Set only the low-stock threshold (SR-RF-59)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/{nit}/low-stock-threshold")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> actualizarUmbralUltimasUnidades(
            @PathVariable String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ActualizarUmbralRequestDTO request);

    @Operation(summary = "Get the low-stock threshold (SR-RF-59)", security = {})
    @GetMapping("/{nit}/low-stock-threshold")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> consultarUmbralUltimasUnidades(
            @PathVariable String nit);

    @Operation(
            summary = "Set only the new-arrival days (SR-RF-60)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/{nit}/new-arrival-days")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> actualizarDiasNuevaLlegada(
            @PathVariable String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ActualizarPeriodoRequestDTO request);

    @Operation(summary = "Get the new-arrival days (SR-RF-60)", security = {})
    @GetMapping("/{nit}/new-arrival-days")
    ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> consultarDiasNuevaLlegada(
            @PathVariable String nit);

    @Operation(
            summary = "Evaluate the low-stock badge of an item with the store threshold (SR-RF-59)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PatchMapping("/{nit}/items/{itemId}/low-stock")
    ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarUltimasUnidades(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @RequestParam(value = "currentStock", required = false) Integer stockActual,
            @Valid @RequestBody(required = false) EvaluarUltimasUnidadesRequestDTO body,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(
            summary = "Evaluate the new-arrival badge of an item within the store period (SR-RF-60)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PatchMapping("/{nit}/items/{itemId}/new-arrival")
    ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarNuevaLlegada(
            @PathVariable String nit,
            @PathVariable Long itemId,
            @RequestParam(value = "arrivalDate", required = false) Instant fechaIngreso,
            @Valid @RequestBody(required = false) EvaluarNuevaLlegadaRequestDTO body,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(
            summary = "Upload a physical-store photo (SR-RF-74)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PostMapping(value = "/{nit}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FotografiaTiendaResponseDTO> registrarFotografia(
            @PathVariable String nit,
            @RequestPart("file") MultipartFile archivo,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(summary = "List the physical-store photos (SR-RF-74)", security = {})
    @GetMapping("/{nit}/photos")
    ResponseEntity<List<FotografiaTiendaResponseDTO>> listarFotografias(
            @PathVariable String nit);

    @Operation(
            summary = "Reorder the physical-store photos and update the cover (SR-RF-74)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/{nit}/photos/order")
    ResponseEntity<List<FotografiaTiendaResponseDTO>> reordenarFotografias(
            @PathVariable String nit,
            @Valid @RequestBody ReordenarFotografiasRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);

    @Operation(
            summary = "Delete a physical-store photo (SR-RF-74)",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @DeleteMapping("/{nit}/photos/{photoId}")
    ResponseEntity<Void> eliminarFotografia(
            @PathVariable String nit,
            @PathVariable("photoId") Long fotoId,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId);
}
