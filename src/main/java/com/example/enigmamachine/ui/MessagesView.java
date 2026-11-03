package com.example.enigmamachine.ui;

import com.example.enigmamachine.ui.viewmodel.MachineViewModel;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Read-only input and output text, with the Clear and Reset actions.
 */
public final class MessagesView extends VBox {

    private static final double SPACING = 8;

    public MessagesView(MainController controller) {
        super(SPACING);
        setPadding(new Insets(SPACING));
        MachineViewModel viewModel = controller.viewModel();
        TextArea input = messageArea(viewModel.inputTextProperty());
        TextArea output = messageArea(viewModel.outputTextProperty());

        Button decrypt = action(
                "Decrypt", "Run the output back through the machine from where the message began.", controller::decrypt);
        decrypt.disableProperty().bind(viewModel.outputTextProperty().isEmpty());

        HBox actions = new HBox(SPACING,
                decrypt,
                action("Clear","Empty the messages; the rotors stay where they are.", controller::clear),
                action("Reset", "Empty the messages and restore the initial rotor positions.", controller::reset));
        actions.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(new Label("Input"), input, new Label("Output"), output, actions);
    }

    private static TextArea messageArea(ObservableValue<String> text) {
        TextArea area = new TextArea();
        area.getStyleClass().add("message-area");
        area.setEditable(false);
        area.setWrapText(true);
        area.setText(text.getValue());
        // Replacing the whole text would send the view back to the top on every letter, so only the change is applied.
        text.addListener((observable, previous, current) -> {
            if (current.startsWith(previous)) {
                area.appendText(current.substring(previous.length()));
            } else if (previous.startsWith(current)) {
                area.deleteText(current.length(), previous.length());
            } else {
                area.setText(current);
            }
        });
        VBox.setVgrow(area, Priority.ALWAYS);
        return area;
    }

    private static Button action(String title, String description, Runnable handler) {
        Button button = new Button(title);
        button.setTooltip(new Tooltip(description));
        // A focused button would fire on Space, so typing a message could wipe it after a click.
        button.setFocusTraversable(false);
        button.setOnAction(event -> handler.run());
        return button;
    }
}
