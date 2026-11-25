package edu.dosw.styleradar.tiendas.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UploadedDocumentResponse", description = "Upload result of a PDF document (store RUT or foundation legal-entity certificate) in Azure Blob Storage")
public record DocumentoSubidoResponseDTO(
        @Schema(description = "Public URL of the stored PDF", example = "https://styleradarblob.blob.core.windows.net/documentos-rut/rut-900123456-1.pdf")
        String url,

        @Schema(description = "Original file name", example = "rut-atelier.pdf")
        @JsonProperty("fileName") String nombreArchivo,

        @Schema(description = "Document MIME type", example = "application/pdf")
        String contentType,

        @Schema(description = "Document size in bytes", example = "124800")
        long tamano,

        @Schema(description = "UTC upload date and time", example = "2026-11-14T20:00:00Z")
        @JsonProperty("registeredAt") Instant fechaCarga
) {
}

