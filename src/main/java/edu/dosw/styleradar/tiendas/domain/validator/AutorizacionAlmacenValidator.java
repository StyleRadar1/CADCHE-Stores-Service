package edu.dosw.styleradar.tiendas.domain.validator;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

@Component
public class AutorizacionAlmacenValidator {

    public void validarAcceso(String nit, Long usuarioId, AlmacenRepositoryPort repositoryPort) {
        if (usuarioId != null) {
            boolean autorizado = repositoryPort.esAdministradorDeAlmacen(nit, usuarioId);
            if (!autorizado) {
                throw new AccesoDenegadoAlmacenException("El usuario autenticado (" + usuarioId + ") no tiene permisos para administrar el almacén con NIT " + nit);
            }
        }
    }
}

