package edu.dosw.styleradar.tiendas.infrastructure.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenamientoNoDisponibleException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaDecididoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaPendienteException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaRegistradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ArchivoNoEsImagenException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaActivaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaDesactivadaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaRegistradaException;
import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.exception.PrendaNoCumpleDistintivoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.exception.RutRequeridoParaAprobacionException;
import edu.dosw.styleradar.tiendas.domain.exception.TamanoArchivoExcedidoException;
import edu.dosw.styleradar.tiendas.domain.model.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({ReglaDeNegocioException.class, ArchivoNoEsImagenException.class})
    public ResponseEntity<ErrorResponseDTO> handleReglaDeNegocio(
            RuntimeException exception, HttpServletRequest request) {
        log.warn("Regla de negocio no satisfecha en {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(CodigoError.BUSINESS_RULE_VIOLATION, exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        log.warn("Argumento inválido en {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(CodigoError.INVALID_DATA, exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(FalloAutenticacionException.class)
    public ResponseEntity<ErrorResponseDTO> handleFalloAutenticacion(
            FalloAutenticacionException exception, HttpServletRequest request) {
        log.warn("Falla de autenticación (401) en {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(CodigoError.UNAUTHENTICATED, exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AccesoDenegadoAlmacenException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccesoDenegado(
            AccesoDenegadoAlmacenException exception, HttpServletRequest request) {
        log.warn("Acceso denegado (403 BOLA/IDOR) en {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(CodigoError.ACCESS_DENIED, exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler({AlmacenNoEncontradoException.class, FundacionNoEncontradaException.class})
    public ResponseEntity<ErrorResponseDTO> handleRecursoNoEncontrado(
            RuntimeException exception, HttpServletRequest request) {
        log.warn("Recurso no encontrado (404) en {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(CodigoError.RESOURCE_NOT_FOUND, exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler({
            AlmacenYaDecididoException.class,
            AlmacenYaRegistradoException.class,
            FundacionYaRegistradaException.class,
            FundacionYaDesactivadaException.class,
            FundacionYaActivaException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleConflicto(
            RuntimeException exception, HttpServletRequest request) {
        log.warn("Conflicto al procesar recurso (409) en {}: {}", request.getRequestURI(), exception.getMessage());
        Map<String, String> validationErrors = Map.of();
        if (exception instanceof AlmacenYaRegistradoException ex) {
            validationErrors = ex.getValidationErrors();
        } else if (exception instanceof FundacionYaRegistradaException ex) {
            validationErrors = ex.getValidationErrors();
        }
        return buildResponse(CodigoError.CONFLICT, exception.getMessage(),
                request.getRequestURI(), validationErrors);
    }

    @ExceptionHandler({TamanoArchivoExcedidoException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ErrorResponseDTO> handlePayloadTooLarge(
            Exception exception, HttpServletRequest request) {
        String mensaje = exception instanceof TamanoArchivoExcedidoException
                ? exception.getMessage()
                : "La imagen supera el tamaño máximo de 6 MB";
        log.warn("Tamaño de archivo excedido (413) en {}: {}", request.getRequestURI(), mensaje);
        return buildResponse(CodigoError.FILE_TOO_LARGE, mensaje,
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler({
            RutRequeridoParaAprobacionException.class,
            AlmacenYaPendienteException.class,
            LimiteFotografiasSuperadoException.class,
            PrendaNoCumpleDistintivoException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleEntidadNoProcesable(
            RuntimeException exception, HttpServletRequest request) {
        log.warn("Operación no procesable (422) en {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(CodigoError.UNPROCESSABLE_OPERATION, exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(edu.dosw.styleradar.tiendas.domain.exception.DatoInvalidoException.class)
    public ResponseEntity<ErrorResponseDTO> handleDatoInvalido(
            edu.dosw.styleradar.tiendas.domain.exception.DatoInvalidoException exception, HttpServletRequest request) {
        return buildResponse(CodigoError.INVALID_DATA, "La solicitud contiene datos inválidos",
                request.getRequestURI(), exception.getErrores());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        Object objetivo = exception.getBindingResult().getTarget();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.putIfAbsent(nombreJson(objetivo, error.getField()), error.getDefaultMessage()));
        java.util.List<org.springframework.validation.FieldError> errores = exception.getBindingResult().getFieldErrors();
        if (!errores.isEmpty() && errores.stream().allMatch(e -> "MotivoDeRechazoValido".equals(e.getCode()))) {
            // SR-RF-62 FA-06 (análisis v2): el motivo de rechazo es una regla de negocio; el campo se marca igual.
            return buildResponse(CodigoError.BUSINESS_RULE_VIOLATION, errores.get(0).getDefaultMessage(),
                    request.getRequestURI(), validationErrors);
        }
        return buildResponse(CodigoError.INVALID_DATA, "La solicitud contiene datos inválidos",
                request.getRequestURI(), validationErrors);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDTO> handleBindingValidation(
            BindException exception, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        Object objetivo = exception.getBindingResult().getTarget();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.putIfAbsent(nombreJson(objetivo, error.getField()), error.getDefaultMessage()));
        return buildResponse(CodigoError.INVALID_DATA, "La solicitud contiene datos inválidos",
                request.getRequestURI(), validationErrors);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestPartException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleInvalidHttpRequest(
            Exception exception, HttpServletRequest request) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof IllegalArgumentException iae
                    && iae.getMessage() != null
                    && iae.getMessage().contains("Campo no permitido")) {
                // SR-RF-62 FA-07 (análisis v2): BUSINESS_RULE_VIOLATION.
                return buildResponse(CodigoError.BUSINESS_RULE_VIOLATION, iae.getMessage(),
                        request.getRequestURI(), Map.of());
            }
            cause = cause.getCause();
        }
        return buildResponse(CodigoError.INVALID_DATA, "La solicitud contiene parámetros inválidos",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AlmacenamientoNoDisponibleException.class)
    public ResponseEntity<ErrorResponseDTO> handleAlmacenamientoNoDisponible(
            AlmacenamientoNoDisponibleException exception, HttpServletRequest request) {
        log.error("Almacenamiento de archivos no disponible en {}", request.getRequestURI());
        return buildResponse(CodigoError.SERVICE_UNAVAILABLE, exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleRutaInexistente(
            NoResourceFoundException exception, HttpServletRequest request) {
        return buildResponse(CodigoError.RESOURCE_NOT_FOUND, "La ruta solicitada no existe",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return buildResponse(CodigoError.METHOD_NOT_ALLOWED, "El método HTTP no está permitido para esta ruta",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        log.error("Error no controlado al procesar la ruta {}", request.getRequestURI(), exception);
        return buildResponse(CodigoError.INTERNAL_ERROR,
                "Ocurrió un error interno. Intente nuevamente más tarde.",
                request.getRequestURI(), Map.of());
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(
            CodigoError codigo, String message, String path, Map<String, String> validationErrors) {
        HttpStatus status = HttpStatus.valueOf(codigo.estadoHttp());
        ErrorResponseDTO errorResponse = ErrorResponseDTO.problema(codigo, status.getReasonPhrase(), message,
                path, validationErrors);
        return ResponseEntity.status(status)
                .contentType(MediaType.valueOf(ErrorResponseDTO.MEDIA_TYPE))
                .body(errorResponse);
    }

    /**
     * Nombre del campo tal como viaja en el JSON (en inglés, según {@code @JsonProperty}); así {@code errors}
     * usa las mismas claves que el cuerpo de la petición.
     */
    static String nombreJson(Object objetivo, String campo) {
        if (objetivo == null || campo == null) {
            return campo;
        }
        for (Class<?> tipo = objetivo.getClass(); tipo != null && tipo != Object.class; tipo = tipo.getSuperclass()) {
            try {
                com.fasterxml.jackson.annotation.JsonProperty propiedad =
                        tipo.getDeclaredField(campo).getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
                return propiedad != null && !propiedad.value().isBlank() ? propiedad.value() : campo;
            } catch (NoSuchFieldException ignorado) {
                // Se busca en la superclase.
            }
        }
        return campo;
    }
}
