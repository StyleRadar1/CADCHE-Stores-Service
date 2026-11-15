package edu.dosw.styleradar.tiendas.domain.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundacionReactivadaEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private String eventoId = UUID.randomUUID().toString();
    private Long fundacionId;
    private String nombreFundacion;
    private Long representanteId;
    private Instant fechaReactivacion;
    private String motivo;

    public FundacionReactivadaEvent(
            Long fundacionId,
            String nombreFundacion,
            Long representanteId,
            Instant fechaReactivacion,
            String motivo) {
        this.eventoId = UUID.randomUUID().toString();
        this.fundacionId = fundacionId;
        this.nombreFundacion = nombreFundacion;
        this.representanteId = representanteId;
        this.fechaReactivacion = fechaReactivacion;
        this.motivo = motivo;
    }
}

