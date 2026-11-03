package edu.dosw.styleradar.tiendas.application.dto.request;

import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Filtros y paginación para consultar solicitudes de verificación de almacenes (SR-RF-62)")
public class VerificacionConsultaRequestDTO {

    @Schema(description = "Filtro opcional por estado real de verificación", example = "PENDIENTE")
    private EstadoVerificacionAlmacen estado;

    @NotNull
    @Min(0)
    @Schema(description = "Número de página de base cero", example = "0", defaultValue = "0")
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Max(100)
    @Schema(description = "Cantidad de registros por página (máximo 100)", example = "20", defaultValue = "20")
    private Integer size = 20;
}

