package io.kestra.plugin.youtube;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ThumbnailSetResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.youtube.helpers.PropertyHelper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Upload and set a custom thumbnail for a YouTube video", description = "Uploads a custom thumbnail image to YouTube and sets it for a specified video. "
        +
        "Maximum file size: 2MB. Accepted formats: JPEG, PNG.")
@Plugin(examples = {
        @Example(title = "Set a custom thumbnail for a video", full = true, code = """
                id: set_video_thumbnail
                namespace: company.team

                tasks:
                  - id: authenticate
                    type: io.kestra.plugin.youtube.OAuth2
                    clientId: "{{ secret('YOUTUBE_CLIENT_ID') }}"
                    clientSecret: "{{ secret('YOUTUBE_CLIENT_SECRET') }}"
                    refreshToken: "{{ secret('YOUTUBE_REFRESH_TOKEN') }}"

                  - id: set_thumbnail
                    type: io.kestra.plugin.youtube.SetThumbnail
                    accessToken: "{{ outputs.authenticate.accessToken }}"
                    videoId: "dQw4w9WgXcQ"
                    thumbnailData: "{{ inputs.thumbnail_base64 }}"
                """),
        @Example(title = "Set thumbnail with specific MIME type", code = """
                  - id: set_custom_thumbnail
                    type: io.kestra.plugin.youtube.SetThumbnail
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    videoId: "video123"
                    thumbnailData: "{{ inputs.image_base64 }}"
                    mimeType: "image/png"
                """),
        @Example(title = "Set thumbnail from HTTP download", code = """
                  - id: download_image
                    type: io.kestra.plugin.http.Download
                    uri: "https://example.com/thumbnail.jpg"

                  - id: set_thumbnail
                    type: io.kestra.plugin.youtube.SetThumbnail
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    videoId: "video123"
                    thumbnailData: "{{ outputs.download_image.body }}"
                """)
})
public class SetThumbnail extends AbstractYoutubeTask implements RunnableTask<SetThumbnail.Output> {

    @Schema(title = "Video ID", description = "The YouTube video ID for which the custom thumbnail is being set")
    @NotNull
    private Property<String> videoId;

    @Schema(title = "Thumbnail data", description = "The thumbnail image data as a Base64-encoded string (JPEG or PNG). Maximum size: 2MB.")
    @NotNull
    private Property<String> thumbnailData;

    @Schema(title = "MIME type", description = "The MIME type of the thumbnail image. If not specified, it will be auto-detected from the image data.")
    @Builder.Default
    private Property<String> mimeType = Property.ofValue("application/octet-stream");

    @Override
    public Output run(RunContext runContext) throws Exception {
        YouTube youtube = createYoutubeService(runContext);

        // Render parameters
        String renderedVideoId = runContext.render(this.videoId).as(String.class).orElseThrow(
                () -> new IllegalArgumentException("videoId is required"));
        String renderedThumbnailData = runContext.render(this.thumbnailData).as(String.class).orElseThrow(
                () -> new IllegalArgumentException("thumbnailData is required"));
        String renderedMimeType = PropertyHelper.safeRender(runContext, this.mimeType, "application/octet-stream",
                String.class);
        renderedMimeType = renderedMimeType.isEmpty()
                ? "application/octet-stream"
                : renderedMimeType;

        // Decode Base64 data
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(renderedThumbnailData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 thumbnail data: " + e.getMessage());
        }

        // Auto-detect MIME type if not explicitly set or is the default
        if (renderedMimeType.equals("application/octet-stream")) {
            renderedMimeType = detectMimeTypeFromBytes(imageBytes);
        }

        // Validate file size (2MB limit)
        if (imageBytes.length > 2 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "Thumbnail file size exceeds 2MB limit. Current size: " + imageBytes.length + " bytes");
        }

        // Create input stream from byte array
        InputStream thumbnailStream = new ByteArrayInputStream(imageBytes);

        // Create the media content
        InputStreamContent mediaContent = new InputStreamContent(renderedMimeType, thumbnailStream);

        // Build the thumbnails.set request
        YouTube.Thumbnails.Set thumbnailSet = youtube.thumbnails().set(renderedVideoId, mediaContent);

        // Execute the request
        ThumbnailSetResponse response = thumbnailSet.execute();

        // Convert response to output - extract all thumbnail variants
        List<ThumbnailData> thumbnails = new ArrayList<>();
        if (response.getItems() != null) {
            for (com.google.api.services.youtube.model.ThumbnailDetails thumbnailDetails : response.getItems()) {
                // YouTube API returns ThumbnailDetails which contains multiple thumbnail sizes
                // Extract each non-null thumbnail
                if (thumbnailDetails.getDefault() != null) {
                    thumbnails.add(buildThumbnailData(thumbnailDetails.getDefault()));
                }
                if (thumbnailDetails.getMedium() != null) {
                    thumbnails.add(buildThumbnailData(thumbnailDetails.getMedium()));
                }
                if (thumbnailDetails.getHigh() != null) {
                    thumbnails.add(buildThumbnailData(thumbnailDetails.getHigh()));
                }
                if (thumbnailDetails.getStandard() != null) {
                    thumbnails.add(buildThumbnailData(thumbnailDetails.getStandard()));
                }
                if (thumbnailDetails.getMaxres() != null) {
                    thumbnails.add(buildThumbnailData(thumbnailDetails.getMaxres()));
                }
            }
        }

        runContext.logger().info("Successfully set thumbnail for video: {} (size: {} bytes)", renderedVideoId,
                imageBytes.length);

        return Output.builder()
                .kind(response.getKind())
                .etag(response.getEtag())
                .items(thumbnails)
                .videoId(renderedVideoId)
                .build();
    }

    /**
     * Detect MIME type from image byte data by examining file headers
     */
    private String detectMimeTypeFromBytes(byte[] imageData) {
        if (imageData.length < 4) {
            return "application/octet-stream";
        }

        // Check for PNG signature: 89 50 4E 47
        if (imageData[0] == (byte) 0x89 && imageData[1] == 0x50 && imageData[2] == 0x4E && imageData[3] == 0x47) {
            return "image/png";
        }

        // Check for JPEG signature: FF D8 FF
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8 && imageData[2] == (byte) 0xFF) {
            return "image/jpeg";
        }

        // Default fallback
        return "application/octet-stream";
    }

    private ThumbnailData buildThumbnailData(com.google.api.services.youtube.model.Thumbnail thumbnail) {
        return ThumbnailData.builder()
                .url(thumbnail.getUrl())
                .width(thumbnail.getWidth() != null ? thumbnail.getWidth().intValue() : null)
                .height(thumbnail.getHeight() != null ? thumbnail.getHeight().intValue() : null)
                .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "Resource type", description = "Identifies the API resource's type (youtube#thumbnailSetResponse)")
        private final String kind;

        @Schema(title = "Etag", description = "The ETag for this resource")
        private final String etag;

        @Schema(title = "Thumbnails", description = "List of thumbnail resources that were set")
        private final List<ThumbnailData> items;

        @Schema(title = "Video ID", description = "The ID of the video for which the thumbnail was set")
        private final String videoId;
    }

    @Builder
    @Getter
    public static class ThumbnailData {

        @Schema(title = "URL", description = "The thumbnail image's URL")
        private final String url;

        @Schema(title = "Width", description = "The thumbnail's width in pixels")
        private final Integer width;

        @Schema(title = "Height", description = "The thumbnail's height in pixels")
        private final Integer height;
    }
}
