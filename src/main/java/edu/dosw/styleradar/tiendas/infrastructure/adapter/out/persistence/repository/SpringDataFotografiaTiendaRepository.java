package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FotografiaTiendaEntity;

@Repository
public interface SpringDataFotografiaTiendaRepository extends JpaRepository<FotografiaTiendaEntity, Long> {

    List<FotografiaTiendaEntity> findByAlmacenNitOrderByOrdenAsc(String almacenNit);

    int countByAlmacenNit(String almacenNit);

    void deleteByIdAndAlmacenNit(Long id, String almacenNit);
}

