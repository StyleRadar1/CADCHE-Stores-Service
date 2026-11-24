package edu.dosw.styleradar.tiendas.domain.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SR-RF-119: un almacén aliado fue eliminado de forma definitiva (evento {@code store.deleted}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenEliminadoEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventoId;
    private String almacenNit;
    private Instant fechaEliminacion;

    public static AlmacenEliminadoEvent de(String nit, Instant fecha) {
        return new AlmacenEliminadoEvent(UUID.randomUUID().toString(), nit, fecha);
    }
}
