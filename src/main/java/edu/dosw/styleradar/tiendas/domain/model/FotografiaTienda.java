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
public class FotografiaTienda {

    private Long id;
    private String almacenNit;
    private String url;
    private int orden;
    private boolean portada;
    private Instant fechaRegistro;
}

