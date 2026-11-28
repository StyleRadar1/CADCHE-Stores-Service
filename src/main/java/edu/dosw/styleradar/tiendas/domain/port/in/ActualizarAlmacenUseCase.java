package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;

public interface ActualizarAlmacenUseCase {

    VerificacionAlmacenResponseDTO actualizarAlmacen(
            String nit,
            ActualizarAlmacenRequestDTO request,
            Long usuarioId,
            String rol);
}
