package edu.dosw.styleradar.tiendas.domain.model;

import java.io.Serializable;
import java.util.Objects;

import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;
import lombok.Getter;

@Getter
public final class PeriodoNuevaLlegada implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int VALOR_POR_DEFECTO = 7;
    public static final int MIN_DIAS = 1;
    public static final int MAX_DIAS = 60;

    private final int dias;

    private PeriodoNuevaLlegada(int dias) {
        if (dias < MIN_DIAS) {
            throw new PeriodoInvalidoException("El periodo de nueva llegada debe ser de al menos 1 día (valor recibido: " + dias + ")");
        }
        if (dias > MAX_DIAS) {
            throw new PeriodoInvalidoException("El periodo de nueva llegada no puede superar los " + MAX_DIAS + " días");
        }
        this.dias = dias;
    }

    public static PeriodoNuevaLlegada de(Integer dias) {
        if (dias == null) {
            return porDefecto();
        }
        return new PeriodoNuevaLlegada(dias);
    }

    public static PeriodoNuevaLlegada porDefecto() {
        return new PeriodoNuevaLlegada(VALOR_POR_DEFECTO);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeriodoNuevaLlegada that)) return false;
        return dias == that.dias;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dias);
    }

    @Override
    public String toString() {
        return String.valueOf(dias);
    }
}
