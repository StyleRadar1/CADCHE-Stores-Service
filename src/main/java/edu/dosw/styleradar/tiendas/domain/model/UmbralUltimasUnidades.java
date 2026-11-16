package edu.dosw.styleradar.tiendas.domain.model;

import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class UmbralUltimasUnidades {

    public static final int VALOR_POR_DEFECTO = 3;
    public static final int MIN_VALOR = 0;
    public static final int MAX_VALOR = 50;

    private final int valor;

    private UmbralUltimasUnidades(int valor) {
        if (valor < MIN_VALOR) {
            throw new UmbralInvalidoException("El umbral de últimas unidades no puede ser negativo (valor proporcionado: " + valor + ")");
        }
        if (valor > MAX_VALOR) {
            throw new UmbralInvalidoException("El umbral de últimas unidades no puede superar el límite de " + MAX_VALOR + " (valor proporcionado: " + valor + ")");
        }
        this.valor = valor;
    }

    public static UmbralUltimasUnidades de(Integer valor) {
        if (valor == null) {
            return new UmbralUltimasUnidades(VALOR_POR_DEFECTO);
        }
        return new UmbralUltimasUnidades(valor);
    }

    public static UmbralUltimasUnidades porDefecto() {
        return new UmbralUltimasUnidades(VALOR_POR_DEFECTO);
    }
}

