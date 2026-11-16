package edu.dosw.styleradar.tiendas.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaActivaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaDesactivadaException;
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

    /** SR-RF-65: el motivo de desactivar o reactivar admite como máximo 500 caracteres. */
    public static final int MAX_LONGITUD_MOTIVO = 500;

    /**
     * Valida el motivo opcional de un cambio de estado (desactivar o reactivar).
     */
    public static void validarMotivoCambioEstado(String motivo) {
        if (motivo != null && motivo.length() > MAX_LONGITUD_MOTIVO) {
            throw new ReglaDeNegocioException("El motivo no puede superar " + MAX_LONGITUD_MOTIVO + " caracteres");
        }
    }

    private static final Pattern PATRON_NIT = Pattern.compile("^[0-9]{8,12}(-[0-9])?$");

    private Long id;
    private String nombre;
    private String nit;
    private String barrio;
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
        validarCamposComunes();
        if (urlCertificadoPersoneriaJuridica == null || urlCertificadoPersoneriaJuridica.isBlank()) {
            throw new ReglaDeNegocioException(
                    "El certificado de personería jurídica en PDF es obligatorio para registrar la fundación aliada");
        }
    }

    public void validarDatosEdicion() {
        validarCamposComunes();
    }

    private void validarCamposComunes() {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre de la fundación es obligatorio");
        }
        int lenNombre = nombre.strip().length();
        if (lenNombre < 4 || lenNombre > 120) {
            throw new ReglaDeNegocioException("El nombre debe tener entre 4 y 120 caracteres");
        }
        if (nit != null) {
            if (nit.isBlank() || !PATRON_NIT.matcher(nit.strip()).matches()) {
                throw new ReglaDeNegocioException("El NIT debe tener un formato válido (ej. 900215884-1)");
            }
        }
        if (barrio != null) {
            if (barrio.isBlank()) {
                throw new ReglaDeNegocioException("El barrio es obligatorio");
            }
            if (barrio.strip().length() > 80) {
                throw new ReglaDeNegocioException("El barrio no puede superar 80 caracteres");
            }
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
    }

    public void actualizarDatosInstitucionales(Fundacion cambios) {
        if (cambios == null) {
            throw new ReglaDeNegocioException("Los datos de actualización no pueden ser nulos");
        }
        cambios.validarDatosEdicion();

        this.nombre = cambios.getNombre().trim();
        if (cambios.getNit() != null && !cambios.getNit().isBlank()) {
            this.nit = cambios.getNit().trim();
        }
        if (cambios.getBarrio() != null && !cambios.getBarrio().isBlank()) {
            this.barrio = cambios.getBarrio().trim();
        }
        this.descripcion = cambios.getDescripcion() != null ? cambios.getDescripcion().trim() : null;
        this.direccion = cambios.getDireccion() != null ? cambios.getDireccion().trim() : null;
        this.telefono = cambios.getTelefono() != null ? cambios.getTelefono().trim() : null;
        this.email = cambios.getEmail() != null ? cambios.getEmail().trim().toLowerCase() : null;
        this.latitud = cambios.getLatitud();
        this.longitud = cambios.getLongitud();
        this.categoriasAceptadas = cambios.getCategoriasAceptadas() != null
                ? new HashSet<>(cambios.getCategoriasAceptadas())
                : new HashSet<>();
        this.representanteId = cambios.getRepresentanteId();
        if (cambios.getUrlCertificadoPersoneriaJuridica() != null && !cambios.getUrlCertificadoPersoneriaJuridica().isBlank()) {
            this.urlCertificadoPersoneriaJuridica = cambios.getUrlCertificadoPersoneriaJuridica().trim();
        }
        this.causasApoyadas = cambios.getCausasApoyadas() != null ? cambios.getCausasApoyadas().trim() : null;
        this.horarioAtencion = cambios.getHorarioAtencion() != null ? cambios.getHorarioAtencion().trim() : null;
        this.politicasDonacion = cambios.getPoliticasDonacion() != null ? cambios.getPoliticasDonacion().trim() : null;
    }

    public void desactivar() {
        if (!this.activa) {
            throw new FundacionYaDesactivadaException("La fundación aliada ya se encuentra desactivada");
        }
        this.activa = false;
    }

    public void reactivar() {
        if (this.activa) {
            throw new FundacionYaActivaException("La fundación aliada ya se encuentra activa");
        }
        this.activa = true;
    }
}
