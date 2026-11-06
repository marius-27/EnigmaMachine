package com.example.enigmamachine.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The diagram interface: the components the signal crosses, laid out from the reflector on the left to the
 * keyboard contacts on the right, with the lampboard and the keyboard below them.
 */
public final class DiagramView extends MachineView {

    /** The components in the order they are drawn, left to right. */
    static final String COMPONENT_ORDER = "Reflector · Left rotor · Middle rotor · Right rotor · Plugboard · Keys";

    public DiagramView(MainController controller) {
        super(controller, "diagram-view");
        Label components = new Label(COMPONENT_ORDER);
        components.getStyleClass().add("diagram-components");
        components.setWrapText(true);
        HBox area = new HBox(ViewParts.SPACING,
                new RotorWindowsView(controller.viewModel().rotorWindowsProperty()), components);
        area.getStyleClass().add("diagram-area");
        area.setAlignment(Pos.CENTER_LEFT);

        TitledPane diagram = ViewParts.section("Diagram", area);
        TitledPane lampboard = ViewParts.section(
                "Lampboard", new LampboardView(controller.viewModel().litLampProperty()));
        TitledPane keyboard = ViewParts.section("Keyboard", keyboardView());

        VBox machine = new VBox(ViewParts.SPACING, diagram, lampboard, keyboard);
        VBox.setVgrow(diagram, Priority.ALWAYS);
        setCenter(machine);
    }
}
