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
@Schema(description = "Request to update only the low-stock threshold of a store")
public class ActualizarUmbralRequestDTO {

    @NotNull(message = "El umbral de últimas unidades es obligatorio")
    @Min(value = 0, message = "El umbral de últimas unidades no puede ser negativo")
    @Max(value = 50, message = "El umbral de últimas unidades no puede superar 50")
    @Schema(description = "Maximum units in stock for the item to be considered LOW_STOCK", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("lowStockThreshold")
    private Integer umbralUltimasUnidades;
}

