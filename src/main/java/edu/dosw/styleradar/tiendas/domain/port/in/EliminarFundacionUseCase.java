package edu.dosw.styleradar.tiendas.domain.port.in;

/** SR-RF-120 Eliminar fundación aliada. */
public interface EliminarFundacionUseCase {

    void eliminar(Long id, String rol);
}
