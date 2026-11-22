package edu.dosw.styleradar.tiendas.application.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@DisplayName("Contrato 3.4 / B-3.2: el motivo de rechazo se valida en el campo motivo")
class MotivoDeRechazoValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<DecisionVerificacionRequestDTO>> validar(DecisionVerificacionAlmacen decision, String motivo) {
        DecisionVerificacionRequestDTO dto = new DecisionVerificacionRequestDTO();
        dto.setDecision(decision);
        dto.setMotivo(motivo);
        return validator.validate(dto);
    }

    @Test
    @DisplayName("Rechazo sin motivo: 'El rechazo exige un motivo no vacío' en el campo motivo")
    void rechazoSinMotivo() {
        var errores = validar(DecisionVerificacionAlmacen.REJECTED, " ");
        assertThat(errores).singleElement().satisfies(e -> {
            assertThat(e.getPropertyPath()).hasToString("motivo");
            assertThat(e.getMessage()).isEqualTo("El rechazo exige un motivo no vacío");
        });
    }

    @Test
    @DisplayName("Rechazo con motivo corto: mensaje de longitud en el campo motivo")
    void rechazoConMotivoCorto() {
        var errores = validar(DecisionVerificacionAlmacen.REJECTED, "corto");
        assertThat(errores).singleElement().satisfies(e -> {
            assertThat(e.getPropertyPath()).hasToString("motivo");
            assertThat(e.getMessage()).isEqualTo("El motivo debe tener entre 10 y 1000 caracteres");
        });
    }

    @Test
    @DisplayName("Aprobar sin motivo y rechazar con 10 caracteres son válidos")
    void casosValidos() {
        assertThat(validar(DecisionVerificacionAlmacen.APPROVED, null)).isEmpty();
        assertThat(validar(DecisionVerificacionAlmacen.REJECTED, "Diez letra")).isEmpty();
    }
}
