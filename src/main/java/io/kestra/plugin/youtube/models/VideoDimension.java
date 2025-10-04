package io.kestra.plugin.youtube.models;

public enum VideoDimension {
    TWO_D,
    THREE_D,
    ANY;

    public String toApiValue() {
        if (this == TWO_D)
            return "2d";
        if (this == THREE_D)
            return "3d";
        return "any";
    }
}
