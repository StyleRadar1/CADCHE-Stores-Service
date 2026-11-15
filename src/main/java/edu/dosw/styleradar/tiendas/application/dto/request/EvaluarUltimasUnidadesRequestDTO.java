package edu.dosw.styleradar.tiendas.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de inventario para evaluar el distintivo de últimas unidades de una prenda (SR-RF-59)")
public class EvaluarUltimasUnidadesRequestDTO {

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock actual no puede ser negativo")
    @Schema(description = "Unidades disponibles de la prenda", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stockActual;

    @Builder.Default
    @Schema(description = "Indica si la prenda se encuentra disponible para la venta", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean disponible = Boolean.TRUE;
}

