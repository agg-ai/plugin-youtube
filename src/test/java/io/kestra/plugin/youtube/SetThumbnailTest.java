package io.kestra.plugin.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.ThumbnailSetResponse;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.junit.annotations.KestraTest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@KestraTest
@MicronautTest
class SetThumbnailTest {

        @Inject
        private RunContextFactory runContextFactory;

        private RunContext runContext;
        private YouTube mockYouTube;
        private YouTube.Thumbnails mockThumbnails;
        private YouTube.Thumbnails.Set mockThumbnailSet;

        @BeforeEach
        void setUp() throws Exception {
                runContext = runContextFactory.of();

                // Mock YouTube service and its methods
                mockYouTube = mock(YouTube.class);
                mockThumbnails = mock(YouTube.Thumbnails.class);
                mockThumbnailSet = mock(YouTube.Thumbnails.Set.class);

                when(mockYouTube.thumbnails()).thenReturn(mockThumbnails);
                when(mockThumbnails.set(anyString(), any())).thenReturn(mockThumbnailSet);
                when(mockThumbnailSet.setOnBehalfOfContentOwner(anyString())).thenReturn(mockThumbnailSet);
        }

        // Helper method to create test thumbnail data as Base64
        private String createTestThumbnailDataBase64() throws Exception {
                InputStream thumbnailStream = Objects.requireNonNull(
                                getClass().getResourceAsStream("/thumbnails/test.png"),
                                "Test thumbnail file not found");
                byte[] imageBytes = thumbnailStream.readAllBytes();
                return Base64.getEncoder().encodeToString(imageBytes);
        }

