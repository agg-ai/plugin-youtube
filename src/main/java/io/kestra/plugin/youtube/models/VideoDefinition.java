package io.kestra.plugin.youtube.models;

public enum VideoDefinition {
    ANY,
    HIGH,
    STANDARD;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
