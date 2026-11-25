package edu.dosw.styleradar.tiendas.domain.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cambio en el conteo de fundaciones activas (evento {@code foundation.status.changed}): +1 al registrar o
 * reactivar una fundación y -1 al desactivarla o al eliminar una que estaba activa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundacionEstadoCambiadoEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Acción que produjo el cambio de estado. */
    public enum Accion { REGISTERED, DEACTIVATED, REACTIVATED, DELETED }

    private String eventoId;
    private Long fundacionId;
    private Accion accion;
    private boolean activa;
    private int delta;
    private Instant fecha;

    public static FundacionEstadoCambiadoEvent registrada(Long id, Instant fecha) {
        return de(id, Accion.REGISTERED, true, 1, fecha);
    }

    public static FundacionEstadoCambiadoEvent desactivada(Long id, Instant fecha) {
        return de(id, Accion.DEACTIVATED, false, -1, fecha);
    }

    public static FundacionEstadoCambiadoEvent reactivada(Long id, Instant fecha) {
        return de(id, Accion.REACTIVATED, true, 1, fecha);
    }

    /** Solo se publica si la fundación eliminada estaba activa. */
    public static FundacionEstadoCambiadoEvent eliminada(Long id, Instant fecha) {
        return de(id, Accion.DELETED, false, -1, fecha);
    }

    private static FundacionEstadoCambiadoEvent de(Long id, Accion accion, boolean activa, int delta, Instant fecha) {
        return new FundacionEstadoCambiadoEvent(UUID.randomUUID().toString(), id, accion, activa, delta,
                fecha != null ? fecha : Instant.now());
    }
}
