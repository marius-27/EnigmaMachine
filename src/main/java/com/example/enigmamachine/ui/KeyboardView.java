package com.example.enigmamachine.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The A-Z keyboard, in the historical Enigma layout. It accepts both mouse clicks and the physical keyboard.
 */
public final class KeyboardView extends VBox {

    private static final List<String> ROWS = List.of("QWERTZUIO", "ASDFGHJK", "PYXCVBNML");
    private static final double SPACING = 8;
    private static final PseudoClass HELD = PseudoClass.getPseudoClass("held");

    private final Map<Character, Button> keys = new HashMap<>();
    private final KeyPressTracker physicalKeys = new KeyPressTracker();
    private Consumer<Character> onLetterPressed = letter -> { };

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

    /** Listens to the physical keyboard while the given stage has its scene shown. */
    public void attachTo(Stage stage) {
        Scene scene = stage.getScene();
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handlePhysicalPress);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, this::handlePhysicalRelease);
        // A key released in another window sends no event here and would otherwise stay held forever.
        stage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                releaseAllPhysicalKeys();
            }
        });
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

    private void handlePhysicalPress(KeyEvent event) {
        if (!event.getCode().isLetterKey() || event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }
        char letter = letterOf(event);
        if (physicalKeys.press(letter)) {
            keys.get(letter).pseudoClassStateChanged(HELD, true);
            onLetterPressed.accept(letter);
        }
    }

    private void handlePhysicalRelease(KeyEvent event) {
        if (event.getCode().isLetterKey()) {
            char letter = letterOf(event);
            physicalKeys.release(letter);
            keys.get(letter).pseudoClassStateChanged(HELD, false);
        }
    }

    private void releaseAllPhysicalKeys() {
        physicalKeys.releaseAll();
        keys.values().forEach(key -> key.pseudoClassStateChanged(HELD, false));
    }

    private static char letterOf(KeyEvent event) {
        return event.getCode().getName().charAt(0);
    }
}
