package edu.dosw.styleradar.tiendas.application.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de evaluación y marcación de distintivos de catálogo del almacén: Últimas unidades (SR-RF-59) y Nueva llegada (SR-RF-60)")
public record DistintivoPrendaAlmacenResponseDTO(
        @Schema(description = "NIT del almacén propietario de la prenda", example = "900123456-1")
        String nit,

        @Schema(description = "Identificador del ítem de catálogo en el almacén", example = "104")
        Long itemId,

        @Schema(description = "Indica si la prenda quedó marcada con el distintivo 'Últimas unidades' al alcanzar el umbral del almacén (SR-RF-59)", example = "true")
        boolean ultimasUnidades,

        @Schema(description = "Umbral configurado en el almacén para activar el distintivo 'Últimas unidades' (por defecto <= 3 unidades)", example = "3")
        Integer umbralUltimasUnidades,

        @Schema(description = "Indica si la prenda quedó marcada con el distintivo 'Nueva llegada' dentro del periodo configurado (SR-RF-60)", example = "true")
        boolean nuevaLlegada,

        @Schema(description = "Días de vigencia configurados en el almacén para el distintivo 'Nueva llegada' (por defecto 7 días)", example = "7")
        Integer diasNuevaLlegada,

        @Schema(description = "Distintivo principal aplicado a la prenda (ULTIMAS_UNIDADES, NUEVA_PRENDA, DISPONIBLE)", example = "ULTIMAS_UNIDADES")
        String distintivoActivo,

        @Schema(description = "Fecha y hora UTC de actualización del distintivo", example = "2026-11-03T01:45:00Z")
        Instant fechaActualizacion
) {
}

