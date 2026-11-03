package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.ui.viewmodel.SettingsViewModel;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

/**
 * Editors for the machine settings. Each rotor has a row with its choice, start position (A-Z) and ring (01-26).
 */
public final class SettingsView extends GridPane {

    private static final double SPACING = 8;
    private static final List<String> ROTOR_SLOTS = List.of("Left", "Middle", "Right");
    private static final List<Integer> ALPHABET_INDEXES = IntStream.range(0, Alphabet.SIZE).boxed().toList();

    public SettingsView(SettingsViewModel settings) {
        setHgap(SPACING);
        setVgap(SPACING);
        setPadding(new Insets(SPACING));

        addRow(0, new Label(), new Label("Rotor"), new Label("Start"), new Label("Ring"));
        for (int slot = 0; slot < settings.rotorSlots(); slot++) {
            addRow(slot + 1, new Label(ROTOR_SLOTS.get(slot)),
                    selector("rotor-selector", List.of(RotorDefinition.values()), settings.rotorProperty(slot), String::valueOf),
                    selector("position-selector", ALPHABET_INDEXES, settings.positionProperty(slot), SettingsView::positionLabel),
                    selector("ring-selector", ALPHABET_INDEXES, settings.ringSettingProperty(slot), SettingsView::ringLabel));
        }
        addRow(settings.rotorSlots() + 1, new Label("Reflector"),
                selector("reflector-selector", List.of(Reflector.values()), settings.reflectorProperty(), String::valueOf));
        add(new PlugboardView(settings), 0, settings.rotorSlots() + 2, 4, 1);
    }

    private static String positionLabel(int index) {
        return String.valueOf(Alphabet.toLetter(index));
    }

    /** Ring settings are numbered from 01 on the machine, while the engine counts from 0. */
    private static String ringLabel(int index) {
        return String.format("%02d", index + 1);
    }

    private static <T> ComboBox<T> selector(
            String styleClass, List<T> choices, Property<T> value, Function<T, String> label) {
        ComboBox<T> selector = new ComboBox<>();
        selector.getItems().setAll(choices);
        selector.getStyleClass().add(styleClass);
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(T choice) {
                return choice == null ? "" : label.apply(choice);
            }

            @Override
            public T fromString(String text) {
                // The selectors are not editable, so text is never turned back into a choice.
                throw new UnsupportedOperationException();
            }
        });
        selector.valueProperty().bindBidirectional(value);
        // A focused selector would open on Space while a message is being typed.
        selector.setFocusTraversable(false);
        return selector;
    }
}
