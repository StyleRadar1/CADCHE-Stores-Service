package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "fundaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "direccion", nullable = false, length = 200)
    private String direccion;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "latitud", nullable = false)
    private Double latitud;

    @Column(name = "longitud", nullable = false)
    private Double longitud;

    @Column(name = "representante_id", nullable = false)
    private Long representanteId;

    @Column(name = "url_certificado_personeria_juridica", length = 300)
    private String urlCertificadoPersoneriaJuridica;

    @Column(name = "causas_apoyadas", length = 250)
    private String causasApoyadas;

    @Column(name = "horario_atencion", length = 150)
    private String horarioAtencion;

    @Column(name = "politicas_donacion", length = 500)
    private String politicasDonacion;

    @Column(name = "activa", nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private Instant fechaRegistro = Instant.now();

    @ElementCollection
    @CollectionTable(
            name = "fundacion_categorias_aceptadas",
            joinColumns = @JoinColumn(name = "fundacion_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_fundacion_categoria", columnNames = {"fundacion_id", "categoria"})
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    @Builder.Default
    private Set<TipoPrenda> categoriasAceptadas = new HashSet<>();
}
