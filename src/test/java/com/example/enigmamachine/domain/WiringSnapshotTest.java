package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.enigmamachine.domain.SignalTrace.Stage;
import com.example.enigmamachine.domain.SignalTrace.Step;
import com.example.enigmamachine.domain.WiringSnapshot.RotorWiring;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WiringSnapshotTest {

    private static final MachineConfiguration BARBAROSSA = new MachineConfiguration(
            List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
            Reflector.B,
            List.of(1, 20, 11),
            List.of(1, 11, 0),
            List.of("AV", "BS", "CG", "DL", "FU", "HZ", "IN", "KM", "OW", "RX"));

    private static final MachineConfiguration PLAIN = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of());

    @Test
    void atRingOneAndPositionAEachRotorIsItsPublishedWiring() {
        WiringSnapshot snapshot = new EnigmaMachine(PLAIN).wiringSnapshot();

        assertEquals(wiringOf(RotorDefinition.I.wiring()), snapshot.rotors().get(0).forward());
        assertEquals(wiringOf(RotorDefinition.II.wiring()), snapshot.rotors().get(1).forward());
        assertEquals(wiringOf(RotorDefinition.III.wiring()), snapshot.rotors().get(2).forward());
        assertEquals(wiringOf("YRUHQSLDPXNGOKMIEBFZCWVJAT"), snapshot.reflectorWiring());
    }

    @Test
    void reportsTheSettingsOfEachRotor() {
        List<RotorWiring> rotors = new EnigmaMachine(BARBAROSSA).wiringSnapshot().rotors();

        assertEquals(List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
                rotors.stream().map(RotorWiring::definition).toList());
        assertEquals(List.of(1, 11, 0), rotors.stream().map(RotorWiring::position).toList());
        assertEquals(List.of(1, 20, 11), rotors.stream().map(RotorWiring::ringSetting).toList());
    }

    @Test
    void positionAndRingThatCancelOutGiveThePublishedWiring() {
        MachineConfiguration shifted = new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(5, 0, 0),
                List.of(5, 0, 0),
                List.of());

        assertEquals(wiringOf(RotorDefinition.I.wiring()),
                new EnigmaMachine(shifted).wiringSnapshot().rotors().get(0).forward());
    }

    @Test
    void positionTurnsTheWiringAgainstTheFixedContacts() {
        MachineConfiguration atB = new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 0, 1),
                List.of());

        // Contact A meets the rotor's B, wired to D, which leaves one contact lower, at C.
        assertEquals(2, new EnigmaMachine(atB).wiringSnapshot().rotors().get(2).forward().get(0));
    }

    @Test
    void plugboardWiringSwapsThePairsBothWays() {
        List<Integer> plugboard = new EnigmaMachine(BARBAROSSA).wiringSnapshot().plugboardWiring();

        assertEquals(Alphabet.toIndex('V'), plugboard.get(Alphabet.toIndex('A')));
        assertEquals(Alphabet.toIndex('A'), plugboard.get(Alphabet.toIndex('V')));
        assertEquals(Alphabet.toIndex('E'), plugboard.get(Alphabet.toIndex('E')));
    }

    /** The wires drawn after a letter are the ones its signal really crossed, both ways. */
    @Test
    void everyTraceFollowsTheSnapshotTakenAtItsPositions() {
        EnigmaMachine machine = new EnigmaMachine(BARBAROSSA);
        for (char letter : "EDPUDNRGYSZRCXNUYTPOMRMBOFKTBZREZKMLXLVEFGUEYIVQ".toCharArray()) {
            SignalTrace trace = machine.encryptTraced(letter);
            WiringSnapshot snapshot = machine.wiringSnapshot();

            assertEquals(trace.positions(), snapshot.rotors().stream().map(RotorWiring::position).toList());
            List<Step> steps = trace.steps();
            assertCrosses(steps.get(0), Stage.PLUGBOARD_IN, snapshot.plugboardWiring());
            assertCrosses(steps.get(1), Stage.RIGHT_ROTOR_FORWARD, snapshot.rotors().get(2).forward());
            assertCrosses(steps.get(2), Stage.MIDDLE_ROTOR_FORWARD, snapshot.rotors().get(1).forward());
            assertCrosses(steps.get(3), Stage.LEFT_ROTOR_FORWARD, snapshot.rotors().get(0).forward());
            assertCrosses(steps.get(4), Stage.REFLECTOR, snapshot.reflectorWiring());
            assertCrosses(steps.get(5), Stage.LEFT_ROTOR_REVERSE, inverse(snapshot.rotors().get(0).forward()));
            assertCrosses(steps.get(6), Stage.MIDDLE_ROTOR_REVERSE, inverse(snapshot.rotors().get(1).forward()));
            assertCrosses(steps.get(7), Stage.RIGHT_ROTOR_REVERSE, inverse(snapshot.rotors().get(2).forward()));
            assertCrosses(steps.get(8), Stage.PLUGBOARD_OUT, snapshot.plugboardWiring());
        }
    }

    @Test
    void snapshotDoesNotChangeWhenTheMachineMovesOn() {
        EnigmaMachine machine = new EnigmaMachine(PLAIN);
        WiringSnapshot before = machine.wiringSnapshot();
        List<Integer> rightBefore = List.copyOf(before.rotors().get(2).forward());

        machine.encrypt('A');

        assertEquals(rightBefore, before.rotors().get(2).forward());
        assertThrows(UnsupportedOperationException.class, () -> before.rotors().get(2).forward().set(0, 0));
    }

    @Test
    void mappingNeedsAllContacts() {
        assertThrows(IllegalArgumentException.class,
                () -> new RotorWiring(RotorDefinition.I, 0, 0, List.of(0, 1, 2)));
        List<Integer> outOfRange = new ArrayList<>(wiringOf(RotorDefinition.I.wiring()));
        outOfRange.set(0, 26);
        assertThrows(IllegalArgumentException.class, () -> new RotorWiring(RotorDefinition.I, 0, 0, outOfRange));
    }

    private static void assertCrosses(Step step, Stage stage, List<Integer> wiring) {
        assertEquals(stage, step.stage());
        assertEquals(step.output(), Alphabet.toLetter(wiring.get(Alphabet.toIndex(step.input()))), stage.name());
    }

    private static List<Integer> inverse(List<Integer> wiring) {
        List<Integer> inverse = new ArrayList<>(wiring);
        for (int contact = 0; contact < wiring.size(); contact++) {
            inverse.set(wiring.get(contact), contact);
        }
        return inverse;
    }

    private static List<Integer> wiringOf(String letters) {
        return letters.chars().map(letter -> Alphabet.toIndex((char) letter)).boxed().toList();
    }
}
