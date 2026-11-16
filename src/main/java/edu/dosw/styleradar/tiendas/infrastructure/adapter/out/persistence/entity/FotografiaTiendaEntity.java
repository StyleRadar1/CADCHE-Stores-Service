package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "almacen_fotos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotografiaTiendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "almacen_nit", nullable = false, length = 30)
    private String almacenNit;

    @Column(name = "url_foto", nullable = false, length = 500)
    private String urlFoto;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "es_portada", nullable = false)
    private boolean esPortada;

    @Column(name = "fecha_subida")
    @Builder.Default
    private Instant fechaSubida = Instant.now();
}

