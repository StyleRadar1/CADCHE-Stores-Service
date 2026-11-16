package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;

public interface ConsultarUmbralUltimasUnidadesUseCase {

    ConfiguracionCatalogoAlmacenResponseDTO consultarConfiguracion(String nit);
}

