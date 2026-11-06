package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.WiringSnapshot;
import com.example.enigmamachine.domain.WiringSnapshot.RotorWiring;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * The components the signal crosses, left to right: reflector, left, middle and right rotor, plugboard and the
 * keys. Each shows its 26 contacts, and the wires between them are drawn from the engine's own wiring snapshot,
 * so they follow the rotor order, the reflector, the positions, the ring settings and the plugboard.
 * The keys column doubles as the lampboard: the contact of the output letter lights up.
 */
public final class WiringDiagramView extends Pane {

    private static final PseudoClass LIT = PseudoClass.getPseudoClass("lit");
    static final double ROW = 16;
    static final double CONTACTS_TOP = 52;
    private static final double PADDING = 8;
    private static final double GAP = 18;
    private static final double CONTACT_WIDTH = 16;
    private static final double INSET = 4;
    private static final double REFLECTOR_WIDTH = 90;
    private static final double ROTOR_WIDTH = 112;
    private static final double KEYS_WIDTH = 56;
    private static final double HEIGHT = CONTACTS_TOP + Alphabet.SIZE * ROW + INSET;
    private static final List<String> SLOT_NAMES = List.of("Left", "Middle", "Right");

    private final Component reflector = new Component("diagram-reflector", REFLECTOR_WIDTH, false, true);
    private final List<Component> rotors = List.of(
            new Component("diagram-rotor", ROTOR_WIDTH, true, true),
            new Component("diagram-rotor", ROTOR_WIDTH, true, true),
            new Component("diagram-rotor", ROTOR_WIDTH, true, true));
    private final Component plugboard = new Component("diagram-plugboard", ROTOR_WIDTH, true, true);
    private final Component keys = new Component("diagram-keys", KEYS_WIDTH, true, false);

    public WiringDiagramView(ObservableValue<WiringSnapshot> wiring, ObservableValue<String> litLamp) {
        getStyleClass().add("wiring-diagram");
        List<Component> components = components();
        double x = PADDING;
        for (int index = 0; index < components.size(); index++) {
            Component component = components.get(index);
            component.relocate(x, PADDING);
            getChildren().add(component);
            if (index + 1 < components.size()) {
                getChildren().add(links(x + component.componentWidth, x + component.componentWidth + GAP));
            }
            x += component.componentWidth + GAP;
        }
        double width = x - GAP + PADDING;
        setMinSize(width, HEIGHT + 2 * PADDING);
        setPrefSize(width, HEIGHT + 2 * PADDING);
        setMaxSize(width, HEIGHT + 2 * PADDING);

        keys.setHeading("Keys", "in / out");
        show(wiring.getValue());
        wiring.addListener((observable, previous, current) -> show(current));
        light(litLamp.getValue());
        litLamp.addListener((observable, previous, current) -> light(current));
    }

    /** How far the diagram may grow to fill the space it has; it never shrinks, so its letters stay readable. */
    static double fitScale(double availableWidth, double availableHeight, double width, double height) {
        // Rounded down, so the grown diagram never spills past the space and brings up scroll bars it does not need.
        double fit = Math.floor(Math.min(availableWidth / width, availableHeight / height) * 100) / 100;
        return Math.max(1, fit);
    }

    /** The letter whose key contact is lit, or an empty string when none is. */
    String litContact() {
        return keys.contacts.stream()
                .filter(contact -> contact.getPseudoClassStates().contains(LIT))
                .map(Label::getText)
                .findFirst()
                .orElse("");
    }

    private void light(String letter) {
        for (Label contact : keys.contacts) {
            contact.pseudoClassStateChanged(LIT, contact.getText().equals(letter));
        }
        keys.setAccessibleText(letter == null || letter.isEmpty()
                ? "Keys and lamps, A to Z, all lamps off"
                : "Keys and lamps, A to Z, lamp " + letter + " lit");
    }

    /** The row on screen of a wire end, which is the contact it is drawn to; A is row 0. */
    static int rowAt(double y) {
        return (int) Math.round((y - CONTACTS_TOP - ROW / 2) / ROW);
    }

    /** For each contact on the right of a rotor, the contact on its left that the drawn wire leads to. */
    List<Integer> drawnRotorWiring(int slot) {
        return rotors.get(slot).drawnWiring();
    }

    List<Integer> drawnPlugboardWiring() {
        return plugboard.drawnWiring();
    }

    /** For each contact of the reflector, the contact its drawn loop returns to. */
    List<Integer> drawnReflectorWiring() {
        List<Integer> wiring = new ArrayList<>(Alphabet.SIZE);
        for (int contact = 0; contact < Alphabet.SIZE; contact++) {
            wiring.add(contact);
        }
        for (Node wire : reflector.wires.getChildren()) {
            CubicCurve loop = (CubicCurve) wire;
            int first = rowAt(loop.getStartY());
            int second = rowAt(loop.getEndY());
            wiring.set(first, second);
            wiring.set(second, first);
        }
        return wiring;
    }

    private List<Component> components() {
        List<Component> components = new ArrayList<>();
        components.add(reflector);
        components.addAll(rotors);
        components.add(plugboard);
        components.add(keys);
        return components;
    }

