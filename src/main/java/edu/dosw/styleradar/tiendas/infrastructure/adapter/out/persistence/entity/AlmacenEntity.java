package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "almacenes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenEntity {

    @Id
    @Column(name = "nit", length = 30)
    private String nit;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "correo_contacto", length = 100)
    private String correoContacto;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Column(name = "reputacion")
    private Double reputacion;

    @Column(name = "umbral_ultimas_unidades")
    private Integer umbralUltimasUnidades;

    @Column(name = "dias_nueva_llegada")
    private Integer diasNuevaLlegada;

    @Column(name = "fecha_actualizacion_catalogo")
    private Instant fechaActualizacionCatalogo;

    @ElementCollection
    @CollectionTable(
            name = "almacen_categorias",
            joinColumns = @JoinColumn(name = "almacen_nit"),
            uniqueConstraints = @UniqueConstraint(name = "uk_almacen_categoria", columnNames = {"almacen_nit", "categoria"})
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    @Builder.Default
    private Set<CategoriaAlmacen> categorias = new HashSet<>();
}

