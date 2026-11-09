package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;

@Component
public class AlmacenPersistenceMapper {

    public Almacen toDomain(AlmacenEntity entity) {
        if (entity == null) {
            return null;
        }
        return Almacen.builder()
                .nit(entity.getNit())
                .nombreComercial(entity.getNombreComercial())
                .descripcion(entity.getDescripcion())
                .telefono(entity.getTelefono())
                .correoContacto(entity.getCorreoContacto())
                .latitud(entity.getLatitud())
                .longitud(entity.getLongitud())
                .reputacion(entity.getReputacion())
                .umbralUltimasUnidades(entity.getUmbralUltimasUnidades())
                .diasNuevaLlegada(entity.getDiasNuevaLlegada())
                .fechaActualizacionCatalogo(entity.getFechaActualizacionCatalogo())
                .categorias(entity.getCategorias() != null ? new HashSet<>(entity.getCategorias()) : new HashSet<>())
                .build();
    }

    public AlmacenEntity toEntity(Almacen domain) {
        if (domain == null) {
            return null;
        }
        return AlmacenEntity.builder()
                .nit(domain.getNit())
                .nombreComercial(domain.getNombreComercial())
                .descripcion(domain.getDescripcion())
                .telefono(domain.getTelefono())
                .correoContacto(domain.getCorreoContacto())
                .latitud(domain.getLatitud())
                .longitud(domain.getLongitud())
                .reputacion(domain.getReputacion())
                .umbralUltimasUnidades(domain.getUmbralUltimasUnidades())
                .diasNuevaLlegada(domain.getDiasNuevaLlegada())
                .fechaActualizacionCatalogo(domain.getFechaActualizacionCatalogo())
                .categorias(domain.getCategorias() != null ? new HashSet<>(domain.getCategorias()) : new HashSet<>())
                .build();
    }
}
