package edu.dosw.styleradar.tiendas.application.dto.response;

import java.time.Instant;
import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información institucional de una fundación aliada registrada en la plataforma (SR-RF-43 / SR-RF-63 / SR-RF-64 / SR-RF-65)")
public class FundacionResponseDTO {

    @Schema(description = "Identificador único de la fundación", example = "3")
    private Long id;

    @Schema(description = "Nombre institucional de la fundación", example = "Fundación Abrigo Solidario")
    private String nombre;

    @Schema(description = "Descripción de la fundación", example = "Recolección de prendas en buen estado para comunidades vulnerables de Bogotá")
    private String descripcion;

    @Schema(description = "Dirección del punto de acopio", example = "Calle 85 # 15-32, Chapinero, Bogotá")
    private String direccion;

    @Schema(description = "Teléfono de contacto", example = "+57 601 742 8899")
    private String telefono;

    @Schema(description = "Correo electrónico institucional", example = "contacto@abrigosolidario.org")
    private String email;

    @Schema(description = "Latitud geográfica WGS84", example = "4.6712")
    private Double latitud;

    @Schema(description = "Longitud geográfica WGS84", example = "-74.0531")
    private Double longitud;

    @Schema(description = "Categorías de prenda que recibe la fundación", example = "[\"SUPERIOR\",\"INFERIOR\",\"CALZADO\"]")
    private Set<TipoPrenda> categoriasAceptadas;

    @Schema(description = "Identificador del usuario representante asignado", example = "24")
    private Long representanteId;

    @Schema(description = "URL del certificado de personería jurídica registrado (SR-RF-63)",
            example = "https://styleradarblob.blob.core.windows.net/fundaciones/certificado-abrigo-solidario.pdf")
    private String urlCertificadoPersoneriaJuridica;

    @Schema(description = "Causas sociales apoyadas y cobertura geográfica (SR-RF-63)",
            example = "Niñez vulnerable, población desplazada y adultos mayores en Bogotá y Cundinamarca")
    private String causasApoyadas;

    @Schema(description = "Horario de atención institucional (SR-RF-64)", example = "Lunes a Sábado de 08:00 a 17:00")
    private String horarioAtencion;

    @Schema(description = "Políticas de recepción de donaciones (SR-RF-64)",
            example = "Se reciben prendas limpias en estado Bueno, Muy bueno o Como nuevo")
    private String politicasDonacion;

    @Schema(description = "Indica si la fundación está activa para recibir nuevas donaciones (SR-RF-65)", example = "true")
    private boolean activa;

    @Schema(description = "Fecha y hora UTC de registro de la fundación", example = "2026-10-20T12:00:00Z")
    private Instant fechaRegistro;
}
