package edu.dosw.styleradar.tiendas.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;

public interface RegistrarFundacionUseCase {

    FundacionResponseDTO registrar(FundacionRequestDTO request, String rol);

    List<FundacionResponseDTO> listarTodas(String rol);
}
