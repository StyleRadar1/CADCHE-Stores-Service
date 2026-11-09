package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AdministradorAlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.AlmacenEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FotografiaTiendaEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.AlmacenPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAdministradorAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataAlmacenRepository;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataFotografiaTiendaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de AlmacenRepositoryAdapter - Persistencia Hexagonal (SR-RF-62 / SR-RF-74)")
class AlmacenRepositoryAdapterTest {

    @Mock
    private SpringDataAlmacenRepository almacenRepository;

    @Mock
    private SpringDataAdministradorAlmacenRepository administradorRepository;

    @Mock
    private SpringDataFotografiaTiendaRepository fotografiaRepository;

    private AlmacenPersistenceMapper mapper;
    private AlmacenRepositoryAdapter adapter;

    private static final String NIT = "900123456";
    private static final Long USUARIO_ID = 100L;

    @BeforeEach
    void setUp() {
        mapper = new AlmacenPersistenceMapper();
        adapter = new AlmacenRepositoryAdapter(almacenRepository, administradorRepository, fotografiaRepository, mapper);
    }

    @Test
    @DisplayName("Debe persistir y mapear entidad de almacén con verificación y RUT")
    void debeGuardarAlmacenCorrectamente() {
        Almacen dominio = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Centro")
                .telefono("3110000000")
                .correoContacto("contacto@tienda.com")
                .umbralUltimasUnidades(4)
                .diasNuevaLlegada(10)
                .estadoVerificacion(EstadoVerificacionAlmacen.APROBADO)
                .urlRutPdf("https://blob/rut.pdf")
                .build();

        AlmacenEntity entidadGuardada = AlmacenEntity.builder()
                .nit(NIT)
                .nombreComercial("Tienda Centro")
                .telefono("3110000000")
                .correoContacto("contacto@tienda.com")
                .umbralUltimasUnidades(4)
                .diasNuevaLlegada(10)
                .fechaActualizacionCatalogo(Instant.now())
                .estadoVerificacion(EstadoVerificacionAlmacen.APROBADO)
                .urlRutPdf("https://blob/rut.pdf")
                .build();

        when(almacenRepository.save(any(AlmacenEntity.class))).thenReturn(entidadGuardada);

        Almacen resultado = adapter.save(dominio);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNit()).isEqualTo(NIT);
        assertThat(resultado.getUmbralUltimasUnidades()).isEqualTo(4);
        assertThat(resultado.getDiasNuevaLlegada()).isEqualTo(10);
        assertThat(resultado.getEstadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.APROBADO);
        assertThat(resultado.getUrlRutPdf()).isEqualTo("https://blob/rut.pdf");
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
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .build();

        when(almacenRepository.findByNit(NIT)).thenReturn(Optional.of(entidad));

        Optional<Almacen> resultado = adapter.findByNit(NIT);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNit()).isEqualTo(NIT);
        assertThat(resultado.get().getUmbralUltimasUnidades()).isEqualTo(5);
        assertThat(resultado.get().getDiasNuevaLlegada()).isEqualTo(12);
        assertThat(resultado.get().getEstadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.PENDIENTE);
    }

    @Test
    @DisplayName("Debe consultar almacenes filtrados por estado de verificación")
    void debeConsultarPorEstadoVerificacion() {
        Pageable pageable = PageRequest.of(0, 10);
        AlmacenEntity entidad = AlmacenEntity.builder()
                .nit(NIT)
                .nombreComercial("Tienda Centro")
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .build();

        when(almacenRepository.findByEstadoVerificacion(eq(EstadoVerificacionAlmacen.PENDIENTE), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entidad), pageable, 1));

        Page<Almacen> resultado = adapter.findByEstadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getNit()).isEqualTo(NIT);
    }

    @Test
    @DisplayName("Debe consultar todos los almacenes para verificación con paginación")
    void debeConsultarTodosParaVerificacion() {
        Pageable pageable = PageRequest.of(0, 10);
        AlmacenEntity entidad = AlmacenEntity.builder()
                .nit(NIT)
                .nombreComercial("Tienda Centro")
                .estadoVerificacion(EstadoVerificacionAlmacen.APROBADO)
                .build();

        when(almacenRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(entidad), pageable, 1));

        Page<Almacen> resultado = adapter.findAllVerificacion(pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getNit()).isEqualTo(NIT);
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

    @Test
    @DisplayName("Debe consultar fotografías por NIT de almacén ordenadas")
    void debeConsultarFotografiasPorAlmacenNit() {
        FotografiaTiendaEntity entity = FotografiaTiendaEntity.builder()
                .id(1L)
                .almacenNit(NIT)
                .urlFoto("https://blob/foto1.jpg")
                .esPortada(true)
                .orden(1)
                .fechaSubida(Instant.now())
                .build();

        when(fotografiaRepository.findByAlmacenNitOrderByOrdenAsc(NIT)).thenReturn(List.of(entity));

        List<FotografiaTienda> resultado = adapter.findFotosByAlmacenNit(NIT);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(1L);
        assertThat(resultado.get(0).getUrl()).isEqualTo("https://blob/foto1.jpg");
        assertThat(resultado.get(0).isPortada()).isTrue();
        verify(fotografiaRepository).findByAlmacenNitOrderByOrdenAsc(NIT);
    }

    @Test
    @DisplayName("Debe persistir y mapear entidad de fotografía de tienda")
    void debeGuardarFotografia() {
        FotografiaTienda dominio = FotografiaTienda.builder()
                .almacenNit(NIT)
                .url("https://blob/foto2.jpg")
                .portada(false)
                .orden(2)
                .fechaRegistro(Instant.now())
                .build();

        FotografiaTiendaEntity entityGuardada = FotografiaTiendaEntity.builder()
                .id(2L)
                .almacenNit(NIT)
                .urlFoto("https://blob/foto2.jpg")
                .esPortada(false)
                .orden(2)
                .fechaSubida(Instant.now())
                .build();

        when(fotografiaRepository.save(any(FotografiaTiendaEntity.class))).thenReturn(entityGuardada);

        FotografiaTienda resultado = adapter.saveFoto(dominio);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(2L);
        assertThat(resultado.getUrl()).isEqualTo("https://blob/foto2.jpg");
        assertThat(resultado.isPortada()).isFalse();
        verify(fotografiaRepository).save(any(FotografiaTiendaEntity.class));
    }

    @Test
    @DisplayName("Debe contar fotografías por NIT de almacén")
    void debeContarFotografiasPorAlmacenNit() {
        when(fotografiaRepository.countByAlmacenNit(NIT)).thenReturn(4);

        int conteo = adapter.countFotosByAlmacenNit(NIT);

        assertThat(conteo).isEqualTo(4);
        verify(fotografiaRepository).countByAlmacenNit(NIT);
    }

    @Test
    @DisplayName("Debe eliminar fotografía por id y nit de almacén")
    void debeEliminarFotografia() {
        adapter.deleteFoto(10L, NIT);

        verify(fotografiaRepository).deleteByIdAndAlmacenNit(10L, NIT);
    }
}
