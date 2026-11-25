package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Schema(name = "StoreCatalogSettingsRequest", description = "Low-stock threshold and new-arrival window of a store")
public class ConfiguracionCatalogoAlmacenRequestDTO {

    @NotNull(message = "El umbral de últimas unidades es obligatorio")
    @Min(value = 0, message = "El umbral de últimas unidades no puede ser negativo")
    @Max(value = 50, message = "El umbral de últimas unidades no puede superar 50")
    @Schema(description = "Maximum units in stock to mark an item as LOW_STOCK", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("lowStockThreshold")
    private Integer umbralUltimasUnidades;

    @NotNull(message = "El periodo en días de nueva llegada es obligatorio")
    @Min(value = 1, message = "El periodo de nueva llegada debe ser de al menos 1 día")
    @Max(value = 60, message = "El periodo de nueva llegada no puede superar 60 días")
    @Schema(description = "Days after publication during which an item keeps the NEW_ARRIVAL badge", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("newArrivalDays")
    private Integer diasNuevaLlegada;
}
