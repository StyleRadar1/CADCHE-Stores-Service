package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update the new-arrival window in days of a store")
public record ActualizarPeriodoRequestDTO(
        @NotNull(message = "El periodo en días de nueva llegada es obligatorio")
        @Min(value = 1, message = "El periodo de nueva llegada debe ser de al menos 1 día")
        @Max(value = 60, message = "El periodo de nueva llegada no puede superar 60 días")
        @Schema(description = "Days a newly published item keeps the NEW_ARRIVAL badge (default 7)", example = "10")
        @JsonProperty("newArrivalDays") Integer diasNuevaLlegada
) {
}
