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
import io.kestra.plugin.youtube.helpers.EnumHelper;
import io.kestra.plugin.youtube.helpers.PropertyHelper;
import io.kestra.plugin.youtube.models.VideoCategory;
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
                    snippetCategory: "10"
                    snippetTags:
                      - "music"
                      - "live"
                      - "concert"
                    privacyStatus: public
                """),
        @Example(title = "Upload a private video with tags", code = """
                  - id: upload_private_video
                    type: io.kestra.plugin.youtube.InsertVideo
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    videoFileUrl: "https://example.com/myvideo.mp4"
                    snippetTitle: "Private Video"
                    snippetDescription: "This video is private."
                    snippetCategory: "27"
                    snippetTags:
                      - "tutorial"
                      - "education"
                    privacyStatus: private
                """),
        @Example(title = "Upload a video with minimal fields", code = """
                  - id: upload_minimal_video
                    type: io.kestra.plugin.youtube.InsertVideo
                    accessToken: "{{ secret('YOUTUBE_ACCESS_TOKEN') }}"
                    videoFileUrl: "https://example.com/shortclip.mp4"
                    snippetTitle: "Short Clip"
                    snippetDescription: "A short video."
                    snippetCategory: "24"
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

    @Schema(title = "Snippet Category", description = "The video's category ID. Common values: 1 (Film & Animation), 2 (Autos & Vehicles), 10 (Music), 15 (Pets & Animals), 17 (Sports), 18 (Short Movies), 19 (Travel & Events), 20 (Gaming), 21 (Videoblogging), 22 (People & Blogs), 23 (Comedy), 24 (Entertainment), 25 (News & Politics), 26 (Howto & Style), 27 (Education), 28 (Science & Technology), 29 (Nonprofits & Activism), 30 (Movies), 31 (Anime/Animation), 32 (Action/Adventure), 33 (Classics), 34 (Comedy), 35 (Documentary), 36 (Drama), 37 (Family), 38 (Foreign), 39 (Horror), 40 (Sci-Fi/Fantasy), 41 (Thriller), 42 (Shorts), 43 (Shows), 44 (Trailers).")
    @NotNull
    private Property<VideoCategory> snippetCategory;

    @Schema(title = "Snippet Tags", description = "A list of keyword tags associated with the video.")
    private Property<List<String>> snippetTags;

    @Schema(title = "Privacy status", description = "The video's privacy status.", allowableValues = { "public",
            "private", "unlisted" })
    @Builder.Default
    private Property<String> privacyStatus = Property.ofValue("public");

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
        List<String> renderedTags = PropertyHelper.safeRenderList(runContext, this.snippetTags, List.of(),
                String.class);
        String renderedPrivacyStatus = PropertyHelper.safeRender(runContext, this.privacyStatus, "public",
                String.class);
        renderedPrivacyStatus = renderedPrivacyStatus.isEmpty()
                ? "public"
                : renderedPrivacyStatus;

        Video video = new Video();
        VideoSnippet snippet = new VideoSnippet();
        snippet.setCategoryId(renderedCategory.getId());
        snippet.setDescription(renderedDescription);
        snippet.setTitle(renderedTitle);
        snippet.setTags(renderedTags);
        video.setSnippet(snippet);

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus(renderedPrivacyStatus);
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
