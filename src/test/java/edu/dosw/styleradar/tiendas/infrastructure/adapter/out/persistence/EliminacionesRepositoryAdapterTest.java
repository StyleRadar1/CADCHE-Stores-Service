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

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("findByRepresentanteId busca la fundación del representante y la convierte al dominio")
    void fundacionPorRepresentante() {
        SpringDataFundacionRepository fundaciones = mock(SpringDataFundacionRepository.class);
        FundacionPersistenceMapper mapper = mock(FundacionPersistenceMapper.class);
        FundacionRepositoryAdapter adapter = new FundacionRepositoryAdapter(fundaciones, mapper);
        edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FundacionEntity entidad =
                new edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FundacionEntity();
        edu.dosw.styleradar.tiendas.domain.model.Fundacion dominio =
                edu.dosw.styleradar.tiendas.domain.model.Fundacion.builder().id(7L).build();
        org.mockito.Mockito.when(fundaciones.findFirstByRepresentanteIdOrderByIdAsc(24L))
                .thenReturn(java.util.Optional.of(entidad));
        org.mockito.Mockito.when(mapper.toDomain(entidad)).thenReturn(dominio);

        org.assertj.core.api.Assertions.assertThat(adapter.findByRepresentanteId(24L)).contains(dominio);
        org.assertj.core.api.Assertions.assertThat(adapter.findByRepresentanteId(99L)).isEmpty();
    }
}
