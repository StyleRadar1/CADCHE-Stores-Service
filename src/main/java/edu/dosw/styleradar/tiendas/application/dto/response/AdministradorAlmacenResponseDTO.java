package edu.dosw.styleradar.tiendas.application.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Asociación persistida entre un usuario ADMIN_ALMACEN y un NIT de almacén")
public record AdministradorAlmacenResponseDTO(
        @Schema(description = "Identificador del registro de asociación", example = "10")
        Long id,

        @Schema(description = "Identificador del usuario con rol ADMIN_ALMACEN", example = "15")
        Long usuarioId,

        @Schema(description = "NIT del almacén asociado", example = "900123456-1")
        String almacenNit,

        @Schema(description = "Identificador del administrador StyleRadar que realizó la asignación", example = "1")
        Long asignadoPorId,

        @Schema(description = "Fecha y hora UTC de la primera asignación", example = "2026-11-02T20:00:00Z")
        Instant fechaAsignacion
) {
}

