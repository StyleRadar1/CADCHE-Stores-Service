package edu.dosw.styleradar.tiendas.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Almacen {

    private String nit;
    private String nombreComercial;
    private String descripcion;
    private String telefono;
    private String correoContacto;
    private Double latitud;
    private Double longitud;
    private Double reputacion;

    @Builder.Default
    private Set<CategoriaAlmacen> categorias = new HashSet<>();

    private Integer umbralUltimasUnidades;

    @Builder.Default
    private Integer diasNuevaLlegada = 7;

    private Instant fechaActualizacionCatalogo;

    public int getUmbralUltimasUnidadesEfectivo() {
        return umbralUltimasUnidades != null ? umbralUltimasUnidades : UmbralUltimasUnidades.VALOR_POR_DEFECTO;
    }

    public int getDiasNuevaLlegadaEfectivo() {
        return diasNuevaLlegada != null ? diasNuevaLlegada : 7;
    }

    public void configurarUmbralUltimasUnidades(int nuevoUmbral) {
        UmbralUltimasUnidades vo = UmbralUltimasUnidades.de(nuevoUmbral);
        this.umbralUltimasUnidades = vo.getValor();
        this.fechaActualizacionCatalogo = Instant.now();
    }

    public void configurarCatalogo(int umbral, int dias) {
        UmbralUltimasUnidades vo = UmbralUltimasUnidades.de(umbral);
        if (dias < 1 || dias > 60) {
            throw new IllegalArgumentException("El periodo de nueva llegada debe estar entre 1 y 60 días");
        }
        this.umbralUltimasUnidades = vo.getValor();
        this.diasNuevaLlegada = dias;
        this.fechaActualizacionCatalogo = Instant.now();
    }
}
