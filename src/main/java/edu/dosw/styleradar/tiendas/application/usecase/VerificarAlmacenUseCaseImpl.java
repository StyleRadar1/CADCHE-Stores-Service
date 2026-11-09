package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.VerificacionConsultaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.in.VerificarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificarAlmacenUseCaseImpl implements VerificarAlmacenUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;

    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO decidir(
            String nit, DecisionVerificacionRequestDTO request, Long adminId, String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);

        if (request == null || request.getDecision() == null || !request.isMotivoValido()) {
            throw new ReglaDeNegocioException("La decisión de verificación o el motivo es inválido");
        }

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        log.info("Decidiendo verificación para almacén NIT={} decisión={} adminId={}",
                nit, request.getDecision(), adminId);

        almacen.decidirVerificacion(request.getDecision(), request.getMotivo(), adminId, Instant.now());
        Almacen guardado = repositoryPort.save(almacen);

        log.info("Almacén NIT={} decidido exitosamente como {}", nit, guardado.getEstadoVerificacion());
        return toResponseDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificacionAlmacenResponseDTO consultarUno(String nit, String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        return toResponseDTO(almacen);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<VerificacionAlmacenResponseDTO> consultar(
            VerificacionConsultaRequestDTO request, String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);

        int pageNum = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        if (pageNum < 0 || size < 1 || size > 100) {
            throw new ReglaDeNegocioException("Paginación de verificación inválida");
        }

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("nombreComercial").ascending().and(Sort.by("nit").ascending()));

        Page<Almacen> page = request.getEstado() != null
                ? repositoryPort.findByEstadoVerificacion(request.getEstado(), pageable)
                : repositoryPort.findAllVerificacion(pageable);

        List<VerificacionAlmacenResponseDTO> content = page.getContent().stream()
                .map(this::toResponseDTO)
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

    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO solicitarRevision(String nit, Long adminId, String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        log.info("Solicitando revisión de almacén NIT={} por adminId={}", nit, adminId);
        almacen.solicitarRevision();
        Almacen guardado = repositoryPort.save(almacen);

        return toResponseDTO(guardado);
    }

    private VerificacionAlmacenResponseDTO toResponseDTO(Almacen a) {
        return new VerificacionAlmacenResponseDTO(
                a.getNit(),
                a.getNombreComercial(),
                a.getDescripcion(),
                a.getTelefono(),
                a.getCorreoContacto(),
                a.getLatitud(),
                a.getLongitud(),
                a.getReputacion(),
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
