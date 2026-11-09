package edu.dosw.styleradar.tiendas.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fundacion {

    private Long id;
    private String nombre;
    private String descripcion;
    private String direccion;
    private String telefono;
    private String email;
    private Double latitud;
    private Double longitud;

    @Builder.Default
    private Set<TipoPrenda> categoriasAceptadas = new HashSet<>();

    private Long representanteId;
    private String urlCertificadoPersoneriaJuridica;
    private String causasApoyadas;
    private String horarioAtencion;
    private String politicasDonacion;

    @Builder.Default
    private boolean activa = true;

    @Builder.Default
    private Instant fechaRegistro = Instant.now();

    private Double distanciaKm;

    public void validarDatosObligatoriosRegistro() {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre de la fundación es obligatorio");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new ReglaDeNegocioException("La dirección de la sede es obligatoria");
        }
        if (email == null || email.isBlank()) {
            throw new ReglaDeNegocioException("El correo electrónico institucional es obligatorio");
        }
        if (latitud == null || latitud < -90.0 || latitud > 90.0) {
            throw new ReglaDeNegocioException("La latitud geográfica debe ubicarse entre -90.0 y 90.0");
        }
        if (longitud == null || longitud < -180.0 || longitud > 180.0) {
            throw new ReglaDeNegocioException("La longitud geográfica debe ubicarse entre -180.0 y 180.0");
        }
        if (categoriasAceptadas == null || categoriasAceptadas.isEmpty()) {
            throw new ReglaDeNegocioException("Debe seleccionar al menos una categoría de prenda aceptada para donación");
        }
        if (representanteId == null) {
            throw new ReglaDeNegocioException("El identificador del usuario representante es obligatorio");
        }
        if (urlCertificadoPersoneriaJuridica == null || urlCertificadoPersoneriaJuridica.isBlank()) {
            throw new ReglaDeNegocioException("El certificado de personería jurídica en PDF es obligatorio para registrar la fundación aliada");
        }
    }

    public void desactivar() {
        this.activa = false;
    }
}
