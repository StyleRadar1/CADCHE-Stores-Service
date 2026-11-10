package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarPeriodoRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarUmbralRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.ArchivoNoEsImagenException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFotografiaTiendaUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.AlmacenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/almacenes")
@RequiredArgsConstructor
public class AlmacenController implements AlmacenApi {

    private final ConfigurarUmbralUltimasUnidadesUseCase configurarUmbralUseCase;
    private final ConsultarUmbralUltimasUnidadesUseCase consultarUmbralUseCase;
    private final ConfigurarPeriodoNuevaLlegadaUseCase configurarPeriodoUseCase;
    private final ConsultarPeriodoNuevaLlegadaUseCase consultarPeriodoUseCase;
    private final RegistrarFotografiaTiendaUseCase registrarFotografiaUseCase;

    @Override
    public ResponseEntity<PageResponseDTO<AlmacenResponseDTO>> consultar(
            @Valid @ModelAttribute ConsultaAlmacenRequestDTO request) {
        return ResponseEntity.ok(new PageResponseDTO<>(
                List.of(),
                request.getPage(),
                request.getSize(),
                0L,
                0,
                true,
                true));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> registrarAlmacen(
            @Valid @RequestBody RegistrarAlmacenRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new VerificacionAlmacenResponseDTO(
                request.getNit(),
                request.getNombreComercial(),
                request.getDescripcion(),
                request.getTelefono(),
                request.getCorreoContacto(),
                request.getLatitud(),
                request.getLongitud(),
                0.0,
                request.getCategorias(),
                EstadoVerificacionAlmacen.PENDIENTE,
                false,
                null,
                null,
                null,
                request.getUrlRutPdf()));
    }

    @Override
    public ResponseEntity<AlmacenResponseDTO> obtenerPorNit(String nit) {
        return ResponseEntity.ok(new AlmacenResponseDTO(
                nit,
                "Atelier Urbano Chapinero",
                "Moda independiente y prendas urbanas en Chapinero",
                4.6533,
                -74.0602,
                4.8,
                Set.of(CategoriaAlmacen.CASUAL, CategoriaAlmacen.VINTAGE)));
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
        ConfiguracionCatalogoAlmacenResponseDTO response = configurarUmbralUseCase.configurarCatalogo(nit, request, usuarioId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> actualizarUmbralUltimasUnidades(
            String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @Valid @RequestBody ActualizarUmbralRequestDTO request) {
        ConfiguracionCatalogoAlmacenResponseDTO response = configurarUmbralUseCase.configurarUmbral(nit, request.getUmbralUltimasUnidades(), usuarioId);
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
        ConfiguracionCatalogoAlmacenResponseDTO response = configurarPeriodoUseCase.configurarPeriodo(nit, request.diasNuevaLlegada(), usuarioId);
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
            Long itemId) {
        return ResponseEntity.ok(new DistintivoPrendaAlmacenResponseDTO(
                nit,
                itemId,
                true,
                3,
                false,
                7,
                "ULTIMAS_UNIDADES",
                Instant.now()));
    }

    @Override
    public ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarNuevaLlegada(
            String nit,
            Long itemId) {
        return ResponseEntity.ok(new DistintivoPrendaAlmacenResponseDTO(
                nit,
                itemId,
                false,
                3,
                true,
                7,
                "NUEVA_PRENDA",
                Instant.now()));
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
            FotografiaTiendaResponseDTO response = registrarFotografiaUseCase.registrarFotografia(
                    nit,
                    archivo.getOriginalFilename(),
                    archivo.getContentType(),
                    archivo.getSize(),
                    archivo.getInputStream(),
                    usuarioId);
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
    public ResponseEntity<Void> eliminarFotografia(
            String nit,
            Long fotoId,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId) {
        registrarFotografiaUseCase.eliminarFotografia(nit, fotoId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
