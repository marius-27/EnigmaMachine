package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RotorDefinitionTest {

    @Test
    void definesFiveHistoricalRotors() {
        assertEquals(5, RotorDefinition.values().length);
    }

    @Test
    void everyWiringUsesEachLetterExactlyOnce() {
        for (RotorDefinition rotor : RotorDefinition.values()) {
            String wiring = rotor.wiring();
            assertEquals(Alphabet.SIZE, wiring.length(), rotor.name());
            for (int index = 0; index < Alphabet.SIZE; index++) {
                assertTrue(wiring.indexOf(Alphabet.toLetter(index)) >= 0, rotor.name());
            }
        }
    }

    @Test
    void placesNotchesAtHistoricalPositions() {
        assertTrue(RotorDefinition.I.hasNotchAt(Alphabet.toIndex('Q')));
        assertTrue(RotorDefinition.II.hasNotchAt(Alphabet.toIndex('E')));
        assertTrue(RotorDefinition.III.hasNotchAt(Alphabet.toIndex('V')));
        assertTrue(RotorDefinition.IV.hasNotchAt(Alphabet.toIndex('J')));
        assertTrue(RotorDefinition.V.hasNotchAt(Alphabet.toIndex('Z')));
    }

    @Test
    void everyRotorHasExactlyOneNotch() {
        for (RotorDefinition rotor : RotorDefinition.values()) {
            int notches = 0;
            for (int position = 0; position < Alphabet.SIZE; position++) {
                if (rotor.hasNotchAt(position)) {
                    notches++;
                }
            }
            assertEquals(1, notches, rotor.name());
        }
    }

    @Test
    void hasNoNotchNextToHistoricalPosition() {
        assertFalse(RotorDefinition.I.hasNotchAt(Alphabet.toIndex('R')));
    }

    @Test
    void rejectsPositionsOutsideAlphabet() {
        assertThrows(IllegalArgumentException.class, () -> RotorDefinition.I.hasNotchAt(26));
    }
}
