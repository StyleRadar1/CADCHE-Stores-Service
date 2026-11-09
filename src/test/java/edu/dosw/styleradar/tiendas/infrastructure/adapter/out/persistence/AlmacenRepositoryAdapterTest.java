package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AdministradorAlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.AlmacenPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAdministradorAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAlmacenRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de AlmacenRepositoryAdapter - Persistencia Hexagonal")
class AlmacenRepositoryAdapterTest {

    @Mock
    private SpringDataAlmacenRepository almacenRepository;

    @Mock
    private SpringDataAdministradorAlmacenRepository administradorRepository;

    private AlmacenPersistenceMapper mapper;
    private AlmacenRepositoryAdapter adapter;

    private static final String NIT = "900123456";
    private static final Long USUARIO_ID = 100L;

    @BeforeEach
    void setUp() {
        mapper = new AlmacenPersistenceMapper();
        adapter = new AlmacenRepositoryAdapter(almacenRepository, administradorRepository, mapper);
    }

    @Test
    @DisplayName("Debe persistir y mapear entidad de almacén con umbral_ultimas_unidades")
    void debeGuardarAlmacenCorrectamente() {
        Almacen dominio = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Centro")
                .telefono("3110000000")
                .correoContacto("contacto@tienda.com")
                .umbralUltimasUnidades(4)
                .diasNuevaLlegada(10)
                .build();

        AlmacenEntity entidadGuardada = AlmacenEntity.builder()
                .nit(NIT)
                .nombreComercial("Tienda Centro")
                .telefono("3110000000")
                .correoContacto("contacto@tienda.com")
                .umbralUltimasUnidades(4)
                .diasNuevaLlegada(10)
                .fechaActualizacionCatalogo(Instant.now())
                .build();

        when(almacenRepository.save(any(AlmacenEntity.class))).thenReturn(entidadGuardada);

        Almacen resultado = adapter.save(dominio);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNit()).isEqualTo(NIT);
        assertThat(resultado.getUmbralUltimasUnidades()).isEqualTo(4);
        assertThat(resultado.getDiasNuevaLlegada()).isEqualTo(10);
        verify(almacenRepository).save(any(AlmacenEntity.class));
    }

    @Test
    @DisplayName("Debe consultar almacén por NIT mapeando a dominio")
    void debeConsultarAlmacenPorNit() {
        AlmacenEntity entidad = AlmacenEntity.builder()
                .nit(NIT)
                .nombreComercial("Tienda Centro")
                .umbralUltimasUnidades(5)
                .diasNuevaLlegada(12)
                .build();

        when(almacenRepository.findByNit(NIT)).thenReturn(Optional.of(entidad));

        Optional<Almacen> resultado = adapter.findByNit(NIT);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNit()).isEqualTo(NIT);
        assertThat(resultado.get().getUmbralUltimasUnidades()).isEqualTo(5);
        assertThat(resultado.get().getDiasNuevaLlegada()).isEqualTo(12);
    }

    @Test
    @DisplayName("Debe validar si usuario es administrador del almacén")
    void debeValidarSiEsAdministrador() {
        when(administradorRepository.existsByAlmacenNitAndUsuarioId(NIT, USUARIO_ID)).thenReturn(true);

        boolean esAdmin = adapter.esAdministradorDeAlmacen(NIT, USUARIO_ID);

        assertThat(esAdmin).isTrue();
        verify(administradorRepository).existsByAlmacenNitAndUsuarioId(NIT, USUARIO_ID);
    }

    @Test
    @DisplayName("Debe asociar nuevo administrador si no existe vínculo previo")
    void debeAsociarAdministradorNuevo() {
        when(administradorRepository.existsByAlmacenNitAndUsuarioId(NIT, USUARIO_ID)).thenReturn(false);

        adapter.asociarAdministrador(NIT, USUARIO_ID);

        verify(administradorRepository).save(any(AdministradorAlmacenEntity.class));
    }

    @Test
    @DisplayName("No debe duplicar vínculo de administrador si ya existe")
    void noDebeDuplicarVinculoSiYaExiste() {
        when(administradorRepository.existsByAlmacenNitAndUsuarioId(NIT, USUARIO_ID)).thenReturn(true);

        adapter.asociarAdministrador(NIT, USUARIO_ID);

        verify(administradorRepository, never()).save(any(AdministradorAlmacenEntity.class));
    }
}
