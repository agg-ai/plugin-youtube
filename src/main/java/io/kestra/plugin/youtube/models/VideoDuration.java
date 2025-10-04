package io.kestra.plugin.youtube.models;

public enum VideoDuration {
    ANY,
    LONG,
    MEDIUM,
    SHORT;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
