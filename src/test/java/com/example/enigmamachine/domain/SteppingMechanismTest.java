package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SteppingMechanismTest {

    @Test
    void stepsOnlyRightRotorAwayFromNotches() {
        assertStep("AAA", "AAB");
    }

    @Test
    void rightRotorNotchTurnsMiddleRotor() {
        assertStep("AAV", "ABW");
    }

    @Test
    void middleRotorNotchTurnsLeftAndMiddleRotors() {
        assertStep("AEW", "BFX");
    }

    private static void assertStep(String start, String expected) {
        Rotor left = rotor(RotorDefinition.I, start.charAt(0));
        Rotor middle = rotor(RotorDefinition.II, start.charAt(1));
        Rotor right = rotor(RotorDefinition.III, start.charAt(2));

        SteppingMechanism.step(left, middle, right);

        assertEquals(
                List.of(
                        Alphabet.toIndex(expected.charAt(0)),
                        Alphabet.toIndex(expected.charAt(1)),
                        Alphabet.toIndex(expected.charAt(2))),
                List.of(left.position(), middle.position(), right.position()));
    }

    private static Rotor rotor(RotorDefinition definition, char position) {
        return new Rotor(definition, 0, Alphabet.toIndex(position));
    }
}
