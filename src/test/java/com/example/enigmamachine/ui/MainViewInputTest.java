package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.engine.EnigmaService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
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
            tap(scene, "7", KeyCode.DIGIT7);

            assertState("", "", "AAA");
        });
    }

    @Test
    void spaceIsEncryptedAsTheLetterX() {
        onFxThread(() -> {
            tap(scene, " ", KeyCode.SPACE);

            assertEquals("X", controller.viewModel().inputTextProperty().get());
            assertEquals("AAB", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void spaceWorksWithoutEventText() {
        onFxThread(() -> {
            tap(scene, "", KeyCode.SPACE);

            assertEquals("X", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void holdingSpaceProcessesOneLetter() {
        onFxThread(() -> {
            press(scene, " ", KeyCode.SPACE);
            press(scene, " ", KeyCode.SPACE);
            release(scene, KeyCode.SPACE);

            assertEquals("X", controller.viewModel().inputTextProperty().get());
        });
    }

    @Test
    void spaceWithAShortcutModifierIsIgnored() {
        onFxThread(() -> {
            fire(scene, new KeyEvent(KeyEvent.KEY_PRESSED, " ", " ", KeyCode.SPACE, false, true, false, false));

            assertState("", "", "AAA");
        });
    }

    @Test
    void positionSelectorsOfferLettersAToZ() {
        onFxThread(() -> {
            ComboBox<Integer> selector = integerSelector(".position-selector");

            assertEquals(26, selector.getItems().size());
            assertEquals("A", selector.getConverter().toString(0));
            assertEquals("Z", selector.getConverter().toString(25));
        });
    }

    @Test
    void ringSelectorsOfferNumbers01To26() {
        onFxThread(() -> {
            ComboBox<Integer> selector = integerSelector(".ring-selector");

            assertEquals(26, selector.getItems().size());
            assertEquals("01", selector.getConverter().toString(0));
            assertEquals("26", selector.getConverter().toString(25));
        });
    }

    @Test
    void positionAndRingSelectorsEditTheirOwnSlot() {
        onFxThread(() -> {
            integerSelectorInRow(".position-selector", 1).setValue(7);
            integerSelectorInRow(".ring-selector", 3).setValue(9);

            MachineConfiguration edited = controller.settings().toConfiguration();
            assertEquals(List.of(7, 0, 0), edited.positions());
            assertEquals(List.of(0, 0, 9), edited.ringSettings());
        });
    }

    @Test
    void editingSettingsDoesNotChangeTheRunningMachine() {
        onFxThread(() -> {
            integerSelectorInRow(".position-selector", 1).setValue(7);

            assertEquals("AAA", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void messageAreasStayAtTheNewestLettersWhileTyping() {
        onFxThread(() -> {
            controller.pasteText("A".repeat(3000));
            scene.snapshot(null);

            tap(scene, "h", KeyCode.H);
            scene.snapshot(null);
            tap(scene, "i", KeyCode.I);
            scene.snapshot(null);

            for (Node area : view.lookupAll(".message-area")) {
                ScrollBar bar = verticalScrollBar(area);
                assertTrue(bar.isVisible(), "The text must be long enough to scroll.");
                assertEquals(bar.getMax(), bar.getValue(), "The view jumped away from the newest letters.");
            }
        });
    }

    @Test
    void messageAreasShowTheSameTextAsTheMessages() {
        onFxThread(() -> {
            controller.pasteText("HELLO");
            controller.deleteLastLetter();
            controller.decrypt();

            List<String> shown = view.lookupAll(".message-area").stream()
                    .map(area -> ((TextArea) area).getText())
                    .sorted()
                    .toList();
            assertEquals(List.of("HELL", "ILBD"), shown);

            controller.clear();

            view.lookupAll(".message-area").forEach(area -> assertEquals("", ((TextArea) area).getText()));
        });
    }

    @Test
    void clickingPlugboardSocketsEditsTheSettingsNotTheMachine() {
        onFxThread(() -> {
            socketLabeled("A").fire();
            socketLabeled("M").fire();

            assertEquals(List.of("AM"), controller.settings().plugboardPairs());
            assertState("", "", "AAA");
        });
    }

    @Test
    void plugboardSocketsShowTheirState() {
        onFxThread(() -> {
            socketLabeled("A").fire();
            assertTrue(hasState(socketLabeled("A"), "pending"));

            socketLabeled("M").fire();
            assertFalse(hasState(socketLabeled("A"), "pending"));
            assertTrue(hasState(socketLabeled("A"), "connected"));
            assertTrue(hasState(socketLabeled("M"), "connected"));
            assertFalse(hasState(socketLabeled("B"), "connected"));

            socketLabeled("M").fire();
            assertFalse(hasState(socketLabeled("A"), "connected"));
        });
    }

    @Test
    void plugboardConnectionsAreListed() {
        onFxThread(() -> {
            Label connections = (Label) view.lookup(".plugboard-connections");
            assertEquals("No connections", connections.getText());

            controller.settings().pressPlugboardLetter('C');
            controller.settings().pressPlugboardLetter('D');
            controller.settings().pressPlugboardLetter('A');
            controller.settings().pressPlugboardLetter('B');

            assertEquals("AB CD", connections.getText());
        });
    }

    @Test
    void applyIsAvailableOnlyWithUnappliedChanges() {
        onFxThread(() -> {
            Button apply = (Button) view.lookup(".apply-settings");
            assertTrue(apply.isDisabled());

            controller.settings().rotorProperty(0).set(RotorDefinition.III);
            assertFalse(apply.isDisabled());

            apply.fire();
            assertTrue(apply.isDisabled());
            assertEquals(List.of(RotorDefinition.III, RotorDefinition.II, RotorDefinition.I),
                    controller.settings().toConfiguration().rotors());
        });
    }

    @Test
    void settingsStatusWarnsBeforeAMessageIsEmptied() {
        onFxThread(() -> {
            Label status = (Label) view.lookup(".settings-status");
            assertEquals("", status.getText());

            controller.settings().reflectorProperty().set(Reflector.C);
            assertEquals("Changes are not applied yet.", status.getText());

            tap(scene, "h", KeyCode.H);
            assertEquals("Applying empties the current message.", status.getText());

            ((Button) view.lookup(".apply-settings")).fire();
            assertEquals("", status.getText());
            assertState("", "", "AAA");
        });
    }

    @Test
    void appliedPositionsAppearInTheRotorWindows() {
        onFxThread(() -> {
            integerSelectorInRow(".position-selector", 1).setValue(7);

            ((Button) view.lookup(".apply-settings")).fire();

            assertEquals("HAA", controller.viewModel().rotorWindowsProperty().get());
        });
    }

    @Test
    void rotorWindowsShowTheRealPositionsFromTheFirstMomentOfTheAnimation() {
        onFxThread(() -> {
            assertEquals("AAA", shownRotorLetters());

            tap(scene, "h", KeyCode.H);
            assertEquals("AAB", shownRotorLetters());

            tap(scene, "e", KeyCode.E);
            assertEquals("AAC", shownRotorLetters());
        });
    }

    @Test
    void rotorWindowsFollowTheEngineThroughADoubleStep() {
        onFxThread(() -> {
            integerSelectorInRow(".position-selector", 2).setValue(3);
            integerSelectorInRow(".position-selector", 3).setValue(20);
            ((Button) view.lookup(".apply-settings")).fire();
            assertEquals("ADU", shownRotorLetters());

            tap(scene, "a", KeyCode.A);
            assertEquals(controller.viewModel().rotorWindowsProperty().get(), shownRotorLetters());
            tap(scene, "a", KeyCode.A);
            assertEquals(controller.viewModel().rotorWindowsProperty().get(), shownRotorLetters());
            tap(scene, "b", KeyCode.B);
            assertEquals("BFX", shownRotorLetters());
        });
    }

    @Test
    void rotorWindowsFollowBackspaceResetAndLongPastes() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);
            tap(scene, "i", KeyCode.I);

            tap(scene, "", KeyCode.BACK_SPACE);
            assertEquals("AAB", shownRotorLetters());

            controller.pasteText("A".repeat(30));
            assertEquals(controller.viewModel().rotorWindowsProperty().get(), shownRotorLetters());

            controller.reset();
            assertEquals("AAA", shownRotorLetters());
        });
    }

    private String shownRotorLetters() {
        StringBuilder letters = new StringBuilder();
        for (Node window : ((Pane) view.lookup(".rotor-windows")).getChildren()) {
            letters.append(((Label) window.lookup(".rotor-window-current")).getText());
        }
        return letters.toString();
    }

    @Test
    void signalPathStartsEmptyAndRevealsOnlyTheFirstStep() {
        onFxThread(() -> {
            assertEquals(9, view.lookupAll(".signal-step").size());
            assertEquals("Press a key to see its signal path.", ((Label) view.lookup(".signal-summary")).getText());

            tap(scene, "h", KeyCode.H);

            assertEquals("H → I, rotors at AAB", ((Label) view.lookup(".signal-summary")).getText());
            long active = view.lookupAll(".signal-step").stream().filter(row -> hasState(row, "active")).count();
            long revealed = view.lookupAll(".signal-letters").stream().filter(Node::isVisible).count();
            assertEquals(1, active);
            assertEquals(1, revealed);
        });
    }

    @Test
    void signalPathEmptiesWhenTheMessageIsCleared() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);

            controller.clear();

            assertEquals("Press a key to see its signal path.", ((Label) view.lookup(".signal-summary")).getText());
            assertEquals(0, view.lookupAll(".signal-step").stream().filter(row -> hasState(row, "active")).count());
        });
    }

    @Test
    void signalControlsAreOffWithoutALetter() {
        onFxThread(() -> {
            assertTrue(signalButton(".signal-play").isDisabled());
            assertTrue(signalButton(".signal-step-button").isDisabled());
            assertEquals("Play", signalButton(".signal-play").getText());
        });
    }

    @Test
    void letterPlaysOnItsOwnOutsideEducationalMode() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);

            assertEquals("Pause", signalButton(".signal-play").getText());
        });
    }

    @Test
    void educationalModeWaitsAtTheFirstStep() {
        onFxThread(() -> {
            educationalMode().setSelected(true);

            tap(scene, "h", KeyCode.H);

            assertEquals("Play", signalButton(".signal-play").getText());
            assertEquals(1, revealedSteps());
        });
    }

    @Test
    void stepMovesOneComponentAtATimeAndStopsAtTheLast() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            tap(scene, "h", KeyCode.H);
            Button step = signalButton(".signal-step-button");

            step.fire();
            assertEquals(2, revealedSteps());

            for (int remaining = 0; remaining < 7; remaining++) {
                step.fire();
            }
            assertEquals(9, revealedSteps());
            assertTrue(step.isDisabled());
        });
    }

    @Test
    void playAfterTheLastStepStartsOver() {
        onFxThread(() -> {
            educationalMode().setSelected(true);
            tap(scene, "h", KeyCode.H);
            for (int step = 0; step < 8; step++) {
                signalButton(".signal-step-button").fire();
            }

            signalButton(".signal-play").fire();

            assertEquals(1, revealedSteps());
            assertEquals("Pause", signalButton(".signal-play").getText());
        });
    }

    @Test
    void pauseAndStepStopThePlayback() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);

            signalButton(".signal-play").fire();
            assertEquals("Play", signalButton(".signal-play").getText());

            signalButton(".signal-play").fire();
            assertEquals("Pause", signalButton(".signal-play").getText());

            signalButton(".signal-step-button").fire();
            assertEquals("Play", signalButton(".signal-play").getText());
        });
    }

    @Test
    void turningOnEducationalModeStopsThePlayback() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);

            educationalMode().setSelected(true);

            assertEquals("Play", signalButton(".signal-play").getText());
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void speedSelectorChangesThePlaybackRate() {
        onFxThread(() -> {
            ComboBox<Double> speed = (ComboBox<Double>) view.lookup(".signal-speed");
            SignalPathView signal = (SignalPathView) view.lookup(".signal-summary").getParent();
            assertEquals(1.0, signal.playbackRate());
            assertEquals("0.5×", speed.getConverter().toString(0.5));
            assertEquals("4×", speed.getConverter().toString(4.0));

            speed.setValue(4.0);

            assertEquals(4.0, signal.playbackRate());
        });
    }

    @Test
    void controlsDoNotChangeTheEncryption() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);
            signalButton(".signal-play").fire();
            signalButton(".signal-step-button").fire();
            signalButton(".signal-play").fire();
            educationalMode().setSelected(true);
            tap(scene, "e", KeyCode.E);
            signalButton(".signal-step-button").fire();
            tap(scene, "l", KeyCode.L);
            tap(scene, "l", KeyCode.L);
            tap(scene, "o", KeyCode.O);

            assertState("HELLO", "ILBDA", "AAF");
        });
    }

    private Button signalButton(String selector) {
        return (Button) view.lookup(selector);
    }

    private CheckBox educationalMode() {
        return (CheckBox) view.lookup(".signal-educational");
    }

    private long revealedSteps() {
        return view.lookupAll(".signal-letters").stream().filter(Node::isVisible).count();
    }

    @Test
    void backspaceKeepsDeletingWhileHeld() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);
            tap(scene, "i", KeyCode.I);
            tap(scene, "m", KeyCode.M);

            press(scene, "", KeyCode.BACK_SPACE);
            press(scene, "", KeyCode.BACK_SPACE);
            release(scene, KeyCode.BACK_SPACE);

            assertState("H", "I", "AAB");
        });
    }

    @Test
    void backspaceWorksWhileAMessageAreaHasFocus() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);

            tap(view.lookup(".message-area"), "", KeyCode.BACK_SPACE);

            assertState("", "", "AAA");
        });
    }

    @Test
    void backspaceWithAShortcutModifierIsIgnored() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);

            fire(scene, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.BACK_SPACE, false, true, false, false));

            assertState("H", "I", "AAB");
        });
    }

    @Test
    void decryptIsAvailableOnlyWhenThereIsOutput() {
        onFxThread(() -> {
            Button decrypt = buttonLabeled("Decrypt");
            assertTrue(decrypt.isDisabled());

            tap(scene, "h", KeyCode.H);

            assertFalse(decrypt.isDisabled());
        });
    }

    @Test
    void decryptButtonRunsTheOutputBackThroughTheMachine() {
        onFxThread(() -> {
            tap(scene, "h", KeyCode.H);

            buttonLabeled("Decrypt").fire();

            assertState("I", "H", "AAB");
        });
    }

    @Test
    void pasteShortcutFiresOncePerPressAndIsNotTypedAsALetter() {
        onFxThread(() -> {
            // A separate keyboard keeps this test away from the system clipboard.
            KeyboardView keyboard = new KeyboardView();
            List<Character> letters = new ArrayList<>();
            int[] pastes = {0};
            keyboard.setOnLetterPressed(letters::add);
            keyboard.setOnPastePressed(() -> pastes[0]++);
            Stage stage = new Stage();
            stage.setScene(new Scene(keyboard));
            keyboard.attachTo(stage);
            Scene keyboardScene = stage.getScene();

            KeyEvent ctrlV = new KeyEvent(KeyEvent.KEY_PRESSED, "v", "v", KeyCode.V, false, true, false, false);
            fire(keyboardScene, ctrlV.copyFor(keyboardScene, keyboardScene));
            fire(keyboardScene, ctrlV.copyFor(keyboardScene, keyboardScene));
            release(keyboardScene, KeyCode.V);
            fire(keyboardScene, ctrlV.copyFor(keyboardScene, keyboardScene));

            assertEquals(2, pastes[0]);
            assertEquals(List.of(), letters);
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
    void lettersStillWorkWhileASettingsSelectorIsTheTarget() {
        onFxThread(() -> {
            tap(view.lookup(".rotor-selector"), "h", KeyCode.H);

            assertState("H", "I", "AAB");
        });
    }

    @Test
    void rotorSelectorsShowAndEditTheSettings() {
        onFxThread(() -> {
            List<ComboBox<?>> selectors = view.lookupAll(".rotor-selector").stream()
                    .sorted(Comparator.comparing(GridPane::getRowIndex))
                    .<ComboBox<?>>map(selector -> (ComboBox<?>) selector)
                    .toList();
            assertEquals(3, selectors.size());

            controller.settings().rotorProperty(0).set(RotorDefinition.III);

            assertEquals(RotorDefinition.III, selectors.get(0).getValue());
            assertEquals(RotorDefinition.I, selectors.get(2).getValue());
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

    @SuppressWarnings("unchecked")
    private ComboBox<Integer> integerSelector(String selector) {
        return (ComboBox<Integer>) view.lookup(selector);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Integer> integerSelectorInRow(String selector, int row) {
        return view.lookupAll(selector).stream()
                .filter(node -> GridPane.getRowIndex(node) == row)
                .map(node -> (ComboBox<Integer>) node)
                .findFirst()
                .orElseThrow();
    }

    private static ScrollBar verticalScrollBar(Node area) {
        return area.lookupAll(".scroll-bar").stream()
                .map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                .findFirst()
                .orElseThrow();
    }

    private Button socketLabeled(String letter) {
        return view.lookupAll(".plugboard-socket").stream()
                .map(Button.class::cast)
                .filter(socket -> socket.getText().equals(letter))
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasState(Node node, String state) {
        return node.getPseudoClassStates().contains(PseudoClass.getPseudoClass(state));
    }

    private Button buttonLabeled(String title) {
        return view.lookupAll(".button").stream()
                .filter(node -> node instanceof Button button && title.equals(button.getText()))
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow();
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
