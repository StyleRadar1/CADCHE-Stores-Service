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
@Schema(description = "Error en formato ProblemDetail (RFC 9457) con las extensiones del contrato StyleRadar")
public class ErrorResponseDTO {

    /** Formato de fecha del contrato: AAAA-MM-DDThh:mm:ss, sin fracciones de segundo. */
    public static final String FORMATO_FECHA = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String MEDIA_TYPE = "application/problem+json";
    public static final String TIPO_BASE = "https://api.styleradar.com/errores/";

    @Schema(description = "URI que identifica el tipo de problema (RFC 9457)", example = "https://api.styleradar.com/errores/datos-invalidos")
    String type;

    @Schema(description = "Resumen del problema: nombre del estado HTTP (RFC 9457)", example = "Bad Request")
    String title;

    @Schema(description = "Código de estado HTTP", example = "400")
    int status;

    @Schema(description = "Explicación del problema para esta ocurrencia (RFC 9457)", example = "La solicitud contiene datos inválidos")
    String detail;

    @Schema(description = "Ruta de la solicitud que produjo el problema (RFC 9457)", example = "/api/v1/almacenes")
    String instance;

    @Schema(description = "Código de error del catálogo; el front elige el texto por este valor", example = "DATOS_INVALIDOS")
    CodigoError codigo;

    @Schema(description = "Fecha y hora en que ocurrió el error", example = "2026-11-03T01:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = FORMATO_FECHA)
    LocalDateTime timestamp;

    @Schema(description = "Alias de title (compatibilidad)", example = "Bad Request")
    String error;

    @Schema(description = "Alias de detail (compatibilidad)", example = "La solicitud contiene datos inválidos")
    String message;

    @Schema(description = "Alias de instance (compatibilidad)", example = "/api/v1/almacenes")
    String path;

    @Schema(description = "Errores de validación por campo cuando aplica", example = "{}")
    Map<String, String> validationErrors;

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
                .codigo(codigo)
                .timestamp(LocalDateTime.now())
                .error(titulo)
                .message(detalle)
                .path(ruta)
                .validationErrors(validationErrors == null ? Map.of() : validationErrors)
                .build();
    }
}
