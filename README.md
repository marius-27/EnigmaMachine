# Enigma Machine

A desktop simulator of the Enigma M3 machine, designed to be historically accurate and easy to test and use.

> This project is under development. The encryption engine works and is covered by tests, but the desktop interface is not connected to it yet.

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

## Enigma M3 model

The engine simulates a three-rotor Enigma M3:

| Component | Supported |
|---|---|
| Rotors | Three distinct rotors chosen from I, II, III, IV and V |
| Reflectors | B and C |
| Ring settings and start positions | A-Z for each rotor |
| Plugboard | Reciprocal letter pairs, up to 13 |
| Stepping | Rotors step before each letter, including the historical double step |

### Message input

- Only the letters A-Z are encrypted. Lowercase letters are treated as uppercase.
- When encrypting a message, spaces, digits, punctuation and letters with diacritics are removed and do not step the rotors. They cannot be recovered after decryption.

### Decryption

Enigma is reciprocal: to decrypt, restore the initial settings and encrypt the ciphertext. Rotor positions change with every letter, so decrypting from already advanced positions produces the wrong text.

### Limitations

- Rotors VI, VII and VIII are not supported.
- Reflector A and the thin reflectors are not supported.
- The four-rotor Enigma M4 and other variants are not supported.
- Historical use of the plugboard was usually limited to 10 pairs, but the engine allows up to 13.

### Verified example

The tests decrypt both parts of a real message sent during Operation Barbarossa on 7 July 1941 and compare the result with its [published decrypt](http://wiki.franklinheath.co.uk/index.php/Enigma/Sample_Decrypts). The [settings and ciphertext](http://wiki.franklinheath.co.uk/index.php/Enigma/Sample_Messages) for the start of the first part are:

| Setting | Value |
|---|---|
| Rotors | II, IV, V |
| Reflector | B |
| Ring settings | B, U, L |
| Start positions | B, L, A |
| Plugboard | AV BS CG DL FU HZ IN KM OW RX |

```text
Ciphertext: EDPUD NRGYS ZRCXN UYTPO
Plaintext:  AUFKL XABTE ILUNG XVONX
```

## Technologies

Java 25, JavaFX 25, Maven, JUnit 5
