package edu.dosw.styleradar.tiendas.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaDecididoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaPendienteException;
import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.exception.RutRequeridoParaAprobacionException;
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

    @Builder.Default
    private EstadoVerificacionAlmacen estadoVerificacion = EstadoVerificacionAlmacen.PENDIENTE;

    private String urlRutPdf;

    private Instant fechaDecisionVerificacion;

    private Long administradorDecisionId;

    private String motivoVerificacion;

    public static final int MAXIMO_FOTOGRAFIAS = 6;

    private String fotoPortadaUrl;

    @Builder.Default
    private List<FotografiaTienda> fotos = new ArrayList<>();

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

    public void decidirVerificacion(DecisionVerificacionAlmacen decision, String motivo, Long adminId, Instant fecha) {
        if (this.estadoVerificacion != EstadoVerificacionAlmacen.PENDIENTE) {
            throw new AlmacenYaDecididoException("El almacén con NIT " + nit
                    + " ya fue decidido previamente. Estado actual: " + estadoVerificacion);
        }
        if (decision == DecisionVerificacionAlmacen.APROBADO && (this.urlRutPdf == null || this.urlRutPdf.isBlank())) {
            throw new RutRequeridoParaAprobacionException("No es posible aprobar el almacén con NIT " + nit
                    + " porque no cuenta con el documento RUT adjunto en PDF");
        }
        if (decision == DecisionVerificacionAlmacen.RECHAZADO && (motivo == null || motivo.isBlank())) {
            throw new ReglaDeNegocioException("El rechazo de un almacén exige un motivo explicativo no vacío");
        }
        this.estadoVerificacion = decision == DecisionVerificacionAlmacen.APROBADO
                ? EstadoVerificacionAlmacen.APROBADO
                : EstadoVerificacionAlmacen.RECHAZADO;
        this.fechaDecisionVerificacion = fecha != null ? fecha : Instant.now();
        this.administradorDecisionId = adminId;
        this.motivoVerificacion = motivo != null ? motivo.strip() : null;
    }

    public void solicitarRevision() {
        if (this.estadoVerificacion == EstadoVerificacionAlmacen.PENDIENTE) {
            throw new AlmacenYaPendienteException("El almacén ya se encuentra pendiente de revisión");
        }
        this.estadoVerificacion = EstadoVerificacionAlmacen.PENDIENTE;
    }

    public FotografiaTienda agregarFotografia(String url, Instant fecha) {
        if (url == null || url.isBlank()) {
            throw new ReglaDeNegocioException("La URL de la fotografía no puede ser nula ni vacía");
        }
        if (this.fotos.size() >= MAXIMO_FOTOGRAFIAS) {
            throw new LimiteFotografiasSuperadoException(
                    "El almacén ya cuenta con el máximo permitido de " + MAXIMO_FOTOGRAFIAS + " fotografías de tienda física");
        }
        int nuevoOrden = this.fotos.size() + 1;
        boolean esPortada = this.fotos.isEmpty() || this.fotoPortadaUrl == null;

        FotografiaTienda foto = FotografiaTienda.builder()
                .almacenNit(this.nit)
                .url(url.strip())
                .orden(nuevoOrden)
                .portada(esPortada)
                .fechaRegistro(fecha != null ? fecha : Instant.now())
                .build();

        this.fotos.add(foto);
        if (esPortada) {
            this.fotoPortadaUrl = foto.getUrl();
        }
        return foto;
    }

    public void eliminarFotografia(Long fotoId) {
        if (fotoId == null) {
            throw new ReglaDeNegocioException("El identificador de la fotografía a eliminar es obligatorio");
        }
        FotografiaTienda fotoAEliminar = this.fotos.stream()
                .filter(f -> fotoId.equals(f.getId()))
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException("La fotografía no pertenece a la galería del almacén"));

        boolean eraPortada = fotoAEliminar.isPortada();
        this.fotos.remove(fotoAEliminar);

        // Reordenar posiciones de 1 a N
        for (int i = 0; i < this.fotos.size(); i++) {
            FotografiaTienda f = this.fotos.get(i);
            f.setOrden(i + 1);
        }

        if (eraPortada) {
            if (!this.fotos.isEmpty()) {
                FotografiaTienda nuevaPortada = this.fotos.get(0);
                nuevaPortada.setPortada(true);
                this.fotoPortadaUrl = nuevaPortada.getUrl();
            } else {
                this.fotoPortadaUrl = null;
            }
        }
    }
}
