package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarNuevaLlegadaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarUltimasUnidadesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionReactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaDecididoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaRegistradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaActivaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaDesactivadaException;
import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.exception.RutRequeridoParaAprobacionException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auditoría de Requerimientos Completos - ms-tiendas (SR-RF-59 a SR-RF-74)")
class RequerimientosCompletosAuditoriaTiendasTest {

    @Mock
    private AlmacenRepositoryPort almacenRepositoryPort;

    @Mock
    private FundacionRepositoryPort fundacionRepositoryPort;

    @Mock
    private FundacionEventPublisherPort eventPublisherPort;

    @Mock
    private AlmacenamientoBlobPort almacenamientoBlobPort;

    @Mock
    private AutorizacionAlmacenValidator autorizacionAlmacenValidator;

    @Mock
    private AutorizacionAdminStyleRadarValidator adminValidator;

    private static final String NIT = "900123456-1";
    private static final String ROLE_ADMIN = "ROLE_ADMIN_STYLERADAR";

    @Test
    @DisplayName("Prueba 1 (SR-RF-59): Validación completa de reglas de negocio para últimas unidades")
    void prueba1_SRRF59_ReglasNegocioUltimasUnidades() {
        MarcarDistintivoPrendaUseCaseImpl useCase = new MarcarDistintivoPrendaUseCaseImpl(
                almacenRepositoryPort, autorizacionAlmacenValidator);

        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .umbralUltimasUnidades(5)
                .diasNuevaLlegada(7)
                .build();
        when(almacenRepositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        // 1. Stock igual al umbral -> ULTIMAS_UNIDADES activo
        DistintivoPrendaAlmacenResponseDTO r1 = useCase.evaluarUltimasUnidades(
                NIT, 101L, new EvaluarUltimasUnidadesRequestDTO(5, true), 10L);
        assertThat(r1.ultimasUnidades()).isTrue();
        assertThat(r1.distintivoActivo()).isEqualTo("ULTIMAS_UNIDADES");

        // 2. Stock superior al umbral -> NINGUNO
        DistintivoPrendaAlmacenResponseDTO r2 = useCase.evaluarUltimasUnidades(
                NIT, 101L, new EvaluarUltimasUnidadesRequestDTO(6, true), 10L);
        assertThat(r2.ultimasUnidades()).isFalse();
        assertThat(r2.distintivoActivo()).isEqualTo("NINGUNO");

        // 3. RN-02: Agotado prevalece sobre el distintivo (stock=0 -> NINGUNO)
        DistintivoPrendaAlmacenResponseDTO r3 = useCase.evaluarUltimasUnidades(
                NIT, 101L, new EvaluarUltimasUnidadesRequestDTO(0, true), 10L);
        assertThat(r3.ultimasUnidades()).isFalse();
        assertThat(r3.distintivoActivo()).isEqualTo("NINGUNO");
    }

    @Test
    @DisplayName("Prueba 2 (SR-RF-60): Validación de ventana de días y RN-02 de prendas retiradas")
    void prueba2_SRRF60_ReglasNegocioNuevaLlegada() {
        MarcarDistintivoPrendaUseCaseImpl useCase = new MarcarDistintivoPrendaUseCaseImpl(
                almacenRepositoryPort, autorizacionAlmacenValidator);

        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .umbralUltimasUnidades(3)
                .diasNuevaLlegada(10)
                .build();
        when(almacenRepositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        Instant ahora = Instant.now();

        // 1. Prenda ingresada dentro de la ventana (hace 5 días <= 10 días)
        DistintivoPrendaAlmacenResponseDTO dentro = useCase.evaluarNuevaLlegada(
                NIT, 202L, new EvaluarNuevaLlegadaRequestDTO(ahora.minus(5, ChronoUnit.DAYS), false), 10L);
        assertThat(dentro.nuevaLlegada()).isTrue();
        assertThat(dentro.distintivoActivo()).isEqualTo("NUEVA_PRENDA");

        // 2. Prenda ingresada fuera de la ventana (hace 11 días > 10 días)
        DistintivoPrendaAlmacenResponseDTO fuera = useCase.evaluarNuevaLlegada(
                NIT, 202L, new EvaluarNuevaLlegadaRequestDTO(ahora.minus(11, ChronoUnit.DAYS), false), 10L);
        assertThat(fuera.nuevaLlegada()).isFalse();
        assertThat(fuera.distintivoActivo()).isEqualTo("NINGUNO");

        // 3. RN-02: Prenda marcada como retirada no recibe distintivo aunque fecha sea reciente
        DistintivoPrendaAlmacenResponseDTO retirada = useCase.evaluarNuevaLlegada(
                NIT, 202L, new EvaluarNuevaLlegadaRequestDTO(ahora.minus(1, ChronoUnit.DAYS), true), 10L);
        assertThat(retirada.nuevaLlegada()).isFalse();
        assertThat(retirada.distintivoActivo()).isEqualTo("NINGUNO");
    }

    @Test
    @DisplayName("Prueba 3 (SR-RF-62-A): Registro de almacén con RUT obligatorio, estado PENDIENTE y rechazo de duplicados")
    void prueba3_SRRF62A_RegistrarAlmacenValidaciones() {
        RegistrarAlmacenUseCaseImpl useCase = new RegistrarAlmacenUseCaseImpl(
                almacenRepositoryPort, almacenamientoBlobPort, autorizacionAlmacenValidator);

        RegistrarAlmacenRequestDTO req = new RegistrarAlmacenRequestDTO(
                NIT, "Moda Urbana", "Ropa exclusiva", "+57 300 123 4567",
                "contacto@modaurbana.com", 4.65, -74.05,
                Set.of(CategoriaAlmacen.CASUAL),
                "https://styleradarblob.blob.core.windows.net/documentos-almacenes/rut/15/doc.pdf"
        );

        when(almacenRepositoryPort.existsByNit(NIT)).thenReturn(false);
        when(almacenRepositoryPort.existeAlmacenParaUsuario(15L)).thenReturn(false);
        when(almacenRepositoryPort.save(any(Almacen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificacionAlmacenResponseDTO res = useCase.registrarAlmacen(req, 15L, "ROLE_ADMIN_ALMACEN");

        assertThat(res.nit()).isEqualTo(NIT);
        assertThat(res.estadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.PENDIENTE);
        verify(almacenRepositoryPort).asociarAdministrador(NIT, 15L);

        // Rechazo si el NIT ya existe
        when(almacenRepositoryPort.existsByNit(NIT)).thenReturn(true);
        assertThatThrownBy(() -> useCase.registrarAlmacen(req, 16L, "ROLE_ADMIN_ALMACEN"))
                .isInstanceOf(AlmacenYaRegistradoException.class);
    }

    @Test
    @DisplayName("Prueba 4 (SR-RF-62): Aprobación rechaza falta de RUT y controla transiciones previas")
    void prueba4_SRRF62_VerificarAlmacenReglasDecision() {
        VerificarAlmacenUseCaseImpl useCase = new VerificarAlmacenUseCaseImpl(
                almacenRepositoryPort, adminValidator);

        Almacen sinRut = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Sin Rut")
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .urlRutPdf(null)
                .build();

        when(almacenRepositoryPort.findByNit(NIT)).thenReturn(Optional.of(sinRut));

        DecisionVerificacionRequestDTO decisionAprobar = new DecisionVerificacionRequestDTO();
        decisionAprobar.setDecision(DecisionVerificacionAlmacen.APROBADO);
        decisionAprobar.setMotivo("Verificación documental exitosa");

        // RN-03: No se puede aprobar sin RUT
        assertThatThrownBy(() -> useCase.decidir(NIT, decisionAprobar, 1L, ROLE_ADMIN))
                .isInstanceOf(RutRequeridoParaAprobacionException.class);

        // Almacén ya aprobado no puede volver a decidirse sin reapertura
        Almacen yaAprobado = Almacen.builder()
                .nit(NIT)
                .estadoVerificacion(EstadoVerificacionAlmacen.APROBADO)
                .urlRutPdf("https://blob/rut.pdf")
                .build();
        when(almacenRepositoryPort.findByNit(NIT)).thenReturn(Optional.of(yaAprobado));

        assertThatThrownBy(() -> useCase.decidir(NIT, decisionAprobar, 1L, ROLE_ADMIN))
                .isInstanceOf(AlmacenYaDecididoException.class);
    }

    @Test
    @DisplayName("Prueba 5 (SR-RF-65): Desactivación y Reactivación de fundaciones con publicación de eventos")
    void prueba5_SRRF65_DesactivarYReactivarFundacionEventos() {
        DesactivarFundacionUseCaseImpl desactivarUseCase = new DesactivarFundacionUseCaseImpl(
                fundacionRepositoryPort, eventPublisherPort, adminValidator);
        ReactivarFundacionUseCaseImpl reactivarUseCase = new ReactivarFundacionUseCaseImpl(
                fundacionRepositoryPort, eventPublisherPort, adminValidator);

        Fundacion fundacionActiva = Fundacion.builder()
                .id(10L)
                .nombre("Fundación Manos Verdes")
                .nit("900888777-1")
                .representanteId(25L)
                .activa(true)
                .build();

        when(fundacionRepositoryPort.findById(10L)).thenReturn(Optional.of(fundacionActiva));
        when(fundacionRepositoryPort.save(any(Fundacion.class))).thenAnswer(i -> i.getArgument(0));

        // 1. Desactivar fundación activa
        FundacionResponseDTO respDesactivada = desactivarUseCase.desactivar(10L, "Mantenimiento anual", ROLE_ADMIN);
        assertThat(respDesactivada.isActiva()).isFalse();
        verify(eventPublisherPort).publicarFundacionDesactivada(any(FundacionDesactivadaEvent.class));

        // 2. Intentar desactivar una fundación ya inactiva -> FundacionYaDesactivadaException (409)
        assertThatThrownBy(() -> desactivarUseCase.desactivar(10L, "Otro motivo", ROLE_ADMIN))
                .isInstanceOf(FundacionYaDesactivadaException.class);

        // 3. Reactivar la fundación inactiva
        FundacionResponseDTO respReactivada = reactivarUseCase.reactivar(10L, "Reapertura de operaciones", ROLE_ADMIN);
        assertThat(respReactivada.isActiva()).isTrue();
        verify(eventPublisherPort).publicarFundacionReactivada(any(FundacionReactivadaEvent.class));
    }

    @Test
    @DisplayName("Prueba 6 (SR-RF-74): Límite estricto de 6 fotografías y actualización de portada al eliminar")
    void prueba6_SRRF74_GaleriaFotografiasLimiteYPortada() {
        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Boutique")
                .build();

        // Agregar 6 fotos consecutivas con IDs simulados
        for (int i = 1; i <= 6; i++) {
            FotografiaTienda f = almacen.agregarFotografia("https://blob/foto" + i + ".jpg", Instant.now());
            f.setId((long) i);
        }

        assertThat(almacen.getFotos()).hasSize(6);
        // RN-02: La primera foto es la portada
        assertThat(almacen.getFotoPortadaUrl()).isEqualTo("https://blob/foto1.jpg");

        // RN-01: Intentar agregar la foto número 7 debe lanzar LimiteFotografiasSuperadoException
        assertThatThrownBy(() -> almacen.agregarFotografia("https://blob/foto7.jpg", Instant.now()))
                .isInstanceOf(LimiteFotografiasSuperadoException.class)
                .hasMessageContaining("máximo permitido de 6 fotografías");

        // RN-03: Al eliminar la foto de portada (foto 1), la siguiente (foto 2) pasa a ser la nueva portada
        Long idFoto1 = almacen.getFotos().get(0).getId();
        almacen.eliminarFotografia(idFoto1);

        assertThat(almacen.getFotos()).hasSize(5);
        assertThat(almacen.getFotoPortadaUrl()).isEqualTo("https://blob/foto2.jpg");
        assertThat(almacen.getFotos().get(0).isPortada()).isTrue();
    }
}
