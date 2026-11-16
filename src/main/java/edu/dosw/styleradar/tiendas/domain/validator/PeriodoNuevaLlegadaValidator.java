package edu.dosw.styleradar.tiendas.domain.validator;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;
import edu.dosw.styleradar.tiendas.domain.model.PeriodoNuevaLlegada;

@Component
public class PeriodoNuevaLlegadaValidator {

    public void validar(Integer dias) {
        if (dias == null) {
            throw new PeriodoInvalidoException("El periodo de días para nueva llegada es obligatorio");
        }
        if (dias < PeriodoNuevaLlegada.MIN_DIAS) {
            throw new PeriodoInvalidoException("El periodo de nueva llegada debe ser de al menos 1 día (valor recibido: " + dias + ")");
        }
        if (dias > PeriodoNuevaLlegada.MAX_DIAS) {
            throw new PeriodoInvalidoException("El periodo de nueva llegada no puede superar los " + PeriodoNuevaLlegada.MAX_DIAS + " días");
        }
    }
}
