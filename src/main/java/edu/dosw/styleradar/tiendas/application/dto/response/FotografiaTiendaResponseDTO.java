package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Physical-store photo of a partner store (SR-RF-74)")
public record FotografiaTiendaResponseDTO(
        @Schema(description = "Photo identifier", example = "1")
        Long id,

        @Schema(description = "NIT of the owning store", example = "900123456-1")
        @JsonProperty("storeNit") String almacenNit,

        @Schema(description = "Photo URL in object storage", example = "https://styleradar.blob.core.windows.net/tiendas/900123456-1/foto-1.webp")
        String url,

        @Schema(description = "Position of the photo in the gallery (1 to 6)", example = "1")
        @JsonProperty("position") int orden,

        @Schema(description = "Whether it is the cover photo of the store page", example = "true")
        @JsonProperty("cover") boolean portada,

        @Schema(description = "UTC upload date and time of the photo", example = "2026-11-03T01:45:00Z")
        @JsonProperty("registeredAt") Instant fechaRegistro
) {
}

