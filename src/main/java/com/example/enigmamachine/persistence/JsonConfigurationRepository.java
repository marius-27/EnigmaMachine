package com.example.enigmamachine.persistence;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Saves configurations as UTF-8 JSON, in the format described by {@link ConfigurationFile}.
 */
public final class JsonConfigurationRepository implements ConfigurationRepository {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void save(MachineConfiguration configuration, Path file) throws ConfigurationFileException {
        try {
            String json = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(ConfigurationFile.from(configuration));
            replaceContents(file, json);
        } catch (IOException e) {
            throw new ConfigurationFileException("Could not save " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public MachineConfiguration load(Path file) throws ConfigurationFileException {
        String name = String.valueOf(file.getFileName());
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            ConfigurationFile contents = mapper.readValue(json, ConfigurationFile.class);
            if (contents == null) {
                throw new ConfigurationFileException(name + " is not a valid Enigma configuration file.", null);
            }
            return contents.toConfiguration();
        } catch (NoSuchFileException e) {
            throw new ConfigurationFileException("The file " + name + " does not exist.", e);
        } catch (JsonProcessingException e) {
            throw new ConfigurationFileException(name + " is not a valid Enigma configuration file.", e);
        } catch (IOException e) {
            throw new ConfigurationFileException("Could not read " + name + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationFileException(name + " has invalid settings: " + e.getMessage(), e);
        }
    }

    /** Writes beside the target first, so a failure while writing cannot damage a file that already exists. */
    private static void replaceContents(Path file, String contents) throws IOException {
        Path target = file.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
