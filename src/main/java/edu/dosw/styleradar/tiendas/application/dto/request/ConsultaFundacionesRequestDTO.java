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
@Schema(description = "Parámetros de consulta de fundaciones aliadas activas cercanas al donante (SR-RF-43)")
public class ConsultaFundacionesRequestDTO {

    @NotNull(message = "latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "latitud debe estar entre -90 y 90")
    @Schema(description = "Latitud geográfica WGS84 del usuario donante", example = "4.6712", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitud;

    @NotNull(message = "longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "longitud debe estar entre -180 y 180")
    @Schema(description = "Longitud geográfica WGS84 del usuario donante", example = "-74.0531", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitud;

    @Schema(description = "Filtro opcional por categoría de prenda aceptada por la fundación", example = "SUPERIOR")
    private TipoPrenda tipo;
}

