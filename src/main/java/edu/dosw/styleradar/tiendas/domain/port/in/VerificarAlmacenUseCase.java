package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.VerificacionConsultaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;

public interface VerificarAlmacenUseCase {

    VerificacionAlmacenResponseDTO decidir(String nit, DecisionVerificacionRequestDTO request, Long adminId, String rol);

    VerificacionAlmacenResponseDTO consultarUno(String nit, String rol);

    PageResponseDTO<VerificacionAlmacenResponseDTO> consultar(VerificacionConsultaRequestDTO request, String rol);

    VerificacionAlmacenResponseDTO solicitarRevision(String nit, Long adminId, String rol);
}
