# Enigma Machine

Simulator desktop pentru mașina Enigma M3, corect din punct de vedere istoric, ușor de testat și de folosit.

> Proiectul este în dezvoltare. Momentan există doar structura aplicației, iar criptarea urmează să fie implementată.

## Cerințe

- JDK 25
- Maven nu trebuie instalat separat, proiectul include Maven Wrapper.

## Comenzi

Pe Windows:

```powershell
.\mvnw.cmd clean javafx:run   # pornește aplicația
.\mvnw.cmd test               # rulează testele
.\mvnw.cmd clean verify       # build complet și teste
```

Pe Linux și macOS, folosește `./mvnw` în loc de `.\mvnw.cmd`.

## Arhitectură

Proiectul separă trei zone de responsabilitate:

| Zonă | Rol | Reguli |
|---|---|---|
| Engine | Rotoare, reflector, plugboard, stepping și traseul semnalului | Nu depinde de JavaFX și poate fi testat independent |
| UI | Ferestre, tastatură, lampboard, setări și animații | Apelează engine-ul, fără să conțină logică de criptare |
| Persistence | Salvarea și încărcarea configurațiilor în JSON | Nu depinde de UI |

## Tehnologii

Java 25, JavaFX 25, Maven, JUnit 5
