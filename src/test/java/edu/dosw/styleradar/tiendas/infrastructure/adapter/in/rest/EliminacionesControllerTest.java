package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionPublicaResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.domain.port.in.DesactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.EditarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.EliminarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.EliminarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ReactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;

@DisplayName("Controladores del análisis v6: eliminaciones y consulta pública de fundaciones")
class EliminacionesControllerTest {

    @Test
    @DisplayName("SR-RF-119: DELETE /api/v1/admin/stores/{nit} responde 204 con el rol del token")
    void eliminarAlmacen() {
        EliminarAlmacenUseCase caso = mock(EliminarAlmacenUseCase.class);
        ResponseEntity<Void> r = new EliminacionAlmacenController(caso)
                .eliminar("900123456-1", "ROLE_STYLERADAR_ADMIN", null);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(caso).eliminar("900123456-1", "ROLE_STYLERADAR_ADMIN");
    }

    @Test
    @DisplayName("SR-RF-120 y SR-RF-117: DELETE de fundación responde 204; la consulta pública omite los datos internos")
    void fundaciones() {
        RegistrarFundacionUseCase registrar = mock(RegistrarFundacionUseCase.class);
        EliminarFundacionUseCase eliminar = mock(EliminarFundacionUseCase.class);
        FundacionController controller = new FundacionController(registrar, mock(EditarFundacionUseCase.class),
                mock(DesactivarFundacionUseCase.class), mock(ReactivarFundacionUseCase.class), eliminar);

        assertThat(controller.eliminar(7L, "ROLE_STYLERADAR_ADMIN", null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(eliminar).eliminar(7L, "ROLE_STYLERADAR_ADMIN");

        FundacionResponseDTO completa = FundacionResponseDTO.builder().id(7L).nombre("Fundación Abrigo").nit("900215884-1")
                .barrio("Chapinero").direccion("Calle 85").email("a@b.org").latitud(4.6).longitud(-74.0)
                .categoriasAceptadas(Set.of(TipoPrenda.TOP)).representanteId(30L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf").activa(true).build();
        ConsultaFundacionesRequestDTO consulta = new ConsultaFundacionesRequestDTO();
        when(registrar.consultarCercanas(consulta)).thenReturn(List.of(completa));

        List<FundacionPublicaResponseDTO> publica = controller.consultarCercanas(consulta).getBody();

        assertThat(publica).singleElement().satisfies(f -> {
            assertThat(f.id()).isEqualTo(7L);
            assertThat(f.name()).isEqualTo("Fundación Abrigo");
            assertThat(f.acceptedCategories()).containsExactly(TipoPrenda.TOP);
            assertThat(f.active()).isTrue();
        });
        assertThat(FundacionPublicaResponseDTO.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("representativeId", "legalEntityCertificateUrl", "registeredAt");
    }

    @Test
    @DisplayName("El constructor sin el caso de eliminar sigue disponible para quien no lo usa")
    void constructorSinEliminar() {
        assertThat(new FundacionController(mock(RegistrarFundacionUseCase.class), mock(EditarFundacionUseCase.class),
                mock(DesactivarFundacionUseCase.class), mock(ReactivarFundacionUseCase.class))).isNotNull();
    }
}
