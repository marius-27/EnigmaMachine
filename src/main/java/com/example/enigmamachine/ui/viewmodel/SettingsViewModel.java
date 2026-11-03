package com.example.enigmamachine.ui.viewmodel;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * The settings being edited. They stay separate from the running machine until they are applied.
 * Rotor slots are ordered left, middle, right. Choosing a rotor already in use swaps the two slots,
 * so the selection always holds three distinct rotors.
 */
public final class SettingsViewModel {

    private final MachineConfiguration initial;
    private final List<ObjectProperty<RotorDefinition>> rotors = new ArrayList<>();
    private final ObjectProperty<Reflector> reflector;

    public SettingsViewModel(MachineConfiguration initial) {
        this.initial = initial;
        this.reflector = new SimpleObjectProperty<>(initial.reflector());
        for (RotorDefinition rotor : initial.rotors()) {
            ObjectProperty<RotorDefinition> slot = new SimpleObjectProperty<>(rotor);
            slot.addListener((observable, previous, current) -> swapDuplicate(slot, previous, current));
            rotors.add(slot);
        }
    }

    public ObjectProperty<RotorDefinition> rotorProperty(int slot) {
        return rotors.get(slot);
    }

    public int rotorSlots() {
        return rotors.size();
    }

    public ObjectProperty<Reflector> reflectorProperty() {
        return reflector;
    }

    public MachineConfiguration toConfiguration() {
        return new MachineConfiguration(
                rotors.stream().map(ObjectProperty::get).toList(),
                reflector.get(),
                initial.ringSettings(),
                initial.positions(),
                initial.plugboardPairs());
    }

    private void swapDuplicate(
            ObjectProperty<RotorDefinition> changed, RotorDefinition previous, RotorDefinition current) {
        for (ObjectProperty<RotorDefinition> other : rotors) {
            if (other != changed && other.get() == current) {
                other.set(previous);
            }
        }
    }
}
