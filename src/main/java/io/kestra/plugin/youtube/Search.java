package io.kestra.plugin.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Search YouTube for videos, channels, and playlists", description = "Search across YouTube resources (videos, channels, playlists) with comprehensive filtering and sorting options")
@Plugin(examples = {
        @Example(title = "Search for videos about a specific topic", full = true, code = """
                id: search_youtube
                namespace: company.team

                tasks:
                  - id: authenticate
                    type: io.kestra.plugin.youtube.OAuth2
                    clientId: "{{ secret('YOUTUBE_CLIENT_ID') }}"
                    clientSecret: "{{ secret('YOUTUBE_CLIENT_SECRET') }}"
                    refreshToken: "{{ secret('YOUTUBE_REFRESH_TOKEN') }}"

                  - id: search
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ outputs.authenticate.accessToken }}"
                    query: "machine learning tutorial"
                    resourceType:
                      - VIDEO
                    maxResults: 25
                    order: RELEVANCE
                """),
        @Example(title = "Search all resource types (videos, channels, playlists)", code = """
                  - id: search_all
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    query: "cooking recipes"
                    maxResults: 50
                    order: VIEW_COUNT
                """),
        @Example(title = "Search videos in a specific channel with filters", code = """
                  - id: search_channel_videos
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    channelId: "UCxxxxxxxxxxxxxx"
                    resourceType:
                      - VIDEO
                    videoDuration: MEDIUM
                    videoDefinition: HIGH
                    publishedAfter: "2024-01-01T00:00:00Z"
                    order: DATE
                """),
        @Example(title = "Search for channels by topic", code = """
                  - id: search_channels
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    query: "technology news"
                    resourceType:
                      - CHANNEL
                    maxResults: 20
                """),
        @Example(title = "Search for playlists", code = """
                  - id: search_playlists
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    query: "workout routines"
                    resourceType:
                      - PLAYLIST
                    maxResults: 30
                """)
})
public class Search extends AbstractYoutubeTask implements RunnableTask<Search.Output> {

    public enum ResourceType {
        VIDEO,
        CHANNEL,
        PLAYLIST;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum Order {
        DATE,
        RATING,
        RELEVANCE,
        TITLE,
        VIDEO_COUNT,
        VIEW_COUNT;

        public String toApiValue() {
            return this.name().toLowerCase().replace("_", "");
        }
    }

    public enum SafeSearch {
        MODERATE,
        NONE,
        STRICT;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum VideoCaption {
        ANY,
        CLOSED_CAPTION,
        NONE;

        public String toApiValue() {
            return this.name().equals("CLOSED_CAPTION") ? "closedCaption" : this.name().toLowerCase();
        }
    }

    public enum VideoDefinition {
        ANY,
        HIGH,
        STANDARD;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum VideoDimension {
        TWO_D,
        THREE_D,
        ANY;

        public String toApiValue() {
            if (this == TWO_D)
                return "2d";
            if (this == THREE_D)
                return "3d";
            return "any";
        }
    }

