package io.kestra.plugin.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.youtube.helpers.EnumHelper;
import io.kestra.plugin.youtube.helpers.PropertyHelper;
import io.kestra.plugin.youtube.models.*;
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
                      - video
                    maxResults: 25
                    order: relevance
                """),
        @Example(title = "Search all resource types (videos, channels, playlists)", code = """
                  - id: search_all
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    query: "cooking recipes"
                    maxResults: 50
                    order: viewcount
                """),
        @Example(title = "Search videos in a specific channel with filters", code = """
                  - id: search_channel_videos
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    channelId: "UCxxxxxxxxxxxxxx"
                    resourceType:
                      - video
                    videoDuration: medium
                    videoDefinition: high
                    publishedAfter: "2024-01-01T00:00:00Z"
                    order: date
                """),
        @Example(title = "Search for channels by topic", code = """
                  - id: search_channels
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    query: "technology news"
                    resourceType:
                      - channel
                    maxResults: 20
                """),
        @Example(title = "Search for playlists", code = """
                  - id: search_playlists
                    type: io.kestra.plugin.youtube.Search
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    query: "workout routines"
                    resourceType:
                      - playlist
                    maxResults: 30
                """)
})
public class Search extends AbstractYoutubeTask implements RunnableTask<SearchOutput> {

    @Schema(title = "For mine", description = "Return results for the authenticated user's channel only. Requires resourceType=video or resourceType=channel.")
    private Property<Boolean> forMine;

    @Schema(title = "Search query", description = "The search query term to search for. You can also use Boolean NOT (-) and OR (|) operators.")
    private Property<String> query;

    @Schema(title = "Resource types to search", description = "Restricts a search query to only retrieve a particular type of resource. Default behavior searches all types.")
    private Property<List<ResourceType>> resourceType;

    @Schema(title = "Channel ID", description = "Restricts search to a particular channel. Value is a YouTube channel ID.")
    private Property<String> channelId;

    @Schema(title = "Maximum results", description = "Maximum number of items to return. Acceptable values are 0 to 50, inclusive.")
    @Builder.Default
    private Property<Integer> maxResults = Property.ofValue(25);

    @Schema(title = "Order", description = "The order parameter specifies the method that will be used to order resources in the API response. The default value is relevance.", allowableValues = {
            "date", "rating",
            "relevance", "title", "videoCount", "viewCount" })
    @Builder.Default
    private Property<String> order = Property.ofValue("relevance");

    @Schema(title = "Published after", description = "Return only resources created after this date (RFC 3339 format: YYYY-MM-DDTHH:MM:SSZ)")
    private Property<String> publishedAfter;

    @Schema(title = "Published before", description = "Return only resources created before this date (RFC 3339 format: YYYY-MM-DDTHH:MM:SSZ)")
    private Property<String> publishedBefore;

    @Schema(title = "Region code", description = "Return search results for the specified country (ISO 3166-1 alpha-2 country code)")
    private Property<String> regionCode;

    @Schema(title = "Relevance language", description = "Return results most relevant to this language (ISO 639-1 two-letter language code)")
    private Property<String> relevanceLanguage;

    @Schema(title = "Safe search", description = "Filter results based on their age-appropriateness", allowableValues = {
            "moderate", "none", "strict" })
    private Property<String> safeSearch;

    @Schema(title = "Topic ID", description = "Filter on videos associated with a particular topic (Freebase topic ID)")
    private Property<String> topicId;

    @Schema(title = "Video caption", description = "Filter on videos based on caption availability. Requires resourceType=video.", allowableValues = {
            "any", "closedCaption", "none" })
    private Property<String> videoCaption;

    @Schema(title = "Video category ID", description = "Filter on videos in a specific category. Requires resourceType=video.")
    private Property<String> videoCategoryId;

    @Schema(title = "Video definition", description = "Filter by video quality. Requires resourceType=video.", allowableValues = {
            "any", "high", "standard" })
    private Property<String> videoDefinition;

    @Schema(title = "Video dimension", description = "Filter on 2D or 3D videos. Requires resourceType=video.", allowableValues = {
            "2d", "3d", "any" })
    private Property<String> videoDimension;

    @Schema(title = "Video duration", description = "Filter by video length. Requires resourceType=video.", allowableValues = {
            "any", "long", "medium", "short" })
    private Property<String> videoDuration;

    @Schema(title = "Video embeddable", description = "Filter on embeddable videos only. Requires resourceType=video.", allowableValues = {
            "any", "true" })
    private Property<String> videoEmbeddable;

    @Schema(title = "Video license", description = "Filter by license type. Requires resourceType=video.", allowableValues = {
            "any", "creativeCommon", "youtube" })
    private Property<String> videoLicense;

    @Schema(title = "Video paid product placement", description = "Filter on videos with paid product placement. Requires resourceType=video.", allowableValues = {
            "any", "true" })
    private Property<String> videoPaidProductPlacement;

    @Schema(title = "Video syndicated", description = "Filter on syndicated videos. Requires resourceType=video.", allowableValues = {
            "any", "true" })
    private Property<String> videoSyndicated;

    @Schema(title = "Video type", description = "Filter by video type. Requires resourceType=video.", allowableValues = {
            "any", "episode", "movie" })
    private Property<String> videoType;

    @Schema(title = "Event type", description = "Filter on broadcasts. Requires resourceType=video.", allowableValues = {
            "completed", "live", "upcoming" })
    private Property<String> eventType;

    @Schema(title = "Channel type", description = "Filter on channel type. Requires resourceType=channel.", allowableValues = {
            "any", "show" })
    private Property<String> channelType;

    @Schema(title = "Page token", description = "Token for pagination to retrieve the next page of results")
    private Property<String> pageToken;

    @Override
    public SearchOutput run(RunContext runContext) throws Exception {
        YouTube youtube = createYoutubeService(runContext);

        // Render all parameters
        String renderedQuery = PropertyHelper.safeRender(runContext, this.query, null, String.class);
        List<ResourceType> renderedTypes = EnumHelper.safeRenderEnumList(runContext, this.resourceType,
                ResourceType.class);
        String renderedChannelId = PropertyHelper.safeRender(runContext, this.channelId, null, String.class);
        Integer renderedMaxResults = PropertyHelper.safeRender(runContext, this.maxResults, 25, Integer.class);
        String renderedOrder = PropertyHelper.safeRender(runContext, this.order, "relevance", String.class);
        String renderedPublishedAfter = PropertyHelper.safeRender(runContext, this.publishedAfter, null, String.class);
        String renderedPublishedBefore = PropertyHelper.safeRender(runContext, this.publishedBefore, null,
                String.class);
        String renderedRegionCode = PropertyHelper.safeRender(runContext, this.regionCode, null, String.class);
        String renderedRelevanceLanguage = PropertyHelper.safeRender(runContext, this.relevanceLanguage, null,
                String.class);
        String renderedSafeSearch = PropertyHelper.safeRender(runContext, this.safeSearch, null, String.class);
        String renderedTopicId = PropertyHelper.safeRender(runContext, this.topicId, null, String.class);
        String renderedVideoCaption = PropertyHelper.safeRender(runContext, this.videoCaption, null, String.class);
        String renderedVideoCategoryId = PropertyHelper.safeRender(runContext, this.videoCategoryId, null,
                String.class);
        String renderedVideoDefinition = PropertyHelper.safeRender(runContext, this.videoDefinition, null,
                String.class);
        String renderedVideoDimension = PropertyHelper.safeRender(runContext, this.videoDimension, null, String.class);
        String renderedVideoDuration = PropertyHelper.safeRender(runContext, this.videoDuration, null, String.class);
        String renderedVideoEmbeddable = PropertyHelper.safeRender(runContext, this.videoEmbeddable, null,
                String.class);
        String renderedVideoLicense = PropertyHelper.safeRender(runContext, this.videoLicense, null, String.class);
        String renderedVideoPaidProductPlacement = PropertyHelper.safeRender(runContext, this.videoPaidProductPlacement,
                null, String.class);
        String renderedVideoSyndicated = PropertyHelper.safeRender(runContext, this.videoSyndicated, null,
                String.class);
        String renderedVideoType = PropertyHelper.safeRender(runContext, this.videoType, null, String.class);
        String renderedEventType = PropertyHelper.safeRender(runContext, this.eventType, null, String.class);
        String renderedChannelType = PropertyHelper.safeRender(runContext, this.channelType, null, String.class);
        Boolean renderedForMine = PropertyHelper.safeRender(runContext, this.forMine, false, Boolean.class);
        String renderedPageToken = PropertyHelper.safeRender(runContext, this.pageToken, null, String.class);

        // Enforce YouTube API rules for forMine parameter
        // When forMine=true, type must be "video" and certain video filters cannot be
        // set
        if (renderedForMine != null && renderedForMine) {
            // Force resource type to video only
            renderedTypes = Collections.singletonList(ResourceType.video);

            // Clear restricted parameters that cannot be used with forMine=true
            renderedVideoDefinition = null;
            renderedVideoDimension = null;
            renderedVideoDuration = null;
            renderedVideoEmbeddable = null;
            renderedVideoLicense = null;
            renderedVideoPaidProductPlacement = null;
            renderedVideoSyndicated = null;
            renderedVideoType = null;
        }

        renderedOrder = renderedOrder.isEmpty() ? "relevance" : renderedOrder;
        renderedMaxResults = renderedMaxResults == 0 ? 25 : renderedMaxResults;

        // Build the search request
        YouTube.Search.List request = youtube.search()
                .list(Collections.singletonList("snippet"))
                .setMaxResults(Long.valueOf(renderedMaxResults))
                .setOrder(renderedOrder);

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
        if (renderedSafeSearch != null && !renderedSafeSearch.isEmpty()) {
            request.setSafeSearch(renderedSafeSearch);
        }
        if (renderedTopicId != null && !renderedTopicId.isEmpty()) {
            request.setTopicId(renderedTopicId);
        }

        // Video-specific filters
        if (renderedVideoCaption != null && !renderedVideoCaption.isEmpty()) {
            request.setVideoCaption(renderedVideoCaption);
        }
        if (renderedVideoCategoryId != null && !renderedVideoCategoryId.isEmpty()) {
            request.setVideoCategoryId(renderedVideoCategoryId);
        }
        if (renderedVideoDefinition != null && !renderedVideoDefinition.isEmpty()) {
            request.setVideoDefinition(renderedVideoDefinition);
        }
        if (renderedVideoDimension != null && !renderedVideoDimension.isEmpty()) {
            request.setVideoDimension(renderedVideoDimension);
        }
        if (renderedVideoDuration != null && !renderedVideoDuration.isEmpty()) {
            request.setVideoDuration(renderedVideoDuration);
        }
        if (renderedVideoEmbeddable != null && !renderedVideoEmbeddable.isEmpty()) {
            request.setVideoEmbeddable(renderedVideoEmbeddable);
        }
        if (renderedVideoLicense != null && !renderedVideoLicense.isEmpty()) {
            request.setVideoLicense(renderedVideoLicense);
        }
        if (renderedVideoPaidProductPlacement != null && !renderedVideoPaidProductPlacement.isEmpty()) {
            request.setVideoPaidProductPlacement(renderedVideoPaidProductPlacement);
        }
        if (renderedVideoSyndicated != null && !renderedVideoSyndicated.isEmpty()) {
            request.setVideoSyndicated(renderedVideoSyndicated);
        }
        if (renderedVideoType != null && !renderedVideoType.isEmpty()) {
            request.setVideoType(renderedVideoType);
        }
        if (renderedEventType != null && !renderedEventType.isEmpty()) {
            request.setEventType(renderedEventType);
        }

        // Channel-specific filters
        if (renderedChannelType != null && !renderedChannelType.isEmpty()) {
            request.setChannelType(renderedChannelType);
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
        List<SearchItem> items = new ArrayList<>();
        int videoCount = 0;
        int channelCount = 0;
        int playlistCount = 0;

        for (SearchResult result : searchResults) {
            String kind = result.getId().getKind();

            // Build the Id object
            SearchResultId.SearchResultIdBuilder idBuilder = SearchResultId.builder().kind(kind);

            String generatedUrl = null;
            switch (kind) {
                case "youtube#video":
                    String videoId = result.getId().getVideoId();
                    idBuilder.videoId(videoId);
                    generatedUrl = "https://www.youtube.com/watch?v=" + videoId;
                    videoCount++;
                    break;
                case "youtube#channel":
                    String channelId = result.getId().getChannelId();
                    idBuilder.channelId(channelId);
                    generatedUrl = "https://www.youtube.com/channel/" + channelId;
                    channelCount++;
                    break;
                case "youtube#playlist":
                    String playlistId = result.getId().getPlaylistId();
                    idBuilder.playlistId(playlistId);
                    generatedUrl = "https://www.youtube.com/playlist?list=" + playlistId;
                    playlistCount++;
                    break;
            }

            // Build the Snippet object
            SearchResultSnippet snippet = SearchResultSnippet.builder()
                    .publishedAt(result.getSnippet().getPublishedAt() != null
                            ? Instant.ofEpochMilli(
                                    result.getSnippet().getPublishedAt().getValue())
                            : null)
                    .channelId(result.getSnippet().getChannelId())
                    .title(result.getSnippet().getTitle())
                    .description(result.getSnippet().getDescription())
                    .thumbnails(result.getSnippet().getThumbnails() != null
                            ? buildThumbnails(result.getSnippet().getThumbnails())
                            : null)
                    .channelTitle(result.getSnippet().getChannelTitle())
                    .liveBroadcastContent(result.getSnippet().getLiveBroadcastContent())
                    .build();

            // Build the Item
            SearchItem item = SearchItem.builder()
                    .kind(kind)
                    .etag(result.getEtag())
                    .id(idBuilder.build())
                    .snippet(snippet)
                    .url(generatedUrl)
                    .build();

            items.add(item);
        }

        // Build PageInfo
        PageInfo pageInfo = null;
        if (response.getPageInfo() != null) {
            pageInfo = PageInfo.builder()
                    .totalResults(response.getPageInfo().getTotalResults())
                    .resultsPerPage(response.getPageInfo().getResultsPerPage())
                    .build();
        }

        return SearchOutput.builder()
                .kind("youtube#searchListResponse")
                .etag(response.getEtag())
                .nextPageToken(response.getNextPageToken())
                .prevPageToken(response.getPrevPageToken())
                .regionCode(response.getRegionCode())
                .pageInfo(pageInfo)
                .items(items)
                .videoCount(videoCount)
                .channelCount(channelCount)
                .playlistCount(playlistCount)
                .build();
    }

    private Thumbnails buildThumbnails(com.google.api.services.youtube.model.ThumbnailDetails apiThumbnails) {
        Thumbnails.ThumbnailsBuilder builder = Thumbnails.builder();

        if (apiThumbnails.getDefault() != null) {
            builder.defaultThumbnail(buildThumbnail(apiThumbnails.getDefault()));
        }
        if (apiThumbnails.getMedium() != null) {
            builder.medium(buildThumbnail(apiThumbnails.getMedium()));
        }
        if (apiThumbnails.getHigh() != null) {
            builder.high(buildThumbnail(apiThumbnails.getHigh()));
        }
        if (apiThumbnails.getStandard() != null) {
            builder.standard(buildThumbnail(apiThumbnails.getStandard()));
        }
        if (apiThumbnails.getMaxres() != null) {
            builder.maxres(buildThumbnail(apiThumbnails.getMaxres()));
        }

        return builder.build();
    }

    private Thumbnail buildThumbnail(com.google.api.services.youtube.model.Thumbnail apiThumbnail) {
        return Thumbnail.builder()
                .url(apiThumbnail.getUrl())
                .width(apiThumbnail.getWidth() != null ? apiThumbnail.getWidth().intValue() : null)
                .height(apiThumbnail.getHeight() != null ? apiThumbnail.getHeight().intValue() : null)
                .build();
    }
}
