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
@Schema(description = "Inventory data to evaluate the low-stock badge of an item (SR-RF-59)")
public class EvaluarUltimasUnidadesRequestDTO {

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock actual no puede ser negativo")
    @Schema(description = "Units in stock of the item", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("currentStock")
    private Integer stockActual;

    @Builder.Default
    @Schema(description = "Whether the item can be sold", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("available")
    private Boolean disponible = Boolean.TRUE;
}

