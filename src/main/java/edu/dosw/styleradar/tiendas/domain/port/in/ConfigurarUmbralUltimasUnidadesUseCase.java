package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;

public interface ConfigurarUmbralUltimasUnidadesUseCase {

    ConfiguracionCatalogoAlmacenResponseDTO configurarUmbral(String nit, Integer umbral, Long usuarioId);

    ConfiguracionCatalogoAlmacenResponseDTO configurarCatalogo(String nit, ConfiguracionCatalogoAlmacenRequestDTO request, Long usuarioId);
}
