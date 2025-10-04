package io.kestra.plugin.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@KestraTest
@MicronautTest
class SearchTest {

        @Inject
        private RunContextFactory runContextFactory;

        private RunContext runContext;
        private YouTube mockYouTube;
        private YouTube.Search mockSearch;
        private YouTube.Search.List mockSearchList;

        @BeforeEach
        void setUp() throws IOException {
                runContext = runContextFactory.of();

                // Create mocks
                mockYouTube = mock(YouTube.class);
                mockSearch = mock(YouTube.Search.class);
                mockSearchList = mock(YouTube.Search.List.class);

                // Setup mock chain
                when(mockYouTube.search()).thenReturn(mockSearch);
                when(mockSearch.list(any())).thenReturn(mockSearchList);

                // Setup fluent interface for mockSearchList
                when(mockSearchList.setType(any())).thenReturn(mockSearchList);
                when(mockSearchList.setMaxResults(any())).thenReturn(mockSearchList);
                when(mockSearchList.setOrder(any())).thenReturn(mockSearchList);
                when(mockSearchList.setQ(any())).thenReturn(mockSearchList);
                when(mockSearchList.setChannelId(any())).thenReturn(mockSearchList);
                when(mockSearchList.setPublishedAfter(any())).thenReturn(mockSearchList);
                when(mockSearchList.setPublishedBefore(any())).thenReturn(mockSearchList);
                when(mockSearchList.setRegionCode(any())).thenReturn(mockSearchList);
                when(mockSearchList.setRelevanceLanguage(any())).thenReturn(mockSearchList);
                when(mockSearchList.setSafeSearch(any())).thenReturn(mockSearchList);
                when(mockSearchList.setTopicId(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoCaption(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoCategoryId(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoDefinition(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoDimension(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoDuration(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoEmbeddable(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoLicense(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoPaidProductPlacement(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoSyndicated(any())).thenReturn(mockSearchList);
                when(mockSearchList.setVideoType(any())).thenReturn(mockSearchList);
                when(mockSearchList.setEventType(any())).thenReturn(mockSearchList);
                when(mockSearchList.setChannelType(any())).thenReturn(mockSearchList);
                when(mockSearchList.setForMine(any())).thenReturn(mockSearchList);
                when(mockSearchList.setPageToken(any())).thenReturn(mockSearchList);
        }

        @Test
        void shouldSearchAllResourceTypes() throws Exception {
                // Given
                SearchListResponse mockResponse = new SearchListResponse();
                List<SearchResult> results = List.of(
                                createVideoResult("vid1", "Video Title", "Video Description"),
                                createChannelResult("chan1", "Channel Title", "Channel Description"),
                                createPlaylistResult("playlist1", "Playlist Title", "Playlist Description"));
                mockResponse.setItems(results);
                mockResponse.setPageInfo(createPageInfo(3));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .query(Property.ofValue("test query"))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getItems(), hasSize(3));
                assertThat(output.getTotalResults(), is(3));
                assertThat(output.getVideoCount(), is(1));
                assertThat(output.getChannelCount(), is(1));
                assertThat(output.getPlaylistCount(), is(1));

                // Verify video result
                Search.SearchResultItem videoItem = output.getItems().stream()
                                .filter(item -> "youtube#video".equals(item.getKind()))
                                .findFirst().orElse(null);
                assertNotNull(videoItem);
                assertThat(videoItem.getVideoId(), is("vid1"));
                assertThat(videoItem.getUrl(), is("https://www.youtube.com/watch?v=vid1"));

                // Verify channel result
                Search.SearchResultItem channelItem = output.getItems().stream()
                                .filter(item -> "youtube#channel".equals(item.getKind()))
                                .findFirst().orElse(null);
                assertNotNull(channelItem);
                assertThat(channelItem.getResourceChannelId(), is("chan1"));
                assertThat(channelItem.getUrl(), is("https://www.youtube.com/channel/chan1"));

                // Verify playlist result
                Search.SearchResultItem playlistItem = output.getItems().stream()
                                .filter(item -> "youtube#playlist".equals(item.getKind()))
                                .findFirst().orElse(null);
                assertNotNull(playlistItem);
                assertThat(playlistItem.getPlaylistId(), is("playlist1"));
                assertThat(playlistItem.getUrl(), is("https://www.youtube.com/playlist?list=playlist1"));
        }

        @Test
        void shouldSearchVideosOnly() throws Exception {
                // Given
                SearchListResponse mockResponse = new SearchListResponse();
                List<SearchResult> results = List.of(
                                createVideoResult("vid1", "Video 1", "Description 1"),
                                createVideoResult("vid2", "Video 2", "Description 2"));
                mockResponse.setItems(results);
                mockResponse.setPageInfo(createPageInfo(2));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .query(Property.ofValue("tutorials"))
                                .resourceType(Property.ofValue(List.of(Search.ResourceType.VIDEO)))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getItems(), hasSize(2));
                assertThat(output.getVideoCount(), is(2));
                assertThat(output.getChannelCount(), is(0));
                assertThat(output.getPlaylistCount(), is(0));

                verify(mockSearchList).setType(Collections.singletonList("video"));
        }

        @Test
        void shouldSearchChannelsOnly() throws Exception {
                // Given
                SearchListResponse mockResponse = new SearchListResponse();
                List<SearchResult> results = List.of(
                                createChannelResult("chan1", "Tech Channel", "Tech content"),
                                createChannelResult("chan2", "News Channel", "News content"));
                mockResponse.setItems(results);
                mockResponse.setPageInfo(createPageInfo(2));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .query(Property.ofValue("technology"))
                                .resourceType(Property.ofValue(List.of(Search.ResourceType.CHANNEL)))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getItems(), hasSize(2));
                assertThat(output.getVideoCount(), is(0));
                assertThat(output.getChannelCount(), is(2));
                assertThat(output.getPlaylistCount(), is(0));

                verify(mockSearchList).setType(Collections.singletonList("channel"));
        }

        @Test
        void shouldSearchPlaylistsOnly() throws Exception {
                // Given
                SearchListResponse mockResponse = new SearchListResponse();
                List<SearchResult> results = List.of(
                                createPlaylistResult("pl1", "Workout Playlist", "Exercise videos"),
                                createPlaylistResult("pl2", "Cooking Playlist", "Recipe videos"));
                mockResponse.setItems(results);
                mockResponse.setPageInfo(createPageInfo(2));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .query(Property.ofValue("workout"))
                                .resourceType(Property.ofValue(List.of(Search.ResourceType.PLAYLIST)))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getItems(), hasSize(2));
                assertThat(output.getVideoCount(), is(0));
                assertThat(output.getChannelCount(), is(0));
                assertThat(output.getPlaylistCount(), is(2));

                verify(mockSearchList).setType(Collections.singletonList("playlist"));
        }

        @Test
        void shouldApplyCommonFilters() throws Exception {
                // Given
                String query = "machine learning";
                String channelId = "UC_test_channel";
                String publishedAfter = "2024-01-01T00:00:00Z";
                String publishedBefore = "2024-12-31T23:59:59Z";
                String regionCode = "US";
                String relevanceLanguage = "en";

                SearchListResponse mockResponse = new SearchListResponse();
                mockResponse.setItems(List.of(createVideoResult("vid1", "Title", "Description")));
                mockResponse.setPageInfo(createPageInfo(1));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .query(Property.ofValue(query))
                                .channelId(Property.ofValue(channelId))
                                .publishedAfter(Property.ofValue(publishedAfter))
                                .publishedBefore(Property.ofValue(publishedBefore))
                                .regionCode(Property.ofValue(regionCode))
                                .relevanceLanguage(Property.ofValue(relevanceLanguage))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                verify(mockSearchList).setQ(query);
                verify(mockSearchList).setChannelId(channelId);
                verify(mockSearchList).setPublishedAfter(publishedAfter);
                verify(mockSearchList).setPublishedBefore(publishedBefore);
                verify(mockSearchList).setRegionCode(regionCode);
                verify(mockSearchList).setRelevanceLanguage(relevanceLanguage);
        }

        @Test
        void shouldApplyVideoSpecificFilters() throws Exception {
                // Given
                SearchListResponse mockResponse = new SearchListResponse();
                mockResponse.setItems(List.of(createVideoResult("vid1", "Title", "Description")));
                mockResponse.setPageInfo(createPageInfo(1));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .resourceType(Property.ofValue(List.of(Search.ResourceType.VIDEO)))
                                .videoDuration(Property.ofValue(Search.VideoDuration.MEDIUM))
                                .videoDefinition(Property.ofValue(Search.VideoDefinition.HIGH))
                                .videoDimension(Property.ofValue(Search.VideoDimension.TWO_D))
                                .videoLicense(Property.ofValue(Search.VideoLicense.CREATIVE_COMMON))
                                .videoCaption(Property.ofValue(Search.VideoCaption.CLOSED_CAPTION))
                                .videoCategoryId(Property.ofValue("28"))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                verify(mockSearchList).setVideoDuration("medium");
                verify(mockSearchList).setVideoDefinition("high");
                verify(mockSearchList).setVideoDimension("2d");
                verify(mockSearchList).setVideoLicense("creativeCommon");
                verify(mockSearchList).setVideoCaption("closedCaption");
                verify(mockSearchList).setVideoCategoryId("28");
        }

        @Test
        void shouldApplyOrderAndPagination() throws Exception {
                // Given
                String pageToken = "NEXT_PAGE_TOKEN";
                String nextToken = "ANOTHER_PAGE_TOKEN";

                SearchListResponse mockResponse = new SearchListResponse();
                mockResponse.setItems(List.of(createVideoResult("vid1", "Title", "Description")));
                mockResponse.setNextPageToken(nextToken);
                mockResponse.setPrevPageToken("PREV_TOKEN");
                mockResponse.setPageInfo(createPageInfo(100));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .query(Property.ofValue("test"))
                                .order(Property.ofValue(Search.Order.VIEW_COUNT))
                                .maxResults(Property.ofValue(50))
                                .pageToken(Property.ofValue(pageToken))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                verify(mockSearchList).setOrder("viewcount");
                verify(mockSearchList).setMaxResults(50L);
                verify(mockSearchList).setPageToken(pageToken);
                assertThat(output.getNextPageToken(), is(nextToken));
                assertThat(output.getPrevPageToken(), is("PREV_TOKEN"));
                assertThat(output.getTotalAvailable(), is(100));
        }

        @Test
        void shouldHandleEmptyResults() throws Exception {
                // Given
                SearchListResponse mockResponse = new SearchListResponse();
                mockResponse.setItems(Collections.emptyList());
                mockResponse.setPageInfo(createPageInfo(0));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .query(Property.ofValue("nonexistent query"))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getItems(), hasSize(0));
                assertThat(output.getTotalResults(), is(0));
                assertThat(output.getVideoCount(), is(0));
                assertThat(output.getChannelCount(), is(0));
                assertThat(output.getPlaylistCount(), is(0));
        }

        @Test
        void shouldSearchMultipleResourceTypes() throws Exception {
                // Given
                SearchListResponse mockResponse = new SearchListResponse();
                List<SearchResult> results = List.of(
                                createVideoResult("vid1", "Video", "Description"),
                                createChannelResult("chan1", "Channel", "Description"));
                mockResponse.setItems(results);
                mockResponse.setPageInfo(createPageInfo(2));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .query(Property.ofValue("test"))
                                .resourceType(Property.ofValue(
                                                List.of(Search.ResourceType.VIDEO, Search.ResourceType.CHANNEL)))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getVideoCount(), is(1));
                assertThat(output.getChannelCount(), is(1));
                assertThat(output.getPlaylistCount(), is(0));

                verify(mockSearchList).setType(Collections.singletonList("video,channel"));
        }

        @Test
        void shouldUseDefaultValues() throws Exception {
                // Given
                SearchListResponse mockResponse = new SearchListResponse();
                mockResponse.setItems(List.of(createVideoResult("vid1", "Title", "Description")));
                mockResponse.setPageInfo(createPageInfo(1));
                when(mockSearchList.execute()).thenReturn(mockResponse);

                Search task = Search.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .build();

                Search spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                Search.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                verify(mockSearchList).setMaxResults(25L);
                verify(mockSearchList).setOrder("relevance");
        }

        // Helper methods
        private SearchResult createVideoResult(String videoId, String title, String description) {
                SearchResult result = new SearchResult();

                ResourceId resourceId = new ResourceId();
                resourceId.setKind("youtube#video");
                resourceId.setVideoId(videoId);
                result.setId(resourceId);

                result.setSnippet(createSnippet(title, description));
                return result;
        }

        private SearchResult createChannelResult(String channelId, String title, String description) {
                SearchResult result = new SearchResult();

                ResourceId resourceId = new ResourceId();
                resourceId.setKind("youtube#channel");
                resourceId.setChannelId(channelId);
                result.setId(resourceId);

                result.setSnippet(createSnippet(title, description));
                return result;
        }

        private SearchResult createPlaylistResult(String playlistId, String title, String description) {
                SearchResult result = new SearchResult();

                ResourceId resourceId = new ResourceId();
                resourceId.setKind("youtube#playlist");
                resourceId.setPlaylistId(playlistId);
                result.setId(resourceId);

                result.setSnippet(createSnippet(title, description));
                return result;
        }

        private SearchResultSnippet createSnippet(String title, String description) {
                SearchResultSnippet snippet = new SearchResultSnippet();
                snippet.setTitle(title);
                snippet.setDescription(description);
                snippet.setChannelId("UC_test_channel");
                snippet.setChannelTitle("Test Channel");
                snippet.setPublishedAt(new com.google.api.client.util.DateTime(System.currentTimeMillis()));

                ThumbnailDetails thumbnails = new ThumbnailDetails();
                Thumbnail defaultThumbnail = new Thumbnail();
                defaultThumbnail.setUrl("https://i.ytimg.com/vi/test/default.jpg");
                thumbnails.setDefault(defaultThumbnail);
                snippet.setThumbnails(thumbnails);

                return snippet;
        }

        private PageInfo createPageInfo(int totalResults) {
                PageInfo pageInfo = new PageInfo();
                pageInfo.setTotalResults(totalResults);
                return pageInfo;
        }
}
