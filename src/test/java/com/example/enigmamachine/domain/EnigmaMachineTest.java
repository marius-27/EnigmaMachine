package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnigmaMachineTest {

    @Test
    void followsFullSignalPath() {
        assertEquals('U', machineWithRightRotorAt('A').encrypt('A'));
    }

    @Test
    void matchesKnownOutputForEachRightRotorPosition() {
        // Rotors I-II-III, reflector B, rings AAA: AAAAA encrypts to BDZGO from start AAA.
        // Stepping is not implemented yet, so each position is set explicitly.
        assertEquals('B', machineWithRightRotorAt('B').encrypt('A'));
        assertEquals('D', machineWithRightRotorAt('C').encrypt('A'));
        assertEquals('Z', machineWithRightRotorAt('D').encrypt('A'));
        assertEquals('G', machineWithRightRotorAt('E').encrypt('A'));
        assertEquals('O', machineWithRightRotorAt('F').encrypt('A'));
    }

    @Test
    void acceptsLowerCaseLetters() {
        assertEquals('U', machineWithRightRotorAt('A').encrypt('a'));
    }

    @Test
    void rejectsInvalidLetters() {
        EnigmaMachine machine = machineWithRightRotorAt('A');

        assertThrows(IllegalArgumentException.class, () -> machine.encrypt('7'));
    }

    @Test
    void requiresConfiguration() {
        assertThrows(NullPointerException.class, () -> new EnigmaMachine(null));
    }

    @Test
    void resetKeepsConfiguredOutput() {
        EnigmaMachine machine = machineWithRightRotorAt('B');

        machine.reset();

        assertEquals('B', machine.encrypt('A'));
    }

    @Test
    void appliesPlugboardBeforeAndAfterRotors() {
        // Without plugboard, position AAA encrypts A to U.
        assertEquals('Z', machineWithPlugboard(List.of("UZ")).encrypt('A'));
        assertEquals('U', machineWithPlugboard(List.of("AB")).encrypt('B'));
    }

    @Test
    void encryptionIsReciprocal() {
        for (MachineConfiguration configuration : sampleConfigurations()) {
            EnigmaMachine machine = new EnigmaMachine(configuration);
            for (int index = 0; index < Alphabet.SIZE; index++) {
                char letter = Alphabet.toLetter(index);
                assertEquals(letter, machine.encrypt(machine.encrypt(letter)), configuration.toString());
            }
        }
    }

    @Test
    void neverEncryptsLetterToItself() {
        for (MachineConfiguration configuration : sampleConfigurations()) {
            EnigmaMachine machine = new EnigmaMachine(configuration);
            for (int index = 0; index < Alphabet.SIZE; index++) {
                char letter = Alphabet.toLetter(index);
                assertNotEquals(letter, machine.encrypt(letter), configuration.toString());
            }
        }
    }

    private static List<MachineConfiguration> sampleConfigurations() {
        return List.of(
                new MachineConfiguration(
                        List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                        Reflector.B,
                        List.of(0, 0, 0),
                        List.of(0, 0, 0),
                        List.of()),
                new MachineConfiguration(
                        List.of(RotorDefinition.IV, RotorDefinition.V, RotorDefinition.I),
                        Reflector.C,
                        List.of(1, 12, 25),
                        List.of(7, 3, 19),
                        List.of("AM", "BT", "QZ")),
                new MachineConfiguration(
                        List.of(RotorDefinition.III, RotorDefinition.I, RotorDefinition.V),
                        Reflector.B,
                        List.of(25, 0, 13),
                        List.of(25, 25, 25),
                        List.of("AB", "CD", "EF", "GH", "IJ", "KL", "MN", "OP", "QR", "ST", "UV", "WX", "YZ")));
    }

    private static EnigmaMachine machineWithPlugboard(List<String> pairs) {
        return new EnigmaMachine(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 0, 0),
                pairs));
    }

    private static EnigmaMachine machineWithRightRotorAt(char position) {
        return new EnigmaMachine(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 0, Alphabet.toIndex(position)),
                List.of()));
    }
}
