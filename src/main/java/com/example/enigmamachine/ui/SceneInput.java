package com.example.enigmamachine.ui;

import java.util.Map;
import java.util.Objects;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * The one place that listens to the physical keyboard of a window. Its filters are added once, so a view that is
 * shown, hidden and shown again never receives a key twice; only the active view's keyboard and shortcuts are used.
 */
public final class SceneInput {

    private static final KeyInputTarget NO_TARGET = new KeyInputTarget() {
        @Override
        public void keyPressed(KeyEvent event) {
        }

        @Override
        public void keyReleased(KeyEvent event) {
        }

        @Override
        public void releaseAllKeys() {
        }
    };

    private final Stage stage;
    private final Scene scene;
    // A filter runs before the focused control, and a focused text area would otherwise swallow the keys.
    private final EventHandler<KeyEvent> pressFilter = event -> target().keyPressed(event);
    private final EventHandler<KeyEvent> releaseFilter = event -> target().keyReleased(event);
    // A key released in another window sends no event here and would otherwise stay held forever.
    private final ChangeListener<Boolean> focusListener = (observable, wasFocused, isFocused) -> {
        if (!isFocused) {
            target().releaseAllKeys();
        }
    };
    private KeyInputTarget target = NO_TARGET;
    private Map<KeyCombination, Runnable> shortcuts = Map.of();
    private boolean disposed;

    private SceneInput(Stage stage) {
        this.stage = stage;
        this.scene = Objects.requireNonNull(stage.getScene(), "The stage needs its scene before input is installed.");
        scene.addEventFilter(KeyEvent.KEY_PRESSED, pressFilter);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, releaseFilter);
        stage.focusedProperty().addListener(focusListener);
    }

    /** Starts listening to the keyboard of the stage's current scene. */
    public static SceneInput installOn(Stage stage) {
        return new SceneInput(Objects.requireNonNull(stage, "Stage is required."));
    }

    /** Sends the keyboard to the given target from now on. Keys held for the previous target are let go. */
    void setKeyTarget(KeyInputTarget newTarget) {
        Objects.requireNonNull(newTarget, "Key target is required.");
        requireInstalled();
        if (newTarget != target) {
            target.releaseAllKeys();
            target = newTarget;
        }
    }

    /** Stops sending the keyboard to the given target, if it is still the one receiving it. */
    void clearKeyTarget(KeyInputTarget oldTarget) {
        if (target == oldTarget) {
            target.releaseAllKeys();
            target = NO_TARGET;
        }
    }

    /** Replaces the shortcuts of the previous view with these. */
    void setShortcuts(Shortcuts newShortcuts) {
        requireInstalled();
        removeShortcuts();
        shortcuts = Map.copyOf(newShortcuts.actions());
        scene.getAccelerators().putAll(shortcuts);
    }

    /** Removes the given shortcuts, if they are still the ones in use. */
    void clearShortcuts(Shortcuts oldShortcuts) {
        if (shortcuts.equals(oldShortcuts.actions())) {
            removeShortcuts();
        }
    }

    /** Removes every listener this added, leaving the scene as it was. */
    public void dispose() {
        if (disposed) {
            return;
        }
        target.releaseAllKeys();
        target = NO_TARGET;
        removeShortcuts();
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, pressFilter);
        scene.removeEventFilter(KeyEvent.KEY_RELEASED, releaseFilter);
        stage.focusedProperty().removeListener(focusListener);
        disposed = true;
    }

    private KeyInputTarget target() {
        return target;
    }

    private void removeShortcuts() {
        // Only the entries this added are removed, in case something else put its own accelerators on the scene.
        shortcuts.forEach((keys, action) -> scene.getAccelerators().remove(keys, action));
        shortcuts = Map.of();
    }

    private void requireInstalled() {
        if (disposed) {
            throw new IllegalStateException("The input of this scene was already disposed.");
        }
    }
}
