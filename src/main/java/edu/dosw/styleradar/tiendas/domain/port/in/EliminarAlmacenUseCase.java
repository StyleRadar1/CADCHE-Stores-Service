package edu.dosw.styleradar.tiendas.domain.port.in;

/** SR-RF-119 Eliminar almacén aliado. */
public interface EliminarAlmacenUseCase {

    void eliminar(String nit, String rol);
}
