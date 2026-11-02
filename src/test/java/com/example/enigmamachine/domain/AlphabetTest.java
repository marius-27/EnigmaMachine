package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlphabetTest {

    @Test
    void mapsLimitsOfAlphabet() {
        assertEquals(0, Alphabet.toIndex('A'));
        assertEquals(25, Alphabet.toIndex('Z'));
        assertEquals('A', Alphabet.toLetter(0));
        assertEquals('Z', Alphabet.toLetter(25));
    }

    @Test
    void convertsEveryIndexBackAndForth() {
        for (int index = 0; index < Alphabet.SIZE; index++) {
            assertEquals(index, Alphabet.toIndex(Alphabet.toLetter(index)));
        }
    }

    @Test
    void treatsLowerCaseAsUpperCase() {
        assertEquals('Q', Alphabet.normalize('q'));
        assertEquals(Alphabet.toIndex('Q'), Alphabet.toIndex('q'));
    }

    @Test
    void acceptsOnlyLatinLetters() {
        assertTrue(Alphabet.isValid('A'));
        assertTrue(Alphabet.isValid('z'));
        for (char invalid : new char[] {'@', '[', '`', '{', ' ', '7', 'Ă', 'ı'}) {
            assertFalse(Alphabet.isValid(invalid), "Should reject: " + invalid);
        }
    }

    @Test
    void rejectsInvalidLetters() {
        assertThrows(IllegalArgumentException.class, () -> Alphabet.normalize('7'));
        assertThrows(IllegalArgumentException.class, () -> Alphabet.toIndex(' '));
        assertThrows(IllegalArgumentException.class, () -> Alphabet.toIndex('ı'));
    }

    @Test
    void rejectsIndexesOutsideAlphabet() {
        assertThrows(IllegalArgumentException.class, () -> Alphabet.toLetter(-1));
        assertThrows(IllegalArgumentException.class, () -> Alphabet.toLetter(26));
    }
}
