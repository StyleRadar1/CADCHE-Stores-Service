package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarFundacionInternaUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.FundacionInternaApi;

/** Rutas internas para ms-segunda-vida-donaciones; las protege {@code ServicioInternoAuthFilter}. */
@RestController
@RequestMapping("/api/v1/internal/foundations")
public class FundacionInternaController implements FundacionInternaApi {

    private final ConsultarFundacionInternaUseCase consulta;

    public FundacionInternaController(ConsultarFundacionInternaUseCase consulta) {
        this.consulta = consulta;
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> porId(Long id) {
        return ResponseEntity.ok(consulta.porId(id));
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> porRepresentante(Long representanteId) {
        return ResponseEntity.ok(consulta.porRepresentante(representanteId));
    }
}
