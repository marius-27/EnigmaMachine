package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.enigmamachine.domain.SignalTrace.Stage;
import com.example.enigmamachine.domain.SignalTrace.Step;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignalTraceTest {

    private static final MachineConfiguration CONFIGURATION = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of("AM"));

    private final EnigmaMachine machine = new EnigmaMachine(CONFIGURATION);

    @Test
    void visitsEveryComponentInSignalOrder() {
        List<Stage> stages = machine.encryptTraced('A').steps().stream().map(Step::stage).toList();

        assertEquals(List.of(Stage.values()), stages);
    }

    @Test
    void reportsTheLetterAndThePlugboardSwap() {
        SignalTrace trace = machine.encryptTraced('a');

        assertEquals('A', trace.input());
        assertEquals(new Step(Stage.PLUGBOARD_IN, 'A', 'M'), trace.steps().get(0));
    }

    @Test
    void reportsPositionsAfterTheRotorsStepped() {
        assertEquals(List.of(0, 0, 1), machine.encryptTraced('A').positions());
    }

    @Test
    void stepsTheRotorsOnceLikeEncrypt() {
        machine.encryptTraced('A');

        assertEquals(List.of(0, 0, 1), machine.positions());
    }

    @Test
    void invalidLetterIsRejectedWithoutStepping() {
        assertThrows(IllegalArgumentException.class, () -> machine.encryptTraced('7'));

        assertEquals(List.of(0, 0, 0), machine.positions());
    }

    @Test
    void traceCannotBeChangedFromOutside() {
        SignalTrace trace = machine.encryptTraced('A');

        assertThrows(UnsupportedOperationException.class, () -> trace.steps().clear());
        assertThrows(UnsupportedOperationException.class, () -> trace.positions().clear());
    }
}
