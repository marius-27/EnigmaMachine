package com.example.enigmamachine.domain;

public final class Alphabet {

    public static final int SIZE = 26;

    private Alphabet() {
    }

    public static boolean isValid(char letter) {
        return isUpperCase(letter) || isLowerCase(letter);
    }

    public static char normalize(char letter) {
        if (isUpperCase(letter)) {
            return letter;
        }
        if (isLowerCase(letter)) {
            return (char) (letter - 'a' + 'A');
        }
        throw new IllegalArgumentException("Invalid letter: '" + letter + "'. Only A-Z is accepted.");
    }

    public static int toIndex(char letter) {
        return normalize(letter) - 'A';
    }

    public static char toLetter(int index) {
        return (char) ('A' + requireIndex(index));
    }

    public static int requireIndex(int index) {
        if (index < 0 || index >= SIZE) {
            throw new IllegalArgumentException("Invalid index: " + index + ". Expected 0-25.");
        }
        return index;
    }

    private static boolean isUpperCase(char letter) {
        return letter >= 'A' && letter <= 'Z';
    }

    private static boolean isLowerCase(char letter) {
        return letter >= 'a' && letter <= 'z';
    }
}
