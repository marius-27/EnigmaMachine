package com.example.enigmamachine.ui;

import com.example.enigmamachine.domain.Alphabet;
import com.example.enigmamachine.domain.SignalTrace;
import com.example.enigmamachine.domain.SignalTrace.Stage;
import com.example.enigmamachine.domain.SignalTrace.Step;
import com.example.enigmamachine.domain.WiringSnapshot;
import com.example.enigmamachine.domain.WiringSnapshot.RotorWiring;
import com.example.enigmamachine.ui.viewmodel.SignalPlayback;
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
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * The components the signal crosses, left to right: reflector, left, middle and right rotor, plugboard and the
 * keys. Each shows its 26 contacts, and the wires between them are drawn from the engine's own wiring snapshot,
 * so they follow the rotor order, the reflector, the positions, the ring settings and the plugboard.
 * The keys column doubles as the lampboard: the contact of the output letter lights up.
 *
 * <p>A signal trace lights the wires its letter crossed, up to the step being played. The way in is drawn solid
 * with arrows to the left, the way back dashed with arrows to the right, so the direction never rests on colour.
 */
public final class WiringDiagramView extends Pane {

    static final double ROW = 16;
    static final double CONTACTS_TOP = 52;
    static final PseudoClass FORWARD = PseudoClass.getPseudoClass("forward");
    static final PseudoClass RETURN = PseudoClass.getPseudoClass("return");
    static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");
    private static final PseudoClass LIT = PseudoClass.getPseudoClass("lit");
    private static final PseudoClass ENTERED = PseudoClass.getPseudoClass("entered");
    private static final double PADDING = 8;
    private static final double GAP = 18;
    private static final double CONTACT_WIDTH = 16;
    private static final double INSET = 4;
    private static final double ARROW = 5;
    private static final double REFLECTOR_WIDTH = 90;
    private static final double ROTOR_WIDTH = 112;
    private static final double KEYS_WIDTH = 56;
    private static final double HEIGHT = CONTACTS_TOP + Alphabet.SIZE * ROW + INSET;
    private static final List<String> SLOT_NAMES = List.of("Left", "Middle", "Right");
    private static final String NO_SIGNAL = "Press a key to follow its signal through the machine.";

    private final Component reflector = new Component("diagram-reflector", REFLECTOR_WIDTH, false, true);
    private final List<Component> rotors = List.of(
            new Component("diagram-rotor", ROTOR_WIDTH, true, true),
            new Component("diagram-rotor", ROTOR_WIDTH, true, true),
            new Component("diagram-rotor", ROTOR_WIDTH, true, true));
    private final Component plugboard = new Component("diagram-plugboard", ROTOR_WIDTH, true, true);
    private final Component keys = new Component("diagram-keys", KEYS_WIDTH, true, false);
    // The fixed connections between neighbouring components, from the reflector side: gap 0 is reflector-left.
    private final List<List<Line>> gaps = new ArrayList<>();
    private final Label stepLabel = new Label(NO_SIGNAL);
    private SignalTrace trace;
    private int activeStep = SignalPlayback.NO_STEP;

