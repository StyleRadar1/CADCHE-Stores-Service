package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultarUmbralUltimasUnidadesUseCaseImpl implements ConsultarUmbralUltimasUnidadesUseCase {

    private final AlmacenRepositoryPort repositoryPort;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionCatalogoAlmacenResponseDTO consultarConfiguracion(String nit) {
        log.info("Consultando configuración de catálogo y umbral de últimas unidades para almacén NIT={}", nit);

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        // Aplica el valor por defecto de 3 si no tiene umbral propio
        int umbralEfectivo = almacen.getUmbralUltimasUnidadesEfectivo();
        int diasEfectivos = almacen.getDiasNuevaLlegadaEfectivo();
        Instant fecha = almacen.getFechaActualizacionCatalogo() != null ? almacen.getFechaActualizacionCatalogo() : Instant.now();

        log.info("Configuración consultada para almacén NIT={}: umbralEfectivo={}, diasEfectivos={}", nit, umbralEfectivo, diasEfectivos);

        return new ConfiguracionCatalogoAlmacenResponseDTO(
                almacen.getNit(),
                umbralEfectivo,
                diasEfectivos,
                fecha
        );
    }
}
