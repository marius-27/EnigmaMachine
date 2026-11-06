package com.example.enigmamachine.ui.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * How signal paths are played, shared by every view of the session, so switching between Classic and Diagram
 * keeps the educational mode and the speed that were chosen in either of them.
 */
public final class PlaybackOptions {

    public static final double NORMAL_SPEED = 1.0;

    private final BooleanProperty educational = new SimpleBooleanProperty(false);
    private final ObjectProperty<Double> speed = new SimpleObjectProperty<>(NORMAL_SPEED);

    /** When on, each new letter waits at its first step until Play or Step is pressed. */
    public BooleanProperty educationalProperty() {
        return educational;
    }

    /** The playback speed, as a multiple of the normal speed. */
    public ObjectProperty<Double> speedProperty() {
        return speed;
    }
}
