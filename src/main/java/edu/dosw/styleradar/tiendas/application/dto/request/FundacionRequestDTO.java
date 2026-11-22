package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data to register or update a partner foundation that receives donations")
public class FundacionRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 4, max = 120, message = "El nombre debe tener entre 4 y 120 caracteres")
    @Schema(description = "Institutional name of the foundation", example = "Fundación Abrigo Solidario", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    private String nombre;

    @NotBlank(message = "El NIT es obligatorio")
    @Pattern(regexp = "^[0-9]{8,12}(-[0-9])?$", message = "El NIT debe tener un formato válido (ej. 900215884-1)")
    @Schema(description = "Unique NIT of the foundation", example = "900215884-1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nit;

    @NotBlank(message = "El barrio es obligatorio")
    @Size(max = 80, message = "El barrio no puede superar 80 caracteres")
    @Schema(description = "Neighborhood of the main site", example = "Chapinero", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("neighborhood")
    private String barrio;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    @Schema(description = "Social work and reach of the foundation", example = "Recolección de prendas en buen estado para comunidades vulnerables de Bogotá")
    @JsonProperty("description")
    private String descripcion;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    @Schema(description = "Address of the main collection point", example = "Calle 85 # 15-32, Chapinero, Bogotá", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("address")
    private String direccion;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    @Schema(description = "Contact phone", example = "+57 601 742 8899")
    @JsonProperty("phone")
    private String telefono;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Schema(description = "Institutional email", example = "contacto@abrigosolidario.org", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Schema(description = "WGS84 latitude of the site", example = "4.6712", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("latitude")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Schema(description = "WGS84 longitude of the site", example = "-74.0531", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("longitude")
    private Double longitud;

    @NotEmpty(message = "Debe aceptar al menos una categoría de prenda")
    @Schema(description = "Garment categories accepted as donations", example = "[\"TOP\",\"BOTTOM\",\"FOOTWEAR\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("acceptedCategories")
    private Set<TipoPrenda> categoriasAceptadas;

    @NotNull(message = "El ID del usuario representante es obligatorio")
    @Schema(description = "Identifier of the active user with the FOUNDATION_REPRESENTATIVE role", example = "24", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("representativeId")
    private Long representanteId;

    @Size(max = 300, message = "La URL del certificado no puede superar 300 caracteres")
    @Schema(description = "URL of the legal-entity certificate of the foundation",
            example = "https://styleradarblob.blob.core.windows.net/fundaciones/certificado-abrigo-solidario.pdf")
    @JsonProperty("legalEntityCertificateUrl")
    private String urlCertificadoPersoneriaJuridica;

    @Size(max = 250, message = "Las causas apoyadas no pueden superar 250 caracteres")
    @Schema(description = "Supported social causes and geographic coverage",
            example = "Niñez vulnerable, población desplazada y adultos mayores en Bogotá y Cundinamarca")
    @JsonProperty("supportedCauses")
    private String causasApoyadas;

    @Size(max = 150, message = "El horario de atención no puede superar 150 caracteres")
    @Schema(description = "Opening hours for donations",
            example = "Lunes a Sábado de 08:00 a 17:00")
    @JsonProperty("openingHours")
    private String horarioAtencion;

    @Size(max = 500, message = "Las políticas de donación no pueden superar 500 caracteres")
    @Schema(description = "Donation policies and required condition of donated garments",
            example = "Se reciben prendas limpias en estado Bueno, Muy bueno o Como nuevo")
    @JsonProperty("donationPolicies")
    private String politicasDonacion;
}
