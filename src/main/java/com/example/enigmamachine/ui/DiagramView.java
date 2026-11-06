package com.example.enigmamachine.ui;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The diagram interface: the components the signal crosses, laid out from the reflector on the left to the
 * keys on the right, with the keyboard below them. The keys column of the diagram lights the output letter, so
 * this mode needs no separate lampboard and the diagram keeps most of the window.
 *
 * <p>The diagram plays the same trace as the Signal tab, step by step: the replay bar under it drives that one
 * playback, so the diagram, the tab and both sets of controls always show the same step.
 */
public final class DiagramView extends MachineView {

    private final WiringDiagramView diagram;

    public DiagramView(MainController controller) {
        super(controller, "diagram-view");
        diagram = new WiringDiagramView(
                controller.viewModel().wiringProperty(), controller.viewModel().litLampProperty());
        SignalPathView signal = signalPathView();
        signal.shownTraceProperty().addListener(
                (observable, previous, trace) -> diagram.showSignal(trace, signal.activeStepProperty().get()));
        signal.activeStepProperty().addListener(
                (observable, previous, step) -> diagram.showSignal(signal.shownTraceProperty().get(), step.intValue()));

        VBox diagramArea = new VBox(ViewParts.SPACING / 2, diagramScroll(), replayFooter(signal));
        VBox.setVgrow(diagramArea.getChildren().get(0), Priority.ALWAYS);
        TitledPane diagramSection = ViewParts.section("Diagram", diagramArea);
        TitledPane keyboard = ViewParts.section("Keyboard", keyboardView());

        VBox machine = new VBox(ViewParts.SPACING, diagramSection, keyboard);
        VBox.setVgrow(diagramSection, Priority.ALWAYS);
        setCenter(machine);
    }

    private Node diagramScroll() {
        // The group takes the diagram's scaled size, and the stack pane keeps it centred in the space left over.
        StackPane content = new StackPane(new Group(diagram));
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("diagram-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // A focused scroll pane would scroll on Space while a message is being typed.
        scroll.setFocusTraversable(false);
        // A large window enlarges the diagram; a small one scrolls it rather than shrinking its letters.
        scroll.viewportBoundsProperty().addListener((observable, previous, viewport) -> {
            double scale = WiringDiagramView.fitScale(
                    viewport.getWidth(), viewport.getHeight(), diagram.getPrefWidth(), diagram.getPrefHeight());
            diagram.setScaleX(scale);
            diagram.setScaleY(scale);
            content.setMinSize(viewport.getWidth(), viewport.getHeight());
        });
        return scroll;
    }

    /** The step in words and the replay controls, which stay in view while the diagram scrolls. */
    private Node replayFooter(SignalPathView signal) {
        VBox footer = new VBox(ViewParts.SPACING / 2, diagram.stepLabel(), signal.replayBar());
        footer.getStyleClass().add("diagram-footer");
        footer.setPadding(new Insets(0, 4, 4, 4));
        return footer;
    }
}
