package com.example.enigmamachine.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The A-Z keyboard, in the historical Enigma layout. It accepts both mouse clicks and the physical keyboard,
 * which reaches it through {@link SceneInput} while its view is the active one.
 */
public final class KeyboardView extends VBox implements KeyInputTarget {

    static final List<String> ROWS =List.of("QWERTZUIO", "ASDFGHJK", "PYXCVBNML");
    private static final double SPACING = 8;
    private static final PseudoClass HELD = PseudoClass.getPseudoClass("held");

    private final Map<Character, Button> keys = new HashMap<>();
    private final KeyPressTracker physicalKeys = new KeyPressTracker();
    private Consumer<Character> onLetterPressed = letter -> { };
    private Runnable onBackspacePressed = () -> { };
    private Runnable onPastePressed = () -> { };

    public KeyboardView() {
        super(SPACING);
        setAlignment(Pos.CENTER);
        for (String row : ROWS) {
            HBox rowBox = new HBox(SPACING);
            rowBox.setAlignment(Pos.CENTER);
            for (char letter : row.toCharArray()) {
                rowBox.getChildren().add(createKey(letter));
            }
            getChildren().add(rowBox);
        }
    }

    public void setOnLetterPressed(Consumer<Character> handler) {
        onLetterPressed = Objects.requireNonNull(handler, "Letter handler is required.");
    }

    public void setOnBackspacePressed(Runnable handler) {
        onBackspacePressed = Objects.requireNonNull(handler, "Backspace handler is required.");
    }

    public void setOnPastePressed(Runnable handler) {
        onPastePressed = Objects.requireNonNull(handler, "Paste handler is required.");
    }

    private Button createKey(char letter) {
        Button key = new Button(String.valueOf(letter));
        key.getStyleClass().add("keyboard-key");
        // Without focus, Space or Enter cannot trigger a key that was clicked earlier.
        key.setFocusTraversable(false);
        key.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                onLetterPressed.accept(letter);
            }
        });
        keys.put(letter, key);
        return key;
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.BACK_SPACE) {
            // Unlike a letter, Backspace keeps deleting while it is held down.
            if (!event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
                onBackspacePressed.run();
            }
            return;
        }
        if (event.getCode() == KeyCode.V && event.isShortcutDown() && !event.isAltDown()) {
            // Holding the keys must not paste the same text again and again.
            if (physicalKeys.press(KeyCode.V, 'V')) {
                onPastePressed.run();
            }
            return;
        }
        letterOf(event).ifPresent(letter -> {
            if (physicalKeys.press(event.getCode(), letter)) {
                keys.get(letter).pseudoClassStateChanged(HELD, true);
                onLetterPressed.accept(letter);
            }
        });
    }

    @Override
    public void keyReleased(KeyEvent event) {
        physicalKeys.release(event.getCode())
                .ifPresent(letter -> keys.get(letter).pseudoClassStateChanged(HELD, false));
    }

    @Override
    public void releaseAllKeys() {
        physicalKeys.releaseAll();
        keys.values().forEach(key -> key.pseudoClassStateChanged(HELD, false));
    }

    private static Optional<Character> letterOf(KeyEvent event) {
        // Ctrl and Alt together mean AltGr, which types characters such as a T with comma on Romanian layouts.
        boolean altGr = event.isControlDown() && event.isAltDown();
        boolean shortcut = event.isMetaDown() || (event.isControlDown() != event.isAltDown());
        if (shortcut) {
            return Optional.empty();
        }
        String text = event.getCode() == KeyCode.SPACE ? " " : event.getText();
        Optional<Character> typed = text.isEmpty() ? Optional.empty() : LetterFolding.toLetter(text.charAt(0));
        if (typed.isPresent() || altGr) {
            return typed;
        }
        // Non-Latin layouts type other alphabets, but the key code still names the Latin letter.
        return event.getCode().isLetterKey() ? Optional.of(event.getCode().getName().charAt(0)) : Optional.empty();
    }
}
