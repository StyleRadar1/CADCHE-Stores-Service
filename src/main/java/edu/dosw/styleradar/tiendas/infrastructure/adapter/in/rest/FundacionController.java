package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.FundacionApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FundacionController implements FundacionApi {

    private final RegistrarFundacionUseCase registrarFundacionUseCase;

    @Override
    public ResponseEntity<FundacionResponseDTO> registrar(
            @Valid @RequestBody FundacionRequestDTO request,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = resolveRole(rolesHeader, roleHeader);
        FundacionResponseDTO creada = registrarFundacionUseCase.registrar(request, rol);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @Override
    public ResponseEntity<List<FundacionResponseDTO>> listarTodas(
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = resolveRole(rolesHeader, roleHeader);
        return ResponseEntity.ok(registrarFundacionUseCase.listarTodas(rol));
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> editar(
            @PathVariable Long id,
            @Valid @RequestBody FundacionRequestDTO request) {
        // Enpoint placeholder para SR-RF-64
        return ResponseEntity.ok(FundacionResponseDTO.builder()
                .id(id)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .categoriasAceptadas(request.getCategoriasAceptadas())
                .representanteId(request.getRepresentanteId())
                .urlCertificadoPersoneriaJuridica(request.getUrlCertificadoPersoneriaJuridica())
                .causasApoyadas(request.getCausasApoyadas())
                .horarioAtencion(request.getHorarioAtencion())
                .politicasDonacion(request.getPoliticasDonacion())
                .activa(true)
                .build());
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> desactivar(@PathVariable Long id) {
        // Enpoint placeholder para SR-RF-65
        return ResponseEntity.ok(FundacionResponseDTO.builder()
                .id(id)
                .nombre("Fundación Desactivada")
                .activa(false)
                .build());
    }

    @Override
    public ResponseEntity<List<FundacionResponseDTO>> consultarCercanas(
            @Valid @ModelAttribute ConsultaFundacionesRequestDTO request) {
        return ResponseEntity.ok(List.of());
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
