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
        return diasNuevaLlegada != null ? diasNuevaLlegada : PeriodoNuevaLlegada.VALOR_POR_DEFECTO;
    }

    public void configurarUmbralUltimasUnidades(int nuevoUmbral) {
        UmbralUltimasUnidades vo = UmbralUltimasUnidades.de(nuevoUmbral);
        this.umbralUltimasUnidades = vo.getValor();
        this.fechaActualizacionCatalogo = Instant.now();
    }

    public void configurarDiasNuevaLlegada(int dias) {
        PeriodoNuevaLlegada vo = PeriodoNuevaLlegada.de(dias);
        this.diasNuevaLlegada = vo.getDias();
        this.fechaActualizacionCatalogo = Instant.now();
    }

    public void configurarCatalogo(int umbral, int dias) {
        UmbralUltimasUnidades uVo = UmbralUltimasUnidades.de(umbral);
        PeriodoNuevaLlegada pVo = PeriodoNuevaLlegada.de(dias);
        this.umbralUltimasUnidades = uVo.getValor();
        this.diasNuevaLlegada = pVo.getDias();
        this.fechaActualizacionCatalogo = Instant.now();
    }
}

