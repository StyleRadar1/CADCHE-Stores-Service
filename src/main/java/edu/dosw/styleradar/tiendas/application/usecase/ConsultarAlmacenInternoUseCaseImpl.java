package edu.dosw.styleradar.tiendas.application.usecase;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarAlmacenInternoUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

public class ConsultarAlmacenInternoUseCaseImpl implements ConsultarAlmacenInternoUseCase {

    private final AlmacenRepositoryPort repositoryPort;

    public ConsultarAlmacenInternoUseCaseImpl(AlmacenRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public AlmacenResponseDTO porAdministrador(Long ownerUserId) {
        if (ownerUserId == null) {
            throw new ReglaDeNegocioException("El parámetro ownerUserId es obligatorio");
        }
        Almacen almacen = repositoryPort.findByAdministradorUsuarioId(ownerUserId)
                .orElseThrow(() -> new AlmacenNoEncontradoException(
                        "No se encontró ningún almacén asociado al usuario: " + ownerUserId));
        return new AlmacenResponseDTO(
                almacen.getNit(),
                almacen.getNombreComercial(),
                almacen.getDescripcion(),
                almacen.getLatitud(),
                almacen.getLongitud(),
                almacen.getReputacion() != null ? almacen.getReputacion() : 0.0,
                almacen.getCategorias()
        );
    }
}
