package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.domain.event.AlmacenEliminadoEvent;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.port.in.EliminarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SR-RF-119: el administrador StyleRadar elimina de forma definitiva un almacén aliado con sus fotografías, su
 * configuración de catálogo y sus administradores (RN-03) y se publica {@code store.deleted} (RN-05).
 */
@Slf4j
@RequiredArgsConstructor
public class EliminarAlmacenUseCaseImpl implements EliminarAlmacenUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final AlmacenamientoBlobPort almacenamientoBlobPort;
    private final AlmacenEventPublisherPort eventPublisher;
    private final AutorizacionAdminStyleRadarValidator adminValidator;

    @Override
    @Transactional
    public void eliminar(String nit, String rol) {
        adminValidator.validarEsAdministradorParaOperacion(rol);
        Almacen almacen = (nit == null ? java.util.Optional.<Almacen>empty() : repositoryPort.findByNit(nit))
                .orElseThrow(() -> new AlmacenNoEncontradoException(
                        "No se encontró ningún almacén registrado con el NIT: " + nit));
        List<String> urlsFotos = repositoryPort.findFotosByAlmacenNit(nit).stream()
                .map(FotografiaTienda::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
        repositoryPort.eliminarPorNit(nit);
        for (String url : urlsFotos) {
            borrarDelBlob(url, "la fotografía", nit);
        }
        // El RUT es un documento del negocio: no debe quedar en el Blob cuando el almacén deja de existir.
        String urlRut = almacen.getUrlRutPdf();
        if (urlRut != null && !urlRut.isBlank()) {
            borrarDelBlob(urlRut, "el RUT", nit);
        }
        eventPublisher.publicarAlmacenEliminado(AlmacenEliminadoEvent.de(nit, Instant.now()));
        log.info("Almacén NIT={} eliminado con {} fotografías", nit, urlsFotos.size());
    }

    private void borrarDelBlob(String url, String que, String nit) {
        try {
            almacenamientoBlobPort.eliminarArchivo(url);
        } catch (RuntimeException ex) {
            log.warn("No se pudo borrar del Blob {} {} del almacén {}: {}", que, url, nit, ex.getMessage());
        }
    }
}
