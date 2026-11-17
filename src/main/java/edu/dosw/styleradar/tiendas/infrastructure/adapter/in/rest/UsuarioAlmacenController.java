package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionUsuarioValidator;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.UsuarioAlmacenApi;
import lombok.RequiredArgsConstructor;

/** Recursos de ms-tiendas que cuelgan de un usuario: {@code /api/v1/users/{userId}/store}. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsuarioAlmacenController implements UsuarioAlmacenApi {

    private static final AutorizacionUsuarioValidator AUTORIZACION = new AutorizacionUsuarioValidator();

    private final RegistrarAlmacenUseCase registrarAlmacenUseCase;

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> obtenerAlmacenDelUsuario(
            @PathVariable("userId") Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        Long solicitante = AuthRequestContextResolver.resolveUserId(usuarioId);
        String roles = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        AUTORIZACION.validarAccesoAlUsuario(solicitante, roles, userId);
        return ResponseEntity.ok(registrarAlmacenUseCase.obtenerMiAlmacen(userId));
    }
}
