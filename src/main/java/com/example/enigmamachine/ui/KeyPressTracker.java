package com.example.enigmamachine.ui;

import java.util.HashSet;
import java.util.Set;

/**
 * Tells a new key press apart from the repeats the operating system sends while a key is held down.
 */
final class KeyPressTracker {

    private final Set<Character> heldKeys = new HashSet<>();

    /** Returns true only for the first press of a key that is not already held. */
    boolean press(char key) {
        return heldKeys.add(key);
    }

    void release(char key) {
        heldKeys.remove(key);
    }

    void releaseAll() {
        heldKeys.clear();
    }
}
