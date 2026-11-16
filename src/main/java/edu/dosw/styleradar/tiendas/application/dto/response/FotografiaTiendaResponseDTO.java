package edu.dosw.styleradar.tiendas.application.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fotografía registrada de la tienda física de un almacén aliado (SR-RF-74)")
public record FotografiaTiendaResponseDTO(
        @Schema(description = "Identificador único de la fotografía", example = "1")
        Long id,

        @Schema(description = "NIT del almacén propietario", example = "900123456-1")
        String almacenNit,

        @Schema(description = "URL de acceso a la fotografía en el almacenamiento de objetos", example = "https://styleradar.blob.core.windows.net/tiendas/900123456-1/foto-1.webp")
        String url,

        @Schema(description = "Posición de la fotografía en la galería (1 a 6)", example = "1")
        int orden,

        @Schema(description = "Indica si es la fotografía principal usada como portada de la página del almacén", example = "true")
        boolean portada,

        @Schema(description = "Fecha y hora UTC de carga de la fotografía", example = "2026-11-03T01:45:00Z")
        Instant fechaRegistro
) {
}

