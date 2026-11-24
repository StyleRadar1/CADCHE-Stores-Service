package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.domain.port.in.EliminarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.EliminacionAlmacenApi;
import lombok.RequiredArgsConstructor;

/** SR-RF-119: DELETE /api/v1/admin/stores/{nit}. */
@RestController
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
public class EliminacionAlmacenController implements EliminacionAlmacenApi {

    private final EliminarAlmacenUseCase eliminarAlmacenUseCase;

    @Override
    public ResponseEntity<Void> eliminar(String nit, String rolesHeader, String roleHeader) {
        eliminarAlmacenUseCase.eliminar(nit, AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader));
        return ResponseEntity.noContent().build();
    }
}
