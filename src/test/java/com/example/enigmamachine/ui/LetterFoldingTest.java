package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class LetterFoldingTest {

    @Test
    void romanianDiacriticsFoldToTheirBaseLetter() {
        assertFolds("ȚțŢţ", 'T');
        assertFolds("ȘșŞş", 'S');
        assertFolds("ĂăÂâ", 'A');
        assertFolds("Îî", 'I');
    }

    @Test
    void plainLettersAreOnlyUpperCased() {
        assertFolds("qQ", 'Q');
    }

    @Test
    void charactersWithoutAnEnigmaLetterAreRejected() {
        for (char typed : "7 @€фß".toCharArray()) {
            assertEquals(Optional.empty(), LetterFolding.toLetter(typed), "Typed: " + typed);
        }
    }

    private static void assertFolds(String typedCharacters, char expected) {
        for (char typed : typedCharacters.toCharArray()) {
            assertEquals(Optional.of(expected), LetterFolding.toLetter(typed), "Typed: " + typed);
        }
    }
}
