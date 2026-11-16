package edu.dosw.styleradar.tiendas.application.dto.response;

import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Información pública de un almacén aliado verificado")
public record AlmacenResponseDTO(
        @Schema(description = "NIT único del almacén aliado", example = "900123456-1")
        String nit,

        @Schema(description = "Nombre comercial de la tienda", example = "Atelier Urbano Chapinero")
        String nombreComercial,

        @Schema(description = "Descripción comercial de la tienda", example = "Moda independiente y prendas urbanas en Chapinero")
        String descripcion,

        @Schema(description = "Latitud geográfica WGS84 del almacén", example = "4.6533")
        Double latitud,

        @Schema(description = "Longitud geográfica WGS84 del almacén", example = "-74.0602")
        Double longitud,

        @Schema(description = "Valoración promedio o reputación del almacén", example = "4.8")
        Double reputacion,

        @Schema(description = "Categorías comerciales asociadas al almacén", example = "[\"CASUAL\",\"VINTAGE\"]")
        Set<CategoriaAlmacen> categorias
) {
}

