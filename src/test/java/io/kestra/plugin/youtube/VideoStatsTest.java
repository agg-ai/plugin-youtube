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
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@KestraTest
@MicronautTest
class VideoStatsTest {

    @Inject
    private RunContextFactory runContextFactory;

    private RunContext runContext;
    private YouTube mockYouTube;
    private YouTube.Videos mockVideos;
    private YouTube.Videos.List mockVideosList;

    @BeforeEach
    void setUp() throws IOException {
        runContext = runContextFactory.of();

        // Create mocks
        mockYouTube = mock(YouTube.class);
        mockVideos = mock(YouTube.Videos.class);
        mockVideosList = mock(YouTube.Videos.List.class);

        // Setup mock chain
        when(mockYouTube.videos()).thenReturn(mockVideos);
        when(mockVideos.list(any())).thenReturn(mockVideosList);

        // Setup fluent interface for mockVideosList
        when(mockVideosList.setId(any())).thenReturn(mockVideosList);
        when(mockVideosList.setMaxResults(any())).thenReturn(mockVideosList);
    }

    @Test
    void shouldGetVideoStatsSuccessfully() throws Exception {
        // Given
        String videoId = "test_video_id";
        BigInteger viewCount = BigInteger.valueOf(1000);
        BigInteger likeCount = BigInteger.valueOf(100);
        BigInteger commentCount = BigInteger.valueOf(50);

        VideoListResponse mockResponse = createMockVideoResponse(videoId, viewCount, likeCount, commentCount);
        when(mockVideosList.execute()).thenReturn(mockResponse);

        VideoStats task = VideoStats.builder()
                .accessToken(Property.ofValue("test_access_token"))
                .videoIds(Property.ofValue(List.of(videoId)))
                .build();

        VideoStats spyTask = spy(task);
        doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

        // When
        VideoStats.Output output = spyTask.run(runContext);

        // Then
        assertNotNull(output);
        assertThat(output.getVideos(), hasSize(1));
        assertThat(output.getTotalVideos(), is(1));
        assertThat(output.getTotalViews(), is(viewCount));
        assertThat(output.getTotalLikes(), is(likeCount));
        assertThat(output.getTotalComments(), is(commentCount));

        VideoStats.VideoStatsData videoData = output.getVideos().get(0);
        assertThat(videoData.getVideoId(), is(videoId));
        assertThat(videoData.getViewCount(), is(viewCount));
        assertThat(videoData.getLikeCount(), is(likeCount));
        assertThat(videoData.getCommentCount(), is(commentCount));
    }

    @Test
    void shouldGetStatsForMultipleVideos() throws Exception {
        // Given
        List<String> videoIds = List.of("vid1", "vid2", "vid3");

        VideoListResponse mockResponse = new VideoListResponse();
        List<Video> videos = List.of(
                createVideo("vid1", BigInteger.valueOf(1000), BigInteger.valueOf(100), BigInteger.valueOf(50)),
                createVideo("vid2", BigInteger.valueOf(2000), BigInteger.valueOf(200), BigInteger.valueOf(100)),
                createVideo("vid3", BigInteger.valueOf(3000), BigInteger.valueOf(300), BigInteger.valueOf(150)));
        mockResponse.setItems(videos);

        when(mockVideosList.execute()).thenReturn(mockResponse);

        VideoStats task = VideoStats.builder()
                .accessToken(Property.ofValue("test_access_token"))
                .videoIds(Property.ofValue(videoIds))
                .build();

        VideoStats spyTask = spy(task);
        doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

        // When
        VideoStats.Output output = spyTask.run(runContext);

        // Then
        assertNotNull(output);
        assertThat(output.getVideos(), hasSize(3));
        assertThat(output.getTotalVideos(), is(3));
        assertThat(output.getTotalViews(), is(BigInteger.valueOf(6000)));
        assertThat(output.getTotalLikes(), is(BigInteger.valueOf(600)));
        assertThat(output.getTotalComments(), is(BigInteger.valueOf(300)));
    }

