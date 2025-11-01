package io.kestra.plugin.youtube.models;

public enum ResourceType {
    video,
    channel,
    playlist;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
