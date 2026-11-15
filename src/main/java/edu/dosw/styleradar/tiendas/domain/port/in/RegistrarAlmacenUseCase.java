package edu.dosw.styleradar.tiendas.domain.port.in;

import java.io.InputStream;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;

public interface RegistrarAlmacenUseCase {

    VerificacionAlmacenResponseDTO registrarAlmacen(RegistrarAlmacenRequestDTO request, Long usuarioId);

    default VerificacionAlmacenResponseDTO registrarAlmacen(RegistrarAlmacenRequestDTO request, Long usuarioId, String rol) {
        return registrarAlmacen(request, usuarioId);
    }

    default VerificacionAlmacenResponseDTO obtenerMiAlmacen(Long usuarioId) {
        throw new UnsupportedOperationException("No implementado");
    }

    default DocumentoSubidoResponseDTO subirDocumentoRut(
            String nombreOriginal, String contentType, long tamano, InputStream contenido, Long usuarioId, String rol) {
        throw new UnsupportedOperationException("No implementado");
    }

    AlmacenResponseDTO obtenerPorNit(String nit);

    PageResponseDTO<AlmacenResponseDTO> consultarAlmacenes(ConsultaAlmacenRequestDTO request);
}
