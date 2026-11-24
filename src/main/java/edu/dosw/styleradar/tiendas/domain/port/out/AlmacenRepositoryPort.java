package edu.dosw.styleradar.tiendas.domain.port.out;

import java.util.List;
import java.util.Optional;


import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.model.Pagina;
import edu.dosw.styleradar.tiendas.domain.model.SolicitudPagina;

public interface AlmacenRepositoryPort {

    Optional<Almacen> findByNit(String nit);

    Optional<Almacen> findByAdministradorUsuarioId(Long usuarioId);

    Almacen save(Almacen almacen);

    boolean existsByNit(String nit);

    boolean existeAlmacenParaUsuario(Long usuarioId);

    boolean esAdministradorDeAlmacen(String nit, Long usuarioId);

    void asociarAdministrador(String nit, Long usuarioId);

    AdministradorAlmacen guardarAsociacionAdministrador(String nit, Long usuarioId, Long asignadoPorId);

    List<AdministradorAlmacen> listarAdministradoresPorNit(String nit);

    Pagina<Almacen> findByEstadoVerificacion(EstadoVerificacionAlmacen estado, SolicitudPagina solicitud);

    Pagina<Almacen> findAllVerificacion(SolicitudPagina solicitud);

    List<FotografiaTienda> findFotosByAlmacenNit(String nit);

    FotografiaTienda saveFoto(FotografiaTienda foto);

    int countFotosByAlmacenNit(String nit);

    void deleteFoto(Long fotoId, String almacenNit);

    /** SR-RF-119: borra el almacén; la base de datos elimina en cascada fotos, categorías y administradores. */
    void eliminarPorNit(String nit);
}
