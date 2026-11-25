package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;

/** Consulta interna de almacén por administrador para llamadas servicio a servicio (BUG-005). */
public interface ConsultarAlmacenInternoUseCase {

    AlmacenResponseDTO porAdministrador(Long ownerUserId);
}
