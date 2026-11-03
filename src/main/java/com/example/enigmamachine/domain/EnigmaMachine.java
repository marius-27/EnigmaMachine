package com.example.enigmamachine.domain;

import java.util.List;
import java.util.Objects;

public final class EnigmaMachine {

    private static final int LEFT = 0;
    private static final int MIDDLE = 1;
    private static final int RIGHT = 2;

    private final MachineConfiguration configuration;
    private final Plugboard plugboard;
    private Rotor left;
    private Rotor middle;
    private Rotor right;

    public EnigmaMachine(MachineConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Machine configuration is required.");
        this.plugboard = new Plugboard(configuration.plugboardPairs());
        reset();
    }

    public void reset() {
        restorePositions(configuration.positions());
    }

    /**
     * Puts the rotors back to positions read earlier from {@link #positions()}.
     */
    public void restorePositions(List<Integer> positions) {
        if (positions.size() != configuration.rotors().size()) {
            throw new IllegalArgumentException("Expected 3 positions, got " + positions.size() + ".");
        }
        left = createRotor(LEFT, positions.get(LEFT));
        middle = createRotor(MIDDLE, positions.get(MIDDLE));
        right = createRotor(RIGHT, positions.get(RIGHT));
    }

    public List<Integer> positions() {
        return List.of(left.position(), middle.position(), right.position());
    }

    public char encrypt(char letter) {
        int input = Alphabet.toIndex(letter);
        SteppingMechanism.step(left, middle, right);
        int signal = plugboard.swap(input);
        signal = right.forward(signal);
        signal = middle.forward(signal);
        signal = left.forward(signal);
        signal = configuration.reflector().reflect(signal);
        signal = left.reverse(signal);
        signal = middle.reverse(signal);
        signal = right.reverse(signal);
        return Alphabet.toLetter(plugboard.swap(signal));
    }

    /**
     * Encrypts the letters of a message. Other characters are dropped and do not step the rotors.
     */
    public String encryptText(String text) {
        StringBuilder output = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            if (Alphabet.isValid(character)) {
                output.append(encrypt(character));
            }
        }
        return output.toString();
    }

    private Rotor createRotor(int slot, int position) {
        return new Rotor(configuration.rotors().get(slot), configuration.ringSettings().get(slot), position);
    }
}
