package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FotografiaTiendaEntity;

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
                .estadoVerificacion(entity.getEstadoVerificacion())
                .urlRutPdf(entity.getUrlRutPdf())
                .fechaDecisionVerificacion(entity.getFechaDecisionVerificacion())
                .administradorDecisionId(entity.getAdministradorDecisionId())
                .motivoVerificacion(entity.getMotivoVerificacion())
                .fotoPortadaUrl(entity.getFotoPortadaUrl())
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
                .estadoVerificacion(domain.getEstadoVerificacion())
                .urlRutPdf(domain.getUrlRutPdf())
                .fechaDecisionVerificacion(domain.getFechaDecisionVerificacion())
                .administradorDecisionId(domain.getAdministradorDecisionId())
                .motivoVerificacion(domain.getMotivoVerificacion())
                .fotoPortadaUrl(domain.getFotoPortadaUrl())
                .categorias(domain.getCategorias() != null ? new HashSet<>(domain.getCategorias()) : new HashSet<>())
                .build();
    }

    public FotografiaTienda toDomain(FotografiaTiendaEntity entity) {
        if (entity == null) {
            return null;
        }
        return FotografiaTienda.builder()
                .id(entity.getId())
                .almacenNit(entity.getAlmacenNit())
                .url(entity.getUrlFoto())
                .orden(entity.getOrden())
                .portada(entity.isEsPortada())
                .fechaRegistro(entity.getFechaSubida())
                .build();
    }

    public FotografiaTiendaEntity toEntity(FotografiaTienda domain) {
        if (domain == null) {
            return null;
        }
        return FotografiaTiendaEntity.builder()
                .id(domain.getId())
                .almacenNit(domain.getAlmacenNit())
                .urlFoto(domain.getUrl())
                .orden(domain.getOrden())
                .esPortada(domain.isPortada())
                .fechaSubida(domain.getFechaRegistro())
                .build();
    }
}
