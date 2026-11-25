package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.domain.event.FundacionEliminadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.in.EliminarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SR-RF-120: el administrador StyleRadar elimina de forma definitiva una fundación aliada y su certificado de
 * personería jurídica (RN-03) y se publica {@code foundation.deleted} (RN-05).
 */
@Slf4j
@RequiredArgsConstructor
public class EliminarFundacionUseCaseImpl implements EliminarFundacionUseCase {

    private final FundacionRepositoryPort repositoryPort;
    private final AlmacenamientoBlobPort almacenamientoBlobPort;
    private final FundacionEventPublisherPort eventPublisher;
    private final AutorizacionAdminStyleRadarValidator adminValidator;

    @Override
    @Transactional
    public void eliminar(Long id, String rol) {
        adminValidator.validarEsAdministradorParaOperacion(rol);
        Fundacion fundacion = (id == null ? java.util.Optional.<Fundacion>empty() : repositoryPort.findById(id))
                .orElseThrow(() -> new FundacionNoEncontradaException("No existe una fundación aliada con ID: " + id));
        String certificado = fundacion.getUrlCertificadoPersoneriaJuridica();
        repositoryPort.eliminarPorId(id);
        if (certificado != null && !certificado.isBlank()) {
            try {
                almacenamientoBlobPort.eliminarArchivo(certificado);
            } catch (RuntimeException ex) {
                log.warn("No se pudo borrar del Blob el certificado de la fundación {}: {}", id, ex.getMessage());
            }
        }
        eventPublisher.publicarFundacionEliminada(FundacionEliminadaEvent.de(id, Instant.now()));
        if (fundacion.isActiva()) {
            eventPublisher.publicarEstadoCambiado(edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent.eliminada(id, Instant.now()));
        }
        log.info("Fundación ID={} eliminada", id);
    }
}
