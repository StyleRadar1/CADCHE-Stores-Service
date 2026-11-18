package edu.dosw.styleradar.tiendas.application.dto.request;

import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Filters and pagination to query store verification requests (SR-RF-62)")
public class VerificacionConsultaRequestDTO {

    @Schema(description = "Optional filter by verification status", example = "PENDING")
    private EstadoVerificacionAlmacen status;

    @NotNull
    @Min(0)
    @Schema(description = "Zero-based page number", example = "0", defaultValue = "0")
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Max(100)
    @Schema(description = "Records per page (max 100)", example = "20", defaultValue = "20")
    private Integer size = 20;
}

