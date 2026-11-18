package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current stock threshold and new-arrival days of a store (SR-RF-59 / SR-RF-60)")
public record ConfiguracionCatalogoAlmacenResponseDTO(
        @Schema(description = "NIT of the partner store", example = "900123456-1")
        String nit,

        @Schema(description = "Threshold used to flag items as LOW_STOCK (SR-RF-59)", example = "3")
        @JsonProperty("lowStockThreshold") Integer umbralUltimasUnidades,

        @Schema(description = "Window in days used to flag items as NEW_ARRIVAL (SR-RF-60)", example = "7")
        @JsonProperty("newArrivalDays") Integer diasNuevaLlegada,

        @Schema(description = "UTC date and time of the last update", example = "2026-11-03T01:40:00Z")
        @JsonProperty("updatedAt") Instant fechaActualizacion
) {
}

