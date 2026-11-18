package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Schema(description = "Publication data to evaluate the new-arrival badge of an item (SR-RF-60)")
public class EvaluarNuevaLlegadaRequestDTO {

    @NotNull(message = "La fecha de publicación es obligatoria")
    @PastOrPresent(message = "La fecha de publicación no puede ser futura")
    @Schema(description = "UTC date and time the item was published (ISO-8601)", example = "2026-11-10T10:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("publishedAt")
    private Instant fechaPublicacion;

    @Builder.Default
    @Schema(description = "Whether the item was withdrawn from the catalog", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("withdrawn")
    private Boolean retirada = Boolean.FALSE;
}

