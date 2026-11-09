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
@Schema(description = "Configuración de umbrales de últimas unidades (SR-RF-59) y ventana de nueva llegada (SR-RF-60) para un almacén aliado")
public class ConfiguracionCatalogoAlmacenRequestDTO {

    @NotNull(message = "El umbral de últimas unidades es obligatorio")
    @Min(value = 0, message = "El umbral de últimas unidades no puede ser negativo")
    @Max(value = 50, message = "El umbral de últimas unidades no puede superar 50")
    @Schema(description = "Cantidad máxima de unidades disponibles en inventario para marcar una prenda como ULTIMAS_UNIDADES (SR-RF-59)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer umbralUltimasUnidades;

    @NotNull(message = "El periodo en días de nueva llegada es obligatorio")
    @Min(value = 1, message = "El periodo de nueva llegada debe ser de al menos 1 día")
    @Max(value = 60, message = "El periodo de nueva llegada no puede superar 60 días")
    @Schema(description = "Número de días posteriores a la publicación durante los cuales una prenda conserva el distintivo NUEVA_PRENDA (SR-RF-60)", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer diasNuevaLlegada;
}
