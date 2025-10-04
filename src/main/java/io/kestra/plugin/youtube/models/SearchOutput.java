package io.kestra.plugin.youtube.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class SearchOutput implements io.kestra.core.models.tasks.Output {

    @Schema(title = "Resource type", description = "Identifies the API resource's type (youtube#searchListResponse)")
    private final String kind;

    @Schema(title = "Etag", description = "The ETag for this resource")
    private final String etag;

    @Schema(title = "Next page token", description = "Token for the next page of results")
    private final String nextPageToken;

    @Schema(title = "Previous page token", description = "Token for the previous page of results")
    private final String prevPageToken;

    @Schema(title = "Region code", description = "The region code that was used for the search query")
    private final String regionCode;

    @Schema(title = "Page information", description = "Paging information for the result set")
    private final PageInfo pageInfo;

    @Schema(title = "Search results", description = "List of search result items")
    private final List<SearchItem> items;

    // Convenience fields (not in YouTube API)
    @Schema(title = "Video count", description = "Number of videos in results (convenience field)")
    private final Integer videoCount;

    @Schema(title = "Channel count", description = "Number of channels in results (convenience field)")
    private final Integer channelCount;

    @Schema(title = "Playlist count", description = "Number of playlists in results (convenience field)")
    private final Integer playlistCount;
}
