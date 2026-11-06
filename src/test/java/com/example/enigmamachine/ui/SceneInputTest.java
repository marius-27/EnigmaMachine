package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.engine.EnigmaService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Shows, hides and shows the view again on one scene, which must never process a key twice or lose a key. */
class SceneInputTest {

    private static boolean toolkitAvailable;

    private MainController controller;
    private MainView view;
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
    void repeatedActivationProcessesEachKeyOnce() {
        onFxThread(() -> {
            for (int switches = 0; switches < 20; switches++) {
                view.deactivate();
                view.activate(input);
            }

            tap(KeyCode.H, "h");

            assertEquals("H", controller.viewModel().inputTextProperty().get());
            assertEquals("AAB", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void activatingTwiceDoesNotAddASecondListener() {
        onFxThread(() -> {
            view.activate(input);

            tap(KeyCode.H, "h");

            assertEquals("H", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void inactiveViewIgnoresTheKeyboardAndItsShortcuts() {
        onFxThread(() -> {
            view.deactivate();

            tap(KeyCode.H, "h");

            assertEquals("", controller.viewModel().inputTextProperty().get());
            assertTrue(scene.getAccelerators().isEmpty());
        });
    }

    @Test
    void reactivatedViewGetsItsShortcutsBack() {
        onFxThread(() -> {
            int shortcuts = scene.getAccelerators().size();
            view.deactivate();
            view.activate(input);

            assertEquals(shortcuts, scene.getAccelerators().size());
            assertTrue(shortcuts > 0);
        });
    }

    @Test
    void deactivatingLetsGoOfAHeldKey() {
        onFxThread(() -> {
            press(KeyCode.H, "h");
            Button key = keyLabeled("H");
            assertTrue(isHeld(key));

            view.deactivate();
            assertFalse(isHeld(key));
            view.activate(input);
            release(KeyCode.H);
            press(KeyCode.H, "h");

            assertEquals("HH", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void keyHeldWhileTheViewChangesTypesNothingUntilItIsReleased() {
        onFxThread(() -> {
            press(KeyCode.H, "h");
            view.deactivate();
            view.activate(input);

            press(KeyCode.H, "h");
            press(KeyCode.H, "h");
            assertEquals("H", controller.viewModel().inputTextProperty().get());

            release(KeyCode.H);
            press(KeyCode.H, "h");
            assertEquals("HH", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void otherKeysStillTypeWhileABlockedKeyIsHeld() {
        onFxThread(() -> {
            press(KeyCode.H, "h");
            view.deactivate();
            view.activate(input);

            tap(KeyCode.E, "e");

            assertEquals("HE", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void windowShortcutsCannotTakeTheKeysOfAView() {
        onFxThread(() -> {
            Shortcuts window = new Shortcuts();
            window.add("Shortcut+D", () -> { });

            assertThrows(IllegalArgumentException.class, () -> input.setWindowShortcuts(window));
        });
    }

    @Test
    void deactivatingStopsTheSignalAnimationAndKeepsTheResult() {
        onFxThread(() -> {
            tap(KeyCode.H, "h");
            SignalPathView signal = (SignalPathView) view.lookup(".signal-summary").getParent();
            assertTrue(signal.isPlaying());

            view.deactivate();

            assertFalse(signal.isPlaying());
            assertEquals("I", controller.viewModel().outputTextProperty().get());
            assertEquals("AAB", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void hiddenViewDoesNotStartPlayingForNewLetters() {
        onFxThread(() -> {
            SignalPathView signal = (SignalPathView) view.lookup(".signal-summary").getParent();
            view.deactivate();

            controller.pressKey('H');

            assertFalse(signal.isPlaying());
            assertEquals("AAB", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void newTargetTakesOverTheKeyboard() {
        onFxThread(() -> {
            KeyboardView other = new KeyboardView();
            List<Character> letters = new ArrayList<>();
            other.setOnLetterPressed(letters::add);

            input.setKeyTarget(other);
            tap(KeyCode.H, "h");

            assertEquals(List.of('H'), letters);
            assertEquals("", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void clearingAnOldTargetLeavesTheNewOneInPlace() {
        onFxThread(() -> {
            KeyboardView other = new KeyboardView();
            List<Character> letters = new ArrayList<>();
            other.setOnLetterPressed(letters::add);
            input.setKeyTarget(other);

            view.deactivate();
            tap(KeyCode.H, "h");

            assertEquals(List.of('H'), letters);
        });
    }

    @Test
    void disposeRemovesEverythingFromTheScene() {
        onFxThread(() -> {
            Runnable foreign = () -> { };
            KeyCombination foreignKeys = KeyCombination.valueOf("Shortcut+Shift+F12");
            scene.getAccelerators().put(foreignKeys, foreign);

            input.dispose();
            tap(KeyCode.H, "h");

            assertEquals("", controller.viewModel().inputTextProperty().get());
            assertEquals(1, scene.getAccelerators().size());
            assertEquals(foreign, scene.getAccelerators().get(foreignKeys));
            assertThrows(IllegalStateException.class, () -> input.setKeyTarget(new KeyboardView()));
        });
    }

    @Test
    void installingNeedsASceneOnTheStage() {
        onFxThread(() -> assertThrows(NullPointerException.class, () -> SceneInput.installOn(new Stage())));
    }

    @Test
    void separateScenesDoNotShareInput() {
        onFxThread(() -> {
            Stage otherStage = new Stage();
            Scene otherScene = new Scene(new StackPane());
            otherStage.setScene(otherScene);
            SceneInput.installOn(otherStage);

            Event.fireEvent(otherScene, keyEvent(KeyEvent.KEY_PRESSED, KeyCode.H, "h"));

            assertEquals("", controller.viewModel().inputTextProperty().get());
        });
    }

    private void setUpView() {
        controller = new MainController(new EnigmaService(new MachineConfiguration(
                List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
                Reflector.B,
                List.of(0, 0, 0),
                List.of(0, 0, 0),
                List.of())));
        view = new MainView(controller);
        scene = new Scene(view);
        Stage stage = new Stage();
        stage.setScene(scene);
        input = SceneInput.installOn(stage);
        view.activate(input);
        view.applyCss();
    }

    private Button keyLabeled(String letter) {
        return view.lookupAll(".keyboard-key").stream()
                .map(Button.class::cast)
                .filter(key -> key.getText().equals(letter))
                .findFirst()
                .orElseThrow();
    }

    private static boolean isHeld(Button key) {
        return key.getPseudoClassStates().contains(PseudoClass.getPseudoClass("held"));
    }

    private void tap(KeyCode code, String text) {
        press(code, text);
        release(code);
    }

    private void release(KeyCode code) {
        Event.fireEvent(scene, keyEvent(KeyEvent.KEY_RELEASED, code, ""));
    }

    private void press(KeyCode code, String text) {
        Event.fireEvent(scene, keyEvent(KeyEvent.KEY_PRESSED, code, text));
    }

    private static KeyEvent keyEvent(javafx.event.EventType<KeyEvent> type, KeyCode code, String text) {
        return new KeyEvent(type, text, text, code, false, false, false, false);
    }

    private void onFxThread(Runnable check) {
        assumeTrue(toolkitAvailable, "JavaFX needs a display to run this test.");
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                setUpView();
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
