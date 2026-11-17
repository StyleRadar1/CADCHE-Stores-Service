package edu.dosw.styleradar.tiendas.application.usecase;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ArchivoNoEsImagenException;
import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.exception.TamanoArchivoExcedidoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFotografiaTiendaUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RegistrarFotografiaTiendaUseCaseImpl implements RegistrarFotografiaTiendaUseCase {

    public static final long MAX_BYTES_IMAGEN = 6L * 1024L * 1024L;

    private static final Set<String> MIMES_IMAGEN_PERMITIDOS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    private static final Set<String> EXTENSIONES_IMAGEN_PERMITIDAS = Set.of(
            "jpg", "jpeg", "png", "webp");

    private final AlmacenRepositoryPort repositoryPort;
    private final AlmacenamientoBlobPort almacenamientoBlobPort;
    private final AutorizacionAlmacenValidator autorizacionValidator;

    @Override
    @Transactional
    public FotografiaTiendaResponseDTO registrarFotografia(
            String nit,
            String nombreOriginal,
            String contentType,
            long tamano,
            InputStream contenido,
            Long usuarioId) {

        if (nit == null || nit.isBlank()) {
            throw new ReglaDeNegocioException("El NIT del almacén es obligatorio");
        }

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén con el NIT: " + nit));

        autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);

        validarArchivoImagen(nombreOriginal, contentType, tamano);

        List<FotografiaTienda> fotosExistentes = repositoryPort.findFotosByAlmacenNit(nit);
        almacen.setFotos(new ArrayList<>(fotosExistentes));

        if (fotosExistentes.size() >= Almacen.MAXIMO_FOTOGRAFIAS) {
            throw new LimiteFotografiasSuperadoException(
                    "El almacén ya cuenta con el máximo permitido de " + Almacen.MAXIMO_FOTOGRAFIAS + " fotografías de tienda física");
        }

        String extension = extraerExtension(nombreOriginal);
        String nombreDestino = UUID.randomUUID().toString() + (extension != null ? "." + extension : ".webp");
        String rutaDestino = nit + "/" + nombreDestino;

        String urlBlob = almacenamientoBlobPort.subirArchivo("tiendas", rutaDestino, contenido, contentType, tamano);

        FotografiaTienda fotoDominio = almacen.agregarFotografia(urlBlob, Instant.now());
        FotografiaTienda fotoGuardada = repositoryPort.saveFoto(fotoDominio);

        repositoryPort.save(almacen);

        log.info("Fotografía ID={} registrada exitosamente para almacén NIT={} (portada={})",
                fotoGuardada.getId(), nit, fotoGuardada.isPortada());

        return mapearDTO(fotoGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FotografiaTiendaResponseDTO> listarFotografias(String nit) {
        if (!repositoryPort.existsByNit(nit)) {
            throw new AlmacenNoEncontradoException("No se encontró ningún almacén con el NIT: " + nit);
        }
        return repositoryPort.findFotosByAlmacenNit(nit).stream()
                .map(this::mapearDTO)
                .toList();
    }

    @Override
    @Transactional
    public void eliminarFotografia(String nit, Long fotoId, Long usuarioId) {
        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén con el NIT: " + nit));

        autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);

        List<FotografiaTienda> fotosExistentes = new ArrayList<>(repositoryPort.findFotosByAlmacenNit(nit));
        String urlEliminada = fotosExistentes.stream()
                .filter(f -> fotoId != null && fotoId.equals(f.getId()))
                .map(FotografiaTienda::getUrl)
                .findFirst()
                .orElse(null);

        almacen.setFotos(fotosExistentes);
        almacen.eliminarFotografia(fotoId);

        if (urlEliminada != null) {
            almacenamientoBlobPort.eliminarArchivo(urlEliminada);
        }
        repositoryPort.deleteFoto(fotoId, nit);

        for (FotografiaTienda foto : almacen.getFotos()) {
            repositoryPort.saveFoto(foto);
        }
        repositoryPort.save(almacen);
        log.info("Fotografía ID={} eliminada del almacén NIT={}", fotoId, nit);
    }

    @Override
    @Transactional
    public List<FotografiaTiendaResponseDTO> reordenarFotografias(String nit, List<Long> fotoIds, Long usuarioId) {
        if (nit == null || nit.isBlank()) {
            throw new ReglaDeNegocioException("El NIT del almacén es obligatorio");
        }

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén con el NIT: " + nit));

        autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);

        List<FotografiaTienda> fotosExistentes = new ArrayList<>(repositoryPort.findFotosByAlmacenNit(nit));
        almacen.setFotos(fotosExistentes);

        almacen.reordenarFotografias(fotoIds);

        List<FotografiaTiendaResponseDTO> resultado = new ArrayList<>();
        for (FotografiaTienda foto : almacen.getFotos()) {
            FotografiaTienda guardada = repositoryPort.saveFoto(foto);
            resultado.add(mapearDTO(guardada != null ? guardada : foto));
        }
        repositoryPort.save(almacen);

        log.info("Galería de fotografías reordenada exitosamente para almacén NIT={}", nit);
        return resultado;
    }

    private void validarArchivoImagen(String nombreOriginal, String contentType, long tamano) {
        if (tamano <= 0) {
            throw new ArchivoNoEsImagenException("El archivo de imagen no puede estar vacío");
        }
        if (tamano > MAX_BYTES_IMAGEN) {
            throw new TamanoArchivoExcedidoException("La imagen supera el tamaño máximo de 6 MB");
        }

        boolean mimeValido = contentType != null && MIMES_IMAGEN_PERMITIDOS.contains(contentType.toLowerCase(Locale.ROOT));
        String extension = extraerExtension(nombreOriginal);
        boolean extensionValida = extension != null && EXTENSIONES_IMAGEN_PERMITIDAS.contains(extension.toLowerCase(Locale.ROOT));

        if (!mimeValido && !extensionValida) {
            throw new ArchivoNoEsImagenException("El archivo '" + nombreOriginal
                    + "' no es una imagen válida. Formatos permitidos: JPEG, PNG, WebP");
        }
    }

    private String extraerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return null;
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1);
    }

    private FotografiaTiendaResponseDTO mapearDTO(FotografiaTienda foto) {
        return new FotografiaTiendaResponseDTO(
                foto.getId(),
                foto.getAlmacenNit(),
                foto.getUrl(),
                foto.getOrden(),
                foto.isPortada(),
                foto.getFechaRegistro());
    }
}
