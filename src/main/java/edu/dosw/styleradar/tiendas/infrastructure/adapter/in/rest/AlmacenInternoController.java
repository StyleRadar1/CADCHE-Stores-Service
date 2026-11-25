package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarAlmacenInternoUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.AlmacenInternoApi;

/** Rutas internas para microservicios StyleRadar; protegidas por {@code ServicioInternoAuthFilter}. */
@RestController
@RequestMapping("/api/v1/internal/stores")
public class AlmacenInternoController implements AlmacenInternoApi {

    private final ConsultarAlmacenInternoUseCase consulta;

    public AlmacenInternoController(ConsultarAlmacenInternoUseCase consulta) {
        this.consulta = consulta;
    }

    @Override
    public ResponseEntity<AlmacenResponseDTO> porAdministrador(Long ownerUserId) {
        return ResponseEntity.ok(consulta.porAdministrador(ownerUserId));
    }
}
