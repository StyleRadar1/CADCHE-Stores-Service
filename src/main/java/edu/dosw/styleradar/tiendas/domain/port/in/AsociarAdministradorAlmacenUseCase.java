package edu.dosw.styleradar.tiendas.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;

public interface AsociarAdministradorAlmacenUseCase {

    AdministradorAlmacenResponseDTO asociar(String nit, Long usuarioId, Long adminId, String rol);

    List<AdministradorAlmacenResponseDTO> listarAdministradores(String nit, Long usuarioId, String rol);
}

