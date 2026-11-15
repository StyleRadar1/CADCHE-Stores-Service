package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
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
import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.ArchivoNoEsImagenException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.MarcarDistintivoPrendaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFotografiaTiendaUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.AlmacenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/almacenes")
@RequiredArgsConstructor
public class AlmacenController implements AlmacenApi {

    private final RegistrarAlmacenUseCase registrarAlmacenUseCase;
    private final ConfigurarUmbralUltimasUnidadesUseCase configurarUmbralUseCase;
    private final ConsultarUmbralUltimasUnidadesUseCase consultarUmbralUseCase;
    private final ConfigurarPeriodoNuevaLlegadaUseCase configurarPeriodoUseCase;
    private final ConsultarPeriodoNuevaLlegadaUseCase consultarPeriodoUseCase;
    private final MarcarDistintivoPrendaUseCase marcarDistintivoUseCase;
    private final RegistrarFotografiaTiendaUseCase registrarFotografiaUseCase;

    @Override
    public ResponseEntity<PageResponseDTO<AlmacenResponseDTO>> consultar(
            @Valid @ModelAttribute ConsultaAlmacenRequestDTO request) {
        return ResponseEntity.ok(registrarAlmacenUseCase.consultarAlmacenes(request));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> obtenerMiAlmacen(
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        Long resolvedId = resolveUserId(usuarioId);
        return ResponseEntity.ok(registrarAlmacenUseCase.obtenerMiAlmacen(resolvedId));
    }

    @Override
    public ResponseEntity<DocumentoSubidoResponseDTO> subirDocumentoRut(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaDeNegocioException("El RUT debe ser un archivo PDF de máximo 5 MB");
        }
        try {
            Long resolvedId = resolveUserId(usuarioId);
            String rol = resolveRole(rolesHeader, roleHeader);
            DocumentoSubidoResponseDTO response = registrarAlmacenUseCase.subirDocumentoRut(
                    archivo.getOriginalFilename(),
                    archivo.getContentType(),
                    archivo.getSize(),
                    archivo.getInputStream(),
                    resolvedId,
                    rol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (java.io.IOException ex) {
            throw new ReglaDeNegocioException("Error al leer el archivo PDF del RUT: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> registrarAlmacen(
            @Valid @RequestBody RegistrarAlmacenRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        Long resolvedId = resolveUserId(usuarioId);
        String rol = resolveRole(rolesHeader, roleHeader);
        VerificacionAlmacenResponseDTO response = (rol != null && !rol.isBlank())
                ? registrarAlmacenUseCase.registrarAlmacen(request, resolvedId, rol)
                : registrarAlmacenUseCase.registrarAlmacen(request, resolvedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<AlmacenResponseDTO> obtenerPorNit(String nit) {
        return ResponseEntity.ok(registrarAlmacenUseCase.obtenerPorNit(nit));
    }

    @Override
    public ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> obtenerConfiguracionCatalogo(String nit) {
        ConfiguracionCatalogoAlmacenResponseDTO response = consultarUmbralUseCase.consultarConfiguracion(nit);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> configurarCatalogo(
            String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ConfiguracionCatalogoAlmacenRequestDTO request) {
        Long resolvedId = resolveUserId(usuarioId);
        ConfiguracionCatalogoAlmacenResponseDTO response = configurarUmbralUseCase.configurarCatalogo(nit, request, resolvedId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> actualizarUmbralUltimasUnidades(
            String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ActualizarUmbralRequestDTO request) {
        Long resolvedId = resolveUserId(usuarioId);
        ConfiguracionCatalogoAlmacenResponseDTO response = configurarUmbralUseCase.configurarUmbral(
                nit, request.getUmbralUltimasUnidades(), resolvedId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> consultarUmbralUltimasUnidades(String nit) {
        ConfiguracionCatalogoAlmacenResponseDTO response = consultarUmbralUseCase.consultarConfiguracion(nit);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> actualizarDiasNuevaLlegada(
            String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ActualizarPeriodoRequestDTO request) {
        Long resolvedId = resolveUserId(usuarioId);
        ConfiguracionCatalogoAlmacenResponseDTO response = configurarPeriodoUseCase.configurarPeriodo(
                nit, request.diasNuevaLlegada(), resolvedId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> consultarDiasNuevaLlegada(String nit) {
        ConfiguracionCatalogoAlmacenResponseDTO response = consultarPeriodoUseCase.consultarPeriodo(nit);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarUltimasUnidades(
            String nit,
            Long itemId,
            @RequestParam(value = "stockActual", required = false) Integer stockActual,
            @Valid @RequestBody(required = false) EvaluarUltimasUnidadesRequestDTO body,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        Long resolvedId = resolveUserId(usuarioId);
        DistintivoPrendaAlmacenResponseDTO response = (body != null && body.getStockActual() != null)
                ? marcarDistintivoUseCase.evaluarUltimasUnidades(nit, itemId, body, resolvedId)
                : marcarDistintivoUseCase.marcarUltimasUnidades(nit, itemId, stockActual, resolvedId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarNuevaLlegada(
            String nit,
            Long itemId,
            @RequestParam(value = "fechaIngreso", required = false) Instant fechaIngreso,
            @Valid @RequestBody(required = false) EvaluarNuevaLlegadaRequestDTO body,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        Long resolvedId = resolveUserId(usuarioId);
        DistintivoPrendaAlmacenResponseDTO response = (body != null && body.getFechaPublicacion() != null)
                ? marcarDistintivoUseCase.evaluarNuevaLlegada(nit, itemId, body, resolvedId)
                : marcarDistintivoUseCase.marcarNuevaLlegada(nit, itemId, fechaIngreso, resolvedId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FotografiaTiendaResponseDTO> registrarFotografia(
            String nit,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoNoEsImagenException("Se requiere adjuntar un archivo de imagen válido");
        }
        try {
            Long resolvedId = resolveUserId(usuarioId);
            FotografiaTiendaResponseDTO response = registrarFotografiaUseCase.registrarFotografia(
                    nit,
                    archivo.getOriginalFilename(),
                    archivo.getContentType(),
                    archivo.getSize(),
                    archivo.getInputStream(),
                    resolvedId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (java.io.IOException ex) {
            throw new ReglaDeNegocioException("Error al leer el contenido del archivo de imagen: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<FotografiaTiendaResponseDTO>> listarFotografias(String nit) {
        return ResponseEntity.ok(registrarFotografiaUseCase.listarFotografias(nit));
    }

    @Override
    public ResponseEntity<List<FotografiaTiendaResponseDTO>> reordenarFotografias(
            String nit,
            @Valid @RequestBody ReordenarFotografiasRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        Long resolvedId = resolveUserId(usuarioId);
        List<FotografiaTiendaResponseDTO> response = registrarFotografiaUseCase.reordenarFotografias(
                nit, request.getFotoIds(), resolvedId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> eliminarFotografia(
            String nit,
            Long fotoId,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        Long resolvedId = resolveUserId(usuarioId);
        registrarFotografiaUseCase.eliminarFotografia(nit, fotoId, resolvedId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Long headerUserId) {
        return AuthRequestContextResolver.resolveUserId(headerUserId);
    }

    private String resolveRole(String rolesHeader, String roleHeader) {
        return AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
    }
}
