package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import java.text.Normalizer;
import java.util.Optional;

/**
 * Reduces a typed character to the A-Z letter an Enigma key can produce, for example T for a typed T with comma.
 */
final class LetterFolding {

    private LetterFolding() {
    }

    static Optional<Character> toLetter(char typed) {
        char base = Normalizer.normalize(String.valueOf(typed), Normalizer.Form.NFD).charAt(0);
        return Alphabet.isValid(base) ? Optional.of(Alphabet.normalize(base)) : Optional.empty();
    }
}
