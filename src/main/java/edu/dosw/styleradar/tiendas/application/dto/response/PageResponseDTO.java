package edu.dosw.styleradar.tiendas.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta paginada estándar")
public class PageResponseDTO<T> {

    @Schema(description = "Elementos contenidos en la página actual")
    private List<T> content;

    @Schema(description = "Índice de página actual (base 0)", example = "0")
    private int page;

    @Schema(description = "Tamaño de página solicitado", example = "20")
    private int size;

    @Schema(description = "Cantidad total de elementos disponibles", example = "1")
    private long totalElements;

    @Schema(description = "Cantidad total de páginas disponibles", example = "1")
    private int totalPages;

    @Schema(description = "Indica si es la primera página", example = "true")
    private boolean first;

    @Schema(description = "Indica si es la última página", example = "true")
    private boolean last;
}

