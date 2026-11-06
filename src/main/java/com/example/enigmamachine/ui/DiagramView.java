package com.example.enigmamachine.ui;

import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The diagram interface: the components the signal crosses, laid out from the reflector on the left to the
 * keys on the right, with the keyboard below them. The keys column of the diagram lights the output letter, so
 * this mode needs no separate lampboard and the diagram keeps most of the window.
 */
public final class DiagramView extends MachineView {

    public DiagramView(MainController controller) {
        super(controller, "diagram-view");
        WiringDiagramView diagram = new WiringDiagramView(
                controller.viewModel().wiringProperty(), controller.viewModel().litLampProperty());
        // The group takes the diagram's scaled size, and the stack pane keeps it centred in the space left over.
        StackPane content = new StackPane(new Group(diagram));
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

        TitledPane diagramSection = ViewParts.section("Diagram", diagramScroll);
        TitledPane keyboard = ViewParts.section("Keyboard", keyboardView());

        VBox machine = new VBox(ViewParts.SPACING, diagramSection, keyboard);
        VBox.setVgrow(diagramSection, Priority.ALWAYS);
        setCenter(machine);
    }
}
