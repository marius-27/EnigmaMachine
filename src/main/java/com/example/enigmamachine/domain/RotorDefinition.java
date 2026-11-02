package com.example.enigmamachine.domain;

public enum RotorDefinition {

    I("EKMFLGDQVZNTOWYHXUSPAIBRCJ", "Q"),
    II("AJDKSIRUXBLHWTMCQGZNPYFVOE", "E"),
    III("BDFHJLCPRTXVZNYEIWGAKMUSQO", "V"),
    IV("ESOVPZJAYQUIRHXLNFTGKDCMWB", "J"),
    V("VZBRGITYUPSDNHLXAWMJQOFECK", "Z");

    private final String wiring;
    private final String notches;

    RotorDefinition(String wiring, String notches) {
        this.wiring = wiring;
        this.notches = notches;
    }

    public String wiring() {
        return wiring;
    }

    public boolean hasNotchAt(int position) {
        return notches.indexOf(Alphabet.toLetter(position)) >= 0;
    }
}
