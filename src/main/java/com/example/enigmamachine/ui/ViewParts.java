package com.example.enigmamachine.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;

/** Building blocks that the Classic and the Diagram views share. */
final class ViewParts {

    static final double SPACING = 12;
    private static final double SIDE_PANEL_WIDTH = 320;

    private ViewParts() {
    }

    /**
     * The Messages, Settings and Signal tabs, reachable with Shortcut+1 to Shortcut+3.
     * The settings and the signal path are taller than a small window, so they scroll instead of being cut off.
     */
    static TabPane sidePanel(MainController controller, Shortcuts shortcuts, SignalPathView signalPathView) {
        TabPane sidePanel = new TabPane(
                tab("Messages", new MessagesView(controller, shortcuts)),
                tab("Settings", scrolling(new SettingsView(controller, shortcuts))),
                tab("Signal", scrolling(signalPathView)));
        sidePanel.setPrefWidth(SIDE_PANEL_WIDTH);
        for (int index = 0; index < sidePanel.getTabs().size(); index++) {
            int tab = index;
            shortcuts.add("Shortcut+" + (index + 1), () -> sidePanel.getSelectionModel().select(tab));
        }
        return sidePanel;
    }

    static Insets sidePanelMargin() {
        return new Insets(0, 0, 0, SPACING);
    }

    static ScrollPane scrolling(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // A focused scroll pane would scroll on Space while a message is being typed.
        scroll.setFocusTraversable(false);
        return scroll;
    }

    static TitledPane section(String title, Node content) {
        TitledPane section = new TitledPane(title, content);
        section.setCollapsible(false);
        section.setMaxHeight(Double.MAX_VALUE);
        return section;
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
}
