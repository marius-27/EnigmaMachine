package com.example.enigmamachine.ui;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    private final StackPane content;
    private final ScrollPane scroll;

    public DiagramView(MainController controller) {
        super(controller, "diagram-view");
        diagram = new WiringDiagramView(
                controller.viewModel().wiringProperty(), controller.viewModel().litLampProperty());
        // The group takes the diagram's scaled size, and the stack pane keeps it centred in the space left over.
        content = new StackPane(new Group(diagram));
        scroll = diagramScroll();
        SignalPathView signal = signalPathView();
        signal.shownTraceProperty().addListener((observable, previous, trace) -> {
            diagram.showSignal(trace, signal.activeStepProperty().get());
            followSignal();
        });
        signal.activeStepProperty().addListener((observable, previous, step) -> {
            diagram.showSignal(signal.shownTraceProperty().get(), step.intValue());
            followSignal();
        });

        VBox diagramArea = new VBox(ViewParts.SPACING / 2, scroll, replayFooter(signal));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        TitledPane diagramSection = ViewParts.section("Diagram", diagramArea);
        TitledPane keyboard = ViewParts.section("Keyboard", keyboardView());

        VBox machine = new VBox(ViewParts.SPACING, diagramSection, keyboard);
        VBox.setVgrow(diagramSection, Priority.ALWAYS);
        setCenter(machine);
    }

    private ScrollPane diagramScroll() {
        ScrollPane diagramScroll = new ScrollPane(content);
        diagramScroll.getStyleClass().add("diagram-scroll");
        diagramScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        diagramScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // A focused scroll pane would scroll on Space while a message is being typed.
        diagramScroll.setFocusTraversable(false);
        // A large window enlarges the diagram; a small one scrolls it rather than shrinking its letters.
        diagramScroll.viewportBoundsProperty().addListener((observable, previous, viewport) -> {
            double scale = WiringDiagramView.fitScale(
                    viewport.getWidth(), viewport.getHeight(), diagram.getPrefWidth(), diagram.getPrefHeight());
            diagram.setScaleX(scale);
            diagram.setScaleY(scale);
            content.setMinSize(viewport.getWidth(), viewport.getHeight());
        });
        return diagramScroll;
    }

    /**
     * Keeps the step being shown in view when the window is too small for the whole diagram. The diagram only
     * scrolls at its normal size, so its own coordinates are those of the scrolled content.
     */
    private void followSignal() {
        Point2D point = diagram.currentPoint();
        if (point == null || diagram.getScaleX() != 1) {
            return;
        }
        double viewportWidth = scroll.getViewportBounds().getWidth();
        double viewportHeight = scroll.getViewportBounds().getHeight();
        scroll.setHvalue(WiringDiagramView.scrollToShow(
                point.getX(), scroll.getHvalue(), viewportWidth, content.getWidth()));
        scroll.setVvalue(WiringDiagramView.scrollToShow(
                point.getY(), scroll.getVvalue(), viewportHeight, content.getHeight()));
    }

    /**
     * The step in words and the replay controls, on one line while there is room for both. In a narrow window the
     * controls move under the text rather than being squeezed until their labels no longer fit.
     */
    private Node replayFooter(SignalPathView signal) {
        Region bar = (Region) signal.replayBar();
        bar.setMinWidth(Region.USE_PREF_SIZE);
        FlowPane footer = new FlowPane(ViewParts.SPACING, ViewParts.SPACING / 2, diagram.stepLabel(), bar);
        footer.getStyleClass().add("diagram-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(0, 4, 4, 4));
        // A long step text wraps inside the footer instead of running past its edge.
        diagram.stepLabel().maxWidthProperty().bind(footer.widthProperty().subtract(8));
        return footer;
    }
}
