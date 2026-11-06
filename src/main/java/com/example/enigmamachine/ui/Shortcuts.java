package com.example.enigmamachine.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;

/**
 * Keyboard shortcuts for the actions on screen. The controls stay out of the focus order, because a focused
 * control would react to Space and Enter while a message is typed, so shortcuts are how the keyboard reaches them.
 * Every shortcut uses Ctrl, which the typing keyboard ignores, so a shortcut never types a letter.
 */
final class Shortcuts {

    private final Map<KeyCombination, Runnable> actions = new LinkedHashMap<>();

    /**
     * Fires the button on the given keys, as a click would. A disabled button ignores them, like a click.
     * The shortcut is added to the button's tooltip so it can be discovered.
     */
    void add(String keys, ButtonBase button) {
        KeyCombination combination = add(keys, button::fire);
        String shortcut = "Shortcut: " + combination.getDisplayText();
        Tooltip tooltip = button.getTooltip();
        if (tooltip == null) {
            button.setTooltip(new Tooltip(shortcut));
        } else {
            tooltip.setText(tooltip.getText() + "\n" + shortcut);
        }
        button.setAccessibleHelp(button.getTooltip().getText());
    }

    KeyCombination add(String keys, Runnable action) {
        KeyCombination combination = KeyCombination.valueOf(keys);
        if (actions.putIfAbsent(combination, Objects.requireNonNull(action, "Shortcut action is required.")) != null) {
            throw new IllegalArgumentException("The shortcut " + keys + " is already in use.");
        }
        return combination;
    }

    /** The shortcuts and their actions; {@link SceneInput} puts them on the scene while their view is active. */
    Map<KeyCombination, Runnable> actions() {
        return Collections.unmodifiableMap(actions);
    }
}
