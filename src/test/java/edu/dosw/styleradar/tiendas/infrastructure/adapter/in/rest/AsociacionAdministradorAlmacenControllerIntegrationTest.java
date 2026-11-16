package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.port.in.AsociarAdministradorAlmacenUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.exception.GlobalExceptionHandler;
import edu.dosw.styleradar.tiendas.infrastructure.security.JwtSecurityValidator;

@WebMvcTest(AsociacionAdministradorAlmacenController.class)
@Import({GlobalExceptionHandler.class, JwtSecurityValidator.class})
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración de API REST - AsociacionAdministradorAlmacenController (SR-RF-62 / BOLA)")
class AsociacionAdministradorAlmacenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AsociarAdministradorAlmacenUseCase useCase;

    private static final String NIT = "900123456-1";

    @Test
    @DisplayName("PUT y GET /api/v1/admin/almacenes/{nit}/administradores deben asociar y listar administradores reales")
    void debeAsociarYListarAdministradoresViaRest() throws Exception {
        AdministradorAlmacenResponseDTO dto = new AdministradorAlmacenResponseDTO(
                10L, 15L, NIT, 1L, Instant.now());

        when(useCase.asociar(eq(NIT), eq(15L), eq(1L), eq("ROLE_ADMIN_STYLERADAR"))).thenReturn(dto);
        when(useCase.listarAdministradores(eq(NIT), eq(15L), eq("ROLE_ADMIN_STYLERADAR"))).thenReturn(List.of(dto));

        mockMvc.perform(put("/api/v1/admin/almacenes/{nit}/administradores/{usuarioId}", NIT, 15L)
                        .header("X-User-Id", 1L)
                        .header("X-User-Roles", "ROLE_ADMIN_STYLERADAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(15))
                .andExpect(jsonPath("$.almacenNit").value(NIT));

        mockMvc.perform(get("/api/v1/admin/almacenes/{nit}/administradores", NIT)
                        .header("X-User-Id", 15L)
                        .header("X-User-Roles", "ROLE_ADMIN_STYLERADAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(15));
    }
}

