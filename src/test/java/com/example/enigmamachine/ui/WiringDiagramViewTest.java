package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.domain.WiringSnapshot;
import com.example.enigmamachine.engine.EnigmaService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Checks that the wires on screen are the wires the engine reports, after every kind of change. */
class WiringDiagramViewTest {

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
    void drawsTheWiringOfTheRunningMachine() {
        onFxThread(this::assertDrawnMatchesEngine);
    }

    @Test
    void headingsShowRotorPositionAndRing() {
        onFxThread(() -> {
            assertEquals(List.of(
                            "Reflector\nB", "Left · II\nB · ring 02", "Middle · IV\nL · ring 21",
                            "Right · V\nA · ring 12", "Plugboard\n10 pairs", "Keys\nin / out"),
                    nodes(".diagram-heading").stream().map(node -> ((Label) node).getText()).toList());
            assertEquals("Left rotor II at B, ring 02", node(".diagram-rotor").getAccessibleText());
        });
    }

    @Test
    void everyComponentShowsContactsAToZ() {
        onFxThread(() -> {
            // Reflector 26, three rotors and the plugboard 52 each, keys 26.
            assertEquals(26 + 4 * 52 + 26, nodes(".diagram-contact").size());
            for (Node component : nodes(".diagram-component")) {
                List<String> letters = component.lookupAll(".diagram-contact").stream()
                        .map(node -> ((Label) node).getText())
                        .distinct()
                        .toList();
                assertEquals(26, letters.size());
                assertEquals("A", letters.get(0));
                assertEquals("Z", letters.get(25));
            }
        });
    }

    @Test
    void contactLettersAreAtLeastTwelvePixels() {
        onFxThread(() -> {
            for (Node contact : nodes(".diagram-contact")) {
                assertTrue(((Label) contact).getFont().getSize() >= 12);
            }
        });
    }

    @Test
    void keysColumnLightsTheOutputLetterLikeALamp() {
        onFxThread(() -> {
            WiringDiagramView diagram = (WiringDiagramView) node(".wiring-diagram");
            assertEquals("", diagram.litContact());

            controller.pressKey('E');
            assertEquals(controller.viewModel().litLampProperty().get(), diagram.litContact());
            assertEquals(1, diagram.litContact().length());
            assertEquals("Keys and lamps, A to Z, lamp " + diagram.litContact() + " lit",
                    node(".diagram-keys").getAccessibleText());

            controller.clear();
            assertEquals("", diagram.litContact());
        });
    }

    @Test
    void diagramGrowsToFillTheSpaceButNeverShrinks() {
        assertEquals(1.0, WiringDiagramView.fitScale(300, 200, 700, 500));
        assertEquals(1.0, WiringDiagramView.fitScale(1400, 400, 700, 500));
        assertEquals(1.5, WiringDiagramView.fitScale(1050, 900, 700, 500));
        // 666 / 500 is 1.332; rounding down keeps the grown diagram inside the space.
        assertEquals(1.33, WiringDiagramView.fitScale(1000, 666, 700, 500));
    }

    @Test
    void reflectorJoinsThirteenPairs() {
        onFxThread(() -> assertEquals(13, node(".diagram-reflector").lookupAll(".diagram-wire").size()));
    }

    @Test
    void followsEachLetterAndBackspace() {
        onFxThread(() -> {
            controller.pressKey('E');
            assertDrawnMatchesEngine();
            assertEquals("Right · V\nB · ring 12", headingOf(3));

            controller.deleteLastLetter();
            assertDrawnMatchesEngine();
            assertEquals("Right · V\nA · ring 12", headingOf(3));
        });
    }

    @Test
    void followsAPasteToItsLastPosition() {
        onFxThread(() -> {
            controller.pasteText("EDPUD NRGYS ZRCXN UYTPO");

            assertDrawnMatchesEngine();
        });
    }

    @Test
    void followsResetAndClear() {
        onFxThread(() -> {
            controller.pasteText("EDPUD");
            controller.clear();
            assertDrawnMatchesEngine();

            controller.reset();
            assertDrawnMatchesEngine();
            assertEquals("Right · V\nA · ring 12", headingOf(3));
        });
    }

    @Test
    void followsAppliedSettingsButNotUnappliedOnes() {
        onFxThread(() -> {
            controller.settings().reflectorProperty().set(Reflector.C);
            controller.settings().rotorProperty(0).set(RotorDefinition.I);
            assertEquals("Reflector\nB", headingOf(0));

            controller.applySettings();

            assertEquals("Reflector\nC", headingOf(0));
            assertEquals("Left · I\nB · ring 02", headingOf(1));
            assertDrawnMatchesEngine();
        });
    }

    private void assertDrawnMatchesEngine() {
        WiringDiagramView diagram = (WiringDiagramView) node(".wiring-diagram");
        WiringSnapshot snapshot = controller.viewModel().wiringProperty().get();
        for (int slot = 0; slot < 3; slot++) {
            assertEquals(snapshot.rotors().get(slot).forward(), diagram.drawnRotorWiring(slot), "rotor " + slot);
        }
        assertEquals(snapshot.reflectorWiring(), diagram.drawnReflectorWiring());
        assertEquals(snapshot.plugboardWiring(), diagram.drawnPlugboardWiring());
        // The rotor windows and the drawn wires come from the same engine state.
        StringBuilder positions = new StringBuilder();
        snapshot.rotors().forEach(rotor -> positions.append((char) ('A' + rotor.position())));
        assertEquals(positions.toString(), controller.viewModel().rotorWindowsProperty().get());
    }

    private String headingOf(int component) {
        return ((Label) nodes(".diagram-heading").stream().toList().get(component)).getText();
    }

    private Node node(String selector) {
        view.applyCss();
        return view.lookup(selector);
    }

    private List<Node> nodes(String selector) {
        view.applyCss();
        return List.copyOf(view.lookupAll(selector));
    }

    private void setUp() {
        controller = new MainController(new EnigmaService(BARBAROSSA));
        view = new DiagramView(controller);
        Scene scene = new Scene(view);
        Stage stage = new Stage();
        stage.setScene(scene);
        view.activate(SceneInput.installOn(stage));
        view.applyCss();
    }

    private void onFxThread(Runnable check) {
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
}
