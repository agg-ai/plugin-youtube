package io.kestra.plugin.youtube.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SearchResultId {
    @Schema(title = "Resource kind", description = "Type of the identified resource (youtube#video, youtube#channel, or youtube#playlist)")
    private final String kind;

    @Schema(title = "Video ID", description = "ID that YouTube uses to uniquely identify a video (only for video results)")
    private final String videoId;

    @Schema(title = "Channel ID", description = "ID that YouTube uses to uniquely identify a channel (only for channel results)")
    private final String channelId;

    @Schema(title = "Playlist ID", description = "ID that YouTube uses to uniquely identify a playlist (only for playlist results)")
    private final String playlistId;
}
