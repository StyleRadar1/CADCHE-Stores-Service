package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AdministradorAlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FotografiaTiendaEntity;

@DisplayName("AlmacenPersistenceMapper: nulos y categorías ausentes")
class AlmacenPersistenceMapperTest {

    private final AlmacenPersistenceMapper mapper = new AlmacenPersistenceMapper();

    @Test
    void entradasNulasDevuelvenNulo() {
        assertNull(mapper.toDomain((AlmacenEntity) null));
        assertNull(mapper.toEntity((Almacen) null));
        assertNull(mapper.toDomain((FotografiaTiendaEntity) null));
        assertNull(mapper.toEntity((FotografiaTienda) null));
        assertNull(mapper.toDomain((AdministradorAlmacenEntity) null));
    }

    @Test
    void categoriasNulasQuedanComoConjuntoVacio() {
        AlmacenEntity entity = new AlmacenEntity();
        entity.setNit("900123456-1");
        entity.setCategorias(null);
        Almacen dominio = mapper.toDomain(entity);
        assertEquals("900123456-1", dominio.getNit());
        assertTrue(dominio.getCategorias().isEmpty());

        dominio.setCategorias(null);
        assertTrue(mapper.toEntity(dominio).getCategorias().isEmpty());
    }
}
