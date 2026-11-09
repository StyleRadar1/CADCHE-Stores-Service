package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FundacionEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.FundacionPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataFundacionRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FundacionRepositoryAdapter implements FundacionRepositoryPort {

    private final SpringDataFundacionRepository repository;
    private final FundacionPersistenceMapper mapper;

    @Override
    public Fundacion save(Fundacion fundacion) {
        FundacionEntity entity = mapper.toEntity(fundacion);
        FundacionEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Fundacion> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Fundacion> findByNombre(String nombre) {
        return repository.findByNombreIgnoreCase(nombre).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNombreIgnoreCase(String nombre) {
        return repository.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public boolean existsByRepresentanteId(Long representanteId) {
        return repository.existsByRepresentanteId(representanteId);
    }

    @Override
    public List<Fundacion> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
