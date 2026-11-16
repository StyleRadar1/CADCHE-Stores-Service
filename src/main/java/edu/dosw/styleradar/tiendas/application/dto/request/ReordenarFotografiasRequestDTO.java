package edu.dosw.styleradar.tiendas.application.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para reordenar la galería de fotografías de la tienda física (SR-RF-74)")
public class ReordenarFotografiasRequestDTO {

    @NotEmpty(message = "El nuevo orden debe incluir todas las fotografías actuales del almacén")
    @Schema(description = "Lista ordenada de los identificadores de las fotografías actuales del almacén", example = "[2, 1, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> fotoIds;
}

