package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.port.in.AsociarAdministradorAlmacenUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.AsociacionAdministradorAlmacenApi;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
public class AsociacionAdministradorAlmacenController implements AsociacionAdministradorAlmacenApi {

    private final AsociarAdministradorAlmacenUseCase asociarAdministradorUseCase;

    @Override
    public ResponseEntity<AdministradorAlmacenResponseDTO> asociar(
            String nit,
            Long usuarioId,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        return ResponseEntity.ok(asociarAdministradorUseCase.asociar(nit, usuarioId, adminId, rol));
    }

    @Override
    public ResponseEntity<List<AdministradorAlmacenResponseDTO>> listarAdministradores(
            String nit,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        return ResponseEntity.ok(asociarAdministradorUseCase.listarAdministradores(nit, usuarioId, rol));
    }
}
