package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.engine.EnigmaService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Sends key and mouse events through the real view. The stage is never shown, so no window opens or takes focus.
 */
class MainViewInputTest {

    private static boolean toolkitAvailable;

    private MainController controller;
    private MainView view;
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
    void physicalKeyProcessesOneLetter() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);

            assertState("H", "I", "AAB");
        });
    }

    @Test
    void holdingAKeyProcessesOneLetter() {
        onFxThread(() -> {
            press(scene, "h", KeyCode.H);
            press(scene, "h", KeyCode.H);
            press(scene, "h", KeyCode.H);
            release(scene, KeyCode.H);

            assertState("H", "I", "AAB");
        });
    }

    @Test
    void keyCanBeUsedAgainAfterRelease() {
        onFxThread(() -> {
            tap(scene, "l", KeyCode.L);
            tap(scene, "l", KeyCode.L);

            assertEquals("LL", controller.viewModel().inputTextProperty().get());
            assertEquals("AAC", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void lettersStillWorkWhileAMessageAreaHasFocus() {
        onFxThread(() -> {
            for (Node area : view.lookupAll(".message-area")) {
                tap(area, "h", KeyCode.H);
            }

            assertEquals("HH", controller.viewModel().inputTextProperty().get());
            assertEquals("AAC", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void shortcutsAndNonLetterKeysDoNotStepTheRotors() {
        onFxThread(() -> {
            fire(scene, new KeyEvent(KeyEvent.KEY_PRESSED, "c", "c", KeyCode.C, false, true, false, false));
            fire(scene, new KeyEvent(KeyEvent.KEY_PRESSED, "c", "c", KeyCode.C, false, false, true, false));
            tap(scene, " ", KeyCode.SPACE);
            tap(scene, "7", KeyCode.DIGIT7);

            assertState("", "", "AAA");
        });
    }

    @Test
    void altGrDiacriticIsTypedAsItsBaseLetter() {
        onFxThread(() -> {
            fire(scene, new KeyEvent(KeyEvent.KEY_PRESSED, "ț", "ț", KeyCode.T, false, true, true, false));

            assertEquals("T", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void diacriticOnANonLetterKeyIsTypedAsItsBaseLetter() {
        onFxThread(() -> {
            tap(scene, "ț", KeyCode.QUOTE);

            assertEquals("T", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void primaryClickProcessesOneLetter() {
        onFxThread(() -> {
            click(keyLabeled("H"), MouseButton.PRIMARY);

            assertState("H", "I", "AAB");
        });
    }

    @Test
    void secondaryClickIsIgnored() {
        onFxThread(() -> {
            click(keyLabeled("H"), MouseButton.SECONDARY);

            assertState("", "", "AAA");
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
        view.attachTo(stage);
        // Creates the control skins, which is where a text area starts to consume key events.
        view.applyCss();
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

    private void assertState(String input, String output, String rotors) {
        assertEquals(input, controller.viewModel().inputTextProperty().get());
        assertEquals(output, controller.viewModel().outputTextProperty().get());
        assertEquals(rotors, controller.viewModel().rotorWindowsProperty().get());
    }

    private Node keyLabeled(String letter) {
        return view.lookupAll(".keyboard-key").stream()
                .filter(key -> ((Button) key).getText().equals(letter))
                .findFirst()
                .orElseThrow();
    }

    private static void tap(EventTarget target, String text, KeyCode code) {
        press(target, text, code);
        release(target, code);
    }

    private static void press(EventTarget target, String text, KeyCode code) {
        fire(target, new KeyEvent(KeyEvent.KEY_PRESSED, text, text, code, false, false, false, false));
    }

    /** Sends no text, because a key release does not always carry the character it typed. */
    private static void release(EventTarget target, KeyCode code) {
        fire(target, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }

    private static void click(Node target, MouseButton button) {
        fire(target, new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, button, 1,
                false, false, false, false,
                button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY,
                false, false, true, new PickResult(target, 0, 0)));
    }

    private static void fire(EventTarget target, Event event) {
        Event.fireEvent(target, event);
    }
}
