package edu.dosw.styleradar.tiendas.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdministradorAlmacen {

    private Long id;
    private Long usuarioId;
    private String almacenNit;
    private Long asignadoPorId;
    private Instant fechaAsignacion;
}

