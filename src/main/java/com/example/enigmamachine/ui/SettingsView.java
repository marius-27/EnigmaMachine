package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.ui.viewmodel.SettingsViewModel;
import java.util.List;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 * Editors for the machine settings.
 */
public final class SettingsView extends GridPane {

    private static final double SPACING = 8;
    private static final List<String> ROTOR_SLOTS = List.of("Left rotor", "Middle rotor", "Right rotor");

    public SettingsView(SettingsViewModel settings) {
        setHgap(SPACING);
        setVgap(SPACING);
        setPadding(new Insets(SPACING));

        for (int slot = 0; slot < settings.rotorSlots(); slot++) {
            addRow(slot, new Label(ROTOR_SLOTS.get(slot)),
                    selector("rotor-selector", RotorDefinition.values(), settings.rotorProperty(slot)));
        }
        addRow(settings.rotorSlots(), new Label("Reflector"),
                selector("reflector-selector", Reflector.values(), settings.reflectorProperty()));
    }

    private static <T> ComboBox<T> selector(String styleClass, T[] choices, Property<T> value) {
        ComboBox<T> selector = new ComboBox<>();
        selector.getItems().setAll(choices);
        selector.getStyleClass().add(styleClass);
        selector.valueProperty().bindBidirectional(value);
        // A focused selector would open on Space while a message is being typed.
        selector.setFocusTraversable(false);
        return selector;
    }
}
