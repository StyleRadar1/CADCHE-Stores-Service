package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.UUID;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
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
        @JsonProperty("categories") Set<CategoriaAlmacen> categorias,

        @Schema(description = "Contact phone of the business", example = "+57 601 312 4455")
        @JsonProperty("phone") String telefono,

        @Schema(description = "Store contact email", example = "contacto@atelierurbano.co")
        @JsonProperty("contactEmail") String correoContacto,

        @Schema(description = "Commercial street address of the store", example = "Carrera 13 # 63-25")
        @JsonProperty("address") String direccionComercial,

        @Schema(description = "Neighborhood (barrio) of the store", example = "Chapinero")
        @JsonProperty("barrio") String barrio,

        @Schema(description = "Business hours of the store", example = "Lun-Sáb 10:00 - 20:00")
        @JsonProperty("hours") String horarioAtencion,

        @Schema(description = "Deterministic UUID identifier of the store", example = "3c2f8a1e-4b5d-3e6f-9a1b-2c3d4e5f6a7b")
        @JsonProperty("almacenId") UUID almacenId
) {
    public AlmacenResponseDTO(
            String nit,
            String nombreComercial,
            String descripcion,
            Double latitud,
            Double longitud,
            Double reputacion,
            Set<CategoriaAlmacen> categorias) {
        this(nit, nombreComercial, descripcion, latitud, longitud, reputacion, categorias,
                null, null, null, null, null, Almacen.calcularAlmacenId(nit));
    }
}

