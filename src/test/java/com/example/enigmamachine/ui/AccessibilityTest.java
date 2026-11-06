package com.example.enigmamachine.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.enigmamachine.App;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AccessibilityTest {

    /** WCAG AA asks for 4.5:1 for normal text. */
    private static final double READABLE_TEXT = 4.5;
    private static final Pattern COLOR_TOKEN = Pattern.compile("(-enigma-[a-z-]+):\\s*(#[0-9a-fA-F]{6});");

    @Test
    void textColorsAreReadableOnTheirBackgrounds() throws IOException {
        Map<String, String> colors = themeColors();
        List<List<String>> pairs = List.of(
                List.of("-enigma-text", "-enigma-background"),
                List.of("-enigma-text", "-enigma-panel"),
                List.of("-enigma-gold-bright", "-enigma-panel"),
                List.of("-enigma-gold-bright", "-enigma-window"),
                List.of("-enigma-lamp-off", "-enigma-window"),
                List.of("-enigma-background", "-enigma-gold"),
                List.of("-enigma-background", "-enigma-gold-bright"),
                List.of("-enigma-text", "-enigma-window"),
                // Contact letters on the signal's path.
                List.of("-enigma-forward", "-enigma-window"),
                List.of("-enigma-return", "-enigma-window"));

        for (List<String> pair : pairs) {
            double ratio = contrast(colors.get(pair.get(0)), colors.get(pair.get(1)));
            assertTrue(ratio >= READABLE_TEXT, pair + " has a contrast of only " + ratio);
        }
    }

    /** WCAG AA asks for 3:1 for lines and other graphics that carry meaning. */
    @Test
    void diagramWiresStandOutFromTheirPanels() throws IOException {
        Map<String, String> colors = themeColors();

        for (String wire : List.of("-enigma-wire", "-enigma-forward", "-enigma-return")) {
            double ratio = contrast(colors.get(wire), colors.get("-enigma-window"));
            assertTrue(ratio >= 3.0, wire + " has a contrast of only " + ratio);
        }
    }

    @Test
    void contrastFollowsTheWcagFormula() {
        assertEquals(21.0, contrast("#000000", "#ffffff"), 0.01);
        assertEquals(1.0, contrast("#d4af37", "#d4af37"), 0.01);
    }

    @Test
    void lampIsDescribedWithItsState() {
        assertEquals("Lamp Q", LampboardView.lampDescription("Q", false));
        assertEquals("Lamp Q, lit", LampboardView.lampDescription("Q", true));
    }

    @Test
    void socketIsDescribedWithItsPartner() {
        List<String> pairs = List.of("AM", "QZ");

        assertEquals("Plugboard A, connected to M", PlugboardView.socketDescription('A', pairs, ""));
        assertEquals("Plugboard Z, connected to Q", PlugboardView.socketDescription('Z', pairs, ""));
        assertEquals("Plugboard B, waiting for a partner", PlugboardView.socketDescription('B', pairs, "B"));
        assertEquals("Plugboard C", PlugboardView.socketDescription('C', pairs, "B"));
    }

    @Test
    void aShortcutCannotBeUsedTwice() {
        Shortcuts shortcuts = new Shortcuts();
        shortcuts.add("Shortcut+D", () -> { });

        assertThrows(IllegalArgumentException.class, () -> shortcuts.add("Shortcut+D", () -> { }));
        assertEquals(1, shortcuts.actions().size());
    }

    private static Map<String, String> themeColors() throws IOException {
        try (InputStream css = App.class.getResourceAsStream("/styles/app.css")) {
            Matcher tokens = COLOR_TOKEN.matcher(new String(css.readAllBytes(), StandardCharsets.UTF_8));
            Map<String, String> colors = new HashMap<>();
            while (tokens.find()) {
                colors.put(tokens.group(1), tokens.group(2));
            }
            return colors;
        }
    }

    private static double contrast(String first, String second) {
        double lighter = Math.max(luminance(first), luminance(second));
        double darker = Math.min(luminance(first), luminance(second));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return 0.2126 * channel(rgb >> 16) + 0.7152 * channel(rgb >> 8) + 0.0722 * channel(rgb);
    }

    private static double channel(int value) {
        double c = (value & 0xFF) / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
