package io.kestra.plugin.youtube.models;

public enum VideoType {
    ANY,
    EPISODE,
    MOVIE;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
