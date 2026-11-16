package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultarPeriodoNuevaLlegadaUseCaseImpl implements ConsultarPeriodoNuevaLlegadaUseCase {

    private final AlmacenRepositoryPort repositoryPort;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionCatalogoAlmacenResponseDTO consultarPeriodo(String nit) {
        log.info("Consultando periodo de nueva llegada para almacén NIT={}", nit);

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        return new ConfiguracionCatalogoAlmacenResponseDTO(
                almacen.getNit(),
                almacen.getUmbralUltimasUnidadesEfectivo(),
                almacen.getDiasNuevaLlegadaEfectivo(),
                almacen.getFechaActualizacionCatalogo() != null ? almacen.getFechaActualizacionCatalogo() : Instant.now()
        );
    }
}