    public enum VideoDuration {
        ANY,
        LONG,
        MEDIUM,
        SHORT;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum VideoEmbeddable {
        ANY,
        TRUE;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum VideoLicense {
        ANY,
        CREATIVE_COMMON,
        YOUTUBE;

        public String toApiValue() {
            return this == CREATIVE_COMMON ? "creativeCommon" : this.name().toLowerCase();
        }
    }

    public enum VideoPaidProductPlacement {
        ANY,
        TRUE;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum VideoSyndicated {
        ANY,
        TRUE;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum VideoType {
        ANY,
        EPISODE,
        MOVIE;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum EventType {
        COMPLETED,
        LIVE,
        UPCOMING;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    public enum ChannelType {
        ANY,
        SHOW;

        public String toApiValue() {
            return this.name().toLowerCase();
        }
    }

    @Schema(title = "Search query", description = "The search query term to search for. You can also use Boolean NOT (-) and OR (|) operators.")
    private Property<String> query;

    @Schema(title = "Resource types to search", description = "Restricts a search query to only retrieve a particular type of resource. Default behavior searches all types.")
    private Property<List<ResourceType>> resourceType;

    @Schema(title = "Channel ID", description = "Restricts search to a particular channel. Value is a YouTube channel ID.")
    private Property<String> channelId;

    @Schema(title = "Maximum results", description = "Maximum number of items to return. Acceptable values are 0 to 50, inclusive.")
    @Builder.Default
    private Property<Integer> maxResults = Property.ofValue(25);

    @Schema(title = "Order", description = "Sort order for search results")
    @Builder.Default
    private Property<Order> order = Property.ofValue(Order.RELEVANCE);

    @Schema(title = "Published after", description = "Return only resources created after this date (RFC 3339 format: YYYY-MM-DDTHH:MM:SSZ)")
    private Property<String> publishedAfter;

    @Schema(title = "Published before", description = "Return only resources created before this date (RFC 3339 format: YYYY-MM-DDTHH:MM:SSZ)")
    private Property<String> publishedBefore;

    @Schema(title = "Region code", description = "Return search results for the specified country (ISO 3166-1 alpha-2 country code)")
    private Property<String> regionCode;

    @Schema(title = "Relevance language", description = "Return results most relevant to this language (ISO 639-1 two-letter language code)")
    private Property<String> relevanceLanguage;

    @Schema(title = "Safe search", description = "Filter results based on their age-appropriateness")
    private Property<SafeSearch> safeSearch;

    @Schema(title = "Topic ID", description = "Filter on videos associated with a particular topic (Freebase topic ID)")
    private Property<String> topicId;

    @Schema(title = "Video caption", description = "Filter on videos based on caption availability. Requires resourceType=VIDEO.")
    private Property<VideoCaption> videoCaption;

    @Schema(title = "Video category ID", description = "Filter on videos in a specific category. Requires resourceType=VIDEO.")
    private Property<String> videoCategoryId;

    @Schema(title = "Video definition", description = "Filter by video quality. Requires resourceType=VIDEO.")
    private Property<VideoDefinition> videoDefinition;

    @Schema(title = "Video dimension", description = "Filter on 2D or 3D videos. Requires resourceType=VIDEO.")
    private Property<VideoDimension> videoDimension;

    @Schema(title = "Video duration", description = "Filter by video length. Requires resourceType=VIDEO.")
    private Property<VideoDuration> videoDuration;

    @Schema(title = "Video embeddable", description = "Filter on embeddable videos only. Requires resourceType=VIDEO.")
    private Property<VideoEmbeddable> videoEmbeddable;

    @Schema(title = "Video license", description = "Filter by license type. Requires resourceType=VIDEO.")
    private Property<VideoLicense> videoLicense;

    @Schema(title = "Video paid product placement", description = "Filter on videos with paid product placement. Requires resourceType=VIDEO.")
    private Property<VideoPaidProductPlacement> videoPaidProductPlacement;

    @Schema(title = "Video syndicated", description = "Filter on syndicated videos. Requires resourceType=VIDEO.")
    private Property<VideoSyndicated> videoSyndicated;

    @Schema(title = "Video type", description = "Filter by video type. Requires resourceType=VIDEO.")
    private Property<VideoType> videoType;

    @Schema(title = "Event type", description = "Filter on broadcasts. Requires resourceType=VIDEO.")
    private Property<EventType> eventType;

    @Schema(title = "Channel type", description = "Filter on channel type. Requires resourceType=CHANNEL.")
    private Property<ChannelType> channelType;

    @Schema(title = "For mine", description = "Return results for the authenticated user's channel only. Requires resourceType=VIDEO or resourceType=CHANNEL.")
    private Property<Boolean> forMine;

    @Schema(title = "Page token", description = "Token for pagination to retrieve the next page of results")
    private Property<String> pageToken;

    @Override
    public Output run(RunContext runContext) throws Exception {
        YouTube youtube = createYoutubeService(runContext);

        // Render all parameters
        String renderedQuery = this.query != null ? runContext.render(this.query).as(String.class).orElse(null) : null;
        List<ResourceType> renderedTypes = this.resourceType != null
                ? runContext.render(this.resourceType).asList(ResourceType.class)
                : null;
        String renderedChannelId = this.channelId != null
                ? runContext.render(this.channelId).as(String.class).orElse(null)
                : null;
        Integer renderedMaxResults = runContext.render(this.maxResults).as(Integer.class).orElse(25);
        Order renderedOrder = runContext.render(this.order).as(Order.class).orElse(Order.RELEVANCE);
        String renderedPublishedAfter = this.publishedAfter != null
                ? runContext.render(this.publishedAfter).as(String.class).orElse(null)
                : null;
        String renderedPublishedBefore = this.publishedBefore != null
                ? runContext.render(this.publishedBefore).as(String.class).orElse(null)
                : null;
        String renderedRegionCode = this.regionCode != null
                ? runContext.render(this.regionCode).as(String.class).orElse(null)
                : null;
        String renderedRelevanceLanguage = this.relevanceLanguage != null
                ? runContext.render(this.relevanceLanguage).as(String.class).orElse(null)
                : null;
        SafeSearch renderedSafeSearch = this.safeSearch != null
                ? runContext.render(this.safeSearch).as(SafeSearch.class).orElse(null)
                : null;
        String renderedTopicId = this.topicId != null ? runContext.render(this.topicId).as(String.class).orElse(null)
                : null;
        VideoCaption renderedVideoCaption = this.videoCaption != null
                ? runContext.render(this.videoCaption).as(VideoCaption.class).orElse(null)
                : null;
        String renderedVideoCategoryId = this.videoCategoryId != null
                ? runContext.render(this.videoCategoryId).as(String.class).orElse(null)
                : null;
        VideoDefinition renderedVideoDefinition = this.videoDefinition != null
                ? runContext.render(this.videoDefinition).as(VideoDefinition.class).orElse(null)
                : null;
        VideoDimension renderedVideoDimension = this.videoDimension != null
                ? runContext.render(this.videoDimension).as(VideoDimension.class).orElse(null)
                : null;
        VideoDuration renderedVideoDuration = this.videoDuration != null
                ? runContext.render(this.videoDuration).as(VideoDuration.class).orElse(null)
                : null;
        VideoEmbeddable renderedVideoEmbeddable = this.videoEmbeddable != null
                ? runContext.render(this.videoEmbeddable).as(VideoEmbeddable.class).orElse(null)
                : null;
        VideoLicense renderedVideoLicense = this.videoLicense != null
                ? runContext.render(this.videoLicense).as(VideoLicense.class).orElse(null)
                : null;
        VideoPaidProductPlacement renderedVideoPaidProductPlacement = this.videoPaidProductPlacement != null
                ? runContext.render(this.videoPaidProductPlacement).as(VideoPaidProductPlacement.class).orElse(null)
                : null;
        VideoSyndicated renderedVideoSyndicated = this.videoSyndicated != null
                ? runContext.render(this.videoSyndicated).as(VideoSyndicated.class).orElse(null)
                : null;
        VideoType renderedVideoType = this.videoType != null
                ? runContext.render(this.videoType).as(VideoType.class).orElse(null)
                : null;
        EventType renderedEventType = this.eventType != null
                ? runContext.render(this.eventType).as(EventType.class).orElse(null)
                : null;
        ChannelType renderedChannelType = this.channelType != null
                ? runContext.render(this.channelType).as(ChannelType.class).orElse(null)
                : null;
        Boolean renderedForMine = this.forMine != null ? runContext.render(this.forMine).as(Boolean.class).orElse(null)
                : null;
        String renderedPageToken = this.pageToken != null
                ? runContext.render(this.pageToken).as(String.class).orElse(null)
                : null;

        // Build the search request
        YouTube.Search.List request = youtube.search()
                .list(Collections.singletonList("snippet"))
                .setMaxResults(Long.valueOf(renderedMaxResults))
                .setOrder(renderedOrder.toApiValue());

        // Set resource types (if not specified, API searches all types by default)
        if (renderedTypes != null && !renderedTypes.isEmpty()) {
            String types = renderedTypes.stream()
                    .map(ResourceType::toApiValue)
                    .collect(Collectors.joining(","));
            request.setType(Collections.singletonList(types));
        }

        // Add common filters
        if (renderedQuery != null && !renderedQuery.isEmpty()) {
            request.setQ(renderedQuery);
        }
        if (renderedChannelId != null && !renderedChannelId.isEmpty()) {
            request.setChannelId(renderedChannelId);
        }
        if (renderedPublishedAfter != null && !renderedPublishedAfter.isEmpty()) {
            request.setPublishedAfter(renderedPublishedAfter);
        }
        if (renderedPublishedBefore != null && !renderedPublishedBefore.isEmpty()) {
            request.setPublishedBefore(renderedPublishedBefore);
        }
        if (renderedRegionCode != null && !renderedRegionCode.isEmpty()) {
            request.setRegionCode(renderedRegionCode);
        }
        if (renderedRelevanceLanguage != null && !renderedRelevanceLanguage.isEmpty()) {
            request.setRelevanceLanguage(renderedRelevanceLanguage);
        }
        if (renderedSafeSearch != null) {
            request.setSafeSearch(renderedSafeSearch.toApiValue());
        }
        if (renderedTopicId != null && !renderedTopicId.isEmpty()) {
            request.setTopicId(renderedTopicId);
        }

        // Video-specific filters
        if (renderedVideoCaption != null) {
            request.setVideoCaption(renderedVideoCaption.toApiValue());
        }
        if (renderedVideoCategoryId != null && !renderedVideoCategoryId.isEmpty()) {
            request.setVideoCategoryId(renderedVideoCategoryId);
        }
        if (renderedVideoDefinition != null) {
            request.setVideoDefinition(renderedVideoDefinition.toApiValue());
        }
        if (renderedVideoDimension != null) {
            request.setVideoDimension(renderedVideoDimension.toApiValue());
        }
        if (renderedVideoDuration != null) {
            request.setVideoDuration(renderedVideoDuration.toApiValue());
        }
        if (renderedVideoEmbeddable != null) {
            request.setVideoEmbeddable(renderedVideoEmbeddable.toApiValue());
        }
        if (renderedVideoLicense != null) {
            request.setVideoLicense(renderedVideoLicense.toApiValue());
        }
        if (renderedVideoPaidProductPlacement != null) {
            request.setVideoPaidProductPlacement(renderedVideoPaidProductPlacement.toApiValue());
        }
        if (renderedVideoSyndicated != null) {
            request.setVideoSyndicated(renderedVideoSyndicated.toApiValue());
        }
        if (renderedVideoType != null) {
            request.setVideoType(renderedVideoType.toApiValue());
        }
        if (renderedEventType != null) {
            request.setEventType(renderedEventType.toApiValue());
        }

        // Channel-specific filters
        if (renderedChannelType != null) {
            request.setChannelType(renderedChannelType.toApiValue());
        }

        // Special filters
        if (renderedForMine != null && renderedForMine) {
            request.setForMine(true);
        }

        // Pagination
        if (renderedPageToken != null && !renderedPageToken.isEmpty()) {
            request.setPageToken(renderedPageToken);
        }

        // Execute the request
        SearchListResponse response = request.execute();
        List<SearchResult> searchResults = response.getItems();

        // Process results and categorize by type
        List<SearchResultItem> items = new ArrayList<>();
        int videoCount = 0;
        int channelCount = 0;
        int playlistCount = 0;

        for (SearchResult result : searchResults) {
            String kind = result.getId().getKind();
            SearchResultItem.SearchResultItemBuilder builder = SearchResultItem.builder()
                    .kind(kind)
                    .title(result.getSnippet().getTitle())
                    .description(result.getSnippet().getDescription())
                    .channelId(result.getSnippet().getChannelId())
                    .channelTitle(result.getSnippet().getChannelTitle())
                    .publishedAt(result.getSnippet().getPublishedAt() != null
                            ? Instant.ofEpochMilli(result.getSnippet().getPublishedAt().getValue())
                            : null)
                    .thumbnailUrl(result.getSnippet().getThumbnails() != null &&
                            result.getSnippet().getThumbnails().getDefault() != null
                                    ? result.getSnippet().getThumbnails().getDefault().getUrl()
                                    : null);

            // Set resource-specific IDs and URLs
            switch (kind) {
                case "youtube#video":
                    String videoId = result.getId().getVideoId();
                    builder.videoId(videoId);
                    builder.url("https://www.youtube.com/watch?v=" + videoId);
                    videoCount++;
                    break;
                case "youtube#channel":
                    String channelIdRes = result.getId().getChannelId();
                    builder.resourceChannelId(channelIdRes);
                    builder.url("https://www.youtube.com/channel/" + channelIdRes);
                    channelCount++;
                    break;
                case "youtube#playlist":
                    String playlistId = result.getId().getPlaylistId();
                    builder.playlistId(playlistId);
                    builder.url("https://www.youtube.com/playlist?list=" + playlistId);
                    playlistCount++;
                    break;
            }

            items.add(builder.build());
        }

        return Output.builder()
                .items(items)
                .totalResults(items.size())
                .videoCount(videoCount)
                .channelCount(channelCount)
                .playlistCount(playlistCount)
                .nextPageToken(response.getNextPageToken())
                .prevPageToken(response.getPrevPageToken())
                .totalAvailable(response.getPageInfo() != null ? response.getPageInfo().getTotalResults() : null)
                .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "List of search results")
        private final List<SearchResultItem> items;

        @Schema(title = "Number of results returned in this response")
        private final Integer totalResults;

        @Schema(title = "Number of videos in results")
        private final Integer videoCount;

        @Schema(title = "Number of channels in results")
        private final Integer channelCount;

        @Schema(title = "Number of playlists in results")
        private final Integer playlistCount;

        @Schema(title = "Token for the next page of results")
        private final String nextPageToken;

        @Schema(title = "Token for the previous page of results")
        private final String prevPageToken;

        @Schema(title = "Total number of results available matching the query (approximate)")
        private final Integer totalAvailable;
    }

    @Builder
    @Getter
    public static class SearchResultItem {
        @Schema(title = "Resource kind (youtube#video, youtube#channel, or youtube#playlist)")
        private final String kind;

        @Schema(title = "Video ID (only for video results)")
        private final String videoId;

        @Schema(title = "Channel ID (only for channel results)")
        private final String resourceChannelId;

        @Schema(title = "Playlist ID (only for playlist results)")
        private final String playlistId;

        @Schema(title = "Result title")
        private final String title;

        @Schema(title = "Result description")
        private final String description;

        @Schema(title = "Channel ID that published this resource")
        private final String channelId;

        @Schema(title = "Channel title that published this resource")
        private final String channelTitle;

        @Schema(title = "Publication date")
        private final Instant publishedAt;

        @Schema(title = "Thumbnail URL")
        private final String thumbnailUrl;

        @Schema(title = "Direct URL to the resource")
        private final String url;
    }
}
