package io.kestra.plugin.youtube.models;

public enum VideoPrivacyStatus {
    PUBLIC,
    PRIVATE,
    UNLISTED;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
