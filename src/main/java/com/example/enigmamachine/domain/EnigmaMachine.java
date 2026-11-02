package com.example.enigmamachine.domain;

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
        left = createRotor(LEFT);
        middle = createRotor(MIDDLE);
        right = createRotor(RIGHT);
    }

    public char encrypt(char letter) {
        int signal = plugboard.swap(Alphabet.toIndex(letter));
        signal = right.forward(signal);
        signal = middle.forward(signal);
        signal = left.forward(signal);
        signal = configuration.reflector().reflect(signal);
        signal = left.reverse(signal);
        signal = middle.reverse(signal);
        signal = right.reverse(signal);
        return Alphabet.toLetter(plugboard.swap(signal));
    }

    private Rotor createRotor(int slot) {
        return new Rotor(
                configuration.rotors().get(slot),
                configuration.ringSettings().get(slot),
                configuration.positions().get(slot));
    }
}
