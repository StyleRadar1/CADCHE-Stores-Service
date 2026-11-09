package edu.dosw.styleradar.tiendas.domain.port.out;

import java.util.Optional;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;

public interface AlmacenRepositoryPort {

    Optional<Almacen> findByNit(String nit);

    Almacen save(Almacen almacen);

    boolean existsByNit(String nit);

    boolean esAdministradorDeAlmacen(String nit, Long usuarioId);

    void asociarAdministrador(String nit, Long usuarioId);
}
