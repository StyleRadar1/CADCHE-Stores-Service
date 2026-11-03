package edu.dosw.styleradar.tiendas.application.dto.request;

import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para registrar o actualizar una fundación aliada receptora de donaciones (SR-RF-63 / SR-RF-64)")
public class FundacionRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    @Schema(description = "Nombre institucional de la fundación", example = "Fundación Abrigo Solidario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    @Schema(description = "Descripción de la labor social y alcance de la fundación", example = "Recolección de prendas en buen estado para comunidades vulnerables de Bogotá")
    private String descripcion;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    @Schema(description = "Dirección física del centro de acopio principal", example = "Calle 85 # 15-32, Chapinero, Bogotá", requiredMode = Schema.RequiredMode.REQUIRED)
    private String direccion;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    @Schema(description = "Teléfono de contacto de la fundación (SR-RF-64)", example = "+57 601 742 8899")
    private String telefono;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Schema(description = "Correo electrónico institucional", example = "contacto@abrigosolidario.org", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Schema(description = "Latitud geográfica WGS84 de la sede", example = "4.6712", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Schema(description = "Longitud geográfica WGS84 de la sede", example = "-74.0531", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitud;

    @NotEmpty(message = "Debe aceptar al menos una categoría de prenda")
    @Schema(description = "Categorías de prendas aceptadas en donación", example = "[\"SUPERIOR\",\"INFERIOR\",\"CALZADO\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<TipoPrenda> categoriasAceptadas;

    @NotNull(message = "El ID del usuario representante es obligatorio")
    @Schema(description = "Identificador del usuario activo al que se le asigna el rol REPRESENTANTE_FUNDACION", example = "24", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long representanteId;

    @Size(max = 300, message = "La URL del certificado no puede superar 300 caracteres")
    @Schema(description = "URL del certificado de personería jurídica de la fundación (SR-RF-63)",
            example = "https://styleradarblob.blob.core.windows.net/fundaciones/certificado-abrigo-solidario.pdf")
    private String urlCertificadoPersoneriaJuridica;

    @Size(max = 250, message = "Las causas apoyadas no pueden superar 250 caracteres")
    @Schema(description = "Causas sociales apoyadas y cobertura geográfica (SR-RF-63)",
            example = "Niñez vulnerable, población desplazada y adultos mayores en Bogotá y Cundinamarca")
    private String causasApoyadas;

    @Size(max = 150, message = "El horario de atención no puede superar 150 caracteres")
    @Schema(description = "Horario de atención y recepción de donaciones (SR-RF-64)",
            example = "Lunes a Sábado de 08:00 a 17:00")
    private String horarioAtencion;

    @Size(max = 500, message = "Las políticas de donación no pueden superar 500 caracteres")
    @Schema(description = "Políticas de recepción y estado requerido de las prendas donadas (SR-RF-64)",
            example = "Se reciben prendas limpias en estado Bueno, Muy bueno o Como nuevo")
    private String politicasDonacion;
}
