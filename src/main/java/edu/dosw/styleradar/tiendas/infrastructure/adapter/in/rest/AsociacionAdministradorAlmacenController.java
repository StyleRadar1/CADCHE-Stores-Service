package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.AsociacionAdministradorAlmacenApi;

@RestController
@RequestMapping("/api/v1/admin/almacenes")
public class AsociacionAdministradorAlmacenController implements AsociacionAdministradorAlmacenApi {

    @Override
    public ResponseEntity<AdministradorAlmacenResponseDTO> asociar(
            String nit,
            Long usuarioId) {
        return ResponseEntity.ok(new AdministradorAlmacenResponseDTO(
                10L,
                usuarioId,
                nit,
                1L,
                Instant.now()));
    }

    @Override
    public ResponseEntity<List<AdministradorAlmacenResponseDTO>> listarAdministradores(String nit) {
        return ResponseEntity.ok(List.of(new AdministradorAlmacenResponseDTO(
                10L,
                15L,
                nit,
                1L,
                Instant.now())));
    }
}
