package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.usecase.ConsultarAlmacenInternoUseCaseImpl;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarAlmacenInternoUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.infrastructure.exception.GlobalExceptionHandler;
import edu.dosw.styleradar.tiendas.infrastructure.security.JwtSecurityValidator;

@WebMvcTest(AlmacenInternoController.class)
@Import({GlobalExceptionHandler.class, JwtSecurityValidator.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "styleradar.internal.auth.username=ms-auth",
        "styleradar.internal.auth.password=test-internal-secret"
})
@DisplayName("Consulta interna de almacén por administrador - BUG-005")
class AlmacenInternoControllerTest {

    private static final String BASIC_AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("ms-auth:test-internal-secret".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarAlmacenInternoUseCase consulta;

    @Test
    @DisplayName("GET /api/v1/internal/stores?ownerUserId=15 responde 200 con nit y nombre comercial")
    void debeRetornarAlmacenDelAdministrador() throws Exception {
        AlmacenResponseDTO dto = new AlmacenResponseDTO(
                "900123456-7",
                "Boutique Norte",
                "Moda urbana",
                4.67,
                -74.05,
                4.9,
                Set.of(CategoriaAlmacen.CASUAL));
        when(consulta.porAdministrador(15L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/internal/stores")
                        .header("Authorization", BASIC_AUTH)
                        .param("ownerUserId", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nit").value("900123456-7"))
                .andExpect(jsonPath("$.tradeName").value("Boutique Norte"));
    }

    @Test
    @DisplayName("GET /api/v1/internal/stores sin auth responde 401; sin ownerUserId 400; si no existe 404")
    void debeRetornar401Y400Y404SegunCorresponda() throws Exception {
        when(consulta.porAdministrador(null))
                .thenThrow(new ReglaDeNegocioException("El parámetro ownerUserId es obligatorio"));
        when(consulta.porAdministrador(99L))
                .thenThrow(new AlmacenNoEncontradoException("No se encontró ningún almacén asociado al usuario: 99"));

        mockMvc.perform(get("/api/v1/internal/stores").param("ownerUserId", "15"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/internal/stores")
                        .header("Authorization", BASIC_AUTH))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/internal/stores")
                        .header("Authorization", BASIC_AUTH)
                        .param("ownerUserId", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ConsultarAlmacenInternoUseCaseImpl resuelve almacén por administrador y valida nulos/ausentes")
    void casoUsoConsultaInterna() {
        AlmacenRepositoryPort repo = mock(AlmacenRepositoryPort.class);
        ConsultarAlmacenInternoUseCaseImpl useCase = new ConsultarAlmacenInternoUseCaseImpl(repo);

        Almacen almacen = Almacen.builder()
                .nit("900111222-3")
                .nombreComercial("Tienda Sur")
                .categorias(Set.of(CategoriaAlmacen.FORMAL))
                .build();
        when(repo.findByAdministradorUsuarioId(15L)).thenReturn(Optional.of(almacen));
        when(repo.findByAdministradorUsuarioId(99L)).thenReturn(Optional.empty());

        AlmacenResponseDTO resultado = useCase.porAdministrador(15L);
        assertThat(resultado.nit()).isEqualTo("900111222-3");
        assertThat(resultado.reputacion()).isEqualTo(0.0);

        assertThatThrownBy(() -> useCase.porAdministrador(null)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> useCase.porAdministrador(99L)).isInstanceOf(AlmacenNoEncontradoException.class);
    }
}
