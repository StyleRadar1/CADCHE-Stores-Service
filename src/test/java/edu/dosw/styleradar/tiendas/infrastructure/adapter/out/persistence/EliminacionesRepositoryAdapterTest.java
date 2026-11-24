package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.AlmacenPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.FundacionPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAdministradorAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataFotografiaTiendaRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataFundacionRepository;

@DisplayName("Adaptadores de persistencia: eliminar almacén y fundación (análisis v6)")
class EliminacionesRepositoryAdapterTest {

    @Test
    @DisplayName("SR-RF-119: borra el almacén por NIT y confirma de inmediato")
    void eliminarAlmacen() {
        SpringDataAlmacenRepository almacenes = mock(SpringDataAlmacenRepository.class);
        AlmacenRepositoryAdapter adapter = new AlmacenRepositoryAdapter(almacenes,
                mock(SpringDataAdministradorAlmacenRepository.class), mock(SpringDataFotografiaTiendaRepository.class),
                mock(AlmacenPersistenceMapper.class));
        adapter.eliminarPorNit("900123456-1");
        InOrder orden = inOrder(almacenes);
        orden.verify(almacenes).deleteById("900123456-1");
        orden.verify(almacenes).flush();
    }

    @Test
    @DisplayName("SR-RF-120: borra la fundación por id y confirma de inmediato")
    void eliminarFundacion() {
        SpringDataFundacionRepository fundaciones = mock(SpringDataFundacionRepository.class);
        FundacionRepositoryAdapter adapter = new FundacionRepositoryAdapter(fundaciones, mock(FundacionPersistenceMapper.class));
        adapter.eliminarPorId(7L);
        InOrder orden = inOrder(fundaciones);
        orden.verify(fundaciones).deleteById(7L);
        orden.verify(fundaciones).flush();
    }
}
