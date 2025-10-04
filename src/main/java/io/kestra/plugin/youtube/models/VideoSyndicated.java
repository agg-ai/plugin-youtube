package io.kestra.plugin.youtube.models;

public enum VideoSyndicated {
    ANY,
    TRUE;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
