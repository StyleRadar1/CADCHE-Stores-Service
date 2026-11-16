package edu.dosw.styleradar.tiendas.domain.validator;


import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;
import edu.dosw.styleradar.tiendas.domain.model.UmbralUltimasUnidades;

public class UmbralInventarioValidator {

    public void validar(Integer umbral) {
        if (umbral == null) {
            throw new UmbralInvalidoException("El umbral de últimas unidades es obligatorio");
        }
        if (umbral < UmbralUltimasUnidades.MIN_VALOR) {
            throw new UmbralInvalidoException("El umbral de últimas unidades no puede ser negativo (valor proporcionado: " + umbral + ")");
        }
        if (umbral > UmbralUltimasUnidades.MAX_VALOR) {
            throw new UmbralInvalidoException("El umbral de últimas unidades no puede superar el límite de " + UmbralUltimasUnidades.MAX_VALOR);
        }
    }
}

