package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.AlmacenApi;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/almacenes")
public class AlmacenController implements AlmacenApi {

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
    public ResponseEntity<AlmacenResponseDTO> obtenerPorNit(@PathVariable String nit) {
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
    public ResponseEntity<ConfiguracionCatalogoAlmacenResponseDTO> configurarCatalogo(
            @PathVariable String nit,
            @Valid @RequestBody ConfiguracionCatalogoAlmacenRequestDTO request) {
        return ResponseEntity.ok(new ConfiguracionCatalogoAlmacenResponseDTO(
                nit,
                request.getUmbralUltimasUnidades(),
                request.getDiasNuevaLlegada(),
                Instant.now()));
    }

    @Override
    public ResponseEntity<DistintivoPrendaAlmacenResponseDTO> marcarUltimasUnidades(
            @PathVariable String nit,
            @PathVariable Long itemId) {
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
            @PathVariable String nit,
            @PathVariable Long itemId) {
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
            @PathVariable String nit,
            @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new FotografiaTiendaResponseDTO(
                1L,
                nit,
                "https://styleradar.blob.core.windows.net/tiendas/" + nit + "/foto-1.webp",
                1,
                true,
                Instant.now()));
    }

    @Override
    public ResponseEntity<List<FotografiaTiendaResponseDTO>> listarFotografias(@PathVariable String nit) {
        return ResponseEntity.ok(List.of());
    }

    @Override
    public ResponseEntity<Void> eliminarFotografia(@PathVariable String nit, @PathVariable Long fotoId) {
        return ResponseEntity.noContent().build();
    }
}
