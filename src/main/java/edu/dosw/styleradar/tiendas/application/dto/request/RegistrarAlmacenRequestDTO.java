package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
@Schema(name = "StoreRegistrationRequest", description = "Data to register a store for verification with NIT and RUT in PDF")
public class RegistrarAlmacenRequestDTO {

    @NotBlank(message = "El NIT del almacén es obligatorio")
    @Pattern(regexp = "^[0-9]{8,12}(-[0-9])?$", message = "El NIT debe tener un formato válido (ej. 900123456-1)")
    @Schema(description = "Unique NIT of the business", example = "900123456-1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nit;

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(min = 2, max = 120, message = "El nombre comercial debe tener entre 2 y 120 caracteres")
    @Schema(description = "Trade name of the store", example = "Atelier Urbano Chapinero", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El correo de contacto debe tener un formato válido")
    @Schema(description = "Official email of the store", example = "contacto@atelierurbano.co", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("contactEmail")
    private String correoContacto;

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Schema(description = "WGS84 latitude of the main site", example = "4.6533")
    @JsonProperty("latitude")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Schema(description = "WGS84 longitude of the main site", example = "-74.0602")
    @JsonProperty("longitude")
    private Double longitud;

    @NotEmpty(message = "Debe indicar al menos una categoría comercial")
    @Schema(description = "Business categories of the store", example = "[\"CASUAL\",\"VINTAGE\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("categories")
    private Set<CategoriaAlmacen> categorias;

    @NotBlank(message = "La URL o referencia del documento RUT en PDF es obligatoria para verificar la personería jurídica")
    @Schema(description = "URL of the RUT document in PDF used to verify the legal entity",
            example = "https://styleradarblob.blob.core.windows.net/documentos-rut/900123456-1/rut-atelier-urbano.pdf",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("rutPdfUrl")
    private String urlRutPdf;
}

