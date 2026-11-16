package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;

public interface ReactivarFundacionUseCase {

    FundacionResponseDTO reactivar(Long id, String rolUsuario);

    FundacionResponseDTO reactivar(Long id, String motivo, String rolUsuario);
}

