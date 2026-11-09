package edu.dosw.styleradar.tiendas.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.response.FotografiaTiendaResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ArchivoNoEsImagenException;
import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - RegistrarFotografiaTiendaUseCase (SR-RF-74)")
class RegistrarFotografiaTiendaUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @Mock
    private AlmacenamientoBlobPort almacenamientoBlobPort;

    @Mock
    private AutorizacionAlmacenValidator autorizacionValidator;

    @InjectMocks
    private RegistrarFotografiaTiendaUseCaseImpl useCase;

    private Almacen almacenValido;
    private static final String NIT = "900123456-1";
    private static final Long ADMIN_ID = 5L;

    @BeforeEach
    void setUp() {
        almacenValido = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Atelier Urbano Chapinero")
                .fotos(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Criterio 1: Cargar primera fotografía la establece como portada y la persiste")
    void debeCargarPrimeraFotografiaComoPortada() {
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenValido));
        when(repositoryPort.findFotosByAlmacenNit(NIT)).thenReturn(List.of());
        when(almacenamientoBlobPort.subirArchivo(eq("tiendas"), anyString(), any(InputStream.class), eq("image/jpeg"), anyLong()))
                .thenReturn("https://styleradarblob.blob.core.windows.net/tiendas/900123456-1/foto1.jpg");
        when(repositoryPort.saveFoto(any(FotografiaTienda.class))).thenAnswer(inv -> {
            FotografiaTienda f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        InputStream contenido = new ByteArrayInputStream("fake-image-bytes".getBytes());
        FotografiaTiendaResponseDTO response = useCase.registrarFotografia(
                NIT, "fachada.jpg", "image/jpeg", 1024, contenido, ADMIN_ID);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(NIT, response.almacenNit());
        assertTrue(response.portada());
        assertEquals(1, response.orden());
        assertEquals("https://styleradarblob.blob.core.windows.net/tiendas/900123456-1/foto1.jpg", response.url());

        verify(almacenamientoBlobPort).subirArchivo(eq("tiendas"), anyString(), any(InputStream.class), eq("image/jpeg"), anyLong());
        verify(repositoryPort).saveFoto(any(FotografiaTienda.class));
        verify(repositoryPort).save(almacenValido);
        assertEquals("https://styleradarblob.blob.core.windows.net/tiendas/900123456-1/foto1.jpg", almacenValido.getFotoPortadaUrl());
    }

    @Test
    @DisplayName("Criterio 1: Cargar segunda fotografía tiene portada = false y orden = 2")
    void debeCargarSegundaFotografiaSinSerPortada() {
        FotografiaTienda primeraFoto = FotografiaTienda.builder()
                .id(1L)
                .almacenNit(NIT)
                .url("https://blob/foto1.jpg")
                .orden(1)
                .portada(true)
                .fechaRegistro(Instant.now())
                .build();

        almacenValido.setFotoPortadaUrl("https://blob/foto1.jpg");

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenValido));
        when(repositoryPort.findFotosByAlmacenNit(NIT)).thenReturn(List.of(primeraFoto));
        when(almacenamientoBlobPort.subirArchivo(anyString(), anyString(), any(), anyString(), anyLong()))
                .thenReturn("https://styleradarblob.blob.core.windows.net/tiendas/900123456-1/foto2.png");
        when(repositoryPort.saveFoto(any(FotografiaTienda.class))).thenAnswer(inv -> {
            FotografiaTienda f = inv.getArgument(0);
            f.setId(2L);
            return f;
        });

        InputStream contenido = new ByteArrayInputStream("fake-png-bytes".getBytes());
        FotografiaTiendaResponseDTO response = useCase.registrarFotografia(
                NIT, "interior.png", "image/png", 2048, contenido, ADMIN_ID);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertFalse(response.portada());
        assertEquals(2, response.orden());
    }

    @Test
    @DisplayName("Criterio 2: Dado que ya tengo 6 fotos, cuando cargo otra, recibo LimiteFotografiasSuperadoException (422)")
    void debeLanzar422CuandoYaTiene6Fotos() {
        List<FotografiaTienda> seisFotos = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            seisFotos.add(FotografiaTienda.builder()
                    .id((long) i)
                    .almacenNit(NIT)
                    .url("https://blob/foto" + i + ".jpg")
                    .orden(i)
                    .portada(i == 1)
                    .build());
        }

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenValido));
        when(repositoryPort.findFotosByAlmacenNit(NIT)).thenReturn(seisFotos);

        InputStream contenido = new ByteArrayInputStream("fake-bytes".getBytes());

        assertThrows(LimiteFotografiasSuperadoException.class, () ->
                useCase.registrarFotografia(NIT, "foto7.jpg", "image/jpeg", 500, contenido, ADMIN_ID));

        verify(almacenamientoBlobPort, never()).subirArchivo(any(), any(), any(), any(), anyLong());
        verify(repositoryPort, never()).saveFoto(any());
    }

    @Test
    @DisplayName("Criterio 3: Dado un archivo que no es imagen (ej. pdf o txt), cuando lo cargo, recibo ArchivoNoEsImagenException (400)")
    void debeLanzar400CuandoArchivoNoEsImagen() {
        InputStream contenidoPdf = new ByteArrayInputStream("fake-pdf".getBytes());

        assertThrows(ArchivoNoEsImagenException.class, () ->
                useCase.registrarFotografia(NIT, "documento.pdf", "application/pdf", 1024, contenidoPdf, ADMIN_ID));

        InputStream contenidoTxt = new ByteArrayInputStream("fake-text".getBytes());
        assertThrows(ArchivoNoEsImagenException.class, () ->
                useCase.registrarFotografia(NIT, "notas.txt", "text/plain", 500, contenidoTxt, ADMIN_ID));

        verify(almacenamientoBlobPort, never()).subirArchivo(any(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("Criterio 4: Dado un almacén que no es mío, cuando cargo fotos, recibo AccesoDenegadoAlmacenException (403)")
    void debeLanzar403CuandoUsuarioNoEsPropietario() {
        doThrow(new AccesoDenegadoAlmacenException("Acceso denegado"))
                .when(autorizacionValidator).validarAcceso(NIT, 999L, repositoryPort);

        InputStream contenido = new ByteArrayInputStream("fake-bytes".getBytes());

        assertThrows(AccesoDenegadoAlmacenException.class, () ->
                useCase.registrarFotografia(NIT, "fachada.jpg", "image/jpeg", 1024, contenido, 999L));

        verify(repositoryPort, never()).findByNit(any());
        verify(almacenamientoBlobPort, never()).subirArchivo(any(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("Negativo 404: Dado un almacén inexistente, cuando cargo fotos, recibo AlmacenNoEncontradoException")
    void debeLanzar404CuandoAlmacenNoExiste() {
        when(repositoryPort.findByNit("000000000")).thenReturn(Optional.empty());

        InputStream contenido = new ByteArrayInputStream("fake-bytes".getBytes());

        assertThrows(AlmacenNoEncontradoException.class, () ->
                useCase.registrarFotografia("000000000", "fachada.jpg", "image/jpeg", 1024, contenido, ADMIN_ID));
    }

    @Test
    @DisplayName("Listar: Retorna la lista de fotografías del almacén")
    void debeListarFotografias() {
        when(repositoryPort.existsByNit(NIT)).thenReturn(true);
        when(repositoryPort.findFotosByAlmacenNit(NIT)).thenReturn(List.of(
                FotografiaTienda.builder().id(1L).almacenNit(NIT).url("https://blob/1.jpg").orden(1).portada(true).build(),
                FotografiaTienda.builder().id(2L).almacenNit(NIT).url("https://blob/2.jpg").orden(2).portada(false).build()));

        List<FotografiaTiendaResponseDTO> lista = useCase.listarFotografias(NIT);

        assertEquals(2, lista.size());
        assertEquals(1L, lista.get(0).id());
        assertTrue(lista.get(0).portada());
        assertEquals(2L, lista.get(1).id());
        assertFalse(lista.get(1).portada());
    }

    @Test
    @DisplayName("Eliminar: Elimina una fotografía y reordena la galería")
    void debeEliminarFotografia() {
        FotografiaTienda f1 = FotografiaTienda.builder().id(10L).almacenNit(NIT).url("https://blob/1.jpg").orden(1).portada(true).build();
        FotografiaTienda f2 = FotografiaTienda.builder().id(20L).almacenNit(NIT).url("https://blob/2.jpg").orden(2).portada(false).build();

        almacenValido.setFotos(new ArrayList<>(List.of(f1, f2)));
        almacenValido.setFotoPortadaUrl("https://blob/1.jpg");

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenValido));
        when(repositoryPort.findFotosByAlmacenNit(NIT)).thenReturn(List.of(f1, f2));

        useCase.eliminarFotografia(NIT, 10L, ADMIN_ID);

        verify(repositoryPort).deleteFoto(10L, NIT);
        verify(repositoryPort).save(almacenValido);
        assertEquals("https://blob/2.jpg", almacenValido.getFotoPortadaUrl());
    }
}

