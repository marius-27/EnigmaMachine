package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.enigmamachine.domain.SignalTrace.Stage;
import com.example.enigmamachine.domain.SignalTrace.Step;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;

class SignalTraceTest {

    private static final String LONG_TEXT = "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG".repeat(20);

    private static final MachineConfiguration CONFIGURATION = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of("AM"));

    private final EnigmaMachine machine = new EnigmaMachine(CONFIGURATION);

    private static final MachineConfiguration BARBAROSSA = new MachineConfiguration(
            List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
            Reflector.B,
            List.of(1, 20, 11),
            List.of(1, 11, 0),
            List.of("AV", "BS", "CG", "DL", "FU", "HZ", "IN", "KM", "OW", "RX"));

    /** Worked out by hand from the published wiring tables, for rotors I, II, III and reflector B at AAB. */
    @Test
    void followsTheHandDerivedPathOfOneLetter() {
        EnigmaMachine plain = new EnigmaMachine(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 0, 0),
                List.of()));

        assertEquals(
                List.of(
                        new Step(Stage.PLUGBOARD_IN, 'A', 'A'),
                        new Step(Stage.RIGHT_ROTOR_FORWARD, 'A', 'C'),
                        new Step(Stage.MIDDLE_ROTOR_FORWARD, 'C', 'D'),
                        new Step(Stage.LEFT_ROTOR_FORWARD, 'D', 'F'),
                        new Step(Stage.REFLECTOR, 'F', 'S'),
                        new Step(Stage.LEFT_ROTOR_REVERSE, 'S', 'S'),
                        new Step(Stage.MIDDLE_ROTOR_REVERSE, 'S', 'E'),
                        new Step(Stage.RIGHT_ROTOR_REVERSE, 'E', 'B'),
                        new Step(Stage.PLUGBOARD_OUT, 'B', 'B')),
                plain.encryptTraced('A').steps());
    }

    @Test
    void lastStepGivesTheEncryptedLetter() {
        EnigmaMachine traced = new EnigmaMachine(BARBAROSSA);
        EnigmaMachine plain = new EnigmaMachine(BARBAROSSA);

        for (char letter : LONG_TEXT.toCharArray()) {
            SignalTrace trace = traced.encryptTraced(letter);

            assertEquals(plain.encrypt(letter), trace.output());
            assertEquals(trace.output(), trace.steps().getLast().output());
        }
    }

    @Test
    void everyStepStartsWhereThePreviousStepEnded() {
        EnigmaMachine machine = new EnigmaMachine(BARBAROSSA);

        for (char letter : LONG_TEXT.toCharArray()) {
            SignalTrace trace = machine.encryptTraced(letter);

            char signal = trace.input();
            for (Step step : trace.steps()) {
                assertEquals(signal, step.input(), () -> step.stage() + " for " + letter);
                signal = step.output();
            }
            assertEquals(trace.output(), signal);
        }
    }

    @Test
    void reportedPositionsAreTheOnesTheMachineWasLeftAt() {
        EnigmaMachine machine = new EnigmaMachine(BARBAROSSA);

        for (char letter : LONG_TEXT.toCharArray()) {
            assertEquals(machine.encryptTraced(letter).positions(), machine.positions());
        }
    }

    @Test
    void plugboardAndReflectorStepsMatchTheirComponents() {
        EnigmaMachine machine = new EnigmaMachine(BARBAROSSA);
        Plugboard plugboard = new Plugboard(BARBAROSSA.plugboardPairs());

        for (char letter : LONG_TEXT.toCharArray()) {
            SignalTrace trace = machine.encryptTraced(letter);

            assertStepMatches(trace, Stage.PLUGBOARD_IN, plugboard::swap);
            assertStepMatches(trace, Stage.REFLECTOR, Reflector.B::reflect);
            assertStepMatches(trace, Stage.PLUGBOARD_OUT, plugboard::swap);
        }
    }

    @Test
    void tracedOutputsDecryptAHistoricalMessage() {
        EnigmaMachine machine = new EnigmaMachine(BARBAROSSA);
        StringBuilder plaintext = new StringBuilder();

        for (char letter : "EDPUDNRGYSZRCXNUYTPOMRMBOFKTBZREZKMLXLVEFGUEYSIOZVEQMIKUBPMM".toCharArray()) {
            plaintext.append(machine.encryptTraced(letter).steps().getLast().output());
        }

        assertEquals("AUFKLXABTEILUNGXVONXKURTINOWAXKURTINOWAXNORDWESTLXSEBEZXSEBE", plaintext.toString());
    }

    @Test
    void reversedTraceIsTheTraceOfTheOutputLetter() {
        SignalTrace forward = new EnigmaMachine(BARBAROSSA).encryptTraced('H');
        SignalTrace backward = new EnigmaMachine(BARBAROSSA).encryptTraced(forward.output());

        assertEquals(forward.input(), backward.output());
        for (int index = 0; index < forward.steps().size(); index++) {
            Step step = forward.steps().get(index);
            Step mirrored = backward.steps().get(forward.steps().size() - 1 - index);
            assertEquals(step.input(), mirrored.output());
            assertEquals(step.output(), mirrored.input());
        }
    }

    private static void assertStepMatches(SignalTrace trace, Stage stage, IntUnaryOperator component) {
        Step step = trace.steps().get(stage.ordinal());
        assertEquals(stage, step.stage());
        assertEquals(
                Alphabet.toLetter(component.applyAsInt(Alphabet.toIndex(step.input()))),
                step.output());
    }

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
