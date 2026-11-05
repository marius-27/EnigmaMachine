package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnexpectedErrorHandlerTest {

    @Test
    void describesAnErrorByItsMessage() {
        String text = UnexpectedErrorHandler.describe(new IllegalStateException("The rotor jammed."));

        assertTrue(text.startsWith("The rotor jammed."), text);
        assertTrue(text.contains("The last action did not finish."), text);
    }

    @Test
    void describesAnErrorWithoutAMessageByItsKind() {
        assertTrue(UnexpectedErrorHandler.describe(new NullPointerException()).startsWith("NullPointerException"));
        assertTrue(UnexpectedErrorHandler.describe(new IllegalStateException(" ")).startsWith("IllegalStateException"));
    }

    @Test
    void requiresAnOwnerWindow() {
        assertThrows(NullPointerException.class, () -> new UnexpectedErrorHandler(null));
    }
}
