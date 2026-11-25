package edu.dosw.styleradar.tiendas.domain.model;

/**
 * Lo que se limpió al eliminarse una cuenta: cuántos vínculos de administrador de almacén se borraron y, si la cuenta
 * representaba una fundación activa, el ID de la fundación desactivada ({@code null} si no hubo ninguna).
 */
public record ResultadoLimpiezaUsuario(long asociacionesEliminadas, Long fundacionDesactivadaId) {
}