    @Test
    void shouldIncludeSnippetWhenRequested() throws Exception {
        // Given
        String videoId = "test_video_id";
        String videoTitle = "Test Video Title";
        String videoDescription = "Test video description";
        String channelId = "test_channel_id";
        String channelTitle = "Test Channel";

        VideoListResponse mockResponse = createMockVideoResponseWithSnippet(
                videoId,
                BigInteger.valueOf(1000),
                BigInteger.valueOf(100),
                BigInteger.valueOf(50),
                videoTitle,
                videoDescription,
                channelId,
                channelTitle);
        when(mockVideosList.execute()).thenReturn(mockResponse);

        VideoStats task = VideoStats.builder()
                .accessToken(Property.ofValue("test_access_token"))
                .videoIds(Property.ofValue(List.of(videoId)))
                .includeSnippet(Property.ofValue(true))
                .build();

        VideoStats spyTask = spy(task);
        doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

        // When
        VideoStats.Output output = spyTask.run(runContext);

        // Then
        assertNotNull(output);
        VideoStats.VideoStatsData videoData = output.getVideos().get(0);
        assertThat(videoData.getTitle(), is(videoTitle));
        assertThat(videoData.getDescription(), is(videoDescription));
        assertThat(videoData.getChannelId(), is(channelId));
        assertThat(videoData.getChannelTitle(), is(channelTitle));
        assertNotNull(videoData.getThumbnailUrl());
        assertNotNull(videoData.getPublishedAt());
    }

    @Test
    void shouldIncludeContentDetailsWhenRequested() throws Exception {
        // Given
        String videoId = "test_video_id";
        String duration = "PT5M30S";
        String dimension = "2d";
        String definition = "hd";

        VideoListResponse mockResponse = createMockVideoResponseWithContentDetails(
                videoId,
                BigInteger.valueOf(1000),
                BigInteger.valueOf(100),
                BigInteger.valueOf(50),
                duration,
                dimension,
                definition);
        when(mockVideosList.execute()).thenReturn(mockResponse);

        VideoStats task = VideoStats.builder()
                .accessToken(Property.ofValue("test_access_token"))
                .videoIds(Property.ofValue(List.of(videoId)))
                .includeContentDetails(Property.ofValue(true))
                .build();

        VideoStats spyTask = spy(task);
        doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

        // When
        VideoStats.Output output = spyTask.run(runContext);

        // Then
        assertNotNull(output);
        VideoStats.VideoStatsData videoData = output.getVideos().get(0);
        assertThat(videoData.getDuration(), is(duration));
        assertThat(videoData.getDimension(), is(dimension));
        assertThat(videoData.getDefinition(), is(definition));
    }

    @Test
    void shouldHandleNullStatistics() throws Exception {
        // Given
        String videoId = "test_video_id";

        VideoListResponse mockResponse = new VideoListResponse();
        Video video = new Video();
        video.setId(videoId);
        video.setStatistics(null); // No statistics available
        mockResponse.setItems(List.of(video));

        when(mockVideosList.execute()).thenReturn(mockResponse);

        VideoStats task = VideoStats.builder()
                .accessToken(Property.ofValue("test_access_token"))
                .videoIds(Property.ofValue(List.of(videoId)))
                .build();

        VideoStats spyTask = spy(task);
        doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

        // When
        VideoStats.Output output = spyTask.run(runContext);

        // Then
        assertNotNull(output);
        assertThat(output.getVideos(), hasSize(1));
        VideoStats.VideoStatsData videoData = output.getVideos().get(0);
        assertThat(videoData.getVideoId(), is(videoId));
        assertNull(videoData.getViewCount());
        assertNull(videoData.getLikeCount());
        assertNull(videoData.getCommentCount());
    }

    @Test
    void shouldHandleEmptyResponse() throws Exception {
        // Given
        VideoListResponse mockResponse = new VideoListResponse();
        mockResponse.setItems(Collections.emptyList());

        when(mockVideosList.execute()).thenReturn(mockResponse);

        VideoStats task = VideoStats.builder()
                .accessToken(Property.ofValue("test_access_token"))
                .videoIds(Property.ofValue(List.of("non_existent_video")))
                .build();

        VideoStats spyTask = spy(task);
        doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

        // When
        VideoStats.Output output = spyTask.run(runContext);

        // Then
        assertNotNull(output);
        assertThat(output.getVideos(), hasSize(0));
        assertThat(output.getTotalVideos(), is(0));
        assertThat(output.getTotalViews(), is(BigInteger.ZERO));
        assertThat(output.getTotalLikes(), is(BigInteger.ZERO));
        assertThat(output.getTotalComments(), is(BigInteger.ZERO));
    }

