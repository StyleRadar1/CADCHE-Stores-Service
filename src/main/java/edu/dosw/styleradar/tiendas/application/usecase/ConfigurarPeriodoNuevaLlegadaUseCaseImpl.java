package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import edu.dosw.styleradar.tiendas.domain.validator.PeriodoNuevaLlegadaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurarPeriodoNuevaLlegadaUseCaseImpl implements ConfigurarPeriodoNuevaLlegadaUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final PeriodoNuevaLlegadaValidator periodoValidator;
    private final AutorizacionAlmacenValidator autorizacionValidator;

    @Override
    @Transactional
    public ConfiguracionCatalogoAlmacenResponseDTO configurarPeriodo(String nit, Integer dias, Long usuarioId) {
        log.info("Configurando periodo de nueva llegada para almacén NIT={} dias={} por usuarioId={}", nit, dias, usuarioId);

        // 1. Validar dominio: dias >= 1 y dias <= 60
        periodoValidator.validar(dias);

        // 2. Verificar existencia del almacén
        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        // 3. Validar autorización BOLA / IDOR
        autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);

        // 4. Actualizar estado en dominio
        almacen.configurarDiasNuevaLlegada(dias);

        // 5. Persistir cambios
        Almacen guardado = repositoryPort.save(almacen);

        log.info("Periodo de nueva llegada configurado con éxito para almacén NIT={}: {} días", nit, guardado.getDiasNuevaLlegada());
        return new ConfiguracionCatalogoAlmacenResponseDTO(
                guardado.getNit(),
                guardado.getUmbralUltimasUnidadesEfectivo(),
                guardado.getDiasNuevaLlegadaEfectivo(),
                guardado.getFechaActualizacionCatalogo() != null ? guardado.getFechaActualizacionCatalogo() : Instant.now()
        );
    }
}
