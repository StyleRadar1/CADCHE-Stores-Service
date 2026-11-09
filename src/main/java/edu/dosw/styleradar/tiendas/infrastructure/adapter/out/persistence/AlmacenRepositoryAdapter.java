package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AdministradorAlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.AlmacenPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAdministradorAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAlmacenRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlmacenRepositoryAdapter implements AlmacenRepositoryPort {

    private final SpringDataAlmacenRepository almacenRepository;
    private final SpringDataAdministradorAlmacenRepository administradorRepository;
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
}

