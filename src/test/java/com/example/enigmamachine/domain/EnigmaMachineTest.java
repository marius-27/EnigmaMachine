package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void requiresEveryComponent() {
        Rotor rotor = new Rotor(RotorDefinition.I, 0, 0);
        Plugboard plugboard = new Plugboard(List.of());

        assertThrows(NullPointerException.class, () -> new EnigmaMachine(null, rotor, rotor, Reflector.B, plugboard));
        assertThrows(NullPointerException.class, () -> new EnigmaMachine(rotor, null, rotor, Reflector.B, plugboard));
        assertThrows(NullPointerException.class, () -> new EnigmaMachine(rotor, rotor, null, Reflector.B, plugboard));
        assertThrows(NullPointerException.class, () -> new EnigmaMachine(rotor, rotor, rotor, null, plugboard));
        assertThrows(NullPointerException.class, () -> new EnigmaMachine(rotor, rotor, rotor, Reflector.B, null));
    }

    private static EnigmaMachine machineWithRightRotorAt(char position) {
        return new EnigmaMachine(
                new Rotor(RotorDefinition.I, 0, 0),
                new Rotor(RotorDefinition.II, 0, 0),
                new Rotor(RotorDefinition.III, 0, Alphabet.toIndex(position)),
                Reflector.B,
                new Plugboard(List.of()));
    }
}
