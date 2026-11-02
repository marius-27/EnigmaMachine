package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RotorSteppingTest {

    private static final List<RotorDefinition> ROTORS_I_II_III =
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III);

    @Test
    void rightRotorAloneStepsAwayFromNotches() {
        assertSteps(ROTORS_I_II_III, "AAA", "AAB", "AAC");
    }

    @Test
    void rightRotorNotchTurnsMiddleRotor() {
        assertSteps(ROTORS_I_II_III, "AAU", "AAV", "ABW", "ABX");
    }

    @Test
    void middleRotorDoubleSteps() {
        assertSteps(ROTORS_I_II_III, "ADU", "ADV", "AEW", "BFX", "BFY");
    }

    @Test
    void middleRotorAtNotchStepsEvenWhenRightRotorIsNot() {
        assertSteps(ROTORS_I_II_III, "AEA", "BFB");
    }

    @Test
    void leftRotorNotchHasNoEffect() {
        assertSteps(ROTORS_I_II_III, "QAA", "QAB");
    }

    @Test
    void rotorsWrapFromZToA() {
        assertSteps(ROTORS_I_II_III, "AAZ", "AAA");
        assertSteps(ROTORS_I_II_III, "AZV", "AAW");
        assertSteps(ROTORS_I_II_III, "ZEA", "AFB");
    }

    @Test
    void notchAtZTurnsNextRotorOnWrap() {
        assertSteps(List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.V), "AAZ", "ABA");
        assertSteps(List.of(RotorDefinition.I, RotorDefinition.V, RotorDefinition.III), "AZA", "BAB");
    }

    @Test
    void ringSettingDoesNotMoveTurnoverPosition() {
        EnigmaMachine machine = new EnigmaMachine(new MachineConfiguration(
                ROTORS_I_II_III, Reflector.B, List.of(3, 7, 11), positions("AAV"), List.of()));

        machine.encrypt('A');

        assertEquals(positions("ABW"), machine.positions());
    }

    @Test
    void returnsToStartAfterFullMechanicalPeriod() {
        EnigmaMachine machine = machineAt(ROTORS_I_II_III, "AAA");

        for (int key = 0; key < 26 * 25 * 26; key++) {
            machine.encrypt('A');
        }

        assertEquals(positions("AAA"), machine.positions());
    }

    @Test
    void encryptsAfterTurnoverNotBefore() {
        EnigmaMachine machine = machineAt(ROTORS_I_II_III, "AAV");

        assertEquals(signalAt("ABW", 'A'), machine.encrypt('A'));
    }

    @Test
    void encryptsAfterDoubleStepNotBefore() {
        EnigmaMachine machine = machineAt(ROTORS_I_II_III, "AEW");

        assertEquals(signalAt("BFX", 'A'), machine.encrypt('A'));
    }

    private static void assertSteps(List<RotorDefinition> rotors, String start, String... expected) {
        EnigmaMachine machine = machineAt(rotors, start);
        for (String next : expected) {
            machine.encrypt('A');
            assertEquals(positions(next), machine.positions(), "Expected " + next);
        }
    }

    private static char signalAt(String windows, char letter) {
        List<Integer> positions = positions(windows);
        Rotor left = new Rotor(RotorDefinition.I, 0, positions.get(0));
        Rotor middle = new Rotor(RotorDefinition.II, 0, positions.get(1));
        Rotor right = new Rotor(RotorDefinition.III, 0, positions.get(2));
        int signal = left.forward(middle.forward(right.forward(Alphabet.toIndex(letter))));
        signal = Reflector.B.reflect(signal);
        return Alphabet.toLetter(right.reverse(middle.reverse(left.reverse(signal))));
    }

    private static EnigmaMachine machineAt(List<RotorDefinition> rotors, String windows) {
        return new EnigmaMachine(new MachineConfiguration(
                rotors, Reflector.B, List.of(0, 0, 0), positions(windows), List.of()));
    }

    private static List<Integer> positions(String windows) {
        return windows.chars().map(letter -> Alphabet.toIndex((char) letter)).boxed().toList();
    }
}
