package edu.dosw.styleradar.tiendas.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@MotivoDeRechazoValido
@Schema(description = "Admin decision to approve or reject a pending store")
public class DecisionVerificacionRequestDTO {

    @NotNull
    @Schema(description = "Final decision on the pending request", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
    private DecisionVerificacionAlmacen decision;

    @Size(max = 1000, message = "El motivo debe tener entre 10 y 1000 caracteres")
    @Schema(description = "Reason for the decision; required when the decision is REJECTED (10 to 1000 characters)", example = "Datos y RUT verificados correctamente")
    @JsonProperty("reason")
    private String motivo;

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isMotivoValido() {
        return decision != DecisionVerificacionAlmacen.REJECTED || (motivo != null && !motivo.isBlank());
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isMotivoLongitudValida() {
        if (decision != DecisionVerificacionAlmacen.REJECTED || motivo == null || motivo.isBlank()) {
            return true;
        }
        int len = motivo.strip().length();
        return len >= 10 && len <= 1000;
    }

    @JsonAnySetter
    public void rechazarCampoDesconocido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo no permitido en la decisión de verificación");
    }
}
