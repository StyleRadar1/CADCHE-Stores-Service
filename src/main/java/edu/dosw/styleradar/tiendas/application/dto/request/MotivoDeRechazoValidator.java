package edu.dosw.styleradar.tiendas.application.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida el motivo de {@link DecisionVerificacionRequestDTO} y asocia el mensaje al campo {@code motivo}.
 */
public class MotivoDeRechazoValidator
        implements ConstraintValidator<MotivoDeRechazoValido, DecisionVerificacionRequestDTO> {

    static final String MENSAJE_VACIO = "El rechazo exige un motivo no vacío";
    static final String MENSAJE_LONGITUD = "El motivo debe tener entre 10 y 1000 caracteres";

    @Override
    public boolean isValid(DecisionVerificacionRequestDTO decision, ConstraintValidatorContext context) {
        if (decision == null) {
            return true;
        }
        String mensaje = !decision.isMotivoValido() ? MENSAJE_VACIO
                : !decision.isMotivoLongitudValida() ? MENSAJE_LONGITUD
                : null;
        if (mensaje == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(mensaje)
                .addPropertyNode("motivo")
                .addConstraintViolation();
        return false;
    }
}
