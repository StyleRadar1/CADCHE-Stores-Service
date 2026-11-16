package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PrecisionBaseDatos: fechas con la precisión de PostgreSQL (microsegundos)")
class PrecisionBaseDatosTest {

    @Test
    @DisplayName("Trunca a microsegundos y respeta null")
    void truncaAMicrosegundos() {
        Instant conNanos = Instant.parse("2026-11-16T15:19:22.727875123Z");

        assertThat(PrecisionBaseDatos.aMicros(conNanos)).isEqualTo(Instant.parse("2026-11-16T15:19:22.727875Z"));
        assertThat(PrecisionBaseDatos.aMicros(null)).isNull();
        assertThat(PrecisionBaseDatos.ahora().getNano() % 1000).isZero();
    }
}
