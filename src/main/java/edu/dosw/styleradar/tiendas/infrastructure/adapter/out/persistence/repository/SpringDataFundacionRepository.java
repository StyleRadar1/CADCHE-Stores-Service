package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FundacionEntity;

@Repository
public interface SpringDataFundacionRepository extends JpaRepository<FundacionEntity, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByRepresentanteId(Long representanteId);

    Optional<FundacionEntity> findByNombreIgnoreCase(String nombre);
}
