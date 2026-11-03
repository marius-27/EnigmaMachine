package com.example.enigmamachine.ui.viewmodel;

import com.example.enigmamachine.domain.SignalTrace;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;

/**
 * Which step of a finished signal trace is being shown. The letter is already encrypted when playback
 * starts, so playback only moves through the record and can never change the result.
 */
public final class SignalPlayback {

    public static final int NO_STEP = -1;

    private final ReadOnlyIntegerWrapper activeStep = new ReadOnlyIntegerWrapper(NO_STEP);
    private SignalTrace trace;

    /** The index of the step being shown, or {@link #NO_STEP} when there is no trace. */
    public ReadOnlyIntegerProperty activeStepProperty() {
        return activeStep.getReadOnlyProperty();
    }

    /** Starts over at the first step of the given trace, or shows nothing for null. */
    public void show(SignalTrace newTrace) {
        trace = newTrace;
        activeStep.set(newTrace == null ? NO_STEP : 0);
    }

    /** Moves to the next step, or returns false if there is none. */
    public boolean advance() {
        if (trace == null || activeStep.get() >= trace.steps().size() - 1) {
            return false;
        }
        activeStep.set(activeStep.get() + 1);
        return true;
    }
}
