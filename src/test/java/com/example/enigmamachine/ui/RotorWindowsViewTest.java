package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RotorWindowsViewTest {

    @Test
    void nextLetterIsAForwardStep() {
        assertTrue(RotorWindowsView.isForwardStep('A', 'B'));
        assertTrue(RotorWindowsView.isForwardStep('Y', 'Z'));
    }

    @Test
    void zToAIsAForwardStep() {
        assertTrue(RotorWindowsView.isForwardStep('Z', 'A'));
    }

    @Test
    void goingBackIsNotAForwardStep() {
        assertFalse(RotorWindowsView.isForwardStep('B', 'A'));
        assertFalse(RotorWindowsView.isForwardStep('A', 'Z'));
    }

    @Test
    void jumpsAndUnchangedLettersAreNotForwardSteps() {
        assertFalse(RotorWindowsView.isForwardStep('A', 'C'));
        assertFalse(RotorWindowsView.isForwardStep('A', 'A'));
    }
}
