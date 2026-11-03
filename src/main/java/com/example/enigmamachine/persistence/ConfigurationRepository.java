package com.example.enigmamachine.persistence;

import com.example.enigmamachine.domain.MachineConfiguration;
import java.nio.file.Path;

/**
 * Keeps machine configurations in files.
 */
public interface ConfigurationRepository {

    void save(MachineConfiguration configuration, Path file) throws ConfigurationFileException;

    MachineConfiguration load(Path file) throws ConfigurationFileException;
}
