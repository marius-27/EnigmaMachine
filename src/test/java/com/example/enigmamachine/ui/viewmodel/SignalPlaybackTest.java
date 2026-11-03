package com.example.enigmamachine.ui.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.enigmamachine.domain.EnigmaMachine;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.domain.SignalTrace;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignalPlaybackTest {

    private final SignalPlayback playback = new SignalPlayback();
    private final EnigmaMachine machine = new EnigmaMachine(new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of()));

    @Test
    void showsNothingAtFirst() {
        assertEquals(SignalPlayback.NO_STEP, active());
        assertFalse(playback.advance());
    }

    @Test
    void newTraceStartsAtTheFirstStep() {
        playback.show(machine.encryptTraced('A'));

        assertEquals(0, active());
    }

    @Test
    void advancesThroughEveryStepAndStopsAtTheLast() {
        SignalTrace trace = machine.encryptTraced('A');
        playback.show(trace);

        for (int step = 1; step < trace.steps().size(); step++) {
            assertTrue(playback.advance());
            assertEquals(step, active());
        }

        assertFalse(playback.advance());
        assertEquals(trace.steps().size() - 1, active());
    }

    @Test
    void newTraceInterruptsTheOneBeingShown() {
        playback.show(machine.encryptTraced('A'));
        playback.advance();
        playback.advance();

        playback.show(machine.encryptTraced('B'));

        assertEquals(0, active());
    }

    @Test
    void nullTraceShowsNothing() {
        playback.show(machine.encryptTraced('A'));

        playback.show(null);

        assertEquals(SignalPlayback.NO_STEP, active());
        assertFalse(playback.advance());
    }

    private int active() {
        return playback.activeStepProperty().get();
    }
}
