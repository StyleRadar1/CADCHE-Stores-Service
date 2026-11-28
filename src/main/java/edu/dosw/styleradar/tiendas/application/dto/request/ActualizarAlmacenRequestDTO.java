package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "StoreUpdateRequest", description = "Optional commercial profile fields to update an existing store (NIT is immutable and passed in the path)")
public class ActualizarAlmacenRequestDTO {

    @Size(min = 2, max = 120, message = "El nombre comercial debe tener entre 2 y 120 caracteres")
    @Schema(description = "Trade name of the store", example = "Atelier Urbano Chapinero")
    @JsonProperty("tradeName")
    private String nombreComercial;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    @Schema(description = "Business description of the store", example = "Moda independiente y prendas urbanas en Chapinero")
    @JsonProperty("description")
    private String descripcion;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    @Schema(description = "Contact phone of the business", example = "+57 601 312 4455")
    @JsonProperty("phone")
    private String telefono;

    @Email(message = "El correo de contacto debe tener un formato válido")
    @Size(max = 100, message = "El correo de contacto no puede superar 100 caracteres")
    @Schema(description = "Official email of the store", example = "contacto@atelierurbano.co")
    @JsonProperty("contactEmail")
    private String correoContacto;

    @Size(max = 120, message = "El representante legal no puede superar 120 caracteres")
    @Schema(description = "Legal representative of the store", example = "Camila Gómez")
    @JsonProperty("representative")
    private String representanteLegal;

    @Size(max = 120, message = "La cuenta de pagos no puede superar 120 caracteres")
    @Schema(description = "Payout account for settlements", example = "Bancolombia Ahorros 012-345678-90")
    @JsonProperty("payoutAccount")
    private String cuentaPagos;

    @Size(max = 200, message = "La dirección comercial no puede superar 200 caracteres")
    @Schema(description = "Commercial street address of the store", example = "Carrera 13 # 63-25")
    @JsonProperty("address")
    private String direccionComercial;

    @Size(max = 80, message = "El barrio no puede superar 80 caracteres")
    @Schema(description = "Neighborhood (barrio) of the store", example = "Chapinero")
    @JsonProperty("barrio")
    private String barrio;

    @Size(max = 150, message = "El horario de atención no puede superar 150 caracteres")
    @Schema(description = "Business hours of the store", example = "Lun-Sáb 10:00 - 20:00")
    @JsonProperty("hours")
    private String horarioAtencion;

    @Schema(description = "Business categories of the store", example = "[\"CASUAL\",\"VINTAGE\"]")
    @JsonProperty("categories")
    private Set<CategoriaAlmacen> categorias;
}
