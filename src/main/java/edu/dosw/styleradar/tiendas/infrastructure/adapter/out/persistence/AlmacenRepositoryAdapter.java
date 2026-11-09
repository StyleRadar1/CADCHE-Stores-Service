package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AdministradorAlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FotografiaTiendaEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.AlmacenPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAdministradorAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataFotografiaTiendaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlmacenRepositoryAdapter implements AlmacenRepositoryPort {

    private final SpringDataAlmacenRepository almacenRepository;
    private final SpringDataAdministradorAlmacenRepository administradorRepository;
    private final SpringDataFotografiaTiendaRepository fotografiaRepository;
    private final AlmacenPersistenceMapper mapper;

    @Override
    public Optional<Almacen> findByNit(String nit) {
        return almacenRepository.findByNit(nit).map(mapper::toDomain);
    }

    @Override
    public Almacen save(Almacen almacen) {
        AlmacenEntity entity = mapper.toEntity(almacen);
        AlmacenEntity saved = almacenRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByNit(String nit) {
        return almacenRepository.existsById(nit);
    }

    @Override
    public boolean esAdministradorDeAlmacen(String nit, Long usuarioId) {
        if (usuarioId == null) {
            return true;
        }
        return administradorRepository.existsByAlmacenNitAndUsuarioId(nit, usuarioId);
    }

    @Override
    public void asociarAdministrador(String nit, Long usuarioId) {
        if (!administradorRepository.existsByAlmacenNitAndUsuarioId(nit, usuarioId)) {
            AdministradorAlmacenEntity vinculo = AdministradorAlmacenEntity.builder()
                    .almacenNit(nit)
                    .usuarioId(usuarioId)
                    .fechaAsignacion(Instant.now())
                    .build();
            administradorRepository.save(vinculo);
        }
    }

    @Override
    public Page<Almacen> findByEstadoVerificacion(EstadoVerificacionAlmacen estado, Pageable pageable) {
        return almacenRepository.findByEstadoVerificacion(estado, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Almacen> findAllVerificacion(Pageable pageable) {
        return almacenRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<FotografiaTienda> findFotosByAlmacenNit(String nit) {
        return fotografiaRepository.findByAlmacenNitOrderByOrdenAsc(nit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public FotografiaTienda saveFoto(FotografiaTienda foto) {
        FotografiaTiendaEntity entity = mapper.toEntity(foto);
        FotografiaTiendaEntity saved = fotografiaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public int countFotosByAlmacenNit(String nit) {
        return fotografiaRepository.countByAlmacenNit(nit);
    }

    @Override
    public void deleteFoto(Long fotoId, String almacenNit) {
        fotografiaRepository.deleteByIdAndAlmacenNit(fotoId, almacenNit);
    }
}
