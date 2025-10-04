package io.kestra.plugin.youtube.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Builder
@Getter
public class SearchResultSnippet {
    @Schema(title = "Published at", description = "Date and time that the resource was created")
    private final Instant publishedAt;

    @Schema(title = "Channel ID", description = "ID of the channel that published the resource")
    private final String channelId;

    @Schema(title = "Title", description = "Title of the search result")
    private final String title;

    @Schema(title = "Description", description = "Description of the search result")
    private final String description;

    @Schema(title = "Thumbnails", description = "Map of thumbnail images for the result")
    private final Thumbnails thumbnails;

    @Schema(title = "Channel title", description = "Title of the channel that published the resource")
    private final String channelTitle;

    @Schema(title = "Live broadcast content", description = "Indicates if the resource is live content (none, upcoming, live, completed)")
    private final String liveBroadcastContent;
}
