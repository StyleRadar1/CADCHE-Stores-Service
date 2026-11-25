package edu.dosw.styleradar.tiendas.domain.port.out;

import edu.dosw.styleradar.tiendas.domain.event.AlmacenEliminadoEvent;
import edu.dosw.styleradar.tiendas.domain.event.VerificacionAlmacenCambiadaEvent;

/** Publicación de eventos de almacenes para los demás servicios. */
public interface AlmacenEventPublisherPort {

    /** SR-RF-119: publica store.deleted. */
    void publicarAlmacenEliminado(AlmacenEliminadoEvent evento);

    /** Publica store.verification.changed cuando cambia el estado de verificación de un almacén. */
    void publicarVerificacionCambiada(VerificacionAlmacenCambiadaEvent evento);
}
