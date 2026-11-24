package edu.dosw.styleradar.tiendas.domain.port.out;

import java.util.List;
import java.util.Optional;

import edu.dosw.styleradar.tiendas.domain.model.Fundacion;

public interface FundacionRepositoryPort {

    Fundacion save(Fundacion fundacion);

    Optional<Fundacion> findById(Long id);

    Optional<Fundacion> findByNombre(String nombre);

    boolean existsByNitIgnoreCase(String nit);

    boolean existsByNitIgnoreCaseAndIdNot(String nit, Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByRepresentanteId(Long representanteId);

    boolean existsByRepresentanteIdAndIdNot(Long representanteId, Long id);

    List<Fundacion> findAll();

    /** SR-RF-120: borra la fundación; la base de datos elimina en cascada sus categorías aceptadas. */
    void eliminarPorId(Long id);
}
