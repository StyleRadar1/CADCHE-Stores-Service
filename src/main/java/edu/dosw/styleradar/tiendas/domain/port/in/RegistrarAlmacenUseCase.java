package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;

public interface RegistrarAlmacenUseCase {

    VerificacionAlmacenResponseDTO registrarAlmacen(RegistrarAlmacenRequestDTO request, Long usuarioId);

    AlmacenResponseDTO obtenerPorNit(String nit);

    PageResponseDTO<AlmacenResponseDTO> consultarAlmacenes(ConsultaAlmacenRequestDTO request);
}

