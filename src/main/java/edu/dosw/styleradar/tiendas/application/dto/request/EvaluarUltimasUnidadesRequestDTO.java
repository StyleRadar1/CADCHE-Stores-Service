package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Schema(name = "LowStockEvaluationRequest", description = "Inventory data to evaluate the low-stock badge of an item")
public class EvaluarUltimasUnidadesRequestDTO {

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock actual no puede ser negativo")
    @Schema(description = "Units in stock of the item", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("currentStock")
    private Integer stockActual;

    @NotNull(message = "Debe indicar si la prenda está disponible")
    @Schema(description = "Whether the item can be sold (required, decision D10)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("available")
    private Boolean disponible;
}

