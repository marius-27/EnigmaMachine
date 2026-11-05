package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import com.example.enigmamachine.ui.viewmodel.SettingsViewModel;
import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

/**
 * Editors for the machine settings. Each rotor has a row with its choice, start position (A-Z) and ring (01-26).
 */
public final class SettingsView extends GridPane {

    private static final double SPACING = 8;
    private static final List<String> ROTOR_SLOTS = List.of("Left", "Middle", "Right");
    private static final List<Integer> ALPHABET_INDEXES = IntStream.range(0, Alphabet.SIZE).boxed().toList();
    private static final String DEFAULT_FILE_NAME = "enigma-settings.json";

    private File lastFolder;

    public SettingsView(MainController controller) {
        SettingsViewModel settings = controller.settings();
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
        add(applySection(controller), 0, settings.rotorSlots() + 3, 4, 1);
    }

    private VBox applySection(MainController controller) {
        BooleanBinding unapplied = controller.settings().unappliedChangesBinding();

        Button apply = action("Apply", "apply-settings", controller::applySettings);
        apply.disableProperty().bind(unapplied.not());
        Button save = action("Save…", "save-settings", () -> chooseFileToSave(controller));
        Button load = action("Load…", "load-settings", () -> chooseFileToLoad(controller));

        Label status = new Label();
        status.getStyleClass().add("settings-status");
        status.setWrapText(true);
        status.textProperty().bind(Bindings
                .when(unapplied.not())
                .then("")
                .otherwise(Bindings
                        .when(controller.viewModel().inputTextProperty().isEmpty())
                        .then("Changes are not applied yet.")
                        .otherwise("Applying empties the current message.")));

        Label message = new Label();
        message.getStyleClass().add("settings-message");
        message.setWrapText(true);
        message.textProperty().bind(controller.settings().messageProperty());
        message.managedProperty().bind(message.textProperty().isNotEmpty());

        return new VBox(SPACING, new HBox(SPACING, apply, save, load), status, message);
    }

    private static Button action(String title, String styleClass, Runnable handler) {
        Button button = new Button(title);
        button.getStyleClass().add(styleClass);
        // A focused button would fire on Space while a message is being typed.
        button.setFocusTraversable(false);
        button.setOnAction(event -> handler.run());
        return button;
    }

    /** The system dialog asks for confirmation itself before a file is overwritten. */
    private void chooseFileToSave(MainController controller) {
        FileChooser chooser = fileChooser("Save settings");
        chooser.setInitialFileName(DEFAULT_FILE_NAME);
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            lastFolder = file.getParentFile();
            controller.saveSettings(file.toPath());
        }
    }

    private void chooseFileToLoad(MainController controller) {
        File file = fileChooser("Load settings").showOpenDialog(getScene().getWindow());
        if (file != null) {
            lastFolder = file.getParentFile();
            controller.loadSettings(file.toPath());
        }
    }

    private FileChooser fileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Enigma settings (*.json)", "*.json"));
        if (lastFolder != null && lastFolder.isDirectory()) {
            chooser.setInitialDirectory(lastFolder);
        }
        return chooser;
    }

    private static String positionLabel(int index) {
        return String.valueOf(Alphabet.toLetter(index));
    }

    /** Ring settings are numbered from 01 on the machine, while the engine counts from 0. */
    private static String ringLabel(int index) {
        return String.format("%02d", index + 1);
    }

    static <T> ComboBox<T> selector(
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
