package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;

public interface ConfigurarPeriodoNuevaLlegadaUseCase {

    ConfiguracionCatalogoAlmacenResponseDTO configurarPeriodo(String nit, Integer dias, Long usuarioId);
}
