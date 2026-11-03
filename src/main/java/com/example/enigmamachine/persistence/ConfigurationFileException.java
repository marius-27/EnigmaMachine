package com.example.enigmamachine.persistence;

/**
 * A configuration file could not be saved or loaded. The message is written to be shown to the user.
 */
public final class ConfigurationFileException extends Exception {

    public ConfigurationFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
