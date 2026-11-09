package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;

@Repository
public interface SpringDataAlmacenRepository extends JpaRepository<AlmacenEntity, String> {

    Optional<AlmacenEntity> findByNit(String nit);
}
