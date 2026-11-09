package edu.dosw.styleradar.tiendas.domain.port.out;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;

public interface FundacionEventPublisherPort {

    void publicarFundacionDesactivada(FundacionDesactivadaEvent evento);
}

