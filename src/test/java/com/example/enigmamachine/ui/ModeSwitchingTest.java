package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.engine.EnigmaService;
import com.example.enigmamachine.persistence.JsonConfigurationRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Uses the machine through both modes of one window and checks that switching never changes what the machine
 * did or will do: messages, positions, Undo, settings, playback and the wires drawn all stay in step.
 */
class ModeSwitchingTest {

    private static final MachineConfiguration DEFAULT = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of());

    private static boolean toolkitAvailable;

    @TempDir
    Path folder;

    private MainController controller;
    private ApplicationShell shell;
    private Scene scene;
    private SceneInput input;

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
    void classicDiagramClassicKeepsMessagesPositionsAndUndo() {
        onFxThread(() -> {
            type("HELLO");
            switchMode();
            switchMode();

            assertState("HELLO", "ILBDA", "AAF");
            tap(KeyCode.BACK_SPACE, "");
            assertState("HELL", "ILBD", "AAE");
        });
    }

    @Test
    void typingContinuesTheSameMessageInTheOtherMode() {
        onFxThread(() -> {
            type("HEL");
            switchMode();
            type("LO");

            assertState("HELLO", "ILBDA", "AAF");
            assertDiagramFollowsEngine();
        });
    }

    @Test
    void undoInDiagramGoesBackThroughLettersTypedInClassic() {
        onFxThread(() -> {
            type("HELLO");
            switchMode();

            for (int letter = 0; letter < 5; letter++) {
                tap(KeyCode.BACK_SPACE, "");
            }

            assertState("", "", "AAA");
            assertDiagramFollowsEngine();
        });
    }

    @Test
    void clearResetAndDecryptWorkInDiagramMode() {
        onFxThread(() -> {
            switchMode();
            type("HELLO");

            shortcut(KeyCode.D, "d");
            assertState("ILBDA", "HELLO", "AAF");

            shortcut(KeyCode.L, "l");
            assertState("", "", "AAF");

            shortcut(KeyCode.R, "r");
            assertState("", "", "AAA");
            assertDiagramFollowsEngine();
        });
    }

    @Test
    void settingsEditedInOneModeShowInTheOtherWithoutBeingApplied() {
        onFxThread(() -> {
            switchMode();
            firstRotorSelector().setValue(RotorDefinition.V);

            switchMode();

            assertEquals(RotorDefinition.V, firstRotorSelector().getValue());
            assertTrue(controller.settings().unappliedChangesBinding().get());
            type("A");
            assertState("A", "B", "AAB");
        });
    }

    @Test
    void applyInDiagramModeRunsTheNewSettingsInBothModes() {
        onFxThread(() -> {
            switchMode();
            controller.settings().reflectorProperty().set(Reflector.C);

            shortcut(KeyCode.ENTER, "");

            assertEquals("Reflector\nC", heading(0));
            assertDiagramFollowsEngine();
            switchMode();
            assertEquals(Reflector.C, controller.settings().reflectorProperty().get());
            assertFalse(controller.settings().unappliedChangesBinding().get());
        });
    }

    @Test
    void loadAndApplyInDiagramModeRedrawsTheWiring() {
        onFxThread(() -> {
            Path file = folder.resolve("settings.json");
            new JsonConfigurationRepository().save(new MachineConfiguration(
                    List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V),
                    Reflector.B,
                    List.of(1, 20, 11),
                    List.of(1, 11, 0),
                    List.of("AV", "BS")), file);
            switchMode();

            controller.loadSettings(file);
            assertEquals("Left · I\nA · ring 01", heading(1));

            controller.applySettings();

            assertEquals("Left · II\nB · ring 02", heading(1));
            assertEquals("Plugboard\n2 pairs", heading(4));
            assertDiagramFollowsEngine();
        });
    }

    @Test
    void keyHeldDuringASwitchAndThenLostFocusTypesAgainAfterwards() {
        onFxThread(() -> {
            press(KeyCode.A, "a");
            switchMode();
            // The release went to another window, so the next press is a fresh one.
            input.windowLostFocus();
            press(KeyCode.A, "a");
            assertState("AA", "BD", "AAC");

            Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.A, false, false, false, false));
            assertFalse(anyKeyHeld());
            switchMode();
            assertFalse(anyKeyHeld());
        });
    }

    @Test
    void keyHeldDuringASwitchDoesNotRepeat() {
        onFxThread(() -> {
            press(KeyCode.A, "a");
            for (int switches = 0; switches < 20; switches++) {
                switchMode();
                press(KeyCode.A, "a");
            }

            assertState("A", "B", "AAB");
        });
    }

    @Test
    void educationalModeAndSpeedStayWhenSwitching() {
        onFxThread(() -> {
            educationalToggle().setSelected(true);
            speedSelector().setValue(2.0);

            switchMode();

            assertTrue(educationalToggle().isSelected());
            assertEquals(2.0, speedSelector().getValue());
            type("A");
            assertFalse(signal().isPlaying());
            assertEquals(2.0, signal().playbackRate());
        });
    }

    @Test
    void switchingInTheMiddleOfAStepThroughKeepsTheResultAndPausesAtTheStart() {
        onFxThread(() -> {
            educationalToggle().setSelected(true);
            type("A");
            for (int step = 0; step < 5; step++) {
                button(".signal-step-button").fire();
            }

            switchMode();

            assertState("A", "B", "AAB");
            assertFalse(signal().isPlaying());
            assertTrue(diagramStep().startsWith("Step 1 of 9"));
            button(".signal-replay-bar .signal-play").fire();
            assertTrue(signal().isPlaying());
            assertState("A", "B", "AAB");
        });
    }

    @Test
    void switchingWhileTheDiagramPlaysStopsItAndKeepsTheResult() {
        onFxThread(() -> {
            switchMode();
            type("HELLO");
            SignalPathView diagramSignal = signal();
            assertTrue(diagramSignal.isPlaying());

            switchMode();

            assertFalse(diagramSignal.isPlaying());
            assertFalse(signal().isPlaying());
            assertState("HELLO", "ILBDA", "AAF");
        });
    }

    private void assertDiagramFollowsEngine() {
        WiringDiagramView diagram = (WiringDiagramView) shell.lookup(".wiring-diagram");
        if (diagram == null) {
            switchMode();
            assertDiagramFollowsEngine();
            switchMode();
            return;
        }
        var snapshot = controller.viewModel().wiringProperty().get();
        for (int slot = 0; slot < 3; slot++) {
            assertEquals(snapshot.rotors().get(slot).forward(), diagram.drawnRotorWiring(slot));
        }
        assertEquals(snapshot.reflectorWiring(), diagram.drawnReflectorWiring());
        assertEquals(snapshot.plugboardWiring(), diagram.drawnPlugboardWiring());
    }

    private void assertState(String inputText, String outputText, String rotors) {
        assertEquals(inputText, controller.viewModel().inputTextProperty().get());
        assertEquals(outputText, controller.viewModel().outputTextProperty().get());
        assertEquals(rotors, controller.viewModel().rotorWindowsProperty().get());
    }

    private String heading(int component) {
        shell.applyCss();
        return ((Label) List.copyOf(shell.lookupAll(".diagram-heading")).get(component)).getText();
    }

    private String diagramStep() {
        shell.applyCss();
        return ((Label) shell.lookup(".diagram-step")).getText();
    }

    private boolean anyKeyHeld() {
        shell.applyCss();
        return shell.lookupAll(".keyboard-key").stream().anyMatch(key -> key.getPseudoClassStates().stream()
                .anyMatch(state -> state.getPseudoClassName().equals("held")));
    }

    private List<ComboBox<?>> rotorSelectors() {
        shell.applyCss();
        return shell.lookupAll(".rotor-selector").stream()
                .sorted(java.util.Comparator.comparing(javafx.scene.layout.GridPane::getRowIndex))
                .<ComboBox<?>>map(node -> (ComboBox<?>) node)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<RotorDefinition> firstRotorSelector() {
        return (ComboBox<RotorDefinition>) rotorSelectors().get(0);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Double> speedSelector() {
        shell.applyCss();
        return (ComboBox<Double>) shell.lookup(".signal-speed");
    }

    private CheckBox educationalToggle() {
        shell.applyCss();
        return (CheckBox) shell.lookup(".signal-educational");
    }

    private SignalPathView signal() {
        shell.applyCss();
        return (SignalPathView) shell.lookup(".signal-summary").getParent();
    }

    private Button button(String selector) {
        shell.applyCss();
        return (Button) shell.lookup(selector);
    }

    private void switchMode() {
        button(".mode-button").fire();
    }

    private void type(String letters) {
        for (char letter : letters.toCharArray()) {
            tap(KeyCode.valueOf(String.valueOf(letter)), String.valueOf(letter).toLowerCase());
        }
    }

    private void tap(KeyCode code, String text) {
        press(code, text);
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }

    private void press(KeyCode code, String text) {
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, text, text, code, false, false, false, false));
    }

    /** Presses the key with Ctrl, or with Command on macOS, which is what "Shortcut" means there. */
    private void shortcut(KeyCode code, String text) {
        boolean mac = System.getProperty("os.name").startsWith("Mac");
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, text, text, code, false, !mac, false, mac));
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, !mac, false, mac));
    }

    private void setUp() {
        controller = new MainController(new EnigmaService(DEFAULT));
        shell = new ApplicationShell(controller);
        scene = new Scene(shell);
        Stage stage = new Stage();
        stage.setScene(scene);
        input = SceneInput.installOn(stage);
        shell.activate(input);
        shell.applyCss();
    }

    private void onFxThread(ThrowingRunnable check) {
        assumeTrue(toolkitAvailable, "JavaFX needs a display to run this test.");
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                setUp();
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

    /** A check that may throw, such as one that saves a settings file first. */
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
