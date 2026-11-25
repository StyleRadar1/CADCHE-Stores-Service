package edu.dosw.styleradar.tiendas.application.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Evento de ms-usuarios al eliminar una cuenta: {@code {"userId": 42, "deletedAt": "..."}}. Se ignoran los demás campos
 * (eventId, eventType...) para tolerar cambios compatibles del productor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioEliminadoEventDTO(Long userId, String deletedAt) {
}
