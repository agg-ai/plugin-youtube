package io.kestra.plugin.youtube.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Thumbnail {
    @Schema(title = "URL", description = "Image URL")
    private final String url;

    @Schema(title = "Width", description = "Image width in pixels")
    private final Integer width;

    @Schema(title = "Height", description = "Image height in pixels")
    private final Integer height;
}
