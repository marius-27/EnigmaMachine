package com.example.enigmamachine.ui.viewmodel;

import com.example.enigmamachine.domain.SignalTrace;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Which step of a finished signal trace is being shown. The letter is already encrypted when playback
 * starts, so playback only moves through the record and can never change the result.
 */
public final class SignalPlayback {

    public static final int NO_STEP = -1;

    private final ReadOnlyIntegerWrapper activeStep = new ReadOnlyIntegerWrapper(NO_STEP);
    private final ReadOnlyBooleanWrapper canAdvance = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyObjectWrapper<SignalTrace> trace = new ReadOnlyObjectWrapper<>();

    /** The trace being played, or null when there is none. */
    public ReadOnlyObjectProperty<SignalTrace> traceProperty() {
        return trace.getReadOnlyProperty();
    }

    /** The index of the step being shown, or {@link #NO_STEP} when there is no trace. */
    public ReadOnlyIntegerProperty activeStepProperty() {
        return activeStep.getReadOnlyProperty();
    }

    /** True while there is a step after the one being shown. */
    public ReadOnlyBooleanProperty canAdvanceProperty() {
        return canAdvance.getReadOnlyProperty();
    }

    /** Starts over at the first step of the given trace, or shows nothing for null. */
    public void show(SignalTrace newTrace) {
        trace.set(newTrace);
        moveTo(newTrace == null ? NO_STEP : 0);
    }

    /** Goes back to the first step of the current trace. */
    public void restart() {
        if (trace.get() != null) {
            moveTo(0);
        }
    }

    /** Moves to the next step, or returns false if there is none. */
    public boolean advance() {
        if (!canAdvance.get()) {
            return false;
        }
        moveTo(activeStep.get() + 1);
        return true;
    }

    private void moveTo(int step) {
        canAdvance.set(trace.get() != null && step < trace.get().steps().size() - 1);
        activeStep.set(step);
    }
}
