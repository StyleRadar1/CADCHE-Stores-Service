package edu.dosw.styleradar.tiendas.domain.port.out;

import java.util.List;
import java.util.Optional;

import edu.dosw.styleradar.tiendas.domain.model.Fundacion;

public interface FundacionRepositoryPort {

    Fundacion save(Fundacion fundacion);

    Optional<Fundacion> findById(Long id);

    Optional<Fundacion> findByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByRepresentanteId(Long representanteId);

    List<Fundacion> findAll();
}
