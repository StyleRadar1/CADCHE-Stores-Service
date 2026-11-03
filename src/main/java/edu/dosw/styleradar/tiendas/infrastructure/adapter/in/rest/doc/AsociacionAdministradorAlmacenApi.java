package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest.doc;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Administradores de almacén", description = "Asociación y consulta de usuarios ADMIN_ALMACEN autorizados por NIT para prevenir vulnerabilidades BOLA/IDOR (SR-RF-62 / SR-RNF-01)")
@SecurityRequirement(name = "bearerAuth")
public interface AsociacionAdministradorAlmacenApi {

    @Operation(
            summary = "Asociar administrador autorizado a un NIT existente",
            description = "Solo ROLE_ADMIN_STYLERADAR con cuenta activa. Vincula de forma idempotente un usuario existente con rol ADMIN_ALMACEN "
                    + "a un almacén existente identificado por su NIT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asociación persistida o ya existente"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT inválido o cuenta suspendida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Requiere ROLE_ADMIN_STYLERADAR", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o almacén inexistente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto concurrente o de integridad", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Usuario objetivo sin rol ADMIN_ALMACEN", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Fallo técnico", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{nit}/administradores/{usuarioId}")
    ResponseEntity<AdministradorAlmacenResponseDTO> asociar(
            @Parameter(description = "NIT del almacén", example = "900123456-1") @PathVariable String nit,
            @Parameter(description = "Identificador del usuario con rol ADMIN_ALMACEN", example = "15") @PathVariable Long usuarioId);

    @Operation(
            summary = "Consultar administradores autorizados de un almacén por NIT",
            description = "ROLE_ADMIN_STYLERADAR o ROLE_ADMIN_ALMACEN propietario. Retorna la lista de administradores autorizados vinculados al NIT para validación de propiedad (prevención BOLA/IDOR).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de administradores autorizados del NIT"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos sobre el NIT consultado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Almacén inexistente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{nit}/administradores")
    ResponseEntity<List<AdministradorAlmacenResponseDTO>> listarAdministradores(
            @Parameter(description = "NIT del almacén", example = "900123456-1") @PathVariable String nit);
}