    private void show(WiringSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        reflector.setHeading("Reflector", snapshot.reflector().name());
        reflector.setAccessibleText("Reflector " + snapshot.reflector().name());
        reflector.drawLoops(snapshot.reflectorWiring());
        for (int slot = 0; slot < rotors.size(); slot++) {
            RotorWiring rotor = snapshot.rotors().get(slot);
            String position = String.valueOf(Alphabet.toLetter(rotor.position()));
            String ring = String.format("%02d", rotor.ringSetting() + 1);
            Component component = rotors.get(slot);
            component.setHeading(SLOT_NAMES.get(slot) + " · " + rotor.definition().name(),
                    position + " · ring " + ring);
            component.setAccessibleText(SLOT_NAMES.get(slot) + " rotor " + rotor.definition().name()
                    + " at " + position + ", ring " + ring);
            component.drawWires(rotor.forward());
        }
        long pairs = countPairs(snapshot.plugboardWiring());
        String pairText = pairs == 0 ? "no pairs" : pairs == 1 ? "1 pair" : pairs + " pairs";
        plugboard.setHeading("Plugboard", pairText);
        plugboard.setAccessibleText("Plugboard, " + pairText);
        plugboard.drawWires(snapshot.plugboardWiring());
    }

    private static long countPairs(List<Integer> plugboardWiring) {
        long swapped = 0;
        for (int contact = 0; contact < plugboardWiring.size(); contact++) {
            if (plugboardWiring.get(contact) != contact) {
                swapped++;
            }
        }
        return swapped / 2;
    }

    /** The fixed connections between neighbouring components: contact A always meets contact A. */
    private static Group links(double fromX, double toX) {
        Group links = new Group();
        for (int contact = 0; contact < Alphabet.SIZE; contact++) {
            double y = PADDING + rowY(contact);
            Line link = new Line(fromX, y, toX, y);
            link.getStyleClass().add("diagram-link");
            links.getChildren().add(link);
        }
        links.setMouseTransparent(true);
        return links;
    }

    private static double rowY(int contact) {
        return CONTACTS_TOP + contact * ROW + ROW / 2;
    }

    private static final class Component extends Pane {

        private final double componentWidth;
        private final Label heading = new Label();
        private final Group wires = new Group();
        private final List<Label> contacts = new ArrayList<>();

        Component(String styleClass, double width, boolean leftContacts, boolean rightContacts) {
            this.componentWidth = width;
            getStyleClass().addAll("diagram-component", styleClass);
            setAccessibleRole(AccessibleRole.TEXT);
            setMinSize(width, HEIGHT);
            setPrefSize(width, HEIGHT);
            setMaxSize(width, HEIGHT);

            Rectangle panel = new Rectangle(width, HEIGHT);
            panel.getStyleClass().add("diagram-panel");
            heading.getStyleClass().add("diagram-heading");
            heading.setAlignment(Pos.CENTER);
            heading.setMinSize(width, CONTACTS_TOP - INSET);
            heading.setPrefSize(width, CONTACTS_TOP - INSET);
            heading.setMaxSize(width, CONTACTS_TOP - INSET);
            getChildren().addAll(panel, heading, wires);
            wires.setMouseTransparent(true);
            if (leftContacts) {
                addContacts(leftContactX());
            }
            if (rightContacts) {
                addContacts(rightContactX());
            }
        }

        void setHeading(String name, String detail) {
            heading.setText(name + "\n" + detail);
        }

        /** Straight wires from each contact on the right to the contact on the left it leads to. */
        void drawWires(List<Integer> mapping) {
            List<Node> lines = new ArrayList<>(mapping.size());
            double fromX = rightContactX();
            double toX = leftContactX() + CONTACT_WIDTH;
            for (int contact = 0; contact < mapping.size(); contact++) {
                Line wire = new Line(fromX, rowY(contact), toX, rowY(mapping.get(contact)));
                wire.getStyleClass().add("diagram-wire");
                lines.add(wire);
            }
            wires.getChildren().setAll(lines);
        }

        /** Loops that bring each contact back to its partner; wider loops join contacts further apart. */
        void drawLoops(List<Integer> mapping) {
            List<Node> loops = new ArrayList<>();
            double x = rightContactX();
            double deepest = x - INSET * 2;
            for (int contact = 0; contact < mapping.size(); contact++) {
                int partner = mapping.get(contact);
                if (partner <= contact) {
                    continue;
                }
                double depth = INSET * 2 + (deepest - INSET * 2) * (partner - contact) / (Alphabet.SIZE - 1.0);
                CubicCurve loop = new CubicCurve(
                        x, rowY(contact), x - depth, rowY(contact), x - depth, rowY(partner), x, rowY(partner));
                loop.getStyleClass().add("diagram-wire");
                loops.add(loop);
            }
            wires.getChildren().setAll(loops);
        }

        List<Integer> drawnWiring() {
            List<Integer> wiring = new ArrayList<>(Alphabet.SIZE);
            for (Node wire : wires.getChildren()) {
                Line line = (Line) wire;
                wiring.add(rowAt(line.getEndY()));
            }
            return wiring;
        }

        private double leftContactX() {
            return INSET;
        }

        private double rightContactX() {
            return componentWidth - INSET - CONTACT_WIDTH;
        }

        private void addContacts(double x) {
            for (int contact = 0; contact < Alphabet.SIZE; contact++) {
                Label letter = new Label(String.valueOf(Alphabet.toLetter(contact)));
                letter.getStyleClass().add("diagram-contact");
                letter.setAlignment(Pos.CENTER);
                letter.setMinSize(CONTACT_WIDTH, ROW);
                letter.setPrefSize(CONTACT_WIDTH, ROW);
                letter.setMaxSize(CONTACT_WIDTH, ROW);
                letter.relocate(x, CONTACTS_TOP + contact * ROW);
                getChildren().add(letter);
                contacts.add(letter);
            }
        }
    }
}
