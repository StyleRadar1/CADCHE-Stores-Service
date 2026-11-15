package edu.dosw.styleradar.tiendas.domain.port.out;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionReactivadaEvent;

public interface FundacionEventPublisherPort {

    void publicarFundacionDesactivada(FundacionDesactivadaEvent evento);

    default void publicarFundacionReactivada(FundacionReactivadaEvent evento) {
        // Implementación por defecto para compatibilidad hacia atrás
    }
}
