package edu.dosw.styleradar.tiendas.domain.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;

public interface AlmacenRepositoryPort {

    Optional<Almacen> findByNit(String nit);

    Almacen save(Almacen almacen);

    boolean existsByNit(String nit);

    boolean esAdministradorDeAlmacen(String nit, Long usuarioId);

    void asociarAdministrador(String nit, Long usuarioId);

    Page<Almacen> findByEstadoVerificacion(EstadoVerificacionAlmacen estado, Pageable pageable);

    Page<Almacen> findAllVerificacion(Pageable pageable);
}
