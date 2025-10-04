package io.kestra.plugin.youtube.models;

public enum Order {
    DATE,
    RATING,
    RELEVANCE,
    TITLE,
    VIDEO_COUNT,
    VIEW_COUNT;

    public String toApiValue() {
        return this.name().toLowerCase().replace("_", "");
    }
}
