# Enigma Machine

A desktop simulator of the Enigma M3 machine, designed to be historically accurate and easy to test and use.

> This project is under development. The encryption engine is being built and the application does not encrypt messages yet.

## Requirements

- JDK 25
- Maven does not need to be installed separately because the project includes the Maven Wrapper.

## Commands

On Windows:

```powershell
.\mvnw.cmd clean javafx:run   # start the application
.\mvnw.cmd test               # run the tests
.\mvnw.cmd clean verify       # run the full build and tests
```

On Linux and macOS, use `./mvnw` instead of `.\mvnw.cmd`.

## Architecture

The project separates three areas of responsibility:

| Area | Role | Rules |
|---|---|---|
| Engine | Rotors, reflector, plugboard, stepping and signal path | Does not depend on JavaFX and can be tested independently |
| UI | Windows, keyboard, lampboard, settings and animations | Calls the engine and contains no encryption logic |
| Persistence | Saving and loading configurations as JSON | Does not depend on the UI |

## Technologies

Java 25, JavaFX 25, Maven, JUnit 5
