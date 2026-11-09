package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AdministradorAlmacenEntity;

@Repository
public interface SpringDataAdministradorAlmacenRepository extends JpaRepository<AdministradorAlmacenEntity, Long> {

    boolean existsByAlmacenNitAndUsuarioId(String almacenNit, Long usuarioId);

    Optional<AdministradorAlmacenEntity> findByAlmacenNitAndUsuarioId(String almacenNit, Long usuarioId);

    List<AdministradorAlmacenEntity> findByAlmacenNit(String almacenNit);
}
