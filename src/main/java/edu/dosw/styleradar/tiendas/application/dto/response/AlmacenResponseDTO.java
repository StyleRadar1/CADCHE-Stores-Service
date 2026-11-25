package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StoreResponse", description = "Public information of a verified partner store")
public record AlmacenResponseDTO(
        @Schema(description = "Unique NIT of the partner store", example = "900123456-1")
        String nit,

        @Schema(description = "Trade name of the store", example = "Atelier Urbano Chapinero")
        @JsonProperty("tradeName") String nombreComercial,

        @Schema(description = "Business description of the store", example = "Moda independiente y prendas urbanas en Chapinero")
        @JsonProperty("description") String descripcion,

        @Schema(description = "WGS84 latitude of the store", example = "4.6533")
        @JsonProperty("latitude") Double latitud,

        @Schema(description = "WGS84 longitude of the store", example = "-74.0602")
        @JsonProperty("longitude") Double longitud,

        @Schema(description = "Average rating of the store", example = "4.8")
        @JsonProperty("rating") Double reputacion,

        @Schema(description = "Business categories of the store", example = "[\"CASUAL\",\"VINTAGE\"]")
        @JsonProperty("categories") Set<CategoriaAlmacen> categorias
) {
}

