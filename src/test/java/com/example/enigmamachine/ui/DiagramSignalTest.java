package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.domain.SignalTrace;
import com.example.enigmamachine.domain.SignalTrace.Stage;
import com.example.enigmamachine.domain.SignalTrace.Step;
import com.example.enigmamachine.engine.EnigmaService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Follows a letter's signal through the diagram, one step of its engine trace at a time. */
class DiagramSignalTest {

    private static final MachineConfiguration BARBAROSSA = new MachineConfiguration(
            List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
            Reflector.B,
            List.of(1, 20, 11),
            List.of(1, 11, 0),
            List.of("AV", "BS", "CG", "DL", "FU", "HZ", "IN", "KM", "OW", "RX"));

    private static boolean toolkitAvailable;

    private MainController controller;
    private DiagramView view;

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            toolkitAvailable = started.await(10, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            toolkitAvailable = true;
        } catch (RuntimeException noDisplay) {
            toolkitAvailable = false;
        }
    }

    @Test
    void nothingIsLitBeforeALetter() {
        onFxThread(() -> {
            assertTrue(diagram().litPath().isEmpty());
            assertNull(diagram().currentArrow());
            assertEquals("Press a key to follow its signal through the machine.", stepText());
        });
    }

    @Test
    void educationalModeStopsAtThePlugboardOnTheWayIn() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            controller.pressKey('E');
            Step first = trace().steps().get(0);

            // The link from the key and the plugboard wire.
            assertEquals(2, diagram().litPath().size());
            assertTrue(diagram().litPath().stream().allMatch(shape -> isLit(shape, WiringDiagramView.FORWARD)));
            assertEquals("Step 1 of 9 · Forward ← · Plugboard: " + first.input() + " → " + first.output(), stepText());
            assertFalse(signal().isPlaying());
        });
    }

    @Test
    void eachStepLightsOneMoreComponentInTheOrderOfTheTrace() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            controller.pressKey('E');
            List<Step> steps = trace().steps();

            for (int index = 0; index < steps.size(); index++) {
                Step step = steps.get(index);
                boolean last = index == steps.size() - 1;
                // Every step lights its wire and the link into it; the last also lights the link to the lamp.
                assertEquals(2 * (index + 1) + (last ? 1 : 0), diagram().litPath().size(), step.stage().name());
                assertEquals(Alphabet.toIndex(step.output()), arrowRow(), step.stage().name());
                assertEquals(pointsRight(step.stage()), arrowPointsRight(), step.stage().name());
                assertTrue(stepText().startsWith("Step " + (index + 1) + " of 9"));
                if (!last) {
                    replayStep().fire();
                }
            }
        });
    }

    @Test
    void theWayBackIsDrawnApartFromTheWayIn() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            controller.pressKey('E');
            for (int step = 0; step < 8; step++) {
                replayStep().fire();
            }

            long forward = diagram().litPath().stream().filter(shape -> isLit(shape, WiringDiagramView.FORWARD)).count();
            long back = diagram().litPath().stream().filter(shape -> isLit(shape, WiringDiagramView.RETURN)).count();

            assertEquals(10, forward);
            assertEquals(9, back);
        });
    }

    @Test
    void lastLitContactIsTheOutputAndTheLamp() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            controller.pressKey('E');
            for (int step = 0; step < 8; step++) {
                replayStep().fire();
            }
            String output = controller.viewModel().outputTextProperty().get();

            assertEquals(Alphabet.toIndex(output.charAt(0)), arrowRow());
            assertEquals(output, diagram().litContact());
            assertEquals(output, controller.viewModel().litLampProperty().get());
            assertTrue(stepText().endsWith("lamp " + output + " lights"));
            assertTrue(replayStep().isDisabled());
        });
    }

    @Test
    void reflectorStepSaysTheSignalTurnsBack() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            controller.pressKey('E');
            for (int step = 0; step < 4; step++) {
                replayStep().fire();
            }

            assertTrue(stepText().startsWith("Step 5 of 9 · Turning back · Reflector: "));
        });
    }

    @Test
    void outsideEducationalModeTheLetterPlaysOnItsOwn() {
        onFxThread(() -> {
            controller.pressKey('E');

            assertTrue(signal().isPlaying());
            assertEquals("Pause", replayPlay().getText());
        });
    }

    @Test
    void replayBarAndSignalTabShareOnePlayback() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            controller.pressKey('E');

            replayStep().fire();

            long revealed = view.lookupAll(".signal-letters").stream().filter(Node::isVisible).count();
            assertEquals(2, revealed);
            assertTrue(stepText().startsWith("Step 2 of 9"));
            CheckBox barToggle = (CheckBox) view.lookup(".signal-replay-bar .signal-educational");
            barToggle.setSelected(false);
            assertFalse(educationalMode().isSelected());
        });
    }

    @Test
    void aPasteShowsOnlyItsLastLetterAndEncryptsEachLetterOnce() {
        onFxThread(() -> {
            MainController plain = new MainController(new EnigmaService(new MachineConfiguration(
                    List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                    Reflector.B, List.of(0, 0, 0), List.of(0, 0, 0), List.of())));
            setUp(plain);
            educationalMode().setSelected(true);

            plain.pasteText("HELLO");

            assertEquals("ILBDA", plain.viewModel().outputTextProperty().get());
            assertEquals("AAF", plain.viewModel().rotorWindowsProperty().get());
            assertEquals('O', trace().input());
            assertEquals(2, diagram().litPath().size());
            assertEquals("A", diagram().litContact());
        });
    }

    @Test
    void aNewLetterCancelsThePathOfThePreviousOne() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            controller.pressKey('E');
            for (int step = 0; step < 5; step++) {
                replayStep().fire();
            }

            controller.pressKey('D');

            assertEquals('D', trace().input());
            assertEquals(2, diagram().litPath().size());
            assertTrue(stepText().startsWith("Step 1 of 9"));
        });
    }

    @Test
    void backspaceAndClearTurnThePathOff() {
        onFxThread(() -> {
            controller.pressKey('E');
            controller.deleteLastLetter();
            assertTrue(diagram().litPath().isEmpty());

            controller.pressKey('E');
            controller.clear();
            assertTrue(diagram().litPath().isEmpty());
            assertNull(diagram().currentArrow());
        });
    }

    @Test
    void pathSurvivesTheWiresBeingRedrawn() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            controller.pressKey('E');
            replayStep().fire();
            int lit = diagram().litPath().size();

            // A wiring update for the same machine state draws new wires, which must be lit again.
            controller.viewModel().showWiring(new EnigmaService(BARBAROSSA).wiringSnapshot());
            controller.viewModel().showWiring(controller.viewModel().wiringProperty().get());

            assertEquals(lit, diagram().litPath().size());
        });
    }

    @Test
    void playbackNeverChangesTheResult() {
        onFxThread(() -> {
            controller.pasteText("EDPUD");
            String output = controller.viewModel().outputTextProperty().get();
            String rotors = controller.viewModel().rotorWindowsProperty().get();

            replayPlay().fire();
            replayStep().fire();
            replayPlay().fire();
            replayStep().fire();

            assertEquals(output, controller.viewModel().outputTextProperty().get());
            assertEquals(rotors, controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void hiddenDiagramDoesNotPlayButShowsTheLastPathWhenBack() {
        onFxThread(() -> {
            view.deactivate();
            controller.pressKey('E');

            assertFalse(signal().isPlaying());
            assertEquals(2, diagram().litPath().size());
            assertNotNull(diagram().currentArrow());
            assertEquals('E', trace().input());
        });
    }

    private static boolean pointsRight(Stage stage) {
        return stage.ordinal() >= Stage.REFLECTOR.ordinal();
    }

    private boolean arrowPointsRight() {
        List<Double> points = diagram().currentArrow().getPoints();
        // The tip is the middle point; the other two sit behind it.
        return points.get(2) > points.get(0);
    }

    private int arrowRow() {
        Polygon arrow = diagram().currentArrow();
        return WiringDiagramView.rowAt(arrow.getPoints().get(3));
    }

    private static boolean isLit(Shape shape, javafx.css.PseudoClass direction) {
        return shape.getPseudoClassStates().contains(direction);
    }

    private SignalTrace trace() {
        return signal().shownTraceProperty().get();
    }

    private String stepText() {
        return diagram().stepLabel().getText();
    }

    private WiringDiagramView diagram() {
        view.applyCss();
        return (WiringDiagramView) view.lookup(".wiring-diagram");
    }

    private SignalPathView signal() {
        view.applyCss();
        return (SignalPathView) view.lookup(".signal-summary").getParent();
    }

    private CheckBox educationalMode() {
        view.applyCss();
        return (CheckBox) view.lookup(".signal-educational");
    }

    private Button replayStep() {
        view.applyCss();
        return (Button) view.lookup(".signal-replay-bar .signal-step-button");
    }

    private Button replayPlay() {
        view.applyCss();
        return (Button) view.lookup(".signal-replay-bar .signal-play");
    }

    private void setUp(MainController newController) {
        controller = newController;
        view = new DiagramView(controller);
        Scene scene = new Scene(view);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setScene(scene);
        view.activate(SceneInput.installOn(stage));
        view.applyCss();
    }

    private void onFxThread(Runnable check) {
        assumeTrue(toolkitAvailable, "JavaFX needs a display to run this test.");
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                setUp(new MainController(new EnigmaService(BARBAROSSA)));
                check.run();
                result.complete(null);
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        try {
            result.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException failure) {
            if (failure.getCause() instanceof Error error) {
                throw error;
            }
            throw new AssertionError(failure.getCause());
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }
}
