package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;

@Repository
public interface SpringDataAlmacenRepository extends JpaRepository<AlmacenEntity, String> {

    Optional<AlmacenEntity> findByNit(String nit);

    Page<AlmacenEntity> findByEstadoVerificacion(EstadoVerificacionAlmacen estadoVerificacion, Pageable pageable);
}
