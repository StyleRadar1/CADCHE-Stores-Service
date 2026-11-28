package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StoreVerificationResponse", description = "Verification status, legal entity (NIT and RUT in PDF) and registration data of a partner store")
public record VerificacionAlmacenResponseDTO(
        @Schema(description = "Store NIT", example = "900123456-1")
        String nit,

        @Schema(description = "Store trade name", example = "Atelier Urbano Chapinero")
        @JsonProperty("tradeName") String nombreComercial,

        @Schema(description = "Store description", example = "Moda independiente y prendas urbanas")
        @JsonProperty("description") String descripcion,

        @Schema(description = "Contact phone of the business", example = "+57 601 312 4455")
        @JsonProperty("phone") String telefono,

        @Schema(description = "Store contact email", example = "contacto@atelierurbano.co")
        @JsonProperty("contactEmail") String correoContacto,

        @Schema(description = "WGS84 latitude", example = "4.6533")
        @JsonProperty("latitude") Double latitud,

        @Schema(description = "WGS84 longitude", example = "-74.0602")
        @JsonProperty("longitude") Double longitud,

        @Schema(description = "Average store rating", example = "4.8")
        @JsonProperty("rating") Double reputacion,

        @Schema(description = "Business categories of the store", example = "[\"CASUAL\",\"VINTAGE\"]")
        @JsonProperty("categories") Set<CategoriaAlmacen> categorias,

        @Schema(description = "Verification status (PENDING, APPROVED, REJECTED). Null means a legacy store never verified", example = "APPROVED")
        @JsonProperty("verificationStatus") EstadoVerificacionAlmacen estadoVerificacion,

        @Schema(description = "Whether the record is a legacy store without explicit verification", example = "false")
        @JsonProperty("legacyUnverified") boolean legadoSinVerificacion,

        @Schema(description = "UTC date and time of the last verification decision", example = "2026-11-02T19:00:00Z")
        @JsonProperty("verificationDecidedAt") Instant fechaDecisionVerificacion,

        @Schema(description = "StyleRadar admin who recorded the last decision", example = "1")
        @JsonProperty("decisionAdminId") Long administradorDecisionId,

        @Schema(description = "Reason or note of the verification decision", example = "Datos revisados")
        @JsonProperty("verificationReason") String motivoVerificacion,

        @Schema(description = "URL of the attached RUT in PDF used to validate the legal entity",
                example = "https://styleradarblob.blob.core.windows.net/documentos-rut/900123456-1/rut-atelier-urbano.pdf")
        @JsonProperty("rutPdfUrl") String urlRutPdf,

        @Schema(description = "Legal representative or administrator reference of the store", example = "Camila Gómez")
        @JsonProperty("representative") String representante,

        @Schema(description = "Payout account for store settlements", example = "Bancolombia Ahorros 012-345678-90")
        @JsonProperty("payoutAccount") String cuentaPagos,

        @Schema(description = "Commercial street address of the store", example = "Carrera 13 # 63-25")
        @JsonProperty("address") String direccionComercial,

        @Schema(description = "Neighborhood (barrio) of the store", example = "Chapinero")
        @JsonProperty("barrio") String barrio,

        @Schema(description = "Business hours of the store", example = "Lun-Sáb 10:00 - 20:00")
        @JsonProperty("hours") String horarioAtencion,

        @Schema(description = "Deterministic UUID identifier of the store", example = "3c2f8a1e-4b5d-3e6f-9a1b-2c3d4e5f6a7b")
        @JsonProperty("almacenId") UUID almacenId
) {
    public VerificacionAlmacenResponseDTO(
            String nit,
            String nombreComercial,
            String descripcion,
            String telefono,
            String correoContacto,
            Double latitud,
            Double longitud,
            Double reputacion,
            Set<CategoriaAlmacen> categorias,
            EstadoVerificacionAlmacen estadoVerificacion,
            boolean legadoSinVerificacion,
            Instant fechaDecisionVerificacion,
            Long administradorDecisionId,
            String motivoVerificacion,
            String urlRutPdf,
            String representante) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto, latitud, longitud, reputacion, categorias,
                estadoVerificacion, legadoSinVerificacion, fechaDecisionVerificacion, administradorDecisionId,
                motivoVerificacion, urlRutPdf, representante, null, null, null, null, Almacen.calcularAlmacenId(nit));
    }

    public VerificacionAlmacenResponseDTO(
            String nit,
            String nombreComercial,
            String descripcion,
            String telefono,
            String correoContacto,
            Double latitud,
            Double longitud,
            Double reputacion,
            Set<CategoriaAlmacen> categorias,
            EstadoVerificacionAlmacen estadoVerificacion,
            boolean legadoSinVerificacion,
            Instant fechaDecisionVerificacion,
            Long administradorDecisionId,
            String motivoVerificacion,
            String urlRutPdf) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto, latitud, longitud, reputacion, categorias,
                estadoVerificacion, legadoSinVerificacion, fechaDecisionVerificacion, administradorDecisionId,
                motivoVerificacion, urlRutPdf, null);
    }

    public VerificacionAlmacenResponseDTO(
            String nit,
            String nombreComercial,
            String descripcion,
            String telefono,
            String correoContacto,
            Double latitud,
            Double longitud,
            Double reputacion,
            Set<CategoriaAlmacen> categorias,
            EstadoVerificacionAlmacen estadoVerificacion,
            boolean legadoSinVerificacion,
            Instant fechaDecisionVerificacion,
            Long administradorDecisionId,
            String motivoVerificacion) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto, latitud, longitud, reputacion, categorias,
                estadoVerificacion, legadoSinVerificacion, fechaDecisionVerificacion, administradorDecisionId,
                motivoVerificacion,
                "https://styleradarblob.blob.core.windows.net/documentos-rut/" + nit + "/rut.pdf",
                null);
    }
}
