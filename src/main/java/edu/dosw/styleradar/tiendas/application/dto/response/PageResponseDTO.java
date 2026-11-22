package edu.dosw.styleradar.tiendas.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard paginated response")
public class PageResponseDTO<T> {

    @Schema(description = "Items of the current page")
    private List<T> content;

    @Schema(description = "Current page index (zero based)", example = "0")
    private int page;

    @Schema(description = "Requested page size", example = "20")
    private int size;

    @Schema(description = "Total number of items", example = "1")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "1")
    private int totalPages;

    @Schema(description = "Whether this is the first page", example = "true")
    private boolean first;

    @Schema(description = "Whether this is the last page", example = "true")
    private boolean last;
}

