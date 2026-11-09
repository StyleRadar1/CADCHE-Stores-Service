package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarPeriodoRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarUmbralRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ArchivoNoEsImagenException;
import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;
import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFotografiaTiendaUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.exception.GlobalExceptionHandler;

@WebMvcTest(AlmacenController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Pruebas de Integración de API REST - Controlador AlmacenController (SR-RF-59 / SR-RF-60 / SR-RF-74)")
class AlmacenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfigurarUmbralUltimasUnidadesUseCase configurarUmbralUseCase;

    @MockBean
    private ConsultarUmbralUltimasUnidadesUseCase consultarUmbralUseCase;

    @MockBean
    private ConfigurarPeriodoNuevaLlegadaUseCase configurarPeriodoUseCase;

    @MockBean
    private ConsultarPeriodoNuevaLlegadaUseCase consultarPeriodoUseCase;

    @MockBean
    private RegistrarFotografiaTiendaUseCase registrarFotografiaUseCase;

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

    // --- Pruebas específicas SR-RF-60: Marcar prenda como nueva llegada ---

    @Test
    @DisplayName("Criterio SR-RF-60: Dado un almacén sin periodo propio, cuando se consulta, entonces aplica el valor por defecto de 7 días (HTTP 200)")
    void debeConsultarPeriodoConValorPorDefectoDe7Dias() throws Exception {
        ConfiguracionCatalogoAlmacenResponseDTO respuestaMock = new ConfiguracionCatalogoAlmacenResponseDTO(
                NIT, 3, 7, Instant.now());
        when(consultarPeriodoUseCase.consultarPeriodo(NIT)).thenReturn(respuestaMock);

        mockMvc.perform(get("/api/v1/almacenes/{nit}/dias-nueva-llegada", NIT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nit").value(NIT))
                .andExpect(jsonPath("$.diasNuevaLlegada").value(7));
    }

    @Test
    @DisplayName("Criterio SR-RF-60: Dado un periodo válido, cuando lo guardo, entonces queda configurado para mi almacén (HTTP 200)")
    void debeActualizarPeriodoValidoExitosamente() throws Exception {
        ActualizarPeriodoRequestDTO request = new ActualizarPeriodoRequestDTO(14);
        ConfiguracionCatalogoAlmacenResponseDTO respuestaMock = new ConfiguracionCatalogoAlmacenResponseDTO(
                NIT, 3, 14, Instant.now());

        when(configurarPeriodoUseCase.configurarPeriodo(eq(NIT), eq(14), eq(10L)))
                .thenReturn(respuestaMock);

        mockMvc.perform(put("/api/v1/almacenes/{nit}/dias-nueva-llegada", NIT)
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diasNuevaLlegada").value(14));
    }

    @Test
    @DisplayName("Criterio SR-RF-60: Dado un periodo de 0 días o menos, cuando lo guardo, entonces recibo 400 (Bad Request)")
    void debeRechazarPeriodoCeroONegativoEnEndpointDedicado() throws Exception {
        String jsonPayload = """
                {
                    "diasNuevaLlegada": 0
                }
                """;

        mockMvc.perform(put("/api/v1/almacenes/{nit}/dias-nueva-llegada", NIT)
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Criterio SR-RF-60: Dado un almacén que no es mío, cuando cambio su periodo, entonces recibo 403 (Forbidden)")
    void debeRechazarModificacionPeriodoAlmacenAjenoConHttp403() throws Exception {
        ActualizarPeriodoRequestDTO request = new ActualizarPeriodoRequestDTO(10);

        when(configurarPeriodoUseCase.configurarPeriodo(eq(NIT), eq(10), eq(999L)))
                .thenThrow(new AccesoDenegadoAlmacenException("El usuario autenticado (999) no tiene permisos para administrar el almacén con NIT " + NIT));

        mockMvc.perform(put("/api/v1/almacenes/{nit}/dias-nueva-llegada", NIT)
                        .header("X-User-Id", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("El usuario autenticado (999) no tiene permisos para administrar el almacén con NIT " + NIT));
    }

    @Test
    @DisplayName("SR-RF-60: Debe retornar HTTP 404 cuando el almacén consultado en periodo no existe")
    void debeRetornar404CuandoAlmacenNoExisteEnPeriodo() throws Exception {
        when(consultarPeriodoUseCase.consultarPeriodo("000000000"))
                .thenThrow(new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: 000000000"));

        mockMvc.perform(get("/api/v1/almacenes/{nit}/dias-nueva-llegada", "000000000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // SR-RF-74: Registrar fotografías de la tienda física
    // =========================================================================

    @Test
    @DisplayName("Criterio SR-RF-74: Dado hasta 6 fotos válidas, cuando las cargo, entonces quedan en mi almacén y la primera se usa como portada (HTTP 201)")
    void debeCargarFotografiaExitosamenteConHttp201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "fachada.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content".getBytes()
        );

        FotografiaTiendaResponseDTO respuestaMock = new FotografiaTiendaResponseDTO(
                1L,
                NIT,
                "https://styleradar.blob.core.windows.net/tiendas/fachada.jpg",
                1,
                true,
                Instant.now()
        );

        when(registrarFotografiaUseCase.registrarFotografia(
                eq(NIT),
                eq("fachada.jpg"),
                eq(MediaType.IMAGE_JPEG_VALUE),
                eq(file.getSize()),
                any(InputStream.class),
                eq(100L)
        )).thenReturn(respuestaMock);

        mockMvc.perform(multipart("/api/v1/almacenes/{nit}/fotografias", NIT)
                        .file(file)
                        .header("X-User-Id", 100L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.almacenNit").value(NIT))
                .andExpect(jsonPath("$.url").value("https://styleradar.blob.core.windows.net/tiendas/fachada.jpg"))
                .andExpect(jsonPath("$.portada").value(true))
                .andExpect(jsonPath("$.orden").value(1));
    }

    @Test
    @DisplayName("Criterio SR-RF-74: Dado que ya tengo 6 fotos, cuando cargo otra, entonces recibo 422 (Unprocessable Entity)")
    void debeRechazarCargaCuandoSeSuperaLimiteDeFotosConHttp422() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "foto7.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake image png".getBytes()
        );

        when(registrarFotografiaUseCase.registrarFotografia(
                eq(NIT),
                anyString(),
                anyString(),
                anyLong(),
                any(InputStream.class),
                eq(100L)
        )).thenThrow(new LimiteFotografiasSuperadoException("El almacén ya cuenta con el número máximo de 6 fotografías"));

        mockMvc.perform(multipart("/api/v1/almacenes/{nit}/fotografias", NIT)
                        .file(file)
                        .header("X-User-Id", 100L))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("El almacén ya cuenta con el número máximo de 6 fotografías"));
    }

    @Test
    @DisplayName("Criterio SR-RF-74: Dado un archivo que no es imagen o vacío, cuando lo cargo, entonces recibo 400 (Bad Request)")
    void debeRechazarArchivoVacioONoImagenConHttp400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "archivo",
                "vacio.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/almacenes/{nit}/fotografias", NIT)
                        .file(emptyFile)
                        .header("X-User-Id", 100L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Se requiere adjuntar un archivo de imagen válido"));
    }

    @Test
    @DisplayName("Criterio SR-RF-74: Dado formato no admitido validado por caso de uso, cuando lo cargo, entonces recibo 400 (Bad Request)")
    void debeRechazarFormatoNoImagenConHttp400() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "archivo",
                "documento.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 fake".getBytes()
        );

        when(registrarFotografiaUseCase.registrarFotografia(
                eq(NIT),
                anyString(),
                anyString(),
                anyLong(),
                any(InputStream.class),
                eq(100L)
        )).thenThrow(new ArchivoNoEsImagenException("El archivo debe ser una imagen válida (JPEG, PNG o WEBP)"));

        mockMvc.perform(multipart("/api/v1/almacenes/{nit}/fotografias", NIT)
                        .file(textFile)
                        .header("X-User-Id", 100L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El archivo debe ser una imagen válida (JPEG, PNG o WEBP)"));
    }

    @Test
    @DisplayName("Criterio SR-RF-74: Dado un almacén que no es mío, cuando cargo fotos, entonces recibo 403 (Forbidden)")
    void debeRechazarCargaEnAlmacenAjenoConHttp403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "tienda.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image".getBytes()
        );

        when(registrarFotografiaUseCase.registrarFotografia(
                eq(NIT),
                anyString(),
                anyString(),
                anyLong(),
                any(InputStream.class),
                eq(999L)
        )).thenThrow(new AccesoDenegadoAlmacenException("El usuario autenticado (999) no tiene permisos para administrar el almacén con NIT " + NIT));

        mockMvc.perform(multipart("/api/v1/almacenes/{nit}/fotografias", NIT)
                        .file(file)
                        .header("X-User-Id", 999L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("El usuario autenticado (999) no tiene permisos para administrar el almacén con NIT " + NIT));
    }

    @Test
    @DisplayName("Criterio SR-RF-74: Dado un almacén inexistente, cuando cargo fotos, entonces recibo 404 (Not Found)")
    void debeRetornar404CuandoAlmacenNoExisteAlCargarFoto() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "tienda.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image".getBytes()
        );

        when(registrarFotografiaUseCase.registrarFotografia(
                eq("999999999"),
                anyString(),
                anyString(),
                anyLong(),
                any(InputStream.class),
                eq(100L)
        )).thenThrow(new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: 999999999"));

        mockMvc.perform(multipart("/api/v1/almacenes/{nit}/fotografias", "999999999")
                        .file(file)
                        .header("X-User-Id", 100L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("SR-RF-74: Debe listar fotografías de un almacén correctamente (HTTP 200)")
    void debeListarFotografiasDeAlmacenConHttp200() throws Exception {
        List<FotografiaTiendaResponseDTO> fotos = List.of(
                new FotografiaTiendaResponseDTO(1L, NIT, "https://blob/foto1.jpg", 1, true, Instant.now()),
                new FotografiaTiendaResponseDTO(2L, NIT, "https://blob/foto2.jpg", 2, false, Instant.now())
        );

        when(registrarFotografiaUseCase.listarFotografias(NIT)).thenReturn(fotos);

        mockMvc.perform(get("/api/v1/almacenes/{nit}/fotografias", NIT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].portada").value(true))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].portada").value(false));
    }

    @Test
    @DisplayName("SR-RF-74: Debe eliminar fotografía de un almacén correctamente (HTTP 204)")
    void debeEliminarFotografiaConHttp204() throws Exception {
        doNothing().when(registrarFotografiaUseCase).eliminarFotografia(NIT, 1L, 100L);

        mockMvc.perform(delete("/api/v1/almacenes/{nit}/fotografias/{fotoId}", NIT, 1L)
                        .header("X-User-Id", 100L))
                .andExpect(status().isNoContent());
    }
}

