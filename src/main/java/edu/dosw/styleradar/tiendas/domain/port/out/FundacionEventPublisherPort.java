package edu.dosw.styleradar.tiendas.domain.port.out;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionReactivadaEvent;

public interface FundacionEventPublisherPort {

    void publicarFundacionDesactivada(FundacionDesactivadaEvent evento);

    void publicarFundacionReactivada(FundacionReactivadaEvent evento);

    /** SR-RF-120: publica foundation.deleted. */
    void publicarFundacionEliminada(edu.dosw.styleradar.tiendas.domain.event.FundacionEliminadaEvent evento);

    /** Publica foundation.status.changed con el delta de fundaciones activas (+1 o -1). */
    void publicarEstadoCambiado(edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent evento);
}
