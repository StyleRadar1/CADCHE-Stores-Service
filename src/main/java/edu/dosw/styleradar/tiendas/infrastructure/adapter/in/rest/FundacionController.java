package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.FundacionApi;
import jakarta.validation.Valid;

@RestController
public class FundacionController implements FundacionApi {

    @Override
    public ResponseEntity<FundacionResponseDTO> registrar(@Valid @RequestBody FundacionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(FundacionResponseDTO.builder()
                .id(3L)
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
                .fechaRegistro(Instant.now())
                .build());
    }

    @Override
    public ResponseEntity<List<FundacionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(List.of());
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> editar(
            @PathVariable Long id,
            @Valid @RequestBody FundacionRequestDTO request) {
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
                .fechaRegistro(Instant.now())
                .build());
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(FundacionResponseDTO.builder()
                .id(id)
                .nombre("Fundación Abrigo Solidario")
                .descripcion("Recolección de prendas en buen estado")
                .direccion("Calle 85 # 15-32, Chapinero, Bogotá")
                .telefono("+57 601 742 8899")
                .email("contacto@abrigosolidario.org")
                .latitud(4.6712)
                .longitud(-74.0531)
                .representanteId(24L)
                .activa(false)
                .fechaRegistro(Instant.now())
                .build());
    }

    @Override
    public ResponseEntity<List<FundacionResponseDTO>> consultarCercanas(
            @Valid @ModelAttribute ConsultaFundacionesRequestDTO request) {
        return ResponseEntity.ok(List.of());
    }
}
