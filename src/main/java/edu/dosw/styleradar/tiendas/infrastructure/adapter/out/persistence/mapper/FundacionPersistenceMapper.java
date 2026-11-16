package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.PrecisionBaseDatos;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FundacionEntity;

@Component
public class FundacionPersistenceMapper {

    public Fundacion toDomain(FundacionEntity entity) {
        if (entity == null) {
            return null;
        }
        return Fundacion.builder()
                .id(entity.getId())
                .nit(entity.getNit())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .direccion(entity.getDireccion())
                .barrio(entity.getBarrio())
                .telefono(entity.getTelefono())
                .email(entity.getEmail())
                .latitud(entity.getLatitud())
                .longitud(entity.getLongitud())
                .representanteId(entity.getRepresentanteId())
                .urlCertificadoPersoneriaJuridica(entity.getUrlCertificadoPersoneriaJuridica())
                .causasApoyadas(entity.getCausasApoyadas())
                .horarioAtencion(entity.getHorarioAtencion())
                .politicasDonacion(entity.getPoliticasDonacion())
                .activa(entity.isActiva())
                .fechaRegistro(entity.getFechaRegistro())
                .categoriasAceptadas(entity.getCategoriasAceptadas() != null ? new HashSet<>(entity.getCategoriasAceptadas()) : new HashSet<>())
                .build();
    }

    public FundacionEntity toEntity(Fundacion domain) {
        if (domain == null) {
            return null;
        }
        return FundacionEntity.builder()
                .id(domain.getId())
                .nit(domain.getNit())
                .nombre(domain.getNombre())
                .descripcion(domain.getDescripcion())
                .direccion(domain.getDireccion())
                .barrio(domain.getBarrio())
                .telefono(domain.getTelefono())
                .email(domain.getEmail())
                .latitud(domain.getLatitud())
                .longitud(domain.getLongitud())
                .representanteId(domain.getRepresentanteId())
                .urlCertificadoPersoneriaJuridica(domain.getUrlCertificadoPersoneriaJuridica())
                .causasApoyadas(domain.getCausasApoyadas())
                .horarioAtencion(domain.getHorarioAtencion())
                .politicasDonacion(domain.getPoliticasDonacion())
                .activa(domain.isActiva())
                .fechaRegistro(PrecisionBaseDatos.aMicros(domain.getFechaRegistro()))
                .categoriasAceptadas(domain.getCategoriasAceptadas() != null ? new HashSet<>(domain.getCategoriasAceptadas()) : new HashSet<>())
                .build();
    }
}
