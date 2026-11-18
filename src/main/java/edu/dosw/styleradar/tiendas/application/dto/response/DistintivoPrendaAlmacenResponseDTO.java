package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of the store catalog badge evaluation: low stock (SR-RF-59) and new arrival (SR-RF-60)")
public record DistintivoPrendaAlmacenResponseDTO(
        @Schema(description = "NIT of the store that owns the item", example = "900123456-1")
        String nit,

        @Schema(description = "Catalog item identifier in the store", example = "104")
        Long itemId,

        @Schema(description = "Whether the item got the low-stock badge after reaching the store threshold (SR-RF-59)", example = "true")
        @JsonProperty("lowStock") boolean ultimasUnidades,

        @Schema(description = "Store threshold for the low-stock badge (default 3 units)", example = "3")
        @JsonProperty("lowStockThreshold") Integer umbralUltimasUnidades,

        @Schema(description = "Whether the item got the new-arrival badge within the configured period (SR-RF-60)", example = "true")
        @JsonProperty("newArrival") boolean nuevaLlegada,

        @Schema(description = "Store window in days for the new-arrival badge (default 7 days)", example = "7")
        @JsonProperty("newArrivalDays") Integer diasNuevaLlegada,

        @Schema(description = "Visible badge of the item: LOW_STOCK, NEW_ARRIVAL or NONE", example = "LOW_STOCK")
        @JsonProperty("activeBadge") String distintivoActivo,

        @Schema(description = "UTC date and time the badge was calculated", example = "2026-11-03T01:45:00Z")
        @JsonProperty("updatedAt") Instant fechaActualizacion
) {
}

