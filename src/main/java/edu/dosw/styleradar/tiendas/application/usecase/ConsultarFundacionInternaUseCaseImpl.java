package edu.dosw.styleradar.tiendas.application.usecase;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.in.ConsultarFundacionInternaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

/**
 * El filtro de servicio interno ya autenticó al microservicio llamante, así que la consulta se resuelve con la misma
 * vista que ve un administrador StyleRadar.
 */
public class ConsultarFundacionInternaUseCaseImpl implements ConsultarFundacionInternaUseCase {

    private final FundacionRepositoryPort repositoryPort;
    private final RegistrarFundacionUseCase fundaciones;

    public ConsultarFundacionInternaUseCaseImpl(FundacionRepositoryPort repositoryPort,
            RegistrarFundacionUseCase fundaciones) {
        this.repositoryPort = repositoryPort;
        this.fundaciones = fundaciones;
    }

    @Override
    @Transactional(readOnly = true)
    public FundacionResponseDTO porId(Long id) {
        return fundaciones.obtenerPorId(id, AutorizacionAdminStyleRadarValidator.ROL_ADMIN_STYLERADAR);
    }

    @Override
    @Transactional(readOnly = true)
    public FundacionResponseDTO porRepresentante(Long representanteId) {
        if (representanteId == null) {
            throw new ReglaDeNegocioException("El parámetro representativeId es obligatorio");
        }
        Long id = repositoryPort.findByRepresentanteId(representanteId).map(Fundacion::getId)
                .orElseThrow(() -> new FundacionNoEncontradaException(
                        "No existe una fundación aliada para el representante con ID: " + representanteId));
        return porId(id);
    }
}
