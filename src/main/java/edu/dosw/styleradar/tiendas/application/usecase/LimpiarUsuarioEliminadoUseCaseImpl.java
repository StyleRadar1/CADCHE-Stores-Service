package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.ResultadoLimpiezaUsuario;
import edu.dosw.styleradar.tiendas.domain.port.in.LimpiarUsuarioEliminadoUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Limpieza idempotente al eliminarse una cuenta en ms-usuarios. Repetir el evento no cambia nada: los vínculos ya no
 * existen y la fundación ya está inactiva. La fundación se desactiva (no se borra) porque conserva historial de
 * donaciones; un administrador de StyleRadar puede asignarle otro representante y reactivarla.
 */
@RequiredArgsConstructor
@Slf4j
public class LimpiarUsuarioEliminadoUseCaseImpl implements LimpiarUsuarioEliminadoUseCase {

    public static final String MOTIVO = "La cuenta del representante fue eliminada en StyleRadar";

    private final AlmacenRepositoryPort almacenes;
    private final FundacionRepositoryPort fundaciones;
    private final FundacionEventPublisherPort eventosFundacion;

    @Override
    @Transactional
    public ResultadoLimpiezaUsuario limpiar(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            log.warn("Evento de cuenta eliminada sin userId válido ({}); se ignora", usuarioId);
            return new ResultadoLimpiezaUsuario(0, null);
        }
        long asociaciones = almacenes.eliminarAsociacionesDeUsuario(usuarioId);
        Long fundacionId = fundaciones.findByRepresentanteId(usuarioId)
                .filter(Fundacion::isActiva)
                .map(this::desactivar)
                .orElse(null);
        log.info("Cuenta {} eliminada: {} vínculo(s) de administrador de almacén borrados; fundación desactivada: {}",
                usuarioId, asociaciones, fundacionId);
        return new ResultadoLimpiezaUsuario(asociaciones, fundacionId);
    }

    private Long desactivar(Fundacion fundacion) {
        fundacion.desactivar();
        Fundacion guardada = fundaciones.save(fundacion);
        Instant ahora = Instant.now();
        eventosFundacion.publicarFundacionDesactivada(FundacionDesactivadaEvent.builder()
                .fundacionId(guardada.getId())
                .nombreFundacion(guardada.getNombre())
                .representanteId(guardada.getRepresentanteId())
                .fechaDesactivacion(ahora)
                .motivo(MOTIVO)
                .build());
        eventosFundacion.publicarEstadoCambiado(FundacionEstadoCambiadoEvent.desactivada(guardada.getId(), ahora));
        return guardada.getId();
    }
}
