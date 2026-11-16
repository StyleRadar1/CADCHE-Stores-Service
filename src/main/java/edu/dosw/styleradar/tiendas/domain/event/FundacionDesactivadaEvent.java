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
public class FundacionDesactivadaEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private String eventoId = UUID.randomUUID().toString();
    private Long fundacionId;
    private String nombreFundacion;
    private Long representanteId;
    private Instant fechaDesactivacion;
    private String motivo;

    public FundacionDesactivadaEvent(
            Long fundacionId,
            String nombreFundacion,
            Long representanteId,
            Instant fechaDesactivacion,
            String motivo) {
        this.eventoId = UUID.randomUUID().toString();
        this.fundacionId = fundacionId;
        this.nombreFundacion = nombreFundacion;
        this.representanteId = representanteId;
        this.fechaDesactivacion = fechaDesactivacion;
        this.motivo = motivo;
    }
}

