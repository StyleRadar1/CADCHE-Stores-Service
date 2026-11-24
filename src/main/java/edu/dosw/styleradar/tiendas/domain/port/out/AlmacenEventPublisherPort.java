package edu.dosw.styleradar.tiendas.domain.port.out;

import edu.dosw.styleradar.tiendas.domain.event.AlmacenEliminadoEvent;

/** Publicación de eventos de almacenes para los demás servicios. */
public interface AlmacenEventPublisherPort {

    /** SR-RF-119: publica store.deleted. */
    void publicarAlmacenEliminado(AlmacenEliminadoEvent evento);
}
