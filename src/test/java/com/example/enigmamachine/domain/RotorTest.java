package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RotorTest {

    @Test
    void usesPlainWiringAtPositionAAndRingA() {
        Rotor rotor = new Rotor(RotorDefinition.I, 0, 0);

        assertEquals(Alphabet.toIndex('E'), rotor.forward(Alphabet.toIndex('A')));
        assertEquals(Alphabet.toIndex('A'), rotor.reverse(Alphabet.toIndex('E')));
    }

    @Test
    void positionShiftsSignal() {
        Rotor rotor = new Rotor(RotorDefinition.I, 0, 1);

        assertEquals(Alphabet.toIndex('J'), rotor.forward(Alphabet.toIndex('A')));
    }

    @Test
    void ringSettingShiftsSignalInOppositeDirection() {
        Rotor rotor = new Rotor(RotorDefinition.I, 1, 0);

        assertEquals(Alphabet.toIndex('K'), rotor.forward(Alphabet.toIndex('A')));
    }

    @Test
    void equalPositionAndRingCancelOut() {
        Rotor plain = new Rotor(RotorDefinition.III, 0, 0);
        Rotor shifted = new Rotor(RotorDefinition.III, 7, 7);

        for (int input = 0; input < Alphabet.SIZE; input++) {
            assertEquals(plain.forward(input), shifted.forward(input));
        }
    }

    @Test
    void reverseUndoesForwardForEveryConfiguration() {
        for (RotorDefinition definition : RotorDefinition.values()) {
            for (int ring = 0; ring < Alphabet.SIZE; ring++) {
                for (int position = 0; position < Alphabet.SIZE; position++) {
                    Rotor rotor = new Rotor(definition, ring, position);
                    for (int input = 0; input < Alphabet.SIZE; input++) {
                        assertEquals(input, rotor.reverse(rotor.forward(input)));
                    }
                }
            }
        }
    }

    @Test
    void stepAdvancesPositionAndChangesSignal() {
        Rotor rotor = new Rotor(RotorDefinition.I, 0, 0);

        rotor.step();

        assertEquals(1, rotor.position());
        assertEquals(Alphabet.toIndex('J'), rotor.forward(Alphabet.toIndex('A')));
    }

    @Test
    void stepWrapsFromZToA() {
        Rotor rotor = new Rotor(RotorDefinition.I, 0, 25);

        rotor.step();

        assertEquals(0, rotor.position());
    }

    @Test
    void detectsNotchAtCurrentPosition() {
        assertTrue(new Rotor(RotorDefinition.I, 0, Alphabet.toIndex('Q')).isAtNotch());
        assertFalse(new Rotor(RotorDefinition.I, 0, Alphabet.toIndex('R')).isAtNotch());
    }

    @Test
    void notchFollowsWindowLetterRegardlessOfRingSetting() {
        assertTrue(new Rotor(RotorDefinition.I, 5, Alphabet.toIndex('Q')).isAtNotch());
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(NullPointerException.class, () -> new Rotor(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Rotor(RotorDefinition.I, 26, 0));
        assertThrows(IllegalArgumentException.class, () -> new Rotor(RotorDefinition.I, 0, -1));
    }

    @Test
    void rejectsSignalsOutsideAlphabet() {
        Rotor rotor = new Rotor(RotorDefinition.I, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> rotor.forward(26));
        assertThrows(IllegalArgumentException.class, () -> rotor.reverse(-1));
    }
}
