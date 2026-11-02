package com.example.enigmamachine.domain;

import java.util.Objects;

public final class EnigmaMachine {

    private final Rotor left;
    private final Rotor middle;
    private final Rotor right;
    private final Reflector reflector;
    private final Plugboard plugboard;

    public EnigmaMachine(Rotor left, Rotor middle, Rotor right, Reflector reflector, Plugboard plugboard) {
        this.left = Objects.requireNonNull(left, "Left rotor is required.");
        this.middle = Objects.requireNonNull(middle, "Middle rotor is required.");
        this.right = Objects.requireNonNull(right, "Right rotor is required.");
        this.reflector = Objects.requireNonNull(reflector, "Reflector is required.");
        this.plugboard = Objects.requireNonNull(plugboard, "Plugboard is required.");
    }

    public char encrypt(char letter) {
        int signal = plugboard.swap(Alphabet.toIndex(letter));
        signal = right.forward(signal);
        signal = middle.forward(signal);
        signal = left.forward(signal);
        signal = reflector.reflect(signal);
        signal = left.reverse(signal);
        signal = middle.reverse(signal);
        signal = right.reverse(signal);
        return Alphabet.toLetter(plugboard.swap(signal));
    }
}
