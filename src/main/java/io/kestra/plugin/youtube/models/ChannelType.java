package io.kestra.plugin.youtube.models;

public enum ChannelType {
    ANY,
    SHOW;

    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
