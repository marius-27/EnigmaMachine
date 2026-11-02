package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnigmaMachineTest {

    private static final int A = 0;
    private static final int Z = 25;

    @Test
    void followsFullSignalPath() {
        // The right rotor steps from Z to A before the signal, so the signal crosses position AAA.
        assertEquals('U', machineStartingAt(Z, List.of()).encrypt('A'));
    }

    @Test
    void matchesKnownOutputFromStartAAA() {
        // Rotors I-II-III, reflector B, rings AAA: AAAAA encrypts to BDZGO from start AAA.
        EnigmaMachine machine = machineStartingAt(A, List.of());

        assertEquals('B', machine.encrypt('A'));
        assertEquals('D', machine.encrypt('A'));
        assertEquals('Z', machine.encrypt('A'));
        assertEquals('G', machine.encrypt('A'));
        assertEquals('O', machine.encrypt('A'));
    }

    @Test
    void stepsRightRotorOnceForEachLetter() {
        EnigmaMachine machine = machineStartingAt(A, List.of());

        machine.encrypt('A');
        assertEquals(List.of(0, 0, 1), machine.positions());

        machine.encrypt('A');
        assertEquals(List.of(0, 0, 2), machine.positions());
    }

    @Test
    void acceptsLowerCaseLetters() {
        assertEquals('U', machineStartingAt(Z, List.of()).encrypt('a'));
    }

    @Test
    void rejectsInvalidLettersWithoutStepping() {
        EnigmaMachine machine = machineStartingAt(A, List.of());

        assertThrows(IllegalArgumentException.class, () -> machine.encrypt('7'));
        assertEquals(List.of(0, 0, 0), machine.positions());
    }

    @Test
    void requiresConfiguration() {
        assertThrows(NullPointerException.class, () -> new EnigmaMachine(null));
    }

    @Test
    void resetRestoresInitialPositions() {
        EnigmaMachine machine = machineStartingAt(A, List.of());
        machine.encrypt('A');
        machine.encrypt('A');

        machine.reset();

        assertEquals(List.of(0, 0, 0), machine.positions());
        assertEquals('B', machine.encrypt('A'));
    }

    @Test
    void appliesPlugboardBeforeAndAfterRotors() {
        // Without plugboard, position AAA encrypts A to U.
        assertEquals('Z', machineStartingAt(Z, List.of("UZ")).encrypt('A'));
        assertEquals('U', machineStartingAt(Z, List.of("AB")).encrypt('B'));
    }

    @Test
    void encryptionIsReciprocal() {
        for (MachineConfiguration configuration : sampleConfigurations()) {
            EnigmaMachine machine = new EnigmaMachine(configuration);
            for (int index = 0; index < Alphabet.SIZE; index++) {
                char letter = Alphabet.toLetter(index);
                char encrypted = machine.encrypt(letter);
                machine.reset();
                assertEquals(letter, machine.encrypt(encrypted), configuration.toString());
                machine.reset();
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

    private static EnigmaMachine machineStartingAt(int rightPosition, List<String> plugboardPairs) {
        return new EnigmaMachine(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 0, rightPosition),
                plugboardPairs));
    }
}