    @Test
    void shouldCalculateTotalsCorrectly() throws Exception {
        // Given
        VideoListResponse mockResponse = new VideoListResponse();
        List<Video> videos = List.of(
                createVideo("vid1", BigInteger.valueOf(1500), BigInteger.valueOf(150), BigInteger.valueOf(75)),
                createVideo("vid2", BigInteger.valueOf(2500), BigInteger.valueOf(250), BigInteger.valueOf(125)),
                createVideo("vid3", BigInteger.valueOf(500), null, BigInteger.valueOf(25)) // Missing like count
        );
        mockResponse.setItems(videos);

        when(mockVideosList.execute()).thenReturn(mockResponse);

        VideoStats task = VideoStats.builder()
                .accessToken(Property.ofValue("test_access_token"))
                .videoIds(Property.ofValue(List.of("vid1", "vid2", "vid3")))
                .build();

        VideoStats spyTask = spy(task);
        doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

        // When
        VideoStats.Output output = spyTask.run(runContext);

        // Then
        assertNotNull(output);
        assertThat(output.getTotalViews(), is(BigInteger.valueOf(4500)));
        assertThat(output.getTotalLikes(), is(BigInteger.valueOf(400))); // Only counts non-null values
        assertThat(output.getTotalComments(), is(BigInteger.valueOf(225)));
    }

    // Helper methods
    private VideoListResponse createMockVideoResponse(String videoId, BigInteger viewCount,
            BigInteger likeCount, BigInteger commentCount) {
        VideoListResponse response = new VideoListResponse();
        Video video = createVideo(videoId, viewCount, likeCount, commentCount);
        response.setItems(List.of(video));
        return response;
    }

    private Video createVideo(String videoId, BigInteger viewCount, BigInteger likeCount, BigInteger commentCount) {
        Video video = new Video();
        video.setId(videoId);

        VideoStatistics stats = new VideoStatistics();
        stats.setViewCount(viewCount);
        stats.setLikeCount(likeCount);
        stats.setCommentCount(commentCount);
        stats.setFavoriteCount(BigInteger.ZERO);

        video.setStatistics(stats);
        return video;
    }

    private VideoListResponse createMockVideoResponseWithSnippet(String videoId, BigInteger viewCount,
            BigInteger likeCount, BigInteger commentCount,
            String title, String description,
            String channelId, String channelTitle) {
        VideoListResponse response = new VideoListResponse();
        Video video = createVideo(videoId, viewCount, likeCount, commentCount);

        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(title);
        snippet.setDescription(description);
        snippet.setChannelId(channelId);
        snippet.setChannelTitle(channelTitle);
        snippet.setPublishedAt(new com.google.api.client.util.DateTime(System.currentTimeMillis()));

        ThumbnailDetails thumbnails = new ThumbnailDetails();
        Thumbnail defaultThumbnail = new Thumbnail();
        defaultThumbnail.setUrl("https://i.ytimg.com/vi/" + videoId + "/default.jpg");
        thumbnails.setDefault(defaultThumbnail);
        snippet.setThumbnails(thumbnails);

        video.setSnippet(snippet);
        response.setItems(List.of(video));
        return response;
    }

    private VideoListResponse createMockVideoResponseWithContentDetails(String videoId, BigInteger viewCount,
            BigInteger likeCount, BigInteger commentCount,
            String duration, String dimension, String definition) {
        VideoListResponse response = new VideoListResponse();
        Video video = createVideo(videoId, viewCount, likeCount, commentCount);

        VideoContentDetails contentDetails = new VideoContentDetails();
        contentDetails.setDuration(duration);
        contentDetails.setDimension(dimension);
        contentDetails.setDefinition(definition);

        video.setContentDetails(contentDetails);
        response.setItems(List.of(video));
        return response;
    }
}
