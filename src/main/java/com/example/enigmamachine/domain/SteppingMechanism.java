package com.example.enigmamachine.domain;

public final class SteppingMechanism {

    private SteppingMechanism() {
    }

    public static void step(Rotor left, Rotor middle, Rotor right) {
        if (middle.isAtNotch()) {
            // Double stepping: the middle rotor also moves itself when it pushes the left rotor.
            left.step();
            middle.step();
        } else if (right.isAtNotch()) {
            middle.step();
        }
        right.step();
    }
}
