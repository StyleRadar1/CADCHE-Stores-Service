package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;

/**
 * Consulta de fundaciones para otros microservicios (canal interno con credenciales de servicio). Devuelve la vista
 * completa, con representante y estado, que la consulta pública no expone (SR-RF-117).
 */
public interface ConsultarFundacionInternaUseCase {

    FundacionResponseDTO porId(Long id);

    FundacionResponseDTO porRepresentante(Long representanteId);
}
