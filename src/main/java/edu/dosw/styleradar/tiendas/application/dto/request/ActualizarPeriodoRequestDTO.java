package edu.dosw.styleradar.tiendas.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para actualizar la ventana de días de nueva llegada de un almacén (SR-RF-60)")
public record ActualizarPeriodoRequestDTO(
        @NotNull(message = "El periodo de nueva llegada es obligatorio")
        @Min(value = 1, message = "El periodo de nueva llegada debe ser de al menos 1 día")
        @Max(value = 60, message = "El periodo de nueva llegada no puede superar los 60 días")
        @Schema(description = "Días durante los cuales una prenda recién creada conserva el distintivo NUEVA_PRENDA (por defecto 7)", example = "10")
        Integer diasNuevaLlegada
) {
}
