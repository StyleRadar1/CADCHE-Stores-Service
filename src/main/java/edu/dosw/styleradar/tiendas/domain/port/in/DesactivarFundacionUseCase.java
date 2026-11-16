package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;

public interface DesactivarFundacionUseCase {

    FundacionResponseDTO desactivar(Long id, String rolUsuario);

    FundacionResponseDTO desactivar(Long id, String motivo, String rolUsuario);
}

