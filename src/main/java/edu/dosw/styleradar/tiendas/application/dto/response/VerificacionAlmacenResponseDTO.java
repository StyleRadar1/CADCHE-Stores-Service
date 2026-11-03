package edu.dosw.styleradar.tiendas.application.dto.response;

import java.time.Instant;
import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de verificación, personería jurídica (NIT y RUT en PDF) y datos de registro de un almacén aliado (SR-RF-62)")
public record VerificacionAlmacenResponseDTO(
        @Schema(description = "NIT del almacén", example = "900123456-1")
        String nit,

        @Schema(description = "Nombre comercial del almacén", example = "Atelier Urbano Chapinero")
        String nombreComercial,

        @Schema(description = "Descripción del almacén", example = "Moda independiente y prendas urbanas")
        String descripcion,

        @Schema(description = "Teléfono de contacto del establecimiento", example = "+57 601 312 4455")
        String telefono,

        @Schema(description = "Correo electrónico de contacto del almacén", example = "contacto@atelierurbano.co")
        String correoContacto,

        @Schema(description = "Latitud geográfica WGS84", example = "4.6533")
        Double latitud,

        @Schema(description = "Longitud geográfica WGS84", example = "-74.0602")
        Double longitud,

        @Schema(description = "Reputación promedio del almacén", example = "4.8")
        Double reputacion,

        @Schema(description = "Categorías comerciales del almacén", example = "[\"CASUAL\",\"VINTAGE\"]")
        Set<CategoriaAlmacen> categorias,

        @Schema(description = "Estado de verificación administrativa (PENDIENTE, APROBADO, RECHAZADO). NULL indica legado sin aprobación administrativa", example = "APROBADO")
        EstadoVerificacionAlmacen estadoVerificacion,

        @Schema(description = "Indica si el registro corresponde a un almacén legado sin verificación explícita", example = "false")
        boolean legadoSinVerificacion,

        @Schema(description = "Fecha y hora UTC de la última decisión de verificación", example = "2026-11-02T19:00:00Z")
        Instant fechaDecisionVerificacion,

        @Schema(description = "Identificador del administrador StyleRadar que registró la última decisión", example = "1")
        Long administradorDecisionId,

        @Schema(description = "Motivo u observación de la decisión de verificación", example = "Datos revisados")
        String motivoVerificacion,

        @Schema(description = "URL del documento RUT adjunto en PDF para validación de personería jurídica (SR-RF-62)",
                example = "https://styleradarblob.blob.core.windows.net/documentos-rut/900123456-1/rut-atelier-urbano.pdf")
        String urlRutPdf
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
            String motivoVerificacion) {
        this(nit, nombreComercial, descripcion, telefono, correoContacto, latitud, longitud, reputacion, categorias,
                estadoVerificacion, legadoSinVerificacion, fechaDecisionVerificacion, administradorDecisionId,
                motivoVerificacion,
                "https://styleradarblob.blob.core.windows.net/documentos-rut/" + nit + "/rut.pdf");
    }
}
