package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaActivaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaDesactivadaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaRegistradaException;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.domain.port.in.DesactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.EditarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ReactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.exception.GlobalExceptionHandler;
import edu.dosw.styleradar.tiendas.infrastructure.security.JwtSecurityValidator;

@WebMvcTest(FundacionController.class)
@Import({GlobalExceptionHandler.class, JwtSecurityValidator.class})
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración de API REST - FundacionController (SR-RF-63 / SR-RF-64 / SR-RF-65)")
class FundacionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrarFundacionUseCase useCase;

    @MockBean
    private EditarFundacionUseCase editarFundacionUseCase;

    @MockBean
    private DesactivarFundacionUseCase desactivarFundacionUseCase;

    @MockBean
    private ReactivarFundacionUseCase reactivarFundacionUseCase;

    private static final String ADMIN_ROLE = "ROLE_ADMIN_STYLERADAR";

    @Test
    @DisplayName("Criterio 1: Dado datos completos y certificado, cuando registro la fundación, entonces queda creada como aliada y recibo 201")
    void debeRegistrarFundacionCompletaRetornando201() throws Exception {
        FundacionRequestDTO request = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("Fundación Abrigo Solidario")
                .descripcion("Recolección de prendas")
                .direccion("Calle 85 # 15-32, Chapinero")
                .barrio("Chapinero")
                .telefono("+57 601 742 8899")
                .email("contacto@abrigosolidario.org")
                .latitud(4.6712)
                .longitud(-74.0531)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR, TipoPrenda.CALZADO))
                .representanteId(24L)
                .urlCertificadoPersoneriaJuridica("https://styleradarblob.blob.core.windows.net/fundaciones/cert.pdf")
                .causasApoyadas("Niñez vulnerable y adultos mayores")
                .build();

        FundacionResponseDTO response = FundacionResponseDTO.builder()
                .id(1L)
                .nit(request.getNit())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .direccion(request.getDireccion())
                .barrio(request.getBarrio())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .categoriasAceptadas(request.getCategoriasAceptadas())
                .representanteId(request.getRepresentanteId())
                .urlCertificadoPersoneriaJuridica(request.getUrlCertificadoPersoneriaJuridica())
                .causasApoyadas(request.getCausasApoyadas())
                .activa(true)
                .fechaRegistro(Instant.now())
                .build();

        when(useCase.registrar(any(FundacionRequestDTO.class), eq(ADMIN_ROLE)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/fundaciones")
                        .header("X-User-Roles", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nit").value("900215884-1"))
                .andExpect(jsonPath("$.barrio").value("Chapinero"))
                .andExpect(jsonPath("$.nombre").value("Fundación Abrigo Solidario"))
                .andExpect(jsonPath("$.activa").value(true))
                .andExpect(jsonPath("$.urlCertificadoPersoneriaJuridica").isNotEmpty());
    }

    @Test
    @DisplayName("Criterio 2: Dado datos incompletos (sin nombre), cuando la registro, entonces recibo 400 y no se crea nada")
    void debeRetornar400CuandoDatosIncompletos() throws Exception {
        FundacionRequestDTO request = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("") // Nombre vacío
                .direccion("Calle 85 # 15-32")
                .barrio("Chapinero")
                .email("contacto@abrigosolidario.org")
                .latitud(4.6712)
                .longitud(-74.0531)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(24L)
                .build();

        mockMvc.perform(post("/api/v1/admin/fundaciones")
                        .header("X-User-Roles", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(useCase, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("Criterio 3: Dado una fundación ya registrada (nombre duplicado), cuando la registro otra vez, recibo 409")
    void debeRetornar409CuandoFundacionYaExiste() throws Exception {
        FundacionRequestDTO request = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("Fundación Existente")
                .direccion("Calle 10")
                .barrio("Chapinero")
                .email("existente@fundacion.org")
                .latitud(4.0)
                .longitud(-74.0)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(24L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf")
                .build();

        when(useCase.registrar(any(FundacionRequestDTO.class), eq(ADMIN_ROLE)))
                .thenThrow(new FundacionYaRegistradaException("Ya existe una fundación aliada registrada con el nombre: Fundación Existente"));

        mockMvc.perform(post("/api/v1/admin/fundaciones")
                        .header("X-User-Roles", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe una fundación aliada registrada con el nombre: Fundación Existente"))
                .andExpect(jsonPath("$.validationErrors.nombre").exists());
    }

    @Test
    @DisplayName("Criterio 4: Dado que no soy administrador, cuando llamo al endpoint, entonces recibo 403")
    void debeRetornar403CuandoNoEsAdministradorStyleRadar() throws Exception {
        FundacionRequestDTO request = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("Fundación Intento")
                .direccion("Calle 10")
                .barrio("Chapinero")
                .email("intento@fundacion.org")
                .latitud(4.0)
                .longitud(-74.0)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(24L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf")
                .build();

        when(useCase.registrar(any(FundacionRequestDTO.class), eq("ROLE_COMPRADOR")))
                .thenThrow(new AccesoDenegadoAlmacenException("Se requiere el rol ROLE_ADMIN_STYLERADAR para gestionar fundaciones aliadas"));

        mockMvc.perform(post("/api/v1/admin/fundaciones")
                        .header("X-User-Roles", "ROLE_COMPRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Se requiere el rol ROLE_ADMIN_STYLERADAR para gestionar fundaciones aliadas"));
    }

    @Test
    @DisplayName("Debe listar todas las fundaciones, obtener por ID y subir certificado PDF retornando 200 / 201")
    void debeListarObtenerPorIdYSubirCertificado() throws Exception {
        FundacionResponseDTO dto = FundacionResponseDTO.builder()
                .id(1L).nit("900215884-1").nombre("Fundación Semillas").barrio("Chapinero").activa(true).build();
        when(useCase.listarTodas(ADMIN_ROLE)).thenReturn(List.of(dto));
        when(useCase.obtenerPorId(1L, ADMIN_ROLE)).thenReturn(dto);
        when(useCase.subirCertificado(any(), any(), anyLong(), any(), eq(ADMIN_ROLE)))
                .thenReturn(new DocumentoSubidoResponseDTO("https://blob/cert.pdf", "cert.pdf", "application/pdf", 120L, Instant.now()));

        mockMvc.perform(get("/api/v1/admin/fundaciones")
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/fundaciones/1")
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nit").value("900215884-1"));

        MockMultipartFile certFile = new MockMultipartFile(
                "archivo", "cert.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4".getBytes());
        mockMvc.perform(multipart("/api/v1/admin/fundaciones/certificados")
                        .file(certFile)
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://blob/cert.pdf"));
    }

    @Test
    @DisplayName("SR-RF-64 API C1: Dado una fundación existente y datos válidos, cuando la edito, entonces retorna 200 y datos actualizados")
    void debeEditarFundacionRetornando200() throws Exception {
        FundacionRequestDTO request = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("Fundación Manos Unidas Internacional")
                .descripcion("Labor social expandida")
                .direccion("Carrera 15 # 85-30, Bogotá")
                .barrio("Chapinero")
                .telefono("+57 311 555 6677")
                .email("info@manosunidas.org")
                .latitud(4.6710)
                .longitud(-74.0550)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR, TipoPrenda.CALZADO))
                .representanteId(25L)
                .horarioAtencion("Lunes a Sábado 07:00 - 18:00")
                .politicasDonacion("Prendas limpias")
                .build();

        FundacionResponseDTO response = FundacionResponseDTO.builder()
                .id(1L)
                .nit(request.getNit())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .direccion(request.getDireccion())
                .barrio(request.getBarrio())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .categoriasAceptadas(request.getCategoriasAceptadas())
                .representanteId(request.getRepresentanteId())
                .horarioAtencion(request.getHorarioAtencion())
                .politicasDonacion(request.getPoliticasDonacion())
                .activa(true)
                .fechaRegistro(Instant.now())
                .build();

        when(editarFundacionUseCase.editar(eq(1L), any(FundacionRequestDTO.class), eq(ADMIN_ROLE)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/fundaciones/1")
                        .header("X-User-Roles", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Fundación Manos Unidas Internacional"))
                .andExpect(jsonPath("$.telefono").value("+57 311 555 6677"))
                .andExpect(jsonPath("$.horarioAtencion").value("Lunes a Sábado 07:00 - 18:00"))
                .andExpect(jsonPath("$.politicasDonacion").value("Prendas limpias"))
                .andExpect(jsonPath("$.representanteId").value(25));
    }

    @Test
    @DisplayName("SR-RF-64 API C2: Dado una fundación inexistente, cuando la edito, entonces recibo 404")
    void debeRetornar404CuandoFundacionAEditarNoExiste() throws Exception {
        FundacionRequestDTO request = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("Fundación Desconocida")
                .direccion("Calle 1 # 2-3")
                .barrio("Chapinero")
                .email("noexiste@fundacion.org")
                .latitud(4.0)
                .longitud(-74.0)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(99L)
                .build();

        when(editarFundacionUseCase.editar(eq(999L), any(FundacionRequestDTO.class), eq(ADMIN_ROLE)))
                .thenThrow(new FundacionNoEncontradaException("No existe una fundación aliada con ID: 999"));

        mockMvc.perform(put("/api/v1/admin/fundaciones/999")
                        .header("X-User-Roles", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No existe una fundación aliada con ID: 999"));
    }

    @Test
    @DisplayName("SR-RF-64 API C3: Dado datos inválidos (nombre vacío), cuando la edito, entonces recibo 400 y no cambia nada")
    void debeRetornar400CuandoDatosDeEdicionSonInvalidos() throws Exception {
        FundacionRequestDTO requestInvalido = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("")
                .direccion("Calle 10")
                .barrio("Chapinero")
                .email("correo@valido.org")
                .latitud(4.0)
                .longitud(-74.0)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(10L)
                .build();

        mockMvc.perform(put("/api/v1/admin/fundaciones/1")
                        .header("X-User-Roles", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(editarFundacionUseCase, never()).editar(any(), any(), any());
    }

    @Test
    @DisplayName("SR-RF-64 API C4: Dado que no soy administrador, cuando llamo al endpoint de edición, entonces recibo 403")
    void debeRetornar403CuandoNoEsAdministradorAlEditar() throws Exception {
        FundacionRequestDTO request = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("Fundación Manos")
                .direccion("Calle 10")
                .barrio("Chapinero")
                .email("correo@valido.org")
                .latitud(4.0)
                .longitud(-74.0)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(10L)
                .build();

        when(editarFundacionUseCase.editar(eq(1L), any(FundacionRequestDTO.class), eq("ROLE_COMPRADOR")))
                .thenThrow(new AccesoDenegadoAlmacenException("Se requiere el rol ROLE_ADMIN_STYLERADAR para gestionar fundaciones aliadas"));

        mockMvc.perform(put("/api/v1/admin/fundaciones/1")
                        .header("X-User-Roles", "ROLE_COMPRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("SR-RF-64 API Negativo: Dado conflicto de duplicidad al editar, recibe 409 Conflict")
    void debeRetornar409CuandoHayConflictoAlEditar() throws Exception {
        FundacionRequestDTO request = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("Fundación Duplicada")
                .direccion("Calle 10")
                .barrio("Chapinero")
                .email("correo@valido.org")
                .latitud(4.0)
                .longitud(-74.0)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(10L)
                .build();

        when(editarFundacionUseCase.editar(eq(1L), any(FundacionRequestDTO.class), eq(ADMIN_ROLE)))
                .thenThrow(new FundacionYaRegistradaException("Ya existe otra fundación aliada registrada con el nombre: Fundación Duplicada"));

        mockMvc.perform(put("/api/v1/admin/fundaciones/1")
                        .header("X-User-Roles", ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe otra fundación aliada registrada con el nombre: Fundación Duplicada"));
    }

    @Test
    @DisplayName("SR-RF-65 API C1 y C2: Dado una fundación activa, cuando la desactivo y luego la reactivo, retorna 200")
    void debeDesactivarYReactivarFundacionRetornando200() throws Exception {
        FundacionResponseDTO responseInactiva = FundacionResponseDTO.builder()
                .id(3L)
                .nombre("Fundación Receptora")
                .activa(false)
                .build();
        FundacionResponseDTO responseActiva = FundacionResponseDTO.builder()
                .id(3L)
                .nombre("Fundación Receptora")
                .activa(true)
                .build();

        when(desactivarFundacionUseCase.desactivar(eq(3L), any(), eq(ADMIN_ROLE)))
                .thenReturn(responseInactiva);
        when(reactivarFundacionUseCase.reactivar(eq(3L), any(), eq(ADMIN_ROLE)))
                .thenReturn(responseActiva)
                .thenThrow(new FundacionYaActivaException("La fundación aliada ya se encuentra activa"));

        mockMvc.perform(patch("/api/v1/admin/fundaciones/3/desactivar")
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.activa").value(false))
                .andExpect(jsonPath("$.nombre").value("Fundación Receptora"));

        mockMvc.perform(patch("/api/v1/admin/fundaciones/3/activar")
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.activa").value(true));

        mockMvc.perform(patch("/api/v1/admin/fundaciones/3/activar")
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("La fundación aliada ya se encuentra activa"));
    }

    @Test
    @DisplayName("SR-RF-65 API C3: Dado una fundación ya inactiva, cuando la desactivo, recibo 409 Conflict")
    void debeRetornar409CuandoFundacionYaEstaDesactivada() throws Exception {
        when(desactivarFundacionUseCase.desactivar(eq(3L), any(), eq(ADMIN_ROLE)))
                .thenThrow(new FundacionYaDesactivadaException("La fundación aliada ya se encuentra desactivada"));

        mockMvc.perform(patch("/api/v1/admin/fundaciones/3/desactivar")
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("La fundación aliada ya se encuentra desactivada"));
    }

    @Test
    @DisplayName("SR-RF-65 API C4: Dado una fundación inexistente, cuando la desactivo, recibo 404 Not Found")
    void debeRetornar404CuandoFundacionADesactivarNoExiste() throws Exception {
        when(desactivarFundacionUseCase.desactivar(eq(999L), any(), eq(ADMIN_ROLE)))
                .thenThrow(new FundacionNoEncontradaException("No existe una fundación aliada con ID: 999"));

        mockMvc.perform(patch("/api/v1/admin/fundaciones/999/desactivar")
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No existe una fundación aliada con ID: 999"));
    }

    @Test
    @DisplayName("SR-RF-65 API C5: Dado que no soy administrador, cuando llamo a desactivar, recibo 403 Forbidden")
    void debeRetornar403CuandoNoEsAdministradorAlDesactivar() throws Exception {
        when(desactivarFundacionUseCase.desactivar(eq(3L), any(), eq("ROLE_COMPRADOR")))
                .thenThrow(new AccesoDenegadoAlmacenException("Se requiere el rol ROLE_ADMIN_STYLERADAR para gestionar fundaciones aliadas"));

        mockMvc.perform(patch("/api/v1/admin/fundaciones/3/desactivar")
                        .header("X-User-Roles", "ROLE_COMPRADOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("SR-RF-43: GET /api/v1/fundaciones retorna fundaciones cercanas activas")
    void debeConsultarFundacionesCercanasRetornando200() throws Exception {
        FundacionResponseDTO dto = FundacionResponseDTO.builder()
                .id(1L).nombre("Fundación Cercana").activa(true).build();
        when(useCase.consultarCercanas(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/fundaciones")
                        .param("latitud", "4.6712")
                        .param("longitud", "-74.0531"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Fundación Cercana"));
    }
}
