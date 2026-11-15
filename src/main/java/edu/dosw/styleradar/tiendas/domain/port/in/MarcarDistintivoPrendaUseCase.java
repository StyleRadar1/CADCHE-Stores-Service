package edu.dosw.styleradar.tiendas.domain.port.in;

import java.time.Instant;

import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarNuevaLlegadaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarUltimasUnidadesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;

public interface MarcarDistintivoPrendaUseCase {

    DistintivoPrendaAlmacenResponseDTO marcarUltimasUnidades(
            String nit, Long itemId, Integer stockActual, Long usuarioId);

    DistintivoPrendaAlmacenResponseDTO evaluarUltimasUnidades(
            String nit, Long itemId, EvaluarUltimasUnidadesRequestDTO request, Long usuarioId);

    DistintivoPrendaAlmacenResponseDTO marcarNuevaLlegada(
            String nit, Long itemId, Instant fechaIngreso, Long usuarioId);

    DistintivoPrendaAlmacenResponseDTO evaluarNuevaLlegada(
            String nit, Long itemId, EvaluarNuevaLlegadaRequestDTO request, Long usuarioId);
}
