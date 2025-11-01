package io.kestra.plugin.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
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

import java.util.*;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Update YouTube video metadata", description = "Updates a video's metadata including title, description, tags, privacy status, and other properties. "
        +
        "Requires the video ID and at least one field to update. Quota cost: 50 units per call.")
@Plugin(examples = {
        @Example(title = "Update video title and description", full = true, code = """
                id: update_video_metadata
                namespace: company.team

                tasks:
                  - id: authenticate
                    type: io.kestra.plugin.youtube.OAuth2
                    clientId: "{{ secret('YOUTUBE_CLIENT_ID') }}"
                    clientSecret: "{{ secret('YOUTUBE_CLIENT_SECRET') }}"
                    refreshToken: "{{ secret('YOUTUBE_REFRESH_TOKEN') }}"

                  - id: update_video
                    type: io.kestra.plugin.youtube.UpdateVideo
                    accessToken: "{{ outputs.authenticate.accessToken }}"
                    videoId: "dQw4w9WgXcQ"
                    snippetTitle: "Updated Video Title"
                    snippetDescription: "This is the updated description"
                    snippetTags:
                      - "tutorial"
                      - "programming"
                      - "youtube"
                """),
        @Example(title = "Update video title only", code = """
                  - id: update_title
                    type: io.kestra.plugin.youtube.UpdateVideo
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    videoId: "video123"
                    snippetTitle: "New Video Title"
                """),
        @Example(title = "Update video tags only", code = """
                  - id: update_tags
                    type: io.kestra.plugin.youtube.UpdateVideo
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    videoId: "video123"
                    snippetTags:
                      - "vlog"
                      - "daily life"
                      - "personal"
                """)
})
public class UpdateVideo extends AbstractYoutubeTask implements RunnableTask<UpdateVideo.Output> {

    @Schema(title = "Video ID", description = "The YouTube video ID to update")
    @NotNull
    private Property<String> videoId;

    @Schema(title = "Snippet Title", description = "The video's title.")
    private Property<String> snippetTitle;

    @Schema(title = "Snippet Description", description = "The video's description")
    private Property<String> snippetDescription;

    @Schema(title = "Snippet Tags", description = "A list of keyword tags associated with the video. Set empty array to remove all tags. Don't specify tags to keep existing tags.")
    private Property<List<String>> snippetTags;

    @Override
    public Output run(RunContext runContext) throws Exception {
        YouTube youtube = createYoutubeService(runContext);

        // Render all parameters
        String renderedVideoId = runContext.render(this.videoId).as(String.class).orElseThrow(
                () -> new IllegalArgumentException("videoId is required"));
        String renderedTitle = PropertyHelper.safeRender(runContext, this.snippetTitle, null, String.class);
        String renderedDescription = PropertyHelper.safeRender(runContext, this.snippetDescription, null, String.class);
        List<String> renderedTags = PropertyHelper.safeRenderList(runContext, this.snippetTags, null,
                String.class);

        // First, get the current video to preserve existing data
        Video currentVideo = getCurrentVideo(youtube, renderedVideoId);

        // Build the updated video object
        Video updatedVideo = buildUpdatedVideo(currentVideo, renderedTitle, renderedDescription, renderedTags);

        // Determine which parts to update
        List<String> partsToUpdate = determinePartsToUpdate(renderedTitle, renderedDescription, renderedTags);

        if (partsToUpdate.isEmpty()) {
            runContext.logger().error("No fields were provided for update. Provide at least one field to update.");
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        // Execute the update
        YouTube.Videos.Update updateRequest = youtube.videos().update(partsToUpdate, updatedVideo);

        Video response = updateRequest.execute();

        runContext.logger().info("Successfully updated video: {} with parts: {}", renderedVideoId,
                String.join(",", partsToUpdate));

        return Output.builder()
                .kind(response.getKind())
                .etag(response.getEtag())
                .id(response.getId())
                .title(response.getSnippet() != null ? response.getSnippet().getTitle() : null)
                .description(response.getSnippet() != null ? response.getSnippet().getDescription() : null)
                .privacyStatus(response.getStatus() != null ? response.getStatus().getPrivacyStatus() : null)
                .build();
    }

    private Video getCurrentVideo(YouTube youtube, String videoId) throws Exception {
        YouTube.Videos.List listRequest = youtube.videos()
                .list(Arrays.asList("snippet", "status", "recordingDetails", "localizations"))
                .setId(Arrays.asList(videoId));

        VideoListResponse response = listRequest.execute();
        if (response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalArgumentException("Video not found: " + videoId);
        }
        return response.getItems().get(0);
    }

    private Video buildUpdatedVideo(Video currentVideo, String title, String description, List<String> tags) {

        Video video = new Video();
        video.setId(currentVideo.getId());

        VideoSnippet snippet = currentVideo.getSnippet();

        if (title != null && !title.isEmpty()) {
            snippet.setTitle(title);
        }
        if (description != null && !description.isEmpty()) {
            snippet.setDescription(description);
        }
        if (tags != null) {
            snippet.setTags(tags);
        }
        video.setSnippet(snippet);

        return video;
    }

    private List<String> determinePartsToUpdate(String title, String description, List<String> tags) {
        List<String> parts = new ArrayList<>();

        if (title != null || description != null || tags != null) {
            parts.add("snippet");
        }

        return parts;
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "Resource type", description = "Identifies the API resource's type (youtube#video)")
        private final String kind;

        @Schema(title = "Etag", description = "The ETag for this resource")
        private final String etag;

        @Schema(title = "Video ID", description = "The YouTube video ID")
        private final String id;

        @Schema(title = "Title", description = "The video's title")
        private final String title;

        @Schema(title = "Description", description = "The video's description")
        private final String description;

        @Schema(title = "Privacy Status", description = "The video's privacy status")
        private final String privacyStatus;
    }
}
