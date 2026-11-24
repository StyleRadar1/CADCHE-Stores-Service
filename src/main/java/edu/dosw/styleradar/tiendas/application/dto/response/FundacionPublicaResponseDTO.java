package edu.dosw.styleradar.tiendas.application.dto.response;

import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SR-RF-117 (análisis v6): datos públicos de una fundación activa. No incluye los datos internos de registro
 * (representante, certificado ni fecha de registro).
 */
@Schema(description = "Public information of an active partner foundation")
public record FundacionPublicaResponseDTO(
        @Schema(example = "3") Long id,
        @Schema(example = "Fundación Abrigo Solidario") String name,
        @Schema(example = "900215884-1") String nit,
        @Schema(example = "Chapinero") String neighborhood,
        String description,
        @Schema(example = "Calle 85 # 15-32, Bogotá") String address,
        String phone,
        @Schema(example = "contacto@abrigosolidario.org") String email,
        @Schema(example = "4.6712") Double latitude,
        @Schema(example = "-74.0531") Double longitude,
        Set<TipoPrenda> acceptedCategories,
        String supportedCauses,
        String openingHours,
        String donationPolicies,
        @Schema(example = "true") boolean active) {

    public static FundacionPublicaResponseDTO de(FundacionResponseDTO f) {
        return new FundacionPublicaResponseDTO(f.getId(), f.getNombre(), f.getNit(), f.getBarrio(), f.getDescripcion(),
                f.getDireccion(), f.getTelefono(), f.getEmail(), f.getLatitud(), f.getLongitud(),
                f.getCategoriasAceptadas(), f.getCausasApoyadas(), f.getHorarioAtencion(), f.getPoliticasDonacion(),
                f.isActiva());
    }
}
