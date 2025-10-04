package io.kestra.plugin.youtube.models;

public enum VideoLicense {
    ANY,
    CREATIVE_COMMON,
    YOUTUBE;

    public String toApiValue() {
        return this == CREATIVE_COMMON ? "creativeCommon" : this.name().toLowerCase();
    }
}
