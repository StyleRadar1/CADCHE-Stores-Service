package edu.dosw.styleradar.tiendas.application.usecase;

import java.util.HashSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaRegistradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.PeriodoNuevaLlegada;
import edu.dosw.styleradar.tiendas.domain.model.UmbralUltimasUnidades;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrarAlmacenUseCaseImpl implements RegistrarAlmacenUseCase {

    private final AlmacenRepositoryPort repositoryPort;

    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO registrarAlmacen(RegistrarAlmacenRequestDTO request, Long usuarioId) {
        if (request == null) {
            throw new ReglaDeNegocioException("La solicitud de registro del almacén no puede ser nula");
        }

        String nitNormalizado = request.getNit() != null ? request.getNit().strip() : null;

        Almacen almacen = Almacen.builder()
                .nit(nitNormalizado)
                .nombreComercial(request.getNombreComercial() != null ? request.getNombreComercial().strip() : null)
                .descripcion(request.getDescripcion() != null ? request.getDescripcion().strip() : null)
                .telefono(request.getTelefono() != null ? request.getTelefono().strip() : null)
                .correoContacto(request.getCorreoContacto() != null ? request.getCorreoContacto().strip().toLowerCase() : null)
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .reputacion(0.0)
                .categorias(request.getCategorias() != null ? new HashSet<>(request.getCategorias()) : new HashSet<>())
                .umbralUltimasUnidades(UmbralUltimasUnidades.VALOR_POR_DEFECTO)
                .diasNuevaLlegada(PeriodoNuevaLlegada.VALOR_POR_DEFECTO)
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .urlRutPdf(request.getUrlRutPdf() != null ? request.getUrlRutPdf().strip() : null)
                .build();

        almacen.validarDatosObligatoriosRegistro();

        if (repositoryPort.existsByNit(nitNormalizado)) {
            throw new AlmacenYaRegistradoException(
                    "El almacén con NIT " + nitNormalizado + " ya se encuentra registrado en la plataforma");
        }

        log.info("Registrando solicitud de vinculación de almacén aliado con NIT={} y nombre='{}'",
                almacen.getNit(), almacen.getNombreComercial());

        Almacen guardado = repositoryPort.save(almacen);

        if (usuarioId != null) {
            repositoryPort.asociarAdministrador(guardado.getNit(), usuarioId);
            log.info("Asociado usuarioId={} como administrador del almacén NIT={}", usuarioId, guardado.getNit());
        }

        return toVerificacionResponseDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public AlmacenResponseDTO obtenerPorNit(String nit) {
        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException(
                        "No se encontró ningún almacén registrado con el NIT: " + nit));
        return toAlmacenResponseDTO(almacen);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AlmacenResponseDTO> consultarAlmacenes(ConsultaAlmacenRequestDTO request) {
        int pageNum = request != null ? request.getPage() : 0;
        int size = request != null ? request.getSize() : 20;

        if (pageNum < 0 || size < 1 || size > 100) {
            throw new ReglaDeNegocioException("Parámetros de paginación inválidos para la consulta de almacenes");
        }

        Pageable pageable = PageRequest.of(
                pageNum,
                size,
                Sort.by("nombreComercial").ascending().and(Sort.by("nit").ascending()));

        Page<Almacen> page = repositoryPort.findByEstadoVerificacion(EstadoVerificacionAlmacen.APROBADO, pageable);

        List<AlmacenResponseDTO> content = page.getContent().stream()
                .filter(a -> request == null
                        || request.getCategoria() == null
                        || (a.getCategorias() != null && a.getCategorias().contains(request.getCategoria())))
                .map(this::toAlmacenResponseDTO)
                .toList();

        return new PageResponseDTO<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private AlmacenResponseDTO toAlmacenResponseDTO(Almacen a) {
        return new AlmacenResponseDTO(
                a.getNit(),
                a.getNombreComercial(),
                a.getDescripcion(),
                a.getLatitud(),
                a.getLongitud(),
                a.getReputacion() != null ? a.getReputacion() : 0.0,
                a.getCategorias()
        );
    }

    private VerificacionAlmacenResponseDTO toVerificacionResponseDTO(Almacen a) {
        return new VerificacionAlmacenResponseDTO(
                a.getNit(),
                a.getNombreComercial(),
                a.getDescripcion(),
                a.getTelefono(),
                a.getCorreoContacto(),
                a.getLatitud(),
                a.getLongitud(),
                a.getReputacion() != null ? a.getReputacion() : 0.0,
                a.getCategorias(),
                a.getEstadoVerificacion(),
                a.getEstadoVerificacion() == null,
                a.getFechaDecisionVerificacion(),
                a.getAdministradorDecisionId(),
                a.getMotivoVerificacion(),
                a.getUrlRutPdf()
        );
    }
}

