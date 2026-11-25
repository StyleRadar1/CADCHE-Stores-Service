package edu.dosw.styleradar.tiendas.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.dosw.styleradar.tiendas.application.usecase.AsociarAdministradorAlmacenUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.ConfigurarPeriodoNuevaLlegadaUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.ConfigurarUmbralUltimasUnidadesUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.ConsultarPeriodoNuevaLlegadaUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.ConsultarUmbralUltimasUnidadesUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.DesactivarFundacionUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.EditarFundacionUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.MarcarDistintivoPrendaUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.ReactivarFundacionUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.RegistrarAlmacenUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.RegistrarFotografiaTiendaUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.RegistrarFundacionUseCaseImpl;
import edu.dosw.styleradar.tiendas.application.usecase.VerificarAlmacenUseCaseImpl;
import edu.dosw.styleradar.tiendas.domain.port.in.AsociarAdministradorAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConfigurarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarPeriodoNuevaLlegadaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarUmbralUltimasUnidadesUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.DesactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.EditarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.MarcarDistintivoPrendaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ReactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFotografiaTiendaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.VerificarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import edu.dosw.styleradar.tiendas.domain.validator.PeriodoNuevaLlegadaValidator;
import edu.dosw.styleradar.tiendas.domain.validator.UmbralInventarioValidator;

/**
 * Inversión de control: registra los validadores del dominio y cada caso de uso ({@code <Nombre>UseCaseImpl})
 * contra su puerto de entrada, inyectando los puertos de salida. Ni el dominio ni los casos de uso llevan
 * estereotipos de Spring (guía de arquitectura hexagonal StyleRadar, reglas de oro 1 y 4).
 */
@Configuration
public class BeanConfig {

    @Bean
    public AutorizacionAdminStyleRadarValidator autorizacionAdminStyleRadarValidator() {
        return new AutorizacionAdminStyleRadarValidator();
    }

    @Bean
    public AutorizacionAlmacenValidator autorizacionAlmacenValidator() {
        return new AutorizacionAlmacenValidator();
    }

    @Bean
    public PeriodoNuevaLlegadaValidator periodoNuevaLlegadaValidator() {
        return new PeriodoNuevaLlegadaValidator();
    }

    @Bean
    public UmbralInventarioValidator umbralInventarioValidator() {
        return new UmbralInventarioValidator();
    }

    @Bean
    public AsociarAdministradorAlmacenUseCase asociarAdministradorAlmacenUseCase(
            AlmacenRepositoryPort repositoryPort,
            AutorizacionAdminStyleRadarValidator adminValidator,
            AutorizacionAlmacenValidator almacenValidator) {
        return new AsociarAdministradorAlmacenUseCaseImpl(repositoryPort, adminValidator, almacenValidator);
    }

    @Bean
    public ConfigurarPeriodoNuevaLlegadaUseCase configurarPeriodoNuevaLlegadaUseCase(
            AlmacenRepositoryPort repositoryPort,
            PeriodoNuevaLlegadaValidator periodoValidator,
            AutorizacionAlmacenValidator autorizacionValidator) {
        return new ConfigurarPeriodoNuevaLlegadaUseCaseImpl(repositoryPort, periodoValidator, autorizacionValidator);
    }

    @Bean
    public ConfigurarUmbralUltimasUnidadesUseCase configurarUmbralUltimasUnidadesUseCase(
            AlmacenRepositoryPort repositoryPort,
            UmbralInventarioValidator umbralValidator,
            PeriodoNuevaLlegadaValidator periodoValidator,
            AutorizacionAlmacenValidator autorizacionValidator) {
        return new ConfigurarUmbralUltimasUnidadesUseCaseImpl(repositoryPort, umbralValidator, periodoValidator, autorizacionValidator);
    }

    @Bean
    public ConsultarPeriodoNuevaLlegadaUseCase consultarPeriodoNuevaLlegadaUseCase(
            AlmacenRepositoryPort repositoryPort) {
        return new ConsultarPeriodoNuevaLlegadaUseCaseImpl(repositoryPort);
    }

    @Bean
    public ConsultarUmbralUltimasUnidadesUseCase consultarUmbralUltimasUnidadesUseCase(
            AlmacenRepositoryPort repositoryPort) {
        return new ConsultarUmbralUltimasUnidadesUseCaseImpl(repositoryPort);
    }

