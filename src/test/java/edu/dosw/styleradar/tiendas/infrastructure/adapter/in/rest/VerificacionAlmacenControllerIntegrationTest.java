package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaDecididoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaPendienteException;
import edu.dosw.styleradar.tiendas.domain.exception.RutRequeridoParaAprobacionException;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.in.VerificarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.exception.GlobalExceptionHandler;
import edu.dosw.styleradar.tiendas.infrastructure.security.JwtSecurityValidator;

@WebMvcTest(VerificacionAlmacenController.class)
@Import({GlobalExceptionHandler.class, JwtSecurityValidator.class})
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración de API REST - VerificacionAlmacenController (SR-RF-62)")
class VerificacionAlmacenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VerificarAlmacenUseCase useCase;

    private static final String NIT = "900123456-1";
    private static final String ADMIN_ROLE = "ROLE_STYLERADAR_ADMIN";

    @Test
    @DisplayName("Criterio 1: Dado un almacén pendiente con NIT y RUT, cuando lo apruebo, entonces responde HTTP 200 y queda registrado como APPROVED")
    void debeAprobarAlmacenPendienteConRutRetornando200() throws Exception {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.APPROVED);
        request.setMotivo("NIT y personería jurídica verificados correctamente");

        VerificacionAlmacenResponseDTO response = new VerificacionAlmacenResponseDTO(
                NIT,
                "Atelier Urbano Chapinero",
                "Moda sostenible",
                "+57 601 312 4455",
                "contacto@atelierurbano.co",
                4.6533,
                -74.0602,
                4.8,
                Set.of(CategoriaAlmacen.CASUAL),
                EstadoVerificacionAlmacen.APPROVED,
                false,
                Instant.now(),
                1L,
                request.getMotivo(),
                "https://styleradarblob.blob.core.windows.net/documentos-rut/" + NIT + "/rut.pdf"
        );

        when(useCase.decidir(eq(NIT), any(DecisionVerificacionRequestDTO.class), eq(1L), eq(ADMIN_ROLE)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nit").value(NIT))
                .andExpect(jsonPath("$.verificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.verificationReason").value("NIT y personería jurídica verificados correctamente"))
                .andExpect(jsonPath("$.rutPdfUrl").value("https://styleradarblob.blob.core.windows.net/documentos-rut/" + NIT + "/rut.pdf"));
    }

    @Test
    @DisplayName("Criterio 1b: Dado un almacén pendiente con NIT y RUT, cuando lo rechazo con motivo, responde HTTP 200 con estado REJECTED")
    void debeRechazarAlmacenPendienteConMotivoRetornando200() throws Exception {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.REJECTED);
        request.setMotivo("El RUT adjunto no coincide con la razón social reportada");

        VerificacionAlmacenResponseDTO response = new VerificacionAlmacenResponseDTO(
                NIT,
                "Atelier Urbano Chapinero",
                "Moda sostenible",
                "+57 601 312 4455",
                "contacto@atelierurbano.co",
                4.6533,
                -74.0602,
                4.8,
                Set.of(CategoriaAlmacen.CASUAL),
                EstadoVerificacionAlmacen.REJECTED,
                false,
                Instant.now(),
                1L,
                request.getMotivo(),
                "https://styleradarblob.blob.core.windows.net/documentos-rut/" + NIT + "/rut.pdf"
        );

        when(useCase.decidir(eq(NIT), any(DecisionVerificacionRequestDTO.class), eq(1L), eq(ADMIN_ROLE)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nit").value(NIT))
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.verificationReason").value("El RUT adjunto no coincide con la razón social reportada"));
    }

    @Test
    @DisplayName("Criterio 2: Dado un almacén sin RUT, cuando intento aprobarlo, entonces recibo HTTP 422 (Unprocessable Entity)")
    void debeRetornar422CuandoAlmacenNoTieneRutParaAprobacion() throws Exception {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.APPROVED);
        request.setMotivo("Aprobando sin documento legal");

        when(useCase.decidir(eq(NIT), any(DecisionVerificacionRequestDTO.class), eq(1L), eq(ADMIN_ROLE)))
                .thenThrow(new RutRequeridoParaAprobacionException("No es posible aprobar el almacén con NIT " + NIT + " porque no cuenta con el documento RUT adjunto en PDF"));

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("No es posible aprobar el almacén con NIT " + NIT + " porque no cuenta con el documento RUT adjunto en PDF"));
    }

    @Test
    @DisplayName("Criterio 3: Dado un almacén ya decidido, cuando lo decido otra vez, entonces recibo HTTP 409 (Conflict)")
    void debeRetornar409CuandoAlmacenYaFueDecididoPreviamente() throws Exception {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.APPROVED);

        when(useCase.decidir(eq(NIT), any(DecisionVerificacionRequestDTO.class), eq(1L), eq(ADMIN_ROLE)))
                .thenThrow(new AlmacenYaDecididoException("El almacén con NIT " + NIT + " ya fue decidido previamente. Estado actual: APPROVED"));

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("El almacén con NIT " + NIT + " ya fue decidido previamente. Estado actual: APPROVED"));
    }

    @Test
    @DisplayName("Criterio 4: Dado que no soy administrador, cuando llamo al endpoint, entonces recibo HTTP 403 (Forbidden)")
    void debeRetornar403CuandoNoEsAdministradorStyleRadar() throws Exception {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.APPROVED);

        when(useCase.decidir(eq(NIT), any(DecisionVerificacionRequestDTO.class), eq(99L), eq("ROLE_COMPRADOR")))
                .thenThrow(new AccesoDenegadoAlmacenException("Se requiere el rol ROLE_STYLERADAR_ADMIN para verificar almacenes aliados"));

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", "ROLE_COMPRADOR")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Se requiere el rol ROLE_STYLERADAR_ADMIN para verificar almacenes aliados"));
    }

    @Test
    @DisplayName("Criterio 5: Dado un almacén inexistente, cuando lo decido, entonces recibo HTTP 404 (Not Found)")
    void debeRetornar404CuandoAlmacenNoExiste() throws Exception {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.APPROVED);

        when(useCase.decidir(eq("000000000"), any(DecisionVerificacionRequestDTO.class), eq(1L), eq(ADMIN_ROLE)))
                .thenThrow(new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: 000000000"));

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", "000000000")
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No se encontró ningún almacén registrado con el NIT: 000000000"));
    }

    @Test
    @DisplayName("Negativo y B-3.2: Debe retornar HTTP 400 cuando el rechazo no tiene motivo o el motivo tiene menos de 10 caracteres o campos no permitidos")
    void debeRetornar400CuandoRechazoCareceDeMotivoOMotivoCortoOCampoExtra() throws Exception {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.REJECTED);
        request.setMotivo("");

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        DecisionVerificacionRequestDTO requestCorto = new DecisionVerificacionRequestDTO();
        requestCorto.setDecision(DecisionVerificacionAlmacen.REJECTED);
        requestCorto.setMotivo("Corto");

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestCorto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(patch("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"campoExtra\":\"noPermitido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Campo no permitido en la decisión de verificación"));
    }

    @Test
    @DisplayName("Debe consultar almacén individual por NIT retornando HTTP 200")
    void debeConsultarAlmacenIndividualPorNit() throws Exception {
        VerificacionAlmacenResponseDTO response = new VerificacionAlmacenResponseDTO(
                NIT,
                "Atelier Urbano Chapinero",
                "Moda sostenible",
                "+57 601 312 4455",
                "contacto@atelierurbano.co",
                4.6533,
                -74.0602,
                4.8,
                Set.of(CategoriaAlmacen.CASUAL),
                EstadoVerificacionAlmacen.PENDING,
                false,
                null,
                null,
                null,
                "https://styleradarblob.blob.core.windows.net/documentos-rut/" + NIT + "/rut.pdf",
                "Usuario #10"
        );

        when(useCase.consultarUno(NIT, ADMIN_ROLE)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/stores/{nit}/verification", NIT)
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nit").value(NIT))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.representative").value("Usuario #10"))
                .andExpect(jsonPath("$.rutPdfUrl").isNotEmpty());
    }

    @Test
    @DisplayName("Debe consultar almacenes paginados retornando HTTP 200")
    void debeConsultarAlmacenesPaginados() throws Exception {
        PageResponseDTO<VerificacionAlmacenResponseDTO> pageResponse = new PageResponseDTO<>(
                List.of(), 0, 20, 0L, 0, true, true);

        when(useCase.consultar(any(), eq(ADMIN_ROLE))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/stores/verifications")
                        .header("X-User-Roles", ADMIN_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("Debe solicitar reapertura de revisión retornando HTTP 200")
    void debeSolicitarRevisionRetornando200() throws Exception {
        VerificacionAlmacenResponseDTO response = new VerificacionAlmacenResponseDTO(
                NIT,
                "Atelier Urbano Chapinero",
                "Moda sostenible",
                "+57 601 312 4455",
                "contacto@atelierurbano.co",
                4.6533,
                -74.0602,
                4.8,
                Set.of(CategoriaAlmacen.CASUAL),
                EstadoVerificacionAlmacen.PENDING,
                false,
                null,
                null,
                null,
                "https://styleradarblob.blob.core.windows.net/documentos-rut/" + NIT + "/rut.pdf"
        );

        when(useCase.solicitarRevision(NIT, 1L, ADMIN_ROLE)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/stores/{nit}/verification/review-request", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Debe retornar HTTP 422 al solicitar revisión sobre un almacén que ya está pendiente")
    void debeRetornar422CuandoAlmacenYaEstaPendiente() throws Exception {
        when(useCase.solicitarRevision(NIT, 1L, ADMIN_ROLE))
                .thenThrow(new AlmacenYaPendienteException("El almacén ya se encuentra pendiente de revisión"));

        mockMvc.perform(post("/api/v1/admin/stores/{nit}/verification/review-request", NIT)
                        .header("X-User-Roles", ADMIN_ROLE)
                        .header("X-User-Id", 1L))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("El almacén ya se encuentra pendiente de revisión"));
    }
}
