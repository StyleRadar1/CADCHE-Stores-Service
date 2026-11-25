package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.FundacionInternaController;

@DisplayName("Consulta interna de fundaciones para ms-segunda-vida-donaciones")
class ConsultarFundacionInternaUseCaseTest {

    private final FundacionRepositoryPort repositorio = mock(FundacionRepositoryPort.class);
    private final RegistrarFundacionUseCase fundaciones = mock(RegistrarFundacionUseCase.class);
    private final ConsultarFundacionInternaUseCaseImpl consulta =
            new ConsultarFundacionInternaUseCaseImpl(repositorio, fundaciones);
    private final FundacionResponseDTO dto = FundacionResponseDTO.builder().id(7L).representanteId(24L).activa(true).build();

    @Test
    @DisplayName("Por ID devuelve la vista completa con representativeId")
    void porId() {
        when(fundaciones.obtenerPorId(7L, "ROLE_STYLERADAR_ADMIN")).thenReturn(dto);

        assertThat(consulta.porId(7L).getRepresentanteId()).isEqualTo(24L);
        assertThat(new FundacionInternaController(consulta).porId(7L).getBody()).isSameAs(dto);
    }

    @Test
    @DisplayName("Por representante resuelve su fundación; 404 si no tiene y 400 sin parámetro")
    void porRepresentante() {
        when(repositorio.findByRepresentanteId(24L)).thenReturn(Optional.of(Fundacion.builder().id(7L).build()));
        when(repositorio.findByRepresentanteId(99L)).thenReturn(Optional.empty());
        when(fundaciones.obtenerPorId(7L, "ROLE_STYLERADAR_ADMIN")).thenReturn(dto);

        assertThat(new FundacionInternaController(consulta).porRepresentante(24L).getBody()).isSameAs(dto);
        assertThatThrownBy(() -> consulta.porRepresentante(99L)).isInstanceOf(FundacionNoEncontradaException.class)
                .hasMessage("No existe una fundación aliada para el representante con ID: 99");
        assertThatThrownBy(() -> consulta.porRepresentante(null)).isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El parámetro representativeId es obligatorio");
    }
}
