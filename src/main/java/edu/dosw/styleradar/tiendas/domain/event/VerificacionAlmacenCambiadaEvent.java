package edu.dosw.styleradar.tiendas.domain.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cambio del estado de verificación de un almacén (evento {@code store.verification.changed}).
 * Se emite al registrar el almacén (sin estado anterior), al aprobarlo o rechazarlo y al reabrir su revisión.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionAlmacenCambiadaEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventoId;
    private String almacenNit;
    private EstadoVerificacionAlmacen estado;
    private EstadoVerificacionAlmacen estadoAnterior;
    private Instant fechaCambio;

    public static VerificacionAlmacenCambiadaEvent de(String nit, EstadoVerificacionAlmacen anterior,
            EstadoVerificacionAlmacen nuevo, Instant fecha) {
        return new VerificacionAlmacenCambiadaEvent(UUID.randomUUID().toString(), nit, nuevo, anterior,
                fecha != null ? fecha : Instant.now());
    }
}
