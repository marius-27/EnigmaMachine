package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.engine.EnigmaService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Switches between Classic and Diagram in one window; the stage is never shown, so no window opens. */
class ApplicationShellTest {

    private static boolean toolkitAvailable;

    private MainController controller;
    private ApplicationShell shell;
    private Scene scene;

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
    void startsInClassicWithTheModeButtonOnTheFirstPage() {
        onFxThread(() -> {
            assertEquals(InterfaceMode.CLASSIC, shell.modeProperty().get());
            assertInstanceOf(MainView.class, shell.getCenter());
            assertEquals("Classic", label(".shell-mode").getText());
            assertTrue(modeButton().getTooltip().getText().startsWith("Switch to Diagram"));
            assertFalse(modeButton().isFocusTraversable());
        });
    }

    @Test
    void modeButtonSwitchesToDiagramAndBack() {
        onFxThread(() -> {
            modeButton().fire();

            assertEquals(InterfaceMode.DIAGRAM, shell.modeProperty().get());
            assertInstanceOf(DiagramView.class, shell.getCenter());
            assertEquals("Diagram", label(".shell-mode").getText());
            assertTrue(modeButton().getTooltip().getText().startsWith("Switch to Classic"));
            assertNotNull(node(".mode-button"));

            modeButton().fire();

            assertEquals(InterfaceMode.CLASSIC, shell.modeProperty().get());
            assertInstanceOf(MainView.class, shell.getCenter());
        });
    }

    @Test
    void onlyTheActiveViewIsInTheWindow() {
        onFxThread(() -> {
            assertNotNull(node(".classic-view"));
            assertNull(node(".diagram-view"));

            modeButton().fire();

            assertNull(node(".classic-view"));
            assertNotNull(node(".diagram-view"));
        });
    }

