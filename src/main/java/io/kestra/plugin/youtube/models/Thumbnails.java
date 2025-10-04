package io.kestra.plugin.youtube.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Thumbnails {
    @Schema(title = "Default thumbnail", description = "Default thumbnail (120x90 pixels)")
    private final Thumbnail defaultThumbnail;

    @Schema(title = "Medium thumbnail", description = "Medium resolution thumbnail (320x180 pixels)")
    private final Thumbnail medium;

    @Schema(title = "High thumbnail", description = "High resolution thumbnail (480x360 pixels)")
    private final Thumbnail high;

    @Schema(title = "Standard thumbnail", description = "Standard resolution thumbnail (640x480 pixels)")
    private final Thumbnail standard;

    @Schema(title = "Maxres thumbnail", description = "Maximum resolution thumbnail (1280x720 pixels)")
    private final Thumbnail maxres;
}
