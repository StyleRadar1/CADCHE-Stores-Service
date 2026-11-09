package edu.dosw.styleradar.tiendas.domain.event;

import java.io.Serializable;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundacionDesactivadaEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fundacionId;
    private String nombreFundacion;
    private Long representanteId;
    private Instant fechaDesactivacion;
    private String motivo;
}

