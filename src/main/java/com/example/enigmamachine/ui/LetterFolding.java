package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import java.text.Normalizer;
import java.util.Optional;

/**
 * Reduces a typed character to the A-Z letter an Enigma key can produce, for example T for a typed T with comma.
 * A space becomes X, the word separator operators used because the machine had no space key.
 */
final class LetterFolding {

    private static final char WORD_SEPARATOR = 'X';

    private LetterFolding() {
    }

    static Optional<Character> toLetter(char typed) {
        if (typed == ' ') {
            return Optional.of(WORD_SEPARATOR);
        }
        char base = Normalizer.normalize(String.valueOf(typed), Normalizer.Form.NFD).charAt(0);
        return Alphabet.isValid(base) ? Optional.of(Alphabet.normalize(base)) : Optional.empty();
    }
}
