package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.VerificacionConsultaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.VerificacionAlmacenApi;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/almacenes")
public class VerificacionAlmacenController implements VerificacionAlmacenApi {

    @InitBinder
    void validarEstado(WebDataBinder binder) {
        binder.registerCustomEditor(EstadoVerificacionAlmacen.class, new java.beans.PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(EstadoVerificacionAlmacen.valueOf(text));
            }
        });
    }

    @Override
    public ResponseEntity<PageResponseDTO<VerificacionAlmacenResponseDTO>> consultar(
            @Valid @ModelAttribute VerificacionConsultaRequestDTO request) {
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
    public ResponseEntity<VerificacionAlmacenResponseDTO> consultarUno(@PathVariable String nit) {
        return ResponseEntity.ok(new VerificacionAlmacenResponseDTO(
                nit,
                "Atelier Urbano Chapinero",
                "Moda independiente y prendas urbanas",
                "+57 601 312 4455",
                "contacto@atelierurbano.co",
                4.6533,
                -74.0602,
                4.8,
                Set.of(CategoriaAlmacen.CASUAL, CategoriaAlmacen.VINTAGE),
                EstadoVerificacionAlmacen.PENDIENTE,
                false,
                null,
                null,
                null));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> decidir(
            @PathVariable String nit,
            @Valid @RequestBody DecisionVerificacionRequestDTO request) {
        var nuevoEstado = request.getDecision() == DecisionVerificacionAlmacen.APROBADO
                ? EstadoVerificacionAlmacen.APROBADO
                : EstadoVerificacionAlmacen.RECHAZADO;
        return ResponseEntity.ok(new VerificacionAlmacenResponseDTO(
                nit,
                "Atelier Urbano Chapinero",
                "Moda independiente y prendas urbanas",
                "+57 601 312 4455",
                "contacto@atelierurbano.co",
                4.6533,
                -74.0602,
                4.8,
                Set.of(CategoriaAlmacen.CASUAL, CategoriaAlmacen.VINTAGE),
                nuevoEstado,
                false,
                Instant.now(),
                1L,
                request.getMotivo()));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> solicitarRevision(@PathVariable String nit) {
        return ResponseEntity.ok(new VerificacionAlmacenResponseDTO(
                nit,
                "Atelier Urbano Chapinero",
                "Moda independiente y prendas urbanas",
                "+57 601 312 4455",
                "contacto@atelierurbano.co",
                4.6533,
                -74.0602,
                4.8,
                Set.of(CategoriaAlmacen.CASUAL, CategoriaAlmacen.VINTAGE),
                EstadoVerificacionAlmacen.PENDIENTE,
                false,
                null,
                null,
                null));
    }
}

