package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;

public interface EditarFundacionUseCase {

    FundacionResponseDTO editar(Long id, FundacionRequestDTO request, String rolUsuario);
}

