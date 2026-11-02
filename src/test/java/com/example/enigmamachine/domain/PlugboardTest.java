package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlugboardTest {

    private static final int A = Alphabet.toIndex('A');
    private static final int M = Alphabet.toIndex('M');
    private static final int Z = Alphabet.toIndex('Z');

    @Test
    void leavesLettersUnchangedWithoutPairs() {
        Plugboard plugboard = new Plugboard(List.of());

        for (int input = 0; input < Alphabet.SIZE; input++) {
            assertEquals(input, plugboard.swap(input));
        }
    }

    @Test
    void connectsLettersInBothDirections() {
        Plugboard plugboard = new Plugboard(List.of("AM"));

        assertEquals(M, plugboard.swap(A));
        assertEquals(A, plugboard.swap(M));
    }

    @Test
    void leavesUnconnectedLettersUnchanged() {
        Plugboard plugboard = new Plugboard(List.of("AM"));

        assertEquals(Z, plugboard.swap(Z));
    }

    @Test
    void acceptsLowerCasePairs() {
        Plugboard plugboard = new Plugboard(List.of("am"));

        assertEquals(M, plugboard.swap(A));
    }

    @Test
    void rejectsLetterConnectedToItself() {
        assertThrows(IllegalArgumentException.class, () -> new Plugboard(List.of("AA")));
    }

    @Test
    void rejectsLetterConnectedTwice() {
        assertThrows(IllegalArgumentException.class, () -> new Plugboard(List.of("AM", "AB")));
        assertThrows(IllegalArgumentException.class, () -> new Plugboard(List.of("AM", "BA")));
        assertThrows(IllegalArgumentException.class, () -> new Plugboard(List.of("AM", "AM")));
    }

    @Test
    void rejectsMalformedPairs() {
        assertThrows(IllegalArgumentException.class, () -> new Plugboard(List.of("A")));
        assertThrows(IllegalArgumentException.class, () -> new Plugboard(List.of("ABC")));
        assertThrows(IllegalArgumentException.class, () -> new Plugboard(List.of("A1")));
    }

    @Test
    void rejectsSignalsOutsideAlphabet() {
        Plugboard plugboard = new Plugboard(List.of());

        assertThrows(IllegalArgumentException.class, () -> plugboard.swap(26));
    }
}
