package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.SignalTrace;
import com.example.enigmamachine.domain.SignalTrace.Stage;
import com.example.enigmamachine.domain.SignalTrace.Step;
import com.example.enigmamachine.ui.viewmodel.SignalPlayback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Lights up the components the last letter crossed, one after another, using the trace the engine reported.
 */
public final class SignalPathView extends VBox {

    private static final double SPACING = 4;
    private static final Duration STEP_DURATION = Duration.millis(300);
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final String NO_TRACE = "Press a key to see its signal path.";
    private static final Map<Stage, String> STAGE_NAMES = Map.of(
            Stage.PLUGBOARD_IN, "Plugboard",
            Stage.RIGHT_ROTOR_FORWARD, "Right rotor",
            Stage.MIDDLE_ROTOR_FORWARD, "Middle rotor",
            Stage.LEFT_ROTOR_FORWARD, "Left rotor",
            Stage.REFLECTOR, "Reflector",
            Stage.LEFT_ROTOR_REVERSE, "Left rotor, back",
            Stage.MIDDLE_ROTOR_REVERSE, "Middle rotor, back",
            Stage.RIGHT_ROTOR_REVERSE, "Right rotor, back",
            Stage.PLUGBOARD_OUT, "Plugboard, back");

    private final SignalPlayback playback = new SignalPlayback();
    private final Label summary = new Label(NO_TRACE);
    private final List<StepRow> rows = new ArrayList<>();
    private final Timeline timeline;

    public SignalPathView(ObservableValue<SignalTrace> lastTrace) {
        super(SPACING);
        setPadding(new Insets(8));
        summary.getStyleClass().add("signal-summary");
        summary.setWrapText(true);
        getChildren().add(summary);
        for (Stage stage : Stage.values()) {
            StepRow row = new StepRow(STAGE_NAMES.get(stage));
            rows.add(row);
            getChildren().add(row);
        }

        timeline = new Timeline(new KeyFrame(STEP_DURATION, event -> playback.advance()));
        timeline.setCycleCount(rows.size() - 1);

        playback.activeStepProperty().addListener((observable, previous, current) -> refreshRows());
        lastTrace.addListener((observable, previous, current) -> show(current));
    }

    private void show(SignalTrace trace) {
        timeline.stop();
        summary.setText(trace == null ? NO_TRACE : describe(trace));
        for (int index = 0; index < rows.size(); index++) {
            rows.get(index).setStep(trace == null ? null : trace.steps().get(index));
        }
        playback.show(trace);
        refreshRows();
        if (trace != null) {
            timeline.playFromStart();
        }
    }

    private void refreshRows() {
        int active = playback.activeStepProperty().get();
        for (int index = 0; index < rows.size(); index++) {
            rows.get(index).setState(index == active, index <= active);
        }
    }

    private static String describe(SignalTrace trace) {
        String positions = trace.positions().stream()
                .map(position -> String.valueOf(Alphabet.toLetter(position)))
                .collect(Collectors.joining());
        return trace.input() + " → " + trace.output() + ", rotors at " + positions;
    }

    private static final class StepRow extends HBox {

        private final Label letters = new Label();

        StepRow(String name) {
            super(SPACING);
            getStyleClass().add("signal-step");
            letters.getStyleClass().add("signal-letters");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            getChildren().addAll(new Label(name), gap, letters);
        }

        void setStep(Step step) {
            letters.setText(step == null ? "" : step.input() + " → " + step.output());
        }

        /** A letter stays hidden until the signal has reached its component. */
        void setState(boolean active, boolean reached) {
            pseudoClassStateChanged(ACTIVE, active);
            letters.setVisible(reached);
        }
    }
}
