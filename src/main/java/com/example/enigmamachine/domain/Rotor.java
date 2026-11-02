package com.example.enigmamachine.domain;

import java.util.Objects;

public final class Rotor {

    private final RotorDefinition definition;
    private final int ringSetting;
    private int position;

    public Rotor(RotorDefinition definition, int ringSetting, int position) {
        this.definition = Objects.requireNonNull(definition, "Rotor definition is required.");
        this.ringSetting = Alphabet.requireIndex(ringSetting);
        this.position = Alphabet.requireIndex(position);
    }

    public int position() {
        return position;
    }

    public void step() {
        position = (position + 1) % Alphabet.SIZE;
    }

    public int forward(int input) {
        int contact = enter(input);
        return exit(Alphabet.toIndex(definition.wiring().charAt(contact)));
    }

    public int reverse(int input) {
        int contact = enter(input);
        return exit(definition.wiring().indexOf(Alphabet.toLetter(contact)));
    }

    private int enter(int input) {
        return Math.floorMod(Alphabet.requireIndex(input) + offset(), Alphabet.SIZE);
    }

    private int exit(int contact) {
        return Math.floorMod(contact - offset(), Alphabet.SIZE);
    }

    private int offset() {
        return position - ringSetting;
    }
}
