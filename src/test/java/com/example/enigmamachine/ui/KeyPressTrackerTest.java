package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyPressTrackerTest {

    private final KeyPressTracker tracker = new KeyPressTracker();

    @Test
    void firstPressIsAccepted() {
        assertTrue(tracker.press('A'));
    }

    @Test
    void repeatsWhileHeldAreRejected() {
        tracker.press('A');

        assertFalse(tracker.press('A'));
        assertFalse(tracker.press('A'));
    }

    @Test
    void keyCanBePressedAgainAfterRelease() {
        tracker.press('A');
        tracker.release('A');

        assertTrue(tracker.press('A'));
    }

    @Test
    void differentKeysAreTrackedSeparately() {
        tracker.press('A');

        assertTrue(tracker.press('B'));
        assertFalse(tracker.press('A'));
    }

    @Test
    void releaseAllForgetsEveryHeldKey() {
        tracker.press('A');
        tracker.press('B');

        tracker.releaseAll();

        assertTrue(tracker.press('A'));
        assertTrue(tracker.press('B'));
    }
}
