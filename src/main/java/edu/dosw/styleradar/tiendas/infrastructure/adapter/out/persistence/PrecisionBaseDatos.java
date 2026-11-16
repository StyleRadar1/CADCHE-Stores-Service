package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * PostgreSQL guarda {@code timestamptz} con microsegundos. Las fechas se truncan a esa precisión antes de
 * persistir para que la respuesta de un alta o cambio sea idéntica a la que devuelve una consulta posterior.
 */
public final class PrecisionBaseDatos {

    private PrecisionBaseDatos() {
    }

    public static Instant aMicros(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant ahora() {
        return aMicros(Instant.now());
    }
}
