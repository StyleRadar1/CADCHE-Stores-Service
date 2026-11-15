package edu.dosw.styleradar.tiendas.application.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de carga de un documento PDF (RUT de almacén o Certificado de personería jurídica de fundación) en Azure Blob Storage")
public record DocumentoSubidoResponseDTO(
        @Schema(description = "URL pública del documento PDF almacenado", example = "https://styleradarblob.blob.core.windows.net/documentos-rut/rut-900123456-1.pdf")
        String url,

        @Schema(description = "Nombre original del archivo cargado", example = "rut-atelier.pdf")
        String nombreArchivo,

        @Schema(description = "Tipo MIME del documento", example = "application/pdf")
        String contentType,

        @Schema(description = "Tamaño en bytes del documento", example = "124800")
        long tamano,

        @Schema(description = "Fecha y hora UTC de carga", example = "2026-11-14T20:00:00Z")
        Instant fechaCarga
) {
}

