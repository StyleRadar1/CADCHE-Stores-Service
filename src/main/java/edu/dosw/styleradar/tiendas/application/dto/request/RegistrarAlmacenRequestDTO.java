package edu.dosw.styleradar.tiendas.application.dto.request;

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
@Schema(description = "Datos para registrar la solicitud de vinculación de un almacén aliado con NIT y RUT en PDF (SR-RF-62)")
public class RegistrarAlmacenRequestDTO {

    @NotBlank(message = "El NIT del almacén es obligatorio")
    @Pattern(regexp = "^[0-9]{8,12}(-[0-9])?$", message = "El NIT debe tener un formato válido (ej. 900123456-1)")
    @Schema(description = "NIT único del establecimiento comercial", example = "900123456-1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nit;

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(min = 2, max = 120, message = "El nombre comercial debe tener entre 2 y 120 caracteres")
    @Schema(description = "Nombre comercial del almacén aliado", example = "Atelier Urbano Chapinero", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombreComercial;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    @Schema(description = "Descripción comercial de la tienda", example = "Moda independiente y prendas urbanas en Chapinero")
    private String descripcion;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    @Schema(description = "Teléfono de contacto del establecimiento", example = "+57 601 312 4455")
    private String telefono;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El correo de contacto debe tener un formato válido")
    @Schema(description = "Correo electrónico oficial del almacén", example = "contacto@atelierurbano.co", requiredMode = Schema.RequiredMode.REQUIRED)
    private String correoContacto;

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Schema(description = "Latitud geográfica WGS84 de la sede principal", example = "4.6533")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Schema(description = "Longitud geográfica WGS84 de la sede principal", example = "-74.0602")
    private Double longitud;

    @NotEmpty(message = "Debe indicar al menos una categoría comercial")
    @Schema(description = "Categorías comerciales del almacén", example = "[\"CASUAL\",\"VINTAGE\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<CategoriaAlmacen> categorias;

    @NotBlank(message = "La URL o referencia del documento RUT en PDF es obligatoria para verificar la personería jurídica")
    @Schema(description = "URL del documento RUT en formato PDF adjunto para verificación de personería jurídica (SR-RF-62)",
            example = "https://styleradarblob.blob.core.windows.net/documentos-rut/900123456-1/rut-atelier-urbano.pdf",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String urlRutPdf;
}

