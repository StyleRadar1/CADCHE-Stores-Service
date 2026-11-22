package edu.dosw.styleradar.tiendas.application.dto.request;

import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Optional parameters of the public list of active partner foundations")
public class ConsultaFundacionesRequestDTO {

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Schema(description = "Optional donor WGS84 latitude; with longitude, results are sorted by distance", example = "4.6712")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Schema(description = "Optional donor WGS84 longitude; with latitude, results are sorted by distance", example = "-74.0531")
    private Double longitude;

    @Schema(description = "Optional filter by garment category accepted by the foundation", example = "TOP")
    private TipoPrenda type;
}

