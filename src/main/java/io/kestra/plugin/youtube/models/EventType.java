package io.kestra.plugin.youtube.models;

public enum EventType {
    COMPLETED,
    LIVE,
    UPCOMING;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
