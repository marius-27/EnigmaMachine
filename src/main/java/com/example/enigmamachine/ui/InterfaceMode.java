package com.example.enigmamachine.ui;

/** The two ways the same machine can be shown. */
public enum InterfaceMode {
    CLASSIC("Classic"),
    DIAGRAM("Diagram");

    private final String displayName;

    InterfaceMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** The mode the Mode button switches to. */
    public InterfaceMode other() {
        return this == CLASSIC ? DIAGRAM : CLASSIC;
    }
}
