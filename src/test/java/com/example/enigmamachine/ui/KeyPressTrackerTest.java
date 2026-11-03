package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

class KeyPressTrackerTest {

    private final KeyPressTracker tracker = new KeyPressTracker();

    @Test
    void firstPressIsAccepted() {
        assertTrue(tracker.press(KeyCode.A, 'A'));
    }

    @Test
    void repeatsWhileHeldAreRejected() {
        tracker.press(KeyCode.A, 'A');

        assertFalse(tracker.press(KeyCode.A, 'A'));
        assertFalse(tracker.press(KeyCode.A, 'A'));
    }

    @Test
    void keyCanBePressedAgainAfterRelease() {
        tracker.press(KeyCode.A, 'A');
        tracker.release(KeyCode.A);

        assertTrue(tracker.press(KeyCode.A, 'A'));
    }

    @Test
    void differentKeysAreTrackedSeparately() {
        tracker.press(KeyCode.A, 'A');

        assertTrue(tracker.press(KeyCode.B, 'B'));
        assertFalse(tracker.press(KeyCode.A, 'A'));
    }

    @Test
    void releaseReturnsTheLetterTheKeyWasPressedWith() {
        tracker.press(KeyCode.QUOTE, 'T');

        assertEquals(Optional.of('T'), tracker.release(KeyCode.QUOTE));
    }

    @Test
    void releaseOfUntrackedKeyReturnsNothing() {
        assertEquals(Optional.empty(), tracker.release(KeyCode.A));
    }

    @Test
    void releaseAllForgetsEveryHeldKey() {
        tracker.press(KeyCode.A, 'A');
        tracker.press(KeyCode.B, 'B');

        tracker.releaseAll();

        assertTrue(tracker.press(KeyCode.A, 'A'));
        assertTrue(tracker.press(KeyCode.B, 'B'));
    }
}
