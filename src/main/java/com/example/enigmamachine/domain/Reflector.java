package com.example.enigmamachine.domain;

public enum Reflector {

    B("YRUHQSLDPXNGOKMIEBFZCWVJAT"),
    C("FVPJIAOYEDRZXWGCTKUQSBNMHL");

    private final String wiring;

    Reflector(String wiring) {
        this.wiring = wiring;
    }

    public int reflect(int input) {
        return Alphabet.toIndex(wiring.charAt(Alphabet.requireIndex(input)));
    }
}
