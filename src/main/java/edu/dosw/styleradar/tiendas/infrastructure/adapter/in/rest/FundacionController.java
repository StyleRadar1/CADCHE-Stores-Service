package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.port.in.DesactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.EditarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.ReactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc.FundacionApi;
import jakarta.validation.Valid;

@RestController
public class FundacionController implements FundacionApi {

    private final RegistrarFundacionUseCase registrarFundacionUseCase;
    private final EditarFundacionUseCase editarFundacionUseCase;
    private final DesactivarFundacionUseCase desactivarFundacionUseCase;
    private final ReactivarFundacionUseCase reactivarFundacionUseCase;

    public FundacionController(
            RegistrarFundacionUseCase registrarFundacionUseCase,
            EditarFundacionUseCase editarFundacionUseCase,
            DesactivarFundacionUseCase desactivarFundacionUseCase) {
        this(registrarFundacionUseCase, editarFundacionUseCase, desactivarFundacionUseCase, null);
    }

    @Autowired
    public FundacionController(
            RegistrarFundacionUseCase registrarFundacionUseCase,
            EditarFundacionUseCase editarFundacionUseCase,
            DesactivarFundacionUseCase desactivarFundacionUseCase,
            @Autowired(required = false) ReactivarFundacionUseCase reactivarFundacionUseCase) {
        this.registrarFundacionUseCase = registrarFundacionUseCase;
        this.editarFundacionUseCase = editarFundacionUseCase;
        this.desactivarFundacionUseCase = desactivarFundacionUseCase;
        this.reactivarFundacionUseCase = reactivarFundacionUseCase;
    }

    @Override
    public ResponseEntity<DocumentoSubidoResponseDTO> subirCertificado(
            @RequestPart("file") MultipartFile archivo,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaDeNegocioException("El certificado debe ser un archivo PDF de máximo 5 MB");
        }
        try {
            String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
            DocumentoSubidoResponseDTO response = registrarFundacionUseCase.subirCertificado(
                    archivo.getOriginalFilename(),
                    archivo.getContentType(),
                    archivo.getSize(),
                    archivo.getInputStream(),
                    rol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (java.io.IOException ex) {
            throw new ReglaDeNegocioException("Error al leer el archivo PDF del certificado: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> registrar(
            @Valid @RequestBody FundacionRequestDTO request,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        FundacionResponseDTO creada = registrarFundacionUseCase.registrar(request, rol);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @Override
    public ResponseEntity<List<FundacionResponseDTO>> listarTodas(
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        return ResponseEntity.ok(registrarFundacionUseCase.listarTodas(rol));
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> obtenerPorId(
            Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        return ResponseEntity.ok(registrarFundacionUseCase.obtenerPorId(id, rol));
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> editar(
            Long id,
            @Valid @RequestBody FundacionRequestDTO request,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        FundacionResponseDTO editada = editarFundacionUseCase.editar(id, request, rol);
        return ResponseEntity.ok(editada);
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> desactivar(
            Long id,
            @RequestParam(value = "reason", required = false) String motivo,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        FundacionResponseDTO desactivada = desactivarFundacionUseCase.desactivar(id, motivo, rol);
        return ResponseEntity.ok(desactivada);
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> activar(
            Long id,
            @RequestParam(value = "reason", required = false) String motivo,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        String rol = AuthRequestContextResolver.resolveRole(rolesHeader, roleHeader);
        FundacionResponseDTO reactivada = reactivarFundacionUseCase.reactivar(id, motivo, rol);
        return ResponseEntity.ok(reactivada);
    }

    @Override
    public ResponseEntity<List<FundacionResponseDTO>> consultarCercanas(
            @Valid @ModelAttribute ConsultaFundacionesRequestDTO request) {
        return ResponseEntity.ok(registrarFundacionUseCase.consultarCercanas(request));
    }
}
