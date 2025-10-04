package io.kestra.plugin.youtube.models;

public enum VideoCaption {
    ANY,
    CLOSED_CAPTION,
    NONE;

    public String toApiValue() {
        return this.name().equals("CLOSED_CAPTION") ? "closedCaption" : this.name().toLowerCase();
    }
}
