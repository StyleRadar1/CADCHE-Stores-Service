package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Schema(name = "ReorderPhotosRequest", description = "Request to reorder the physical-store photo gallery")
public class ReordenarFotografiasRequestDTO {

    @NotEmpty(message = "El nuevo orden debe incluir todas las fotografías actuales del almacén")
    @Schema(description = "Ordered list with the identifiers of every current photo of the store", example = "[2, 1, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("photoIds")
    private List<Long> fotoIds;
}

