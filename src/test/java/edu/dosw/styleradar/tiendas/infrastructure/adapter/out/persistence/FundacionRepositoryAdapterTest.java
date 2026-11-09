package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity.FundacionEntity;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.FundacionPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.repository.SpringDataFundacionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de FundacionRepositoryAdapter - Persistencia Hexagonal (SR-RF-63)")
class FundacionRepositoryAdapterTest {

    @Mock
    private SpringDataFundacionRepository repository;

    private FundacionPersistenceMapper mapper;
    private FundacionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new FundacionPersistenceMapper();
        adapter = new FundacionRepositoryAdapter(repository, mapper);
    }

    @Test
    @DisplayName("Debe persistir y mapear entidad de fundación con certificado")
    void debeGuardarFundacionCorrectamente() {
        Fundacion dominio = Fundacion.builder()
                .nombre("Fundación Ropa Limpia")
                .direccion("Carrera 7 # 45-10")
                .email("info@ropalimpia.org")
                .latitud(4.65)
                .longitud(-74.06)
                .representanteId(30L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf")
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .build();

        FundacionEntity entidadGuardada = FundacionEntity.builder()
                .id(1L)
                .nombre("Fundación Ropa Limpia")
                .direccion("Carrera 7 # 45-10")
                .email("info@ropalimpia.org")
                .latitud(4.65)
                .longitud(-74.06)
                .representanteId(30L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf")
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .activa(true)
                .fechaRegistro(Instant.now())
                .build();

        when(repository.save(any(FundacionEntity.class))).thenReturn(entidadGuardada);

        Fundacion resultado = adapter.save(dominio);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Fundación Ropa Limpia");
        assertThat(resultado.getUrlCertificadoPersoneriaJuridica()).isEqualTo("https://blob/cert.pdf");
        verify(repository).save(any(FundacionEntity.class));
    }

    @Test
    @DisplayName("Debe validar si existe por nombre ignorando mayúsculas")
    void debeValidarExistenciaPorNombre() {
        when(repository.existsByNombreIgnoreCase("Fundación Abrigo")).thenReturn(true);

        boolean existe = adapter.existsByNombreIgnoreCase("Fundación Abrigo");

        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("Debe validar si existe por representante ID")
    void debeValidarExistenciaPorRepresentante() {
        when(repository.existsByRepresentanteId(25L)).thenReturn(true);

        boolean existe = adapter.existsByRepresentanteId(25L);

        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("Debe consultar todas las fundaciones mapeando a dominio")
    void debeConsultarTodas() {
        FundacionEntity entidad = FundacionEntity.builder()
                .id(1L)
                .nombre("Fundación Semillas")
                .direccion("Calle 10")
                .email("info@semillas.org")
                .latitud(4.0)
                .longitud(-74.0)
                .representanteId(5L)
                .build();

        when(repository.findAll()).thenReturn(List.of(entidad));

        List<Fundacion> resultado = adapter.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Fundación Semillas");
    }

    @Test
    @DisplayName("Debe consultar por ID")
    void debeConsultarPorId() {
        FundacionEntity entidad = FundacionEntity.builder()
                .id(1L)
                .nombre("Fundación Semillas")
                .direccion("Calle 10")
                .email("info@semillas.org")
                .latitud(4.0)
                .longitud(-74.0)
                .representanteId(5L)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(entidad));

        Optional<Fundacion> resultado = adapter.findById(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(1L);
    }
}
