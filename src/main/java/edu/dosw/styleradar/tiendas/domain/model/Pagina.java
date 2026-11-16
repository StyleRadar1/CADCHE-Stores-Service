package edu.dosw.styleradar.tiendas.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados del dominio, independiente de Spring Data (regla de oro 1: el dominio no conoce frameworks).
 */
public record Pagina<T>(List<T> contenido, int numero, int tamano, long totalElementos, int totalPaginas) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public boolean esPrimera() {
        return numero == 0;
    }

    public boolean esUltima() {
        return numero + 1 >= totalPaginas;
    }

    public <R> Pagina<R> map(Function<? super T, ? extends R> transformacion) {
        return new Pagina<>(contenido.stream().<R>map(transformacion).toList(), numero, tamano, totalElementos, totalPaginas);
    }
}
