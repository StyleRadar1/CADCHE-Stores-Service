package edu.dosw.styleradar.tiendas.domain.port.out;

import java.io.InputStream;

public interface AlmacenamientoBlobPort {

    String subirArchivo(String contenedor, String rutaDestino, InputStream contenido, String contentType, long tamano);

    void eliminarArchivo(String urlArchivo);
}

