package edu.dosw.styleradar.tiendas.application.dto.response;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import edu.dosw.styleradar.tiendas.domain.model.CodigoError;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Error en formato ProblemDetail (RFC 9457, {@code application/problem+json}) común a ms-auth, ms-usuarios y
 * ms-tiendas (CADCHE-121). {@code error}, {@code message} y {@code path} se conservan como alias de
 * {@code title}, {@code detail} e {@code instance} para no romper al front mientras migra.
 */
@Value
@Builder
@Schema(name = "ErrorResponse", description = "Error in ProblemDetail format (RFC 9457) with the StyleRadar extensions. Only detail/message and the per-field messages are in Spanish")
public class ErrorResponseDTO {

    /** Formato de fecha del contrato: AAAA-MM-DDThh:mm:ss, sin fracciones de segundo. */
    public static final String FORMATO_FECHA = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String MEDIA_TYPE = "application/problem+json";
    public static final String TIPO_BASE = "https://api.styleradar.com/errors/";

    @Schema(description = "URI identifying the problem type: https://api.styleradar.com/errors/{code in lower case with hyphens}", example = "https://api.styleradar.com/errors/invalid-data")
    String type;

    @Schema(description = "HTTP status name (RFC 9457)", example = "Bad Request")
    String title;

    @Schema(description = "HTTP status code", example = "400")
    int status;

    @Schema(description = "Problem explanation for the user (Spanish)", example = "La solicitud contiene datos inválidos")
    String detail;

    @Schema(description = "Request path that produced the problem", example = "/api/v1/stores")
    String instance;

    @Schema(description = "Fixed error code; the front end picks the text to show with it", example = "INVALID_DATA")
    CodigoError code;

    @Schema(description = "Date and time of the error (compatibility alias)", example = "2026-11-03T01:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = FORMATO_FECHA)
    LocalDateTime timestamp;

    @Schema(description = "Alias of title (compatibility)", example = "Bad Request")
    String error;

    @Schema(description = "Alias of detail (compatibility)", example = "La solicitud contiene datos inválidos")
    String message;

    @Schema(description = "Alias of instance (compatibility)", example = "/api/v1/stores")
    String path;

    @Schema(description = "Per-field messages (Spanish) when an input field breaks its rule", example = "{}")
    Map<String, String> errors;

    /**
     * Construye el problema con los campos RFC 9457, sus alias de compatibilidad y el código del catálogo.
     */
    public static ErrorResponseDTO problema(CodigoError codigo, String titulo, String detalle, String ruta,
            Map<String, String> validationErrors) {
        return ErrorResponseDTO.builder()
                .type(TIPO_BASE + codigo.name().toLowerCase(Locale.ROOT).replace('_', '-'))
                .title(titulo)
                .status(codigo.estadoHttp())
                .detail(detalle)
                .instance(ruta)
                .code(codigo)
                .timestamp(LocalDateTime.now())
                .error(titulo)
                .message(detalle)
                .path(ruta)
                .errors(validationErrors == null ? Map.of() : validationErrors)
                .build();
    }
}
