package com.example.enigmamachine.ui.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingsViewModelTest {

    private static final MachineConfiguration INITIAL = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(1, 2, 3),
            List.of(4, 5, 6),
            List.of("AB"));

    private final SettingsViewModel settings = new SettingsViewModel(INITIAL);

    @Test
    void startsFromTheGivenConfiguration() {
        assertEquals(INITIAL, settings.toConfiguration());
    }

    @Test
    void choosingAnUnusedRotorChangesOnlyThatSlot() {
        settings.rotorProperty(0).set(RotorDefinition.V);

        assertEquals(
                List.of(RotorDefinition.V, RotorDefinition.II, RotorDefinition.III),
                settings.toConfiguration().rotors());
    }

    @Test
    void choosingARotorInUseSwapsTheTwoSlots() {
        settings.rotorProperty(0).set(RotorDefinition.III);

        assertEquals(
                List.of(RotorDefinition.III, RotorDefinition.II, RotorDefinition.I),
                settings.toConfiguration().rotors());
    }

    @Test
    void reflectorCanBeChanged() {
        settings.reflectorProperty().set(Reflector.C);

        assertEquals(Reflector.C, settings.toConfiguration().reflector());
    }

    @Test
    void changingARotorKeepsTheOtherSettings() {
        settings.rotorProperty(2).set(RotorDefinition.IV);
        settings.reflectorProperty().set(Reflector.C);

        MachineConfiguration edited = settings.toConfiguration();

        assertEquals(INITIAL.ringSettings(), edited.ringSettings());
        assertEquals(INITIAL.positions(), edited.positions());
        assertEquals(INITIAL.plugboardPairs(), edited.plugboardPairs());
    }

    @Test
    void startPositionsAreEditedPerSlot() {
        settings.positionProperty(0).set(25);
        settings.positionProperty(2).set(0);

        assertEquals(List.of(25, 5, 0), settings.toConfiguration().positions());
        assertEquals(INITIAL.ringSettings(), settings.toConfiguration().ringSettings());
    }

    @Test
    void ringSettingsAreEditedPerSlot() {
        settings.ringSettingProperty(1).set(25);

        assertEquals(List.of(1, 25, 3), settings.toConfiguration().ringSettings());
        assertEquals(INITIAL.positions(), settings.toConfiguration().positions());
    }

    @Test
    void swappingRotorsLeavesPositionsAndRingsInTheirSlots() {
        settings.rotorProperty(0).set(RotorDefinition.III);

        assertEquals(INITIAL.positions(), settings.toConfiguration().positions());
        assertEquals(INITIAL.ringSettings(), settings.toConfiguration().ringSettings());
    }
}
