package edu.dosw.styleradar.tiendas.domain.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SR-RF-120: una fundación aliada fue eliminada de forma definitiva (evento {@code foundation.deleted}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundacionEliminadaEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventoId;
    private Long fundacionId;
    private Instant fechaEliminacion;

    public static FundacionEliminadaEvent de(Long id, Instant fecha) {
        return new FundacionEliminadaEvent(UUID.randomUUID().toString(), id, fecha);
    }
}
