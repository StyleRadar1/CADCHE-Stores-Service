package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import edu.dosw.styleradar.tiendas.domain.validator.PeriodoNuevaLlegadaValidator;
import edu.dosw.styleradar.tiendas.domain.validator.UmbralInventarioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurarUmbralUltimasUnidadesUseCaseImpl implements ConfigurarUmbralUltimasUnidadesUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final UmbralInventarioValidator umbralValidator;
    private final PeriodoNuevaLlegadaValidator periodoValidator;
    private final AutorizacionAlmacenValidator autorizacionValidator;

    @Override
    @Transactional
    public ConfiguracionCatalogoAlmacenResponseDTO configurarUmbral(String nit, Integer umbral, Long usuarioId) {
        log.info("Configurando umbral de últimas unidades para almacén NIT={} umbral={} por usuarioId={}", nit, umbral, usuarioId);

        // 1. Validar dominio: umbral no negativo ni excediendo límite
        umbralValidator.validar(umbral);

        // 2. Verificar existencia del almacén
        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        // 3. Validar autorización BOLA / IDOR
        autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);

        // 4. Actualizar estado en dominio
        almacen.configurarUmbralUltimasUnidades(umbral);

        // 5. Persistir cambios
        Almacen guardado = repositoryPort.save(almacen);

        log.info("Umbral configurado con éxito para almacén NIT={}: {} unidades", nit, guardado.getUmbralUltimasUnidades());
        return toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public ConfiguracionCatalogoAlmacenResponseDTO configurarCatalogo(String nit, ConfiguracionCatalogoAlmacenRequestDTO request, Long usuarioId) {
        log.info("Configurando catálogo completo para almacén NIT={} por usuarioId={}", nit, usuarioId);

        // 1. Validar dominio
        umbralValidator.validar(request.getUmbralUltimasUnidades());
        periodoValidator.validar(request.getDiasNuevaLlegada());

        // 2. Verificar existencia
        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        // 3. Validar autorización
        autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);

        // 4. Actualizar estado
        almacen.configurarCatalogo(request.getUmbralUltimasUnidades(), request.getDiasNuevaLlegada());

        // 5. Persistir
        Almacen guardado = repositoryPort.save(almacen);

        return toResponseDTO(guardado);
    }

    private ConfiguracionCatalogoAlmacenResponseDTO toResponseDTO(Almacen almacen) {
        return new ConfiguracionCatalogoAlmacenResponseDTO(
                almacen.getNit(),
                almacen.getUmbralUltimasUnidadesEfectivo(),
                almacen.getDiasNuevaLlegadaEfectivo(),
                almacen.getFechaActualizacionCatalogo() != null ? almacen.getFechaActualizacionCatalogo() : Instant.now()
        );
    }
}