    @Bean
    public DesactivarFundacionUseCase desactivarFundacionUseCase(
            FundacionRepositoryPort repositoryPort,
            FundacionEventPublisherPort eventPublisherPort,
            AutorizacionAdminStyleRadarValidator adminValidator) {
        return new DesactivarFundacionUseCaseImpl(repositoryPort, eventPublisherPort, adminValidator);
    }

    @Bean
    public EditarFundacionUseCase editarFundacionUseCase(
            FundacionRepositoryPort repositoryPort,
            AutorizacionAdminStyleRadarValidator adminValidator) {
        return new EditarFundacionUseCaseImpl(repositoryPort, adminValidator);
    }

    @Bean
    public MarcarDistintivoPrendaUseCase marcarDistintivoPrendaUseCase(
            AlmacenRepositoryPort repositoryPort,
            AutorizacionAlmacenValidator autorizacionValidator) {
        return new MarcarDistintivoPrendaUseCaseImpl(repositoryPort, autorizacionValidator);
    }

    @Bean
    public ReactivarFundacionUseCase reactivarFundacionUseCase(
            FundacionRepositoryPort repositoryPort,
            FundacionEventPublisherPort eventPublisherPort,
            AutorizacionAdminStyleRadarValidator adminValidator) {
        return new ReactivarFundacionUseCaseImpl(repositoryPort, eventPublisherPort, adminValidator);
    }

    @Bean
    public RegistrarAlmacenUseCase registrarAlmacenUseCase(
            AlmacenRepositoryPort repositoryPort,
            AlmacenamientoBlobPort almacenamientoBlobPort,
            AutorizacionAlmacenValidator autorizacionValidator,
            edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort eventPublisher) {
        return new RegistrarAlmacenUseCaseImpl(repositoryPort, almacenamientoBlobPort, autorizacionValidator, eventPublisher);
    }

    @Bean
    public RegistrarFotografiaTiendaUseCase registrarFotografiaTiendaUseCase(
            AlmacenRepositoryPort repositoryPort,
            AlmacenamientoBlobPort almacenamientoBlobPort,
            AutorizacionAlmacenValidator autorizacionValidator) {
        return new RegistrarFotografiaTiendaUseCaseImpl(repositoryPort, almacenamientoBlobPort, autorizacionValidator);
    }

    @Bean
    public RegistrarFundacionUseCase registrarFundacionUseCase(
            FundacionRepositoryPort repositoryPort,
            AutorizacionAdminStyleRadarValidator adminValidator,
            AlmacenamientoBlobPort almacenamientoBlobPort,
            FundacionEventPublisherPort eventPublisherPort) {
        return new RegistrarFundacionUseCaseImpl(repositoryPort, adminValidator, almacenamientoBlobPort, eventPublisherPort);
    }

    @Bean
    public VerificarAlmacenUseCase verificarAlmacenUseCase(
            AlmacenRepositoryPort repositoryPort,
            AutorizacionAdminStyleRadarValidator adminValidator,
            edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort eventPublisher) {
        return new VerificarAlmacenUseCaseImpl(repositoryPort, adminValidator, eventPublisher);
    }

    @Bean
    public edu.dosw.styleradar.tiendas.domain.port.in.EliminarAlmacenUseCase eliminarAlmacenUseCase(
            AlmacenRepositoryPort repositoryPort,
            AlmacenamientoBlobPort almacenamientoBlobPort,
            edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort eventPublisher,
            AutorizacionAdminStyleRadarValidator adminValidator) {
        return new edu.dosw.styleradar.tiendas.application.usecase.EliminarAlmacenUseCaseImpl(
                repositoryPort, almacenamientoBlobPort, eventPublisher, adminValidator);
    }

    @Bean
    public edu.dosw.styleradar.tiendas.domain.port.in.EliminarFundacionUseCase eliminarFundacionUseCase(
            FundacionRepositoryPort repositoryPort,
            AlmacenamientoBlobPort almacenamientoBlobPort,
            edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort eventPublisher,
            AutorizacionAdminStyleRadarValidator adminValidator) {
        return new edu.dosw.styleradar.tiendas.application.usecase.EliminarFundacionUseCaseImpl(
                repositoryPort, almacenamientoBlobPort, eventPublisher, adminValidator);
    }
}
