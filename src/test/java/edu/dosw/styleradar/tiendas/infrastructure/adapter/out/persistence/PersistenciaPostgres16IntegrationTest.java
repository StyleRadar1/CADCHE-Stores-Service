package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.Pagina;
import edu.dosw.styleradar.tiendas.domain.model.SolicitudPagina;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.AlmacenPersistenceMapper;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.mapper.FundacionPersistenceMapper;

/**
 * Integración contra PostgreSQL 16 real (Testcontainers): aplica las migraciones Flyway V1..V5 con
 * {@code ddl-auto=validate} (el esquema debe coincidir con las entidades JPA) y ejercita los adaptadores.
 * Se omite automáticamente si la máquina no tiene Docker.
 */
@DataJpaTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AlmacenRepositoryAdapter.class, AlmacenPersistenceMapper.class,
        FundacionRepositoryAdapter.class, FundacionPersistenceMapper.class})
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Integración PostgreSQL 16 · Flyway V1..V5 y adaptadores de persistencia de ms-tiendas")
class PersistenciaPostgres16IntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("styleradar_tiendas");

    @DynamicPropertySource
    static void conexion(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private AlmacenRepositoryAdapter almacenes;

    @Autowired
    private FundacionRepositoryAdapter fundaciones;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway aplica V1..V8 sobre PostgreSQL 16 y el esquema valida contra las entidades JPA")
    void migracionesAplicadasEnPostgres16() {
        String version = jdbc.queryForObject("SHOW server_version", String.class);
        Integer migraciones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE AND version IS NOT NULL", Integer.class);

        assertThat(version).startsWith("16.");
        assertThat(migraciones).isEqualTo(8);
    }

    @Test
    @DisplayName("Guarda almacenes, asocia administradores (BOLA) y pagina por estado con orden estable")
    void almacenesAdministradoresYPaginacion() {
        almacenes.save(Almacen.builder().nit("900111222-3").nombreComercial("Zeta Moda")
                .categorias(Set.of(CategoriaAlmacen.CASUAL)).estadoVerificacion(EstadoVerificacionAlmacen.APPROVED)
                .build());
        almacenes.save(Almacen.builder().nit("900333444-5").nombreComercial("Alfa Ropa")
                .categorias(Set.of(CategoriaAlmacen.SPORTY)).estadoVerificacion(EstadoVerificacionAlmacen.APPROVED)
                .build());
        almacenes.save(Almacen.builder().nit("900555666-7").nombreComercial("Pendiente SAS").build());
        almacenes.asociarAdministrador("900111222-3", 42L);

        Pagina<Almacen> aprobados = almacenes.findByEstadoVerificacion(
                EstadoVerificacionAlmacen.APPROVED, new SolicitudPagina(0, 10));

        assertThat(aprobados.map(Almacen::getNombreComercial).contenido()).containsExactly("Alfa Ropa", "Zeta Moda");
        assertThat(aprobados.totalElementos()).isEqualTo(2);
        assertThat(almacenes.findAllVerificacion(new SolicitudPagina(0, 2)).totalPaginas()).isEqualTo(2);
        assertThat(almacenes.esAdministradorDeAlmacen("900111222-3", 42L)).isTrue();
        assertThat(almacenes.esAdministradorDeAlmacen("900111222-3", 7L)).isFalse();
        assertThat(almacenes.findByAdministradorUsuarioId(42L)).get()
                .extracting(Almacen::getNit).isEqualTo("900111222-3");
    }

    @Test
    @DisplayName("Guarda fundaciones con categorías aceptadas y respeta la unicidad de nombre y NIT")
    void fundacionesConCategoriasYUnicidad() {
        Fundacion guardada = fundaciones.save(Fundacion.builder()
                .nombre("Fundación Abrigar").nit("901234567-8").barrio("Chapinero")
                .direccion("Calle 45 # 13-20").email("contacto@abrigar.org")
                .latitud(4.64).longitud(-74.06).representanteId(30L)
                .categoriasAceptadas(Set.of(TipoPrenda.TOP, TipoPrenda.FOOTWEAR))
                .build());

        assertThat(guardada.getId()).isNotNull();
        assertThat(fundaciones.findById(guardada.getId())).get()
                .satisfies(f -> assertThat(f.getCategoriasAceptadas())
                        .containsExactlyInAnyOrder(TipoPrenda.TOP, TipoPrenda.FOOTWEAR));
        assertThat(fundaciones.existsByNombreIgnoreCase("fundación abrigar")).isTrue();
        assertThat(fundaciones.existsByNitIgnoreCase("901234567-8")).isTrue();
        assertThat(fundaciones.existsByRepresentanteId(30L)).isTrue();
    }
}
