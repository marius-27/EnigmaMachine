package com.example.enigmamachine.persistence;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The JSON format of a saved machine configuration, written the way the machine labels its settings:
 * rotor and reflector names, start positions as letters, and ring settings from 1 to 26.
 *
 * <p>Only the initial settings are saved. Rotor positions reached while typing and the message itself
 * belong to the session, so loading a file always starts a fresh message.
 *
 * <p>Rotors, ring settings and positions are ordered left, middle, right.
 *
 * @param formatVersion the version of this format, so that later versions can recognize older files
 */
public record ConfigurationFile(
        int formatVersion,
        List<String> rotors,
        String reflector,
        List<Integer> ringSettings,
        List<String> positions,
        List<String> plugboard) {

    public static final int CURRENT_VERSION = 1;

    public static ConfigurationFile from(MachineConfiguration configuration) {
        return new ConfigurationFile(
                CURRENT_VERSION,
                configuration.rotors().stream().map(RotorDefinition::name).toList(),
                configuration.reflector().name(),
                configuration.ringSettings().stream().map(index -> index + 1).toList(),
                configuration.positions().stream().map(index -> String.valueOf(Alphabet.toLetter(index))).toList(),
                configuration.plugboardPairs());
    }

    /**
     * Rebuilds the configuration, or throws {@link IllegalArgumentException} with a readable message
     * if this file is from another format version or holds invalid settings.
     */
    public MachineConfiguration toConfiguration() {
        if (formatVersion != CURRENT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported format version " + formatVersion + ". Expected " + CURRENT_VERSION + ".");
        }
        if (reflector == null) {
            throw new IllegalArgumentException("The file has no reflector.");
        }
        return new MachineConfiguration(
                convert(rotors, "rotors", ConfigurationFile::rotorNamed),
                reflectorNamed(reflector),
                convert(ringSettings, "ring settings", ConfigurationFile::ringIndex),
                convert(positions, "positions", ConfigurationFile::positionIndex),
                convert(plugboard, "plugboard", Function.identity()));
    }

    private static <S, T> List<T> convert(List<S> values, String name, Function<S, T> conversion) {
        if (values == null) {
            throw new IllegalArgumentException("The file has no " + name + ".");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("The file has an empty value in " + name + ".");
        }
        return values.stream().map(conversion).toList();
    }

    private static RotorDefinition rotorNamed(String name) {
        for (RotorDefinition rotor : RotorDefinition.values()) {
            if (rotor.name().equals(name)) {
                return rotor;
            }
        }
        throw new IllegalArgumentException("Unknown rotor: '" + name + "'.");
    }

    private static Reflector reflectorNamed(String name) {
        for (Reflector candidate : Reflector.values()) {
            if (candidate.name().equals(name)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown reflector: '" + name + "'.");
    }

    private static Integer ringIndex(Integer ring) {
        if (ring < 1 || ring > Alphabet.SIZE) {
            throw new IllegalArgumentException("Invalid ring setting: " + ring + ". Expected 1-26.");
        }
        return ring - 1;
    }

    private static Integer positionIndex(String position) {
        if (position.length() != 1 || !Alphabet.isValid(position.charAt(0))) {
            throw new IllegalArgumentException("Invalid position: '" + position + "'. Expected a letter A-Z.");
        }
        return Alphabet.toIndex(position.charAt(0));
    }
}