    public WiringDiagramView(ObservableValue<WiringSnapshot> wiring, ObservableValue<String> litLamp) {
        getStyleClass().add("wiring-diagram");
        stepLabel.getStyleClass().add("diagram-step");
        stepLabel.setWrapText(true);
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

    /** What the step being shown does, in words; it sits under the diagram and is read by screen readers. */
    Label stepLabel() {
        return stepLabel;
    }

    /**
     * Lights the wires of the given trace up to and including the given step. The trace is a record of a letter
     * that is already encrypted, so this only changes what is drawn.
     */
    void showSignal(SignalTrace newTrace, int step) {
        trace = newTrace;
        activeStep = newTrace == null ? SignalPlayback.NO_STEP : step;
        drawSignal();
    }

    static String describeStep(SignalTrace trace, int step) {
        if (trace == null || step < 0) {
            return NO_SIGNAL;
        }
        Step shown = trace.steps().get(step);
        boolean returning = shown.stage().ordinal() > Stage.REFLECTOR.ordinal();
        String direction = shown.stage() == Stage.REFLECTOR ? "Turning back"
                : returning ? "Return →" : "Forward ←";
        String text = "Step " + (step + 1) + " of " + trace.steps().size() + " · " + direction + " · "
                + SignalPathView.stageName(shown.stage()) + ": " + shown.input() + " → " + shown.output();
        if (step == trace.steps().size() - 1) {
            text += " · lamp " + trace.output() + " lights";
        }
        return text;
    }

    /** The letter whose key contact is lit, or an empty string when none is. */
    String litContact() {
        return keys.contacts.stream()
                .filter(contact -> contact.getPseudoClassStates().contains(LIT))
                .map(Label::getText)
                .findFirst()
                .orElse("");
    }

    /** Every lit wire and link, with how it is lit, for checking what the diagram shows. */
    List<Shape> litPath() {
        List<Shape> lit = new ArrayList<>();
        for (Component component : components()) {
            component.wires.getChildren().stream().map(Shape.class::cast).filter(WiringDiagramView::isLit)
                    .forEach(lit::add);
        }
        gaps.forEach(gap -> gap.stream().filter(WiringDiagramView::isLit).forEach(lit::add));
        return lit;
    }

    /** The direction arrow of the step being shown, or null when nothing is shown. */
    Polygon currentArrow() {
        for (Component component : components()) {
            if (!component.arrow.getPoints().isEmpty() && component.arrow.isVisible()) {
                return component.arrow;
            }
        }
        return null;
    }

    private static boolean isLit(Shape shape) {
        return shape.getPseudoClassStates().contains(FORWARD) || shape.getPseudoClassStates().contains(RETURN);
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
        // New wires start unlit, so the path being played is drawn on them again.
        drawSignal();
    }

    private void drawSignal() {
        for (Component component : components()) {
            component.clearSignal();
        }
        gaps.forEach(gap -> gap.forEach(link -> setLit(link, null, false)));
        stepLabel.setText(describeStep(trace, activeStep));
        stepLabel.setAccessibleText(stepLabel.getText());
        if (trace == null || activeStep < 0) {
            return;
        }
        keys.contacts.get(Alphabet.toIndex(trace.input())).pseudoClassStateChanged(ENTERED, true);
        for (int index = 0; index <= activeStep; index++) {
            Step step = trace.steps().get(index);
            Route route = routeOf(step.stage());
            int input = Alphabet.toIndex(step.input());
            int output = Alphabet.toIndex(step.output());
            PseudoClass direction = route.returning() ? RETURN : FORWARD;
            boolean current = index == activeStep;
            setLit(gaps.get(route.entryGap()).get(input), direction, false);
            route.component().lightWire(input, output, route.returning(), direction, current);
            if (step.stage() == Stage.PLUGBOARD_OUT) {
                // The signal leaves the plugboard for the lamp of the output letter.
                setLit(gaps.get(gaps.size() - 1).get(output), RETURN, false);
            }
        }
    }

    /**
     * Where a stage is drawn. A rotor's wires are listed by their contact on the right, which is where the signal
     * enters on the way in and leaves on the way back; the entry gap is the link the signal crosses to get there.
     */
    private Route routeOf(Stage stage) {
        return switch (stage) {
            case PLUGBOARD_IN -> new Route(plugboard, false, 4);
            case RIGHT_ROTOR_FORWARD -> new Route(rotors.get(2), false, 3);
            case MIDDLE_ROTOR_FORWARD -> new Route(rotors.get(1), false, 2);
            case LEFT_ROTOR_FORWARD -> new Route(rotors.get(0), false, 1);
            case REFLECTOR -> new Route(reflector, false, 0);
            case LEFT_ROTOR_REVERSE -> new Route(rotors.get(0), true, 0);
            case MIDDLE_ROTOR_REVERSE -> new Route(rotors.get(1), true, 1);
            case RIGHT_ROTOR_REVERSE -> new Route(rotors.get(2), true, 2);
            case PLUGBOARD_OUT -> new Route(plugboard, true, 3);
        };
    }

    private record Route(Component component, boolean returning, int entryGap) {
    }

    private static void setLit(Shape shape, PseudoClass direction, boolean current) {
        shape.pseudoClassStateChanged(FORWARD, direction == FORWARD);
        shape.pseudoClassStateChanged(RETURN, direction == RETURN);
        shape.pseudoClassStateChanged(CURRENT, current);
        // Lit wires are drawn above the others without changing the order the wires are listed in.
        shape.setViewOrder(direction == null ? 0 : current ? -2 : -1);
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
    private Group links(double fromX, double toX) {
        Group links = new Group();
        List<Line> gap = new ArrayList<>(Alphabet.SIZE);
        for (int contact = 0; contact < Alphabet.SIZE; contact++) {
            double y = PADDING + rowY(contact);
            Line link = new Line(fromX, y, toX, y);
            link.getStyleClass().add("diagram-link");
            links.getChildren().add(link);
            gap.add(link);
        }
        gaps.add(gap);
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
        // Each contact's wire: for a rotor or the plugboard the wire from that contact on the right, for the
        // reflector the loop that contact belongs to.
        private final List<Shape> wireByContact = new ArrayList<>();
        private final Polygon arrow = new Polygon();

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
            arrow.getStyleClass().add("diagram-arrow");
            arrow.setVisible(false);
            arrow.setMouseTransparent(true);
            getChildren().addAll(panel, heading, wires, arrow);
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
            wireByContact.clear();
            double fromX = rightContactX();
            double toX = leftContactX() + CONTACT_WIDTH;
            for (int contact = 0; contact < mapping.size(); contact++) {
                Line wire = new Line(fromX, rowY(contact), toX, rowY(mapping.get(contact)));
                wire.getStyleClass().add("diagram-wire");
                lines.add(wire);
                wireByContact.add(wire);
            }
            wires.getChildren().setAll(lines);
        }

        /** Loops that bring each contact back to its partner; wider loops join contacts further apart. */
        void drawLoops(List<Integer> mapping) {
            List<Node> loops = new ArrayList<>();
            Shape[] loopOf = new Shape[mapping.size()];
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
                loopOf[contact] = loop;
                loopOf[partner] = loop;
            }
            wireByContact.clear();
            wireByContact.addAll(List.of(loopOf));
            wires.getChildren().setAll(loops);
        }

        void clearSignal() {
            wires.getChildren().forEach(wire -> setLit((Shape) wire, null, false));
            contacts.forEach(contact -> contact.pseudoClassStateChanged(ENTERED, false));
            arrow.setVisible(false);
            arrow.getPoints().clear();
        }

        /**
         * Lights the wire a step crossed. On the way in a wire is found by the contact the signal entered on the
         * right; on the way back by the contact it leaves from on the right, which is the step's output.
         */
        void lightWire(int input, int output, boolean returning, PseudoClass direction, boolean current) {
            if (wireByContact.isEmpty()) {
                return;
            }
            Shape wire = wireByContact.get(returning ? output : input);
            setLit(wire, direction, current);
            if (current) {
                // The arrow sits where the signal leaves: on the left on the way in, on the right on the way back
                // and after the reflector has turned it around.
                boolean pointsRight = returning || wire instanceof CubicCurve;
                double x = pointsRight ? rightContactX() : leftContactX() + CONTACT_WIDTH;
                double y = rowY(output);
                arrow.getPoints().setAll(pointsRight
                        ? new Double[] {x - ARROW, y - ARROW, x, y, x - ARROW, y + ARROW}
                        : new Double[] {x + ARROW, y - ARROW, x, y, x + ARROW, y + ARROW});
                arrow.pseudoClassStateChanged(FORWARD, direction == FORWARD);
                arrow.pseudoClassStateChanged(RETURN, direction == RETURN);
                arrow.setVisible(true);
            }
        }

        List<Integer> drawnWiring() {
            List<Integer> wiring = new ArrayList<>(Alphabet.SIZE);
            for (Shape wire : wireByContact) {
                wiring.add(rowAt(((Line) wire).getEndY()));
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
