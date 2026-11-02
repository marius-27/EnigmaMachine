package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReflectorTest {

    @Test
    void usesHistoricalWiring() {
        assertEquals(Alphabet.toIndex('Y'), Reflector.B.reflect(Alphabet.toIndex('A')));
        assertEquals(Alphabet.toIndex('F'), Reflector.C.reflect(Alphabet.toIndex('A')));
    }

    @Test
    void connectsLettersInReciprocalPairs() {
        for (Reflector reflector : Reflector.values()) {
            for (int input = 0; input < Alphabet.SIZE; input++) {
                assertEquals(input, reflector.reflect(reflector.reflect(input)), reflector.name());
            }
        }
    }

    @Test
    void neverConnectsLetterToItself() {
        for (Reflector reflector : Reflector.values()) {
            for (int input = 0; input < Alphabet.SIZE; input++) {
                assertNotEquals(input, reflector.reflect(input), reflector.name());
            }
        }
    }

    @Test
    void rejectsSignalsOutsideAlphabet() {
        assertThrows(IllegalArgumentException.class, () -> Reflector.B.reflect(26));
        assertThrows(IllegalArgumentException.class, () -> Reflector.B.reflect(-1));
    }
}
