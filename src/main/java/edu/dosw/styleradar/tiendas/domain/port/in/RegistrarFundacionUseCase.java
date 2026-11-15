package edu.dosw.styleradar.tiendas.domain.port.in;

import java.io.InputStream;
import java.util.List;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;

public interface RegistrarFundacionUseCase {

    FundacionResponseDTO registrar(FundacionRequestDTO request, String rol);

    List<FundacionResponseDTO> listarTodas(String rol);

    default FundacionResponseDTO obtenerPorId(Long id, String rol) {
        throw new UnsupportedOperationException("No implementado");
    }

    default DocumentoSubidoResponseDTO subirCertificado(
            String nombreOriginal, String contentType, long tamano, InputStream contenido, String rol) {
        throw new UnsupportedOperationException("No implementado");
    }

    List<FundacionResponseDTO> consultarCercanas(ConsultaFundacionesRequestDTO request);
}