    @Test
    void shortcutSwitchesTheModeInBothDirections() {
        onFxThread(() -> {
            shortcut(KeyCode.M, "m");
            assertEquals(InterfaceMode.DIAGRAM, shell.modeProperty().get());

            shortcut(KeyCode.M, "m");
            assertEquals(InterfaceMode.CLASSIC, shell.modeProperty().get());
            assertEquals("", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void switchingKeepsTheMessagesAndThePositions() {
        onFxThread(() -> {
            for (int letter = 0; letter < 5; letter++) {
                tap(KeyCode.A, "a");
            }
            assertState("AAAAA", "BDZGO", "AAF");

            modeButton().fire();
            assertState("AAAAA", "BDZGO", "AAF");
            modeButton().fire();

            assertState("AAAAA", "BDZGO", "AAF");
        });
    }

    @Test
    void twentySwitchesThenOneKeyEncryptOnce() {
        onFxThread(() -> {
            for (int switches = 0; switches < 20; switches++) {
                modeButton().fire();
            }

            tap(KeyCode.A, "a");

            assertState("A", "B", "AAB");
        });
    }

    @Test
    void typingWorksInTheDiagramMode() {
        onFxThread(() -> {
            modeButton().fire();

            tap(KeyCode.A, "a");

            assertState("A", "B", "AAB");
        });
    }

    @Test
    void diagramModeOffersTheMachineAndTheSidePanel() {
        onFxThread(() -> {
            modeButton().fire();

            assertEquals(26, nodes(".keyboard-key").size());
            // The keys column of the diagram is the lampboard of this mode.
            assertTrue(nodes(".lamp").isEmpty());
            assertEquals(26, nodes(".diagram-keys .diagram-contact").size());
            assertNotNull(node(".wiring-diagram"));
            assertNotNull(node(".message-area"));
            assertNotNull(node(".rotor-selector"));
            assertNotNull(node(".signal-summary"));
        });
    }

    @Test
    void unappliedSettingsSurviveSwitchingAndAreNotApplied() {
        onFxThread(() -> {
            controller.settings().rotorProperty(0).set(RotorDefinition.III);

            modeButton().fire();
            modeButton().fire();

            assertEquals(RotorDefinition.III, controller.settings().rotorProperty(0).get());
            assertTrue(controller.settings().unappliedChangesBinding().get());
            tap(KeyCode.A, "a");
            assertState("A", "B", "AAB");
        });
    }

    @Test
    void switchingWhileAKeyIsHeldLeavesNoStuckKey() {
        onFxThread(() -> {
            press(KeyCode.A, "a");
            modeButton().fire();
            release(KeyCode.A);
            // The new view never saw the press, so its release is ignored and the next press is a new letter.
            press(KeyCode.A, "a");
            release(KeyCode.A);

            assertState("AA", "BD", "AAC");
            assertFalse(nodes(".keyboard-key").stream()
                    .anyMatch(key -> key.getPseudoClassStates().stream()
                            .anyMatch(state -> state.getPseudoClassName().equals("held"))));
        });
    }

    @Test
    void repeatsOfAKeyHeldAcrossASwitchDoNotEncrypt() {
        onFxThread(() -> {
            press(KeyCode.A, "a");
            modeButton().fire();
            // The operating system keeps repeating the held key; it must be released before it types again.
            press(KeyCode.A, "a");
            press(KeyCode.A, "a");

            assertState("A", "B", "AAB");
        });
    }

    @Test
    void switchingDuringTheAnimationKeepsTheResult() {
        onFxThread(() -> {
            tap(KeyCode.A, "a");
            SignalPathView classicSignal = signalPath();
            assertTrue(classicSignal.isPlaying());

            modeButton().fire();

            assertFalse(classicSignal.isPlaying());
            assertFalse(signalPath().isPlaying());
            assertEquals("A → B, rotors at AAB", label(".signal-summary").getText());
            assertState("A", "B", "AAB");
        });
    }

    @Test
    void eachModeHasItsOwnShortcutsAndModeStaysAvailable() {
        onFxThread(() -> {
            int classic = scene.getAccelerators().size();
            modeButton().fire();

            assertEquals(classic, scene.getAccelerators().size());
            tap(KeyCode.H, "h");
            shortcut(KeyCode.L, "l");

            assertEquals("", controller.viewModel().inputTextProperty().get());
            assertEquals("AAB", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void headerShowsTheStatus() {
        onFxThread(() -> {
            tap(KeyCode.H, "h");
            shortcut(KeyCode.R, "r");

            assertEquals("Reset. The rotors are back at AAA.", label(".shell-status").getText());
        });
    }

    @Test
    void shellCanBeActivatedOnlyOnce() {
        onFxThread(() -> assertThrows(IllegalStateException.class,
                () -> shell.activate(SceneInput.installOn((Stage) scene.getWindow()))));
    }

    private void setUpShell() {
        controller = new MainController(new EnigmaService(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 0, 0),
                List.of())));
        shell = new ApplicationShell(controller);
        scene = new Scene(shell);
        Stage stage = new Stage();
        stage.setScene(scene);
        shell.activate(SceneInput.installOn(stage));
        shell.applyCss();
    }

    /** The window is never shown, so the skins of a newly shown view exist only once its CSS is applied. */
    private Node node(String selector) {
        shell.applyCss();
        return shell.lookup(selector);
    }

    private Set<Node> nodes(String selector) {
        shell.applyCss();
        return shell.lookupAll(selector);
    }

    private SignalPathView signalPath() {
        return (SignalPathView) node(".signal-summary").getParent();
    }

    private Button modeButton() {
        return (Button) node(".mode-button");
    }

    private Label label(String selector) {
        return (Label) node(selector);
    }

    private void assertState(String input, String output, String rotors) {
        assertEquals(input, controller.viewModel().inputTextProperty().get());
        assertEquals(output, controller.viewModel().outputTextProperty().get());
        assertEquals(rotors, controller.viewModel().rotorWindowsProperty().get());
    }

    private void tap(KeyCode code, String text) {
        press(code, text);
        release(code);
    }

    private void press(KeyCode code, String text) {
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, text, text, code, false, false, false, false));
    }

    private void release(KeyCode code) {
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }

    /** Presses the key with Ctrl, or with Command on macOS, which is what "Shortcut" means there. */
    private void shortcut(KeyCode code, String text) {
        boolean mac = System.getProperty("os.name").startsWith("Mac");
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, text, text, code, false, !mac, false, mac));
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, !mac, false, mac));
    }

    private void onFxThread(Runnable check) {
        assumeTrue(toolkitAvailable, "JavaFX needs a display to run this test.");
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                setUpShell();
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
