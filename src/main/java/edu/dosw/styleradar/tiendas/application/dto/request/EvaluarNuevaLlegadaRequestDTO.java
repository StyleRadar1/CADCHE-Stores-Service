package edu.dosw.styleradar.tiendas.application.dto.request;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de publicación para evaluar el distintivo de nueva llegada de una prenda (SR-RF-60)")
public class EvaluarNuevaLlegadaRequestDTO {

    @NotNull(message = "La fecha de publicación es obligatoria")
    @PastOrPresent(message = "La fecha de publicación no puede ser futura")
    @Schema(description = "Fecha y hora UTC de publicación de la prenda (ISO-8601)", example = "2026-11-10T10:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant fechaPublicacion;

    @Builder.Default
    @Schema(description = "Indica si la prenda fue retirada del catálogo", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean retirada = Boolean.FALSE;
}

