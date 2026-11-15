package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.in.MarcarDistintivoPrendaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarcarDistintivoPrendaUseCaseImpl implements MarcarDistintivoPrendaUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final AutorizacionAlmacenValidator autorizacionValidator;

    @Override
    @Transactional(readOnly = true)
    public DistintivoPrendaAlmacenResponseDTO marcarUltimasUnidades(
            String nit, Long itemId, Integer stockActual, Long usuarioId) {
        if (itemId == null || itemId <= 0) {
            throw new ReglaDeNegocioException("El identificador de la prenda debe ser mayor a cero");
        }

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException(
                        "No se encontró ningún almacén registrado con el NIT: " + nit));

        autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);

        int stockEfectivo = stockActual != null ? stockActual : almacen.getUmbralUltimasUnidadesEfectivo();
        boolean cumple = almacen.evaluarUltimasUnidades(stockEfectivo);

        log.info("Prenda itemId={} del almacén NIT={} marcada como ULTIMAS_UNIDADES (stock={}, umbral={})",
                itemId, nit, stockEfectivo, almacen.getUmbralUltimasUnidadesEfectivo());

        return new DistintivoPrendaAlmacenResponseDTO(
                almacen.getNit(),
                itemId,
                cumple,
                almacen.getUmbralUltimasUnidadesEfectivo(),
                false,
                almacen.getDiasNuevaLlegadaEfectivo(),
                "ULTIMAS_UNIDADES",
                Instant.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DistintivoPrendaAlmacenResponseDTO marcarNuevaLlegada(
            String nit, Long itemId, Instant fechaIngreso, Long usuarioId) {
        if (itemId == null || itemId <= 0) {
            throw new ReglaDeNegocioException("El identificador de la prenda debe ser mayor a cero");
        }

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException(
                        "No se encontró ningún almacén registrado con el NIT: " + nit));

        autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);

        Instant ahora = Instant.now();
        Instant fechaEfectiva = fechaIngreso != null ? fechaIngreso : ahora;
        boolean cumple = almacen.evaluarNuevaLlegada(fechaEfectiva, ahora);

        log.info("Prenda itemId={} del almacén NIT={} marcada como NUEVA_PRENDA (fechaIngreso={}, diasVigencia={})",
                itemId, nit, fechaEfectiva, almacen.getDiasNuevaLlegadaEfectivo());

        return new DistintivoPrendaAlmacenResponseDTO(
                almacen.getNit(),
                itemId,
                false,
                almacen.getUmbralUltimasUnidadesEfectivo(),
                cumple,
                almacen.getDiasNuevaLlegadaEfectivo(),
                "NUEVA_PRENDA",
                ahora
        );
    }
}

