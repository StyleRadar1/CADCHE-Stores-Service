package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "administradores_almacenes",
        uniqueConstraints = @UniqueConstraint(name = "uk_admin_almacen", columnNames = {"usuario_id", "almacen_nit"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdministradorAlmacenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "almacen_nit", nullable = false, length = 30)
    private String almacenNit;

    @Column(name = "fecha_asignacion")
    @Builder.Default
    private Instant fechaAsignacion = Instant.now();
}

