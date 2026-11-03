package com.example.enigmamachine.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import javafx.scene.input.KeyCode;

/**
 * Tells a new key press apart from the repeats the operating system sends while a key is held down.
 * Each key remembers its letter, because a key release does not always say which character it typed.
 */
final class KeyPressTracker {

    private final Map<KeyCode, Character> heldKeys = new EnumMap<>(KeyCode.class);

    /** Returns true only for the first press of a key that is not already held. */
    boolean press(KeyCode key, char letter) {
        return heldKeys.putIfAbsent(key, letter) == null;
    }

    /** Returns the letter the key was pressed with, or empty if the key was not being tracked. */
    Optional<Character> release(KeyCode key) {
        return Optional.ofNullable(heldKeys.remove(key));
    }

    void releaseAll() {
        heldKeys.clear();
    }
}
