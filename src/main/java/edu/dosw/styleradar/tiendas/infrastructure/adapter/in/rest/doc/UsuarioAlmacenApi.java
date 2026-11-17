package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Almacenes", description = "Almacenes aliados de StyleRadar")
public interface UsuarioAlmacenApi {

    @Operation(
            summary = "Consultar el almacén que administra un usuario (SR-RF-59 / SR-RF-62-A)",
            description = "El usuario se identifica en la ruta. Solo puede consultarlo el mismo usuario (Tienda) o un "
                    + "ADMIN_STYLERADAR; cualquier otra identidad recibe 403.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Almacén administrado por el usuario"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "El usuario de la ruta no es quien hace la petición", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario no administra ningún almacén", content = @Content(mediaType = ErrorResponseDTO.MEDIA_TYPE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{userId}/store")
    ResponseEntity<VerificacionAlmacenResponseDTO> obtenerAlmacenDelUsuario(
            @Parameter(description = "Identificador del usuario dueño del almacén", example = "10") @PathVariable("userId") Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader);
}
