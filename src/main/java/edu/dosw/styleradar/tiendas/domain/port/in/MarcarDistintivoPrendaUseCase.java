package edu.dosw.styleradar.tiendas.domain.port.in;

import java.time.Instant;

import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;

public interface MarcarDistintivoPrendaUseCase {

    DistintivoPrendaAlmacenResponseDTO marcarUltimasUnidades(
            String nit, Long itemId, Integer stockActual, Long usuarioId);

    DistintivoPrendaAlmacenResponseDTO marcarNuevaLlegada(
            String nit, Long itemId, Instant fechaIngreso, Long usuarioId);
}

