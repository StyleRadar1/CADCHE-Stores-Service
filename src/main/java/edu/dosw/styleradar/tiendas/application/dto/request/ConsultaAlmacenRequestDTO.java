package edu.dosw.styleradar.tiendas.application.dto.request;

import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Pagination and optional category filter to query approved stores")
public class ConsultaAlmacenRequestDTO {

    @Schema(description = "Optional store category", example = "CASUAL")
    private CategoriaAlmacen category;

    @Min(0)
    @Schema(description = "Zero-based page number", example = "0", defaultValue = "0")
    private int page = 0;

    @Min(1)
    @Max(100)
    @Schema(description = "Stores per page (1 to 100)", example = "20", defaultValue = "20")
    private int size = 20;
}

