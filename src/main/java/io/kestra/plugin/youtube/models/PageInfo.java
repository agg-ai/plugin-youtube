package io.kestra.plugin.youtube.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PageInfo {
    @Schema(title = "Total results", description = "Total number of results in the result set (approximate)")
    private final Integer totalResults;

    @Schema(title = "Results per page", description = "Number of results included in the API response")
    private final Integer resultsPerPage;
}
