package edu.dosw.styleradar.tiendas.domain.port.in;

import edu.dosw.styleradar.tiendas.domain.model.ResultadoLimpiezaUsuario;

/**
 * Reacciona a la eliminación de una cuenta en ms-usuarios: quita al usuario como administrador de almacenes y desactiva
 * la fundación que representaba, para que no queden vínculos con una cuenta que ya no existe.
 */
public interface LimpiarUsuarioEliminadoUseCase {

    ResultadoLimpiezaUsuario limpiar(Long usuarioId);
}
