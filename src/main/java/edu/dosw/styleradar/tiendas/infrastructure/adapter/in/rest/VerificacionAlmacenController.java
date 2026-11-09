package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.VerificacionConsultaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.in.VerificarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.VerificacionAlmacenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/almacenes")
@RequiredArgsConstructor
public class VerificacionAlmacenController implements VerificacionAlmacenApi {

    private final VerificarAlmacenUseCase verificarAlmacenUseCase;

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
            @Valid @ModelAttribute VerificacionConsultaRequestDTO request,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = resolveRole(rolesHeader, roleHeader);
        return ResponseEntity.ok(verificarAlmacenUseCase.consultar(request, rol));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> consultarUno(
            @PathVariable String nit,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = resolveRole(rolesHeader, roleHeader);
        return ResponseEntity.ok(verificarAlmacenUseCase.consultarUno(nit, rol));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> decidir(
            @PathVariable String nit,
            @Valid @RequestBody DecisionVerificacionRequestDTO request,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {
        String rol = resolveRole(rolesHeader, roleHeader);
        Long idAdmin = adminId != null ? adminId : 1L;
        return ResponseEntity.ok(verificarAlmacenUseCase.decidir(nit, request, idAdmin, rol));
    }

    @Override
    public ResponseEntity<VerificacionAlmacenResponseDTO> solicitarRevision(
            @PathVariable String nit,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {
        String rol = resolveRole(rolesHeader, roleHeader);
        Long idAdmin = adminId != null ? adminId : 1L;
        return ResponseEntity.ok(verificarAlmacenUseCase.solicitarRevision(nit, idAdmin, rol));
    }

    private String resolveRole(String rolesHeader, String roleHeader) {
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            return rolesHeader;
        }
        if (roleHeader != null && !roleHeader.isBlank()) {
            return roleHeader;
        }
        return null;
    }
}
