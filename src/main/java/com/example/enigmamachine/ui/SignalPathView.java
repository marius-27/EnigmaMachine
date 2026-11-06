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
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Lights up the components the last letter crossed, one after another, using the trace the engine reported.
 * The letter is already encrypted by then, so the controls here cannot change any result.
 */
public final class SignalPathView extends VBox {

    private static final double SPACING = 4;
    private static final Duration STEP_DURATION = Duration.millis(300);
    private static final List<Double> SPEEDS = List.of(0.5, 1.0, 2.0, 4.0);
    private static final double NORMAL_SPEED = 1.0;
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
    private final CheckBox educationalMode = new CheckBox("Educational mode");
    private final ObjectProperty<Double> speed = new SimpleObjectProperty<>(NORMAL_SPEED);
    private final Timeline timeline;
    private boolean active = true;

    public SignalPathView(ObservableValue<SignalTrace> lastTrace, Shortcuts shortcuts) {
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

        timeline = new Timeline(new KeyFrame(STEP_DURATION, event -> playNextStep()));
        timeline.setCycleCount(Animation.INDEFINITE);
        speed.addListener((observable, previous, current) -> timeline.setRate(current));

        getChildren().add(controls(shortcuts));

        playback.activeStepProperty().addListener((observable, previous, current) -> refreshRows());
        lastTrace.addListener((observable, previous, current) -> show(current));
    }

    private Node controls(Shortcuts shortcuts) {
        BooleanBinding playing = timeline.statusProperty().isEqualTo(Animation.Status.RUNNING);

        Button play = controlButton("signal-play", this::togglePlaying);
        play.textProperty().bind(Bindings.when(playing).then("Pause").otherwise("Play"));
        play.disableProperty().bind(playback.activeStepProperty().isEqualTo(SignalPlayback.NO_STEP));

        Button step = controlButton("signal-step-button", this::stepOnce);
        step.setText("Step");
        step.disableProperty().bind(playback.canAdvanceProperty().not());

        Node speedSelector = SettingsView.selector("signal-speed", SPEEDS, speed, SignalPathView::speedLabel);
        speedSelector.setAccessibleText("Animation speed");

        educationalMode.getStyleClass().add("signal-educational");
        educationalMode.setTooltip(new Tooltip("Each new letter waits at its first step until you press Play or Step."));
        // A focused check box would toggle on Space while a message is being typed.
        educationalMode.setFocusTraversable(false);
        educationalMode.selectedProperty().addListener((observable, wasOn, isOn) -> {
            if (isOn) {
                timeline.stop();
            }
        });

        shortcuts.add("Shortcut+P", play);
        shortcuts.add("Shortcut+N", step);
        shortcuts.add("Shortcut+E", educationalMode);

        HBox buttons = new HBox(8, play, step, new Label("Speed"), speedSelector);
        buttons.setAlignment(Pos.CENTER_LEFT);
        VBox controls = new VBox(8, buttons, educationalMode);
        controls.setPadding(new Insets(8, 0, 0, 0));
        return controls;
    }

    private static Button controlButton(String styleClass, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add(styleClass);
        // A focused button would fire on Space while a message is being typed.
        button.setFocusTraversable(false);
        button.setOnAction(event -> action.run());
        return button;
    }

    /** The speed the path plays at, as a multiple of the normal speed. */
    double playbackRate() {
        return timeline.getRate();
    }

    boolean isPlaying() {
        return timeline.getStatus() == Animation.Status.RUNNING;
    }

    /**
     * A hidden view stops playing and does not start again for new letters, but it keeps showing the last trace,
     * so the path can be replayed once the view is visible again.
     */
    void setActive(boolean isActive) {
        active = isActive;
        if (!isActive) {
            timeline.stop();
        }
    }

    private void togglePlaying() {
        if (isPlaying()) {
            timeline.stop();
            return;
        }
        if (!playback.canAdvanceProperty().get()) {
            playback.restart();
        }
        timeline.playFromStart();
    }

    private void stepOnce() {
        timeline.stop();
        playback.advance();
    }

    private void playNextStep() {
        playback.advance();
        if (!playback.canAdvanceProperty().get()) {
            timeline.stop();
        }
    }

    private void show(SignalTrace trace) {
        timeline.stop();
        summary.setText(trace == null ? NO_TRACE : describe(trace));
        for (int index = 0; index < rows.size(); index++) {
            rows.get(index).setStep(trace == null ? null : trace.steps().get(index));
        }
        playback.show(trace);
        refreshRows();
        if (trace != null && active && !educationalMode.isSelected()) {
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

    private static String speedLabel(double rate) {
        return (rate == Math.rint(rate) ? String.valueOf((int) rate) : String.valueOf(rate)) + "×";
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
