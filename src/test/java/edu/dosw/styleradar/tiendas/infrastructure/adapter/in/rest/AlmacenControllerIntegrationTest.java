package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarUmbralRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.exception.GlobalExceptionHandler;

@WebMvcTest(AlmacenController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Pruebas de Integración de API REST - Controlador AlmacenController (SR-RF-59)")
class AlmacenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfigurarUmbralUltimasUnidadesUseCase configurarUmbralUseCase;

    @MockBean
    private ConsultarUmbralUltimasUnidadesUseCase consultarUmbralUseCase;

    private static final String NIT = "900123456";

    @Test
    @DisplayName("Criterio: Dado un almacén sin umbral propio, cuando se consulta, entonces aplica el valor por defecto de 3 (HTTP 200)")
    void debeConsultarConfiguracionConValorPorDefectoDe3() throws Exception {
        ConfiguracionCatalogoAlmacenResponseDTO respuestaMock = new ConfiguracionCatalogoAlmacenResponseDTO(
                NIT, 3, 15, Instant.now());
        when(consultarUmbralUseCase.consultarConfiguracion(NIT)).thenReturn(respuestaMock);

        mockMvc.perform(get("/api/v1/almacenes/{nit}/configuracion-catalogo", NIT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nit").value(NIT))
                .andExpect(jsonPath("$.umbralUltimasUnidades").value(3))
                .andExpect(jsonPath("$.diasNuevaLlegada").value(15));
    }

    @Test
    @DisplayName("Criterio: Dado un umbral válido, cuando lo guardo, entonces queda configurado para mi almacén (HTTP 200)")
    void debeGuardarUmbralValidoExitosamente() throws Exception {
        ConfiguracionCatalogoAlmacenRequestDTO request = new ConfiguracionCatalogoAlmacenRequestDTO(5, 20);
        ConfiguracionCatalogoAlmacenResponseDTO respuestaMock = new ConfiguracionCatalogoAlmacenResponseDTO(
                NIT, 5, 20, Instant.now());

        when(configurarUmbralUseCase.configurarCatalogo(eq(NIT), any(ConfiguracionCatalogoAlmacenRequestDTO.class), eq(10L)))
                .thenReturn(respuestaMock);

        mockMvc.perform(put("/api/v1/almacenes/{nit}/configuracion-catalogo", NIT)
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nit").value(NIT))
                .andExpect(jsonPath("$.umbralUltimasUnidades").value(5))
                .andExpect(jsonPath("$.diasNuevaLlegada").value(20));
    }

    @Test
    @DisplayName("Criterio: Dado un umbral negativo, cuando lo guardo, entonces recibo 400 (Bad Request)")
    void debeRechazarUmbralNegativoConHttp400() throws Exception {
        // Enviar un request con umbral negativo (-1)
        String jsonPayload = """
                {
                    "umbralUltimasUnidades": -1,
                    "diasNuevaLlegada": 15
                }
                """;

        mockMvc.perform(put("/api/v1/almacenes/{nit}/configuracion-catalogo", NIT)
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Criterio: Dado un almacén que no es mío, cuando cambio su umbral, entonces recibo 403 (Forbidden)")
    void debeRechazarModificacionDeAlmacenAjenoConHttp403() throws Exception {
        ConfiguracionCatalogoAlmacenRequestDTO request = new ConfiguracionCatalogoAlmacenRequestDTO(4, 15);

        when(configurarUmbralUseCase.configurarCatalogo(eq(NIT), any(ConfiguracionCatalogoAlmacenRequestDTO.class), eq(999L)))
                .thenThrow(new AccesoDenegadoAlmacenException("El usuario autenticado (999) no tiene permisos para administrar el almacén con NIT " + NIT));

        mockMvc.perform(put("/api/v1/almacenes/{nit}/configuracion-catalogo", NIT)
                        .header("X-User-Id", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("El usuario autenticado (999) no tiene permisos para administrar el almacén con NIT " + NIT));
    }

    @Test
    @DisplayName("Endpoint dedicado: PUT /api/v1/almacenes/{nit}/umbral-ultimas-unidades con umbral válido retorna 200")
    void debeActualizarUmbralEnEndpointDedicado() throws Exception {
        ActualizarUmbralRequestDTO request = new ActualizarUmbralRequestDTO(6);
        ConfiguracionCatalogoAlmacenResponseDTO respuestaMock = new ConfiguracionCatalogoAlmacenResponseDTO(
                NIT, 6, 15, Instant.now());

        when(configurarUmbralUseCase.configurarUmbral(eq(NIT), eq(6), eq(10L)))
                .thenReturn(respuestaMock);

        mockMvc.perform(put("/api/v1/almacenes/{nit}/umbral-ultimas-unidades", NIT)
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umbralUltimasUnidades").value(6));
    }

    @Test
    @DisplayName("Endpoint dedicado: PUT /api/v1/almacenes/{nit}/umbral-ultimas-unidades con umbral negativo retorna 400")
    void debeRechazarUmbralNegativoEnEndpointDedicado() throws Exception {
        String jsonPayload = """
                {
                    "umbralUltimasUnidades": -5
                }
                """;

        mockMvc.perform(put("/api/v1/almacenes/{nit}/umbral-ultimas-unidades", NIT)
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Endpoint dedicado: GET /api/v1/almacenes/{nit}/umbral-ultimas-unidades retorna 200")
    void debeConsultarUmbralEnEndpointDedicado() throws Exception {
        ConfiguracionCatalogoAlmacenResponseDTO respuestaMock = new ConfiguracionCatalogoAlmacenResponseDTO(
                NIT, 3, 15, Instant.now());
        when(consultarUmbralUseCase.consultarConfiguracion(NIT)).thenReturn(respuestaMock);

        mockMvc.perform(get("/api/v1/almacenes/{nit}/umbral-ultimas-unidades", NIT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umbralUltimasUnidades").value(3));
    }

    @Test
    @DisplayName("Debe retornar HTTP 404 cuando el almacén no existe")
    void debeRetornar404CuandoAlmacenNoExiste() throws Exception {
        when(consultarUmbralUseCase.consultarConfiguracion("000000000"))
                .thenThrow(new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: 000000000"));

        mockMvc.perform(get("/api/v1/almacenes/{nit}/configuracion-catalogo", "000000000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No se encontró ningún almacén registrado con el NIT: 000000000"));
    }
}
