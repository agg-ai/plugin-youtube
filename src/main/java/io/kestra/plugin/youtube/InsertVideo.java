package io.kestra.plugin.youtube;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.youtube.models.VideoCategory;
import io.kestra.plugin.youtube.models.VideoPrivacyStatus;
import io.kestra.plugin.youtube.helpers.EnumHelper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Insert a video to YouTube", description = "Uploads a video to YouTube. Maximum file size: 256GB.")
@Plugin(examples = {
        @Example(title = "Upload a video to YouTube", full = true, code = """
                id: insert_video
                namespace: company.team

                tasks:
                  - id: authenticate
                    type: io.kestra.plugin.youtube.OAuth2
                    clientId: "{{ secret('YOUTUBE_CLIENT_ID') }}"
                    clientSecret: "{{ secret('YOUTUBE_CLIENT_SECRET') }}"
                    refreshToken: "{{ secret('YOUTUBE_REFRESH_TOKEN') }}"

                  - id: upload_video
                    type: io.kestra.plugin.youtube.InsertVideo
                    accessToken: "{{ outputs.authenticate.accessToken }}"
                    videoFileUrl: "{{ inputs.video_url }}"
                    snippetTitle: "My Video Title"
                    snippetDescription: "This is a description of the video."
                    snippetCategory: MUSIC
                    snippetTags:
                      - "music"
                      - "live"
                      - "concert"
                    privacyStatus: PUBLIC
                """),
        @Example(title = "Upload a private video with tags", code = """
                  - id: upload_private_video
                    type: io.kestra.plugin.youtube.InsertVideo
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    videoFileUrl: "https://example.com/myvideo.mp4"
                    snippetTitle: "Private Video"
                    snippetDescription: "This video is private."
                    snippetCategory: EDUCATION
                    snippetTags:
                      - "tutorial"
                      - "education"
                    privacyStatus: PRIVATE
                """),
        @Example(title = "Upload a video with minimal fields", code = """
                  - id: upload_minimal_video
                    type: io.kestra.plugin.youtube.InsertVideo
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    videoFileUrl: "https://example.com/shortclip.mp4"
                    snippetTitle: "Short Clip"
                    snippetDescription: "A short video."
                    snippetCategory: ENTERTAINMENT
                """)
})
public class InsertVideo extends AbstractYoutubeTask implements RunnableTask<InsertVideo.Output> {
    @Schema(title = "Video file URL", description = "The URL of the video file to upload.")
    @NotNull
    private Property<String> videoFileUrl;

    @Schema(title = "Snippet Title", description = "The video's title.")
    @NotNull
    private Property<String> snippetTitle;

    @Schema(title = "Snippet Description", description = "The video's description")
    @NotNull
    private Property<String> snippetDescription;

    @Schema(title = "Snippet Category", description = "The video's category. Possible values: FILM_ANIMATION, AUTOS_VEHICLES, MUSIC, PETS_ANIMALS, SPORTS, SHORT_MOVIES, TRAVEL_EVENTS, GAMING, VIDEOBLOGGING, PEOPLE_BLOGS, COMEDY, ENTERTAINMENT, NEWS_POLITICS, HOWTO_STYLE, EDUCATION, SCIENCE_TECHNOLOGY, NONPROFITS_ACTIVISM, MOVIES, ANIME_ANIMATION, ACTION_ADVENTURE, CLASSICS, COMEDY_2, DOCUMENTARY, DRAMA, FAMILY, FOREIGN, HORROR, SCIFI_FANTASY, THRILLER, SHORTS, SHOWS, TRAILERS.")
    @NotNull
    private Property<VideoCategory> snippetCategory;

    @Schema(title = "Snippet Tags", description = "A list of keyword tags associated with the video.")
    private Property<List<String>> snippetTags;

    @Schema(title = "Privacy status", description = "The video's privacy status. Possible value: PUBLIC, PRIVATE, UNLISTED.")
    @Builder.Default
    private Property<VideoPrivacyStatus> privacyStatus = Property.ofValue(VideoPrivacyStatus.PUBLIC);

    @Override
    public Output run(RunContext runContext) throws Exception {
        YouTube youtube = createYoutubeService(runContext);

        String renderedVideoFileUrl = runContext.render(this.videoFileUrl).as(String.class).orElseThrow(
                () -> new IllegalArgumentException("videoFileUrl is required"));
        String renderedTitle = runContext.render(this.snippetTitle).as(String.class).orElseThrow(
                () -> new IllegalArgumentException("snippetTitle is required"));
        String renderedDescription = runContext.render(this.snippetDescription).as(String.class).orElseThrow(
                () -> new IllegalArgumentException("snippetDescription is required"));
        VideoCategory renderedCategory = EnumHelper.safeRenderEnum(runContext, this.snippetCategory,
                VideoCategory.class);
        List<String> renderedTags = this.snippetTags != null ? runContext.render(this.snippetTags).asList(String.class)
                : List.of();
        VideoPrivacyStatus renderedPrivacyStatus = EnumHelper.safeRenderEnum(runContext, this.privacyStatus,
                VideoPrivacyStatus.class);

        Video video = new Video();
        VideoSnippet snippet = new VideoSnippet();
        snippet.setCategoryId(renderedCategory.getId());
        snippet.setDescription(renderedDescription);
        snippet.setTitle(renderedTitle);
        snippet.setTags(renderedTags);
        video.setSnippet(snippet);

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus(renderedPrivacyStatus.toApiValue());
        video.setStatus(status);

        InputStream inputStream = new URI(renderedVideoFileUrl).toURL().openStream();
        InputStreamContent mediaContent = new InputStreamContent("video/*", inputStream);
        mediaContent.setLength(-1);
        mediaContent.setCloseInputStream(true);

        List<String> parts = List.of("snippet", "status");

        YouTube.Videos.Insert request = youtube.videos().insert(parts, video, mediaContent);
        Video response = request.execute();

        return Output.builder()
                .kind(response.getKind())
                .etag(response.getEtag())
                .id(response.getId())
                .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "Resource type", description = "Identifies the API resource's type (youtube#thumbnailSetResponse)")
        private final String kind;

        @Schema(title = "Etag", description = "The ETag for this resource")
        private final String etag;

        @Schema(title = "Video ID", description = "The YouTube video ID")
        private final String id;
    }
}
