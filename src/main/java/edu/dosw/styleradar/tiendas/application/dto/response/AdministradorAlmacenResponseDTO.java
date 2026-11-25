package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StoreAdministratorResponse", description = "Stored association between a STORE_ADMIN user and a store NIT")
public record AdministradorAlmacenResponseDTO(
        @Schema(description = "Association record identifier", example = "10")
        Long id,

        @Schema(description = "Identifier of the STORE_ADMIN user", example = "15")
        @JsonProperty("userId") Long usuarioId,

        @Schema(description = "NIT of the associated store", example = "900123456-1")
        @JsonProperty("storeNit") String almacenNit,

        @Schema(description = "StyleRadar admin who made the assignment (from the JWT); null when the link was created by the store registration", example = "1", nullable = true)
        @JsonProperty("assignedById") Long asignadoPorId,

        @Schema(description = "UTC date and time of the first assignment", example = "2026-11-02T20:00:00Z")
        @JsonProperty("assignedAt") Instant fechaAsignacion
) {
}

