package io.kestra.plugin.youtube.models;

public enum VideoPaidProductPlacement {
    ANY,
    TRUE;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
