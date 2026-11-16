package edu.dosw.styleradar.tiendas.domain.model;

import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;

/**
 * Solicitud de página validada en el dominio: página desde 0 y tamaño entre 1 y 100.
 */
public record SolicitudPagina(int numero, int tamano) {

    public static final int TAMANO_MAXIMO = 100;

    public static SolicitudPagina de(Integer numero, Integer tamano, String mensajeError) {
        int pagina = numero != null ? numero : 0;
        int size = tamano != null ? tamano : 20;
        if (pagina < 0 || size < 1 || size > TAMANO_MAXIMO) {
            throw new ReglaDeNegocioException(mensajeError);
        }
        return new SolicitudPagina(pagina, size);
    }
}
