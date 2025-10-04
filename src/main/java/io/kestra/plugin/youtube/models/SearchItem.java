package io.kestra.plugin.youtube.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SearchItem {
    @Schema(title = "Resource kind", description = "Identifies the resource type (youtube#searchResult)")
    private final String kind;

    @Schema(title = "Etag", description = "The ETag for this resource")
    private final String etag;

    @Schema(title = "Resource ID", description = "Information about the identified resource")
    private final SearchResultId id;

    @Schema(title = "Snippet", description = "Basic details about the search result")
    private final SearchResultSnippet snippet;

    // Convenience field (not in YouTube API)
    @Schema(title = "Direct URL", description = "Direct URL to the resource (convenience field)")
    private final String url;
}
