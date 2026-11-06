package com.example.enigmamachine.ui;

import javafx.scene.input.KeyEvent;

/** Receives the physical keyboard from {@link SceneInput} while its view is the active one. */
interface KeyInputTarget {

    void keyPressed(KeyEvent event);

    void keyReleased(KeyEvent event);

    /** Forgets every key held down, because its release will not reach this target. */
    void releaseAllKeys();
}
