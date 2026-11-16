package edu.dosw.styleradar.tiendas.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
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
@Schema(description = "Solicitud para actualizar exclusivamente el umbral de últimas unidades de un almacén aliado (SR-RF-59)")
public class ActualizarUmbralRequestDTO {

    @NotNull(message = "El umbral de últimas unidades es obligatorio")
    @Min(value = 0, message = "El umbral de últimas unidades no puede ser negativo")
    @Max(value = 50, message = "El umbral de últimas unidades no puede superar 50")
    @Schema(description = "Cantidad máxima de unidades disponibles en inventario para considerar la prenda como ULTIMAS_UNIDADES", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer umbralUltimasUnidades;
}

