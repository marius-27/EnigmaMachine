package com.example.enigmamachine.ui;

/**
 * A view that can be shown and hidden in the same window. While inactive it receives no keys, owns no shortcuts
 * and plays no animation, so a hidden view can never react to what is typed in the visible one.
 */
public interface SwitchableView {

    /** Takes the keyboard and the shortcuts of the window. */
    void activate(SceneInput input);

    /** Gives the keyboard and the shortcuts back and stops any animation. Calling it twice does nothing more. */
    void deactivate();
}
