package io.kestra.plugin.youtube.models;

public enum SafeSearch {
    MODERATE,
    NONE,
    STRICT;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
