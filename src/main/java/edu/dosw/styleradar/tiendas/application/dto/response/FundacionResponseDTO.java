package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Schema(description = "Institutional information of a partner foundation (SR-RF-43 / SR-RF-63 / SR-RF-64 / SR-RF-65)")
public class FundacionResponseDTO {

    @Schema(description = "Foundation identifier", example = "3")
    private Long id;

    @Schema(description = "Institutional name of the foundation", example = "Fundación Abrigo Solidario")
    @JsonProperty("name")
    private String nombre;

    @Schema(description = "NIT of the foundation", example = "900215884-1")
    private String nit;

    @Schema(description = "Neighborhood of the foundation site", example = "Chapinero")
    @JsonProperty("neighborhood")
    private String barrio;

    @Schema(description = "Foundation description", example = "Recolección de prendas en buen estado para comunidades vulnerables de Bogotá")
    @JsonProperty("description")
    private String descripcion;

    @Schema(description = "Collection point address", example = "Calle 85 # 15-32, Chapinero, Bogotá")
    @JsonProperty("address")
    private String direccion;

    @Schema(description = "Contact phone", example = "+57 601 742 8899")
    @JsonProperty("phone")
    private String telefono;

    @Schema(description = "Institutional email", example = "contacto@abrigosolidario.org")
    private String email;

    @Schema(description = "WGS84 latitude", example = "4.6712")
    @JsonProperty("latitude")
    private Double latitud;

    @Schema(description = "WGS84 longitude", example = "-74.0531")
    @JsonProperty("longitude")
    private Double longitud;

    @Schema(description = "Garment categories the foundation accepts", example = "[\"TOP\",\"BOTTOM\",\"FOOTWEAR\"]")
    @JsonProperty("acceptedCategories")
    private Set<TipoPrenda> categoriasAceptadas;

    @Schema(description = "Identifier of the assigned representative user", example = "24")
    @JsonProperty("representativeId")
    private Long representanteId;

    @Schema(description = "URL of the registered legal-entity certificate (SR-RF-63)",
            example = "https://styleradarblob.blob.core.windows.net/fundaciones/certificado-abrigo-solidario.pdf")
    @JsonProperty("legalEntityCertificateUrl")
    private String urlCertificadoPersoneriaJuridica;

    @Schema(description = "Supported social causes and geographic coverage (SR-RF-63)",
            example = "Niñez vulnerable, población desplazada y adultos mayores en Bogotá y Cundinamarca")
    @JsonProperty("supportedCauses")
    private String causasApoyadas;

    @Schema(description = "Opening hours (SR-RF-64)", example = "Lunes a Sábado de 08:00 a 17:00")
    @JsonProperty("openingHours")
    private String horarioAtencion;

    @Schema(description = "Donation policies (SR-RF-64)",
            example = "Se reciben prendas limpias en estado Bueno, Muy bueno o Como nuevo")
    @JsonProperty("donationPolicies")
    private String politicasDonacion;

    @Schema(description = "Whether the foundation accepts new donations (SR-RF-65)", example = "true")
    @JsonProperty("active")
    private boolean activa;

    @Schema(description = "UTC registration date and time of the foundation", example = "2026-10-20T12:00:00Z")
    @JsonProperty("registeredAt")
    private Instant fechaRegistro;
}
