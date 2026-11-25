package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.model.Pagina;
import edu.dosw.styleradar.tiendas.domain.model.SolicitudPagina;
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
    public Optional<Almacen> findByDeterministicUuid(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return almacenRepository.findAll().stream()
                .filter(entity -> entity.getNit() != null && !entity.getNit().isBlank())
                .filter(entity -> uuid.equals(uuidDeterministicoParaNit(entity.getNit())))
                .findFirst()
                .map(mapper::toDomain);
    }

    public static UUID uuidDeterministicoParaNit(String nit) {
        return UUID.nameUUIDFromBytes(
                ("ms-tiendas:almacen:" + nit.trim()).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<Almacen> findByAdministradorUsuarioId(Long usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }
        return administradorRepository.findFirstByUsuarioIdOrderByFechaAsignacionAsc(usuarioId)
                .flatMap(vinculo -> almacenRepository.findByNit(vinculo.getAlmacenNit()))
                .map(mapper::toDomain);
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
    public boolean existeAlmacenParaUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        return administradorRepository.existsByUsuarioId(usuarioId);
    }

    @Override
    public boolean esAdministradorDeAlmacen(String nit, Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        return administradorRepository.existsByAlmacenNitAndUsuarioId(nit, usuarioId);
    }

    @Override
    public void asociarAdministrador(String nit, Long usuarioId) {
        if (!administradorRepository.existsByAlmacenNitAndUsuarioId(nit, usuarioId)) {
            AdministradorAlmacenEntity vinculo = AdministradorAlmacenEntity.builder()
                    .almacenNit(nit)
                    .usuarioId(usuarioId)
                    .fechaAsignacion(PrecisionBaseDatos.ahora())
                    .build();
            administradorRepository.save(vinculo);
        }
    }

    @Override
    public AdministradorAlmacen guardarAsociacionAdministrador(String nit, Long usuarioId, Long asignadoPorId) {
        return administradorRepository.findByAlmacenNitAndUsuarioId(nit, usuarioId)
                .map(mapper::toDomain)
                .orElseGet(() -> {
                    AdministradorAlmacenEntity vinculo = AdministradorAlmacenEntity.builder()
                            .almacenNit(nit)
                            .usuarioId(usuarioId)
                            .asignadoPorId(asignadoPorId)
                            .fechaAsignacion(PrecisionBaseDatos.ahora())
                            .build();
                    return mapper.toDomain(administradorRepository.save(vinculo));
                });
    }

    @Override
    public List<AdministradorAlmacen> listarAdministradoresPorNit(String nit) {
        return administradorRepository.findByAlmacenNit(nit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Pagina<Almacen> findByEstadoVerificacion(EstadoVerificacionAlmacen estado, SolicitudPagina solicitud) {
        return aPagina(almacenRepository.findByEstadoVerificacion(estado, aPageable(solicitud)));
    }

    @Override
    public Pagina<Almacen> findAllVerificacion(SolicitudPagina solicitud) {
        return aPagina(almacenRepository.findAll(aPageable(solicitud)));
    }

    /**
     * Orden estable para la paginación: nombre comercial y NIT ascendentes.
     */
    private static Pageable aPageable(SolicitudPagina solicitud) {
        return PageRequest.of(solicitud.numero(), solicitud.tamano(),
                Sort.by("nombreComercial").ascending().and(Sort.by("nit").ascending()));
    }

    private Pagina<Almacen> aPagina(Page<AlmacenEntity> pagina) {
        return new Pagina<>(pagina.getContent().stream().map(mapper::toDomain).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
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

    @Override
    public void eliminarPorNit(String nit) {
        almacenRepository.deleteById(nit);
        almacenRepository.flush();
    }

    @Override
    public long eliminarAsociacionesDeUsuario(Long usuarioId) {
        return administradorRepository.deleteByUsuarioId(usuarioId);
    }
}
