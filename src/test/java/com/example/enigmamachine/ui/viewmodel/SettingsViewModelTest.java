package com.example.enigmamachine.ui.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void twoLettersFormAPair() {
        press("CD");

        assertEquals(List.of("AB", "CD"), settings.toConfiguration().plugboardPairs());
        assertEquals("", settings.pendingPlugboardLetterProperty().get());
    }

    @Test
    void firstLetterWaitsForItsPartner() {
        press("C");

        assertEquals("C", settings.pendingPlugboardLetterProperty().get());
        assertEquals(List.of("AB"), settings.toConfiguration().plugboardPairs());
    }

    @Test
    void pairsAreWrittenAndListedAlphabetically() {
        press("ZY");
        press("DC");

        assertEquals(List.of("AB", "CD", "YZ"), settings.plugboardPairs());
    }

    @Test
    void pressingTheWaitingLetterAgainCancelsIt() {
        press("CC");

        assertEquals("", settings.pendingPlugboardLetterProperty().get());
        assertEquals(List.of("AB"), settings.plugboardPairs());
    }

    @Test
    void pressingAConnectedLetterRemovesItsPair() {
        press("B");

        assertEquals(List.of(), settings.plugboardPairs());
        assertEquals("", settings.pendingPlugboardLetterProperty().get());
    }

    @Test
    void aWaitingLetterCannotJoinALetterThatIsAlreadyConnected() {
        press("CA");

        assertEquals(List.of(), settings.plugboardPairs());
        assertEquals("", settings.pendingPlugboardLetterProperty().get());
    }

    @Test
    void allThirteenPairsAreAcceptedByTheEngine() {
        press("CDEFGHIJKLMNOPQRSTUVWXYZ");

        assertEquals(13, settings.toConfiguration().plugboardPairs().size());
    }

    @Test
    void warnsOnlyAboveTenPairs() {
        press("CDEFGHIJKLMNOPQRST");
        assertEquals(10, settings.plugboardPairs().size());
        assertEquals("", settings.plugboardWarningProperty().get());

        press("UV");
        assertEquals("The M3 normally used at most 10 pairs.", settings.plugboardWarningProperty().get());

        press("U");
        assertEquals("", settings.plugboardWarningProperty().get());
    }

    @Test
    void plugboardListCannotBeChangedFromOutside() {
        assertThrows(UnsupportedOperationException.class, () -> settings.plugboardPairs().clear());
    }

    private void press(String letters) {
        letters.chars().forEach(letter -> settings.pressPlugboardLetter((char) letter));
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
