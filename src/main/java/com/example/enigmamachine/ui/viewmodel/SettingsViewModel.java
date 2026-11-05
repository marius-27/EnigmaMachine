package com.example.enigmamachine.ui.viewmodel;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * The settings being edited. They stay separate from the running machine until they are applied.
 * Rotor slots are ordered left, middle, right. Choosing a rotor already in use swaps the two slots,
 * so the selection always holds three distinct rotors. Positions and ring settings are indexes from 0 to 25.
 */
public final class SettingsViewModel {

    private static final int USUAL_PLUGBOARD_PAIRS = 10;

    private final List<ObjectProperty<RotorDefinition>> rotors = new ArrayList<>();
    private final ObservableList<String> plugboardPairs = FXCollections.observableArrayList();
    private final ObservableList<String> readOnlyPlugboardPairs = FXCollections.unmodifiableObservableList(plugboardPairs);
    private final ReadOnlyStringWrapper pendingPlugboardLetter = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper plugboardWarning = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper message = new ReadOnlyStringWrapper("");
    private final List<ObjectProperty<Integer>> positions;
    private final List<ObjectProperty<Integer>> ringSettings;
    private final ObjectProperty<Reflector> reflector;
    private final ObjectProperty<MachineConfiguration> applied = new SimpleObjectProperty<>();
    private final BooleanBinding unappliedChanges;

    public SettingsViewModel(MachineConfiguration initial) {
        plugboardPairs.addListener((ListChangeListener<String>) change -> updatePlugboardWarning());
        initial.plugboardPairs().forEach(pair -> connect(pair.charAt(0), pair.charAt(1)));
        this.reflector = new SimpleObjectProperty<>(initial.reflector());
        this.positions = propertiesOf(initial.positions());
        this.ringSettings = propertiesOf(initial.ringSettings());
        for (RotorDefinition rotor : initial.rotors()) {
            ObjectProperty<RotorDefinition> slot = new SimpleObjectProperty<>(rotor);
            slot.addListener((observable, previous, current) -> swapDuplicate(slot, previous, current));
            rotors.add(slot);
        }
        markApplied();

        List<Observable> editors = new ArrayList<>(List.of(reflector, plugboardPairs));
        editors.addAll(rotors);
        editors.addAll(positions);
        editors.addAll(ringSettings);
        // A message about a save, load or apply describes the settings as they were, so any edit makes it stale.
        editors.forEach(editor -> editor.addListener(observable -> message.set("")));

        List<Observable> dependencies = new ArrayList<>(editors);
        dependencies.add(applied);
        this.unappliedChanges = Bindings.createBooleanBinding(
                this::differsFromApplied, dependencies.toArray(Observable[]::new));
    }

    /**
     * Fills the editors with the given settings. Like any other edit, this leaves the running machine
     * alone until the settings are applied.
     */
    public void load(MachineConfiguration configuration) {
        for (int slot = 0; slot < rotors.size(); slot++) {
            rotors.get(slot).set(configuration.rotors().get(slot));
            positions.get(slot).set(configuration.positions().get(slot));
            ringSettings.get(slot).set(configuration.ringSettings().get(slot));
        }
        reflector.set(configuration.reflector());
        pendingPlugboardLetter.set("");
        plugboardPairs.clear();
        configuration.plugboardPairs().forEach(pair -> connect(pair.charAt(0), pair.charAt(1)));
    }

    /**
     * The result of the last save, load or apply, or an empty string. Editing any setting empties it,
     * so it never describes settings that have changed since.
     */
    public ReadOnlyStringProperty messageProperty() {
        return message.getReadOnlyProperty();
    }

    public void showMessage(String text) {
        message.set(text);
    }

    /** True while the edited settings differ from those the machine is running with. */
    public BooleanBinding unappliedChangesBinding() {
        return unappliedChanges;
    }

    /** Records that the machine now runs with the edited settings. A letter still waiting for a partner is dropped. */
    public void markApplied() {
        pendingPlugboardLetter.set("");
        applied.set(toConfiguration());
    }