        // Helper method to create JPEG test data as Base64
        private String createJpegTestDataBase64() {
                // Create minimal JPEG header bytes
                byte[] jpegBytes = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A,
                                0x46, 0x49, 0x46 };
                return Base64.getEncoder().encodeToString(jpegBytes);
        }

        @Test
        void shouldSetThumbnailSuccessfully() throws Exception {
                // Given
                String testData = createTestThumbnailDataBase64();
                ThumbnailSetResponse mockResponse = createMockResponse(
                                "test-video-id",
                                "youtube#thumbnailSetResponse",
                                "etag123");
                when(mockThumbnailSet.execute()).thenReturn(mockResponse);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("test-video-id"))
                                .thumbnailData(Property.ofValue(testData))
                                .build();

                SetThumbnail spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                SetThumbnail.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getKind(), is("youtube#thumbnailSetResponse"));
                assertThat(output.getEtag(), is("etag123"));
                assertThat(output.getVideoId(), is("test-video-id"));
                assertThat(output.getItems(), hasSize(3));

                // Verify the thumbnails.set was called
                verify(mockThumbnails).set(eq("test-video-id"), any());
                verify(mockThumbnailSet).execute();
        }

        @Test
        void shouldAutoDetectJpegMimeType() throws Exception {
                // Given
                String testData = createJpegTestDataBase64();
                ThumbnailSetResponse mockResponse = createMockResponse(
                                "video123",
                                "youtube#thumbnailSetResponse",
                                "etag456");
                when(mockThumbnailSet.execute()).thenReturn(mockResponse);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(testData))
                                .build();

                SetThumbnail spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                SetThumbnail.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                verify(mockThumbnailSet).execute();
        }

        @Test
        void shouldAutoDetectPngMimeType() throws Exception {
                // Given
                String testData = createTestThumbnailDataBase64();
                ThumbnailSetResponse mockResponse = createMockResponse(
                                "video123",
                                "youtube#thumbnailSetResponse",
                                "etag789");
                when(mockThumbnailSet.execute()).thenReturn(mockResponse);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(testData))
                                .build();

                SetThumbnail spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                SetThumbnail.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                verify(mockThumbnailSet).execute();
        }

        @Test
        void shouldUseExplicitMimeType() throws Exception {
                // Given
                String testData = createTestThumbnailDataBase64();
                ThumbnailSetResponse mockResponse = createMockResponse(
                                "video123",
                                "youtube#thumbnailSetResponse",
                                "etag999");
                when(mockThumbnailSet.execute()).thenReturn(mockResponse);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(testData))
                                .mimeType(Property.ofValue("image/jpeg"))
                                .build();

                SetThumbnail spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                SetThumbnail.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                verify(mockThumbnailSet).execute();
        }

        @Test
        void shouldNotSetOnBehalfOfContentOwnerWhenNull() throws Exception {
                // Given
                String testData = createTestThumbnailDataBase64();
                ThumbnailSetResponse mockResponse = createMockResponse(
                                "video123",
                                "youtube#thumbnailSetResponse",
                                "etag111");
                when(mockThumbnailSet.execute()).thenReturn(mockResponse);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(testData))
                                .build();

                SetThumbnail spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                SetThumbnail.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                verify(mockThumbnailSet, never()).setOnBehalfOfContentOwner(anyString());
                verify(mockThumbnailSet).execute();
        }

        @Test
        void shouldHandleEmptyThumbnailsList() throws Exception {
                // Given
                String testData = createTestThumbnailDataBase64();
                ThumbnailSetResponse mockResponse = new ThumbnailSetResponse();
                mockResponse.setKind("youtube#thumbnailSetResponse");
                mockResponse.setEtag("etag222");
                mockResponse.setItems(List.of()); // Empty list

                when(mockThumbnailSet.execute()).thenReturn(mockResponse);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(testData))
                                .build();

                SetThumbnail spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                SetThumbnail.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getItems(), empty());
        }

        @Test
        void shouldHandleNullThumbnailsList() throws Exception {
                // Given
                String testData = createTestThumbnailDataBase64();
                ThumbnailSetResponse mockResponse = new ThumbnailSetResponse();
                mockResponse.setKind("youtube#thumbnailSetResponse");
                mockResponse.setEtag("etag333");
                mockResponse.setItems(null); // Null list

                when(mockThumbnailSet.execute()).thenReturn(mockResponse);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(testData))
                                .build();

                SetThumbnail spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                SetThumbnail.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getItems(), empty());
        }

        @Test
        void shouldThrowExceptionWhenVideoIdIsNull() {
                // Given
                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .thumbnailData(Property.ofValue("dGVzdA==")) // "test" in Base64
                                .build();

                SetThumbnail spyTask = spy(task);

                // When/Then
                assertThrows(Exception.class, () -> spyTask.run(runContext));
        }

        @Test
        void shouldThrowExceptionWhenThumbnailDataIsNull() {
                // Given
                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .build();

                SetThumbnail spyTask = spy(task);

                // When/Then
                assertThrows(Exception.class, () -> spyTask.run(runContext));
        }

        @Test
        void shouldThrowExceptionWhenFileSizeExceedsLimit() {
                // Given - create Base64 data larger than 2MB
                byte[] largeData = new byte[2 * 1024 * 1024 + 1]; // 2MB + 1 byte
                String largeBase64 = Base64.getEncoder().encodeToString(largeData);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(largeBase64))
                                .build();

                SetThumbnail spyTask = spy(task);

                // When/Then
                Exception exception = assertThrows(Exception.class, () -> spyTask.run(runContext));
                assertThat(exception.getMessage(), containsString("exceeds 2MB limit"));
        }

        @Test
        void shouldThrowExceptionWhenInvalidBase64() {
                // Given - invalid Base64 string
                String invalidBase64 = "invalid-base64-data!@#";

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(invalidBase64))
                                .build();

                SetThumbnail spyTask = spy(task);

                // When/Then
                Exception exception = assertThrows(Exception.class, () -> spyTask.run(runContext));
                assertThat(exception.getMessage(), containsString("Invalid Base64"));
        }

        @Test
        void shouldHandleThumbnailsWithAllDimensions() throws Exception {
                // Given
                String testData = createTestThumbnailDataBase64();
                ThumbnailSetResponse mockResponse = new ThumbnailSetResponse();
                mockResponse.setKind("youtube#thumbnailSetResponse");
                mockResponse.setEtag("etag444");

                ThumbnailDetails thumbnailDetails = new ThumbnailDetails();
                thumbnailDetails.setDefault(createThumbnail("https://i.ytimg.com/default.jpg", 120L, 90L));
                thumbnailDetails.setMedium(createThumbnail("https://i.ytimg.com/medium.jpg", 320L, 180L));
                thumbnailDetails.setHigh(createThumbnail("https://i.ytimg.com/high.jpg", 480L, 360L));
                thumbnailDetails.setStandard(createThumbnail("https://i.ytimg.com/standard.jpg", 640L, 480L));
                thumbnailDetails.setMaxres(createThumbnail("https://i.ytimg.com/maxres.jpg", 1280L, 720L));

                mockResponse.setItems(List.of(thumbnailDetails));

                when(mockThumbnailSet.execute()).thenReturn(mockResponse);

                SetThumbnail task = SetThumbnail.builder()
                                .accessToken(Property.ofValue("test_access_token"))
                                .videoId(Property.ofValue("video123"))
                                .thumbnailData(Property.ofValue(testData))
                                .build();

                SetThumbnail spyTask = spy(task);
                doReturn(mockYouTube).when(spyTask).createYoutubeService(any(RunContext.class));

                // When
                SetThumbnail.Output output = spyTask.run(runContext);

                // Then
                assertNotNull(output);
                assertThat(output.getItems(), hasSize(5));

                // Verify all thumbnails have correct properties
                assertThat(output.getItems().get(0).getUrl(), is("https://i.ytimg.com/default.jpg"));
                assertThat(output.getItems().get(0).getWidth(), is(120));
                assertThat(output.getItems().get(0).getHeight(), is(90));

                assertThat(output.getItems().get(4).getUrl(), is("https://i.ytimg.com/maxres.jpg"));
                assertThat(output.getItems().get(4).getWidth(), is(1280));
                assertThat(output.getItems().get(4).getHeight(), is(720));
        }

        // Helper methods

        private ThumbnailSetResponse createMockResponse(String videoId, String kind, String etag) {
                ThumbnailSetResponse response = new ThumbnailSetResponse();
                response.setKind(kind);
                response.setEtag(etag);

                // YouTube API returns ThumbnailDetails which contains multiple thumbnail sizes
                ThumbnailDetails thumbnailDetails = new ThumbnailDetails();
                thumbnailDetails.setDefault(
                                createThumbnail("https://i.ytimg.com/vi/" + videoId + "/default.jpg", 120L, 90L));
                thumbnailDetails.setMedium(
                                createThumbnail("https://i.ytimg.com/vi/" + videoId + "/medium.jpg", 320L, 180L));
                thumbnailDetails.setHigh(
                                createThumbnail("https://i.ytimg.com/vi/" + videoId + "/high.jpg", 480L, 360L));

                response.setItems(List.of(thumbnailDetails));

                return response;
        }

        private Thumbnail createThumbnail(String url, Long width, Long height) {
                Thumbnail thumbnail = new Thumbnail();
                thumbnail.setUrl(url);
                thumbnail.setWidth(width);
                thumbnail.setHeight(height);
                return thumbnail;
        }
}
