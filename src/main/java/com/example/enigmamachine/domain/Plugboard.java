package com.example.enigmamachine.domain;

import java.util.List;

public final class Plugboard {

    private final int[] mapping = new int[Alphabet.SIZE];

    public Plugboard(List<String> pairs) {
        for (int index = 0; index < Alphabet.SIZE; index++) {
            mapping[index] = index;
        }
        for (String pair : pairs) {
            connect(pair);
        }
    }

    public int swap(int input) {
        return mapping[Alphabet.requireIndex(input)];
    }

    private void connect(String pair) {
        if (pair.length() != 2) {
            throw new IllegalArgumentException("Invalid plugboard pair: '" + pair + "'. Expected two letters.");
        }
        int first = Alphabet.toIndex(pair.charAt(0));
        int second = Alphabet.toIndex(pair.charAt(1));
        if (first == second) {
            throw new IllegalArgumentException("Invalid plugboard pair: '" + pair + "'. A letter cannot connect to itself.");
        }
        if (mapping[first] != first || mapping[second] != second) {
            throw new IllegalArgumentException("Invalid plugboard pair: '" + pair + "'. A letter is already connected.");
        }
        mapping[first] = second;
        mapping[second] = first;
    }
}
