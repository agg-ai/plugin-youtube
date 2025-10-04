package io.kestra.plugin.youtube.models;

public enum ResourceType {
    VIDEO,
    CHANNEL,
    PLAYLIST;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
