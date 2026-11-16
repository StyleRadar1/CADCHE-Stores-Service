package edu.dosw.styleradar.tiendas.domain.port.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;

public interface AlmacenRepositoryPort {

    Optional<Almacen> findByNit(String nit);

    Optional<Almacen> findByAdministradorUsuarioId(Long usuarioId);

    Almacen save(Almacen almacen);

    boolean existsByNit(String nit);

    boolean existeAlmacenParaUsuario(Long usuarioId);

    boolean esAdministradorDeAlmacen(String nit, Long usuarioId);

    void asociarAdministrador(String nit, Long usuarioId);

    AdministradorAlmacen guardarAsociacionAdministrador(String nit, Long usuarioId);

    List<AdministradorAlmacen> listarAdministradoresPorNit(String nit);

    Page<Almacen> findByEstadoVerificacion(EstadoVerificacionAlmacen estado, Pageable pageable);

    Page<Almacen> findAllVerificacion(Pageable pageable);

    List<FotografiaTienda> findFotosByAlmacenNit(String nit);

    FotografiaTienda saveFoto(FotografiaTienda foto);

    int countFotosByAlmacenNit(String nit);

    void deleteFoto(Long fotoId, String almacenNit);
}
