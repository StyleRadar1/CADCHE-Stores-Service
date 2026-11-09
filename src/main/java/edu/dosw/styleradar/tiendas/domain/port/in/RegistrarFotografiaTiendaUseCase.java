package edu.dosw.styleradar.tiendas.domain.port.in;

import java.io.InputStream;
import java.util.List;

import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;

public interface RegistrarFotografiaTiendaUseCase {

    FotografiaTiendaResponseDTO registrarFotografia(
            String nit,
            String nombreOriginal,
            String contentType,
            long tamano,
            InputStream contenido,
            Long usuarioId);

    List<FotografiaTiendaResponseDTO> listarFotografias(String nit);

    void eliminarFotografia(String nit, Long fotoId, Long usuarioId);
}

