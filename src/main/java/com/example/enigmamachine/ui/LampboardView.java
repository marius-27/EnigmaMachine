package com.example.enigmamachine.ui;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The lamps, laid out like the keyboard. The lamp of the last output letter stays on until the next key press.
 */
public final class LampboardView extends VBox {

    private static final double SPACING = 8;
    private static final PseudoClass LIT = PseudoClass.getPseudoClass("lit");

    private final Map<String, Label> lamps = new HashMap<>();

    public LampboardView(ObservableValue<String> litLamp) {
        super(SPACING);
        setAlignment(Pos.CENTER);
        for (String row : KeyboardView.ROWS) {
            HBox rowBox = new HBox(SPACING);
            rowBox.setAlignment(Pos.CENTER);
            for (char letter : row.toCharArray()) {
                rowBox.getChildren().add(createLamp(String.valueOf(letter)));
            }
            getChildren().add(rowBox);
        }
        litLamp.addListener((observable, previous, current) -> {
            setLit(previous, false);
            setLit(current, true);
        });
    }

    private Label createLamp(String letter) {
        Label lamp = new Label(letter);
        lamp.getStyleClass().add("lamp");
        lamp.setAccessibleText(lampDescription(letter, false));
        lamps.put(letter, lamp);
        return lamp;
    }

    private void setLit(String letter, boolean lit) {
        if (!letter.isEmpty()) {
            Label lamp = lamps.get(letter);
            lamp.pseudoClassStateChanged(LIT, lit);
            lamp.setAccessibleText(lampDescription(letter, lit));
        }
    }

    /** The glow is the only sign of a lit lamp on screen, so screen readers are told in words. */
    static String lampDescription(String letter, boolean lit) {
        return "Lamp " + letter + (lit ? ", lit" : "");
    }
}
