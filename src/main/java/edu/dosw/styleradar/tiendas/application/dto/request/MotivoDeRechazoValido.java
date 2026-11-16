package edu.dosw.styleradar.tiendas.application.dto.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * SR-RF-62 (B-3.2): al rechazar, el motivo es obligatorio y tiene entre 10 y 1000 caracteres. El error se reporta
 * en el campo {@code motivo} para que el front lo muestre debajo de él (contrato 3.4).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MotivoDeRechazoValidator.class)
public @interface MotivoDeRechazoValido {

    String message() default "El motivo debe tener entre 10 y 1000 caracteres";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