    public ObjectProperty<RotorDefinition> rotorProperty(int slot) {
        return rotors.get(slot);
    }

    public ObjectProperty<Integer> positionProperty(int slot) {
        return positions.get(slot);
    }

    public ObjectProperty<Integer> ringSettingProperty(int slot) {
        return ringSettings.get(slot);
    }

    public int rotorSlots() {
        return rotors.size();
    }

    public ObjectProperty<Reflector> reflectorProperty() {
        return reflector;
    }

    /** The connected pairs in alphabetical order, each written like "AB". */
    public ObservableList<String> plugboardPairs() {
        return readOnlyPlugboardPairs;
    }

    /** The letter waiting for a partner, or an empty string when none is. */
    public ReadOnlyStringProperty pendingPlugboardLetterProperty() {
        return pendingPlugboardLetter.getReadOnlyProperty();
    }

    /** A note for more pairs than were usual on the M3, or an empty string. It never blocks the settings. */
    public ReadOnlyStringProperty plugboardWarningProperty() {
        return plugboardWarning.getReadOnlyProperty();
    }

    /**
     * A connected letter loses its pair. Otherwise the first letter waits for a partner, pressing it again
     * cancels the wait, and a second letter completes the pair. A letter can therefore never be in two pairs.
     */
    public void pressPlugboardLetter(char key) {
        char letter = Alphabet.normalize(key);
        String pending = pendingPlugboardLetter.get();
        pendingPlugboardLetter.set("");

        boolean disconnected = plugboardPairs.removeIf(pair -> pair.indexOf(letter) >= 0);
        if (disconnected || pending.equals(String.valueOf(letter))) {
            return;
        }
        if (pending.isEmpty()) {
            pendingPlugboardLetter.set(String.valueOf(letter));
        } else {
            connect(pending.charAt(0), letter);
        }
    }

    public MachineConfiguration toConfiguration() {
        return new MachineConfiguration(
                valuesOf(rotors),
                reflector.get(),
                valuesOf(ringSettings),
                valuesOf(positions),
                List.copyOf(plugboardPairs));
    }

    // Compares the raw values, because a configuration cannot be built while two rotor slots are mid-swap.
    private boolean differsFromApplied() {
        MachineConfiguration current = applied.get();
        return !valuesOf(rotors).equals(current.rotors())
                || reflector.get() != current.reflector()
                || !valuesOf(ringSettings).equals(current.ringSettings())
                || !valuesOf(positions).equals(current.positions())
                || !plugboardPairs.equals(current.plugboardPairs());
    }

    private void connect(char firstLetter, char secondLetter) {
        char first = Alphabet.normalize(firstLetter);
        char second = Alphabet.normalize(secondLetter);
        plugboardPairs.add(first < second ? "" + first + second : "" + second + first);
        FXCollections.sort(plugboardPairs);
    }

    private void updatePlugboardWarning() {
        plugboardWarning.set(plugboardPairs.size() > USUAL_PLUGBOARD_PAIRS
                ? "The M3 normally used at most " + USUAL_PLUGBOARD_PAIRS + " pairs."
                : "");
    }

    private void swapDuplicate(
            ObjectProperty<RotorDefinition> changed, RotorDefinition previous, RotorDefinition current) {
        for (ObjectProperty<RotorDefinition> other : rotors) {
            if (other != changed && other.get() == current) {
                other.set(previous);
            }
        }
    }

    private static List<ObjectProperty<Integer>> propertiesOf(List<Integer> values) {
        List<ObjectProperty<Integer>> properties = new ArrayList<>();
        for (Integer value : values) {
            properties.add(new SimpleObjectProperty<>(value));
        }
        return properties;
    }

    private static <T> List<T> valuesOf(List<ObjectProperty<T>> properties) {
        return properties.stream().map(ObjectProperty::get).toList();
    }
}
