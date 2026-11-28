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

    @Test
    void mapeaCamposPerfilComercialIdaYVuelta() {
        Almacen dominio = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Atelier Urbano")
                .representanteLegal("Camila Gómez")
                .cuentaPagos("Bancolombia 012-345678-90")
                .direccionComercial("Carrera 13 # 63-25")
                .barrio("Chapinero")
                .horarioAtencion("Lun-Sáb 10:00 - 20:00")
                .build();

        AlmacenEntity entity = mapper.toEntity(dominio);
        assertEquals("Camila Gómez", entity.getRepresentanteLegal());
        assertEquals("Bancolombia 012-345678-90", entity.getCuentaPagos());
        assertEquals("Carrera 13 # 63-25", entity.getDireccionComercial());
        assertEquals("Chapinero", entity.getBarrio());
        assertEquals("Lun-Sáb 10:00 - 20:00", entity.getHorarioAtencion());

        Almacen reconstruido = mapper.toDomain(entity);
        assertEquals("Camila Gómez", reconstruido.getRepresentanteLegal());
        assertEquals("Bancolombia 012-345678-90", reconstruido.getCuentaPagos());
        assertEquals("Carrera 13 # 63-25", reconstruido.getDireccionComercial());
        assertEquals("Chapinero", reconstruido.getBarrio());
        assertEquals("Lun-Sáb 10:00 - 20:00", reconstruido.getHorarioAtencion());
    }
}
