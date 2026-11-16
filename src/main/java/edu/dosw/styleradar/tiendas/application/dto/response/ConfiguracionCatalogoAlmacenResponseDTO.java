package edu.dosw.styleradar.tiendas.application.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuración vigente de umbrales de stock y días de nueva llegada de un almacén (SR-RF-59 / SR-RF-60)")
public record ConfiguracionCatalogoAlmacenResponseDTO(
        @Schema(description = "NIT del almacén aliado", example = "900123456-1")
        String nit,

        @Schema(description = "Umbral configurado para identificar prendas como ULTIMAS_UNIDADES (SR-RF-59)", example = "3")
        Integer umbralUltimasUnidades,

        @Schema(description = "Ventana en días configurada para identificar prendas como NUEVA_PRENDA (SR-RF-60)", example = "7")
        Integer diasNuevaLlegada,

        @Schema(description = "Fecha y hora UTC de la última actualización", example = "2026-11-03T01:40:00Z")
        Instant fechaActualizacion
) {
}

