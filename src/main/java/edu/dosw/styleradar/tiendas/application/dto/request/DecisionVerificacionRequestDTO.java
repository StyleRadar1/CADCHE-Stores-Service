package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Decisión administrativa para aprobar o rechazar la vinculación pendiente de un almacén aliado (SR-RF-62)")
public class DecisionVerificacionRequestDTO {

    @NotNull
    @Schema(description = "Decisión final sobre la solicitud pendiente", example = "APROBADO", requiredMode = Schema.RequiredMode.REQUIRED)
    private DecisionVerificacionAlmacen decision;

    @Size(max = 1000)
    @Schema(description = "Motivo de la decisión; obligatorio cuando la decisión es RECHAZADO (máximo 1000 caracteres)", example = "Datos y RUT verificados correctamente")
    private String motivo;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "El rechazo exige un motivo no vacío")
    public boolean isMotivoValido() {
        return decision != DecisionVerificacionAlmacen.RECHAZADO || (motivo != null && !motivo.isBlank());
    }

    @JsonAnySetter
    public void rechazarCampoDesconocido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo no permitido en la decisión de verificación");
    }
}

