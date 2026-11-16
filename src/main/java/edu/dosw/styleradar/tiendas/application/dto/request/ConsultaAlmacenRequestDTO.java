package edu.dosw.styleradar.tiendas.application.dto.request;

import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Parámetros de paginación y filtro opcional por categoría comercial para consultar almacenes aliados")
public class ConsultaAlmacenRequestDTO {

    @Schema(description = "Categoría comercial opcional del almacén", example = "URBANA")
    private CategoriaAlmacen categoria;

    @Min(0)
    @Schema(description = "Número de página de base cero", example = "0", defaultValue = "0")
    private int page = 0;

    @Min(1)
    @Max(100)
    @Schema(description = "Cantidad de almacenes por página (entre 1 y 100)", example = "20", defaultValue = "20")
    private int size = 20;
}

