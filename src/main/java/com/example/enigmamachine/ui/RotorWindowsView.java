package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Shows the rotor letters the machine reports, rolling a letter into place when a rotor steps forward.
 * The stepping itself stays in the engine: this view only reacts to the positions it is given, and the
 * real letter is in place from the first moment of the animation.
 */
public final class RotorWindowsView extends HBox {

    private static final double SPACING = 8;
    private static final double WINDOW_WIDTH = 44;
    private static final double WINDOW_HEIGHT = 44;
    private static final Duration ROLL_DURATION = Duration.millis(120);

    private final List<RotorWindow> windows = new ArrayList<>();

    public RotorWindowsView(ObservableValue<String> rotorWindows) {
        super(SPACING);
        setAlignment(Pos.CENTER);
        getStyleClass().add("rotor-windows");
        for (char letter : rotorWindows.getValue().toCharArray()) {
            RotorWindow window = new RotorWindow(letter);
            windows.add(window);
            getChildren().add(window);
        }
        rotorWindows.addListener((observable, previous, current) -> {
            for (int slot = 0; slot < windows.size(); slot++) {
                windows.get(slot).show(current.charAt(slot));
            }
        });
    }

    /** Only a single step forward rolls. Backspace, Reset and Apply jump straight to the new letter. */
    static boolean isForwardStep(char previous, char current) {
        return (Alphabet.toIndex(previous) + 1) % Alphabet.SIZE == Alphabet.toIndex(current);
    }

    private static final class RotorWindow extends StackPane {

        private final Label shown;
        private final Label leaving = letterLabel("");
        private final ParallelTransition roll;

        RotorWindow(char letter) {
            shown = letterLabel(String.valueOf(letter));
            shown.getStyleClass().add("rotor-window-current");
            leaving.setVisible(false);

            getStyleClass().add("rotor-window");
            setMinSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            setMaxSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            setClip(new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT));
            getChildren().addAll(leaving, shown);

            TranslateTransition rollIn = new TranslateTransition(ROLL_DURATION, shown);
            rollIn.setFromY(WINDOW_HEIGHT);
            rollIn.setToY(0);
            TranslateTransition rollOut = new TranslateTransition(ROLL_DURATION, leaving);
            rollOut.setFromY(0);
            rollOut.setToY(-WINDOW_HEIGHT);
            roll = new ParallelTransition(rollIn, rollOut);
            roll.setOnFinished(event -> leaving.setVisible(false));
        }

        void show(char letter) {
            char previous = shown.getText().charAt(0);
            if (letter == previous) {
                return;
            }
            // A newer key press takes over from an animation that is still running.
            roll.stop();
            shown.setText(String.valueOf(letter));
            if (isForwardStep(previous, letter)) {
                leaving.setText(String.valueOf(previous));
                leaving.setVisible(true);
                roll.playFromStart();
            } else {
                leaving.setVisible(false);
                shown.setTranslateY(0);
            }
        }

        private static Label letterLabel(String text) {
            Label label = new Label(text);
            label.getStyleClass().add("rotor-window-letter");
            return label;
        }
    }
}
