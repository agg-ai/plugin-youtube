package io.kestra.plugin.youtube.models;

public enum VideoEmbeddable {
    ANY,
    TRUE;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
