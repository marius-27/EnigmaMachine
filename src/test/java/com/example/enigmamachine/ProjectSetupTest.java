package com.example.enigmamachine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectSetupTest {

    @Test
    void runsOnJava25OrNewer() {
        assertTrue(Runtime.version().feature() >= 25);
    }
}
