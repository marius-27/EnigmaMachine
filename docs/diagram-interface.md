# Diagram interface specification

Stage 1, commit 50. Branch: `feature/12-diagram-interface`.

This document specifies the second interface; it does not implement it.

## Reference and visual target

Inspiration: Coding Cassowary, [Coding the Enigma machine - Part 1](https://www.youtube.com/watch?v=sbm2dmkmqgQ).
The published chapters cover the keyboard, plugboard, rotors, reflector and
enciphering a letter. The exact reference frame and visual comparison remain
pending the user's timestamp or screenshot. The proposed composition below is
a design draft, not a verified reproduction of a video frame.

The target is a component diagram with visible contacts and wires. The
existing machine interface is called **Classic**, and the new view **Diagram**.
See [the static layout draft](diagram-interface-layout.svg).

## Layout

At the current default window size of 1100 by 760, a common header contains
the application title, active mode and a button labelled **Mode** at the right.
The button remains available in both modes. Its tooltip describes the target:
“Switch to Diagram” or “Switch to Classic”.

The diagram occupies the main area, with components arranged left to right:
reflector, left rotor, middle rotor, right rotor, plugboard and keyboard/lamp
contacts. Each component shows A–Z contacts vertically. Rotor headings show
the selected rotor, current position and ring setting. Wires reflect the actual
configuration and rotor offsets, rather than a decorative fixed pattern.

The forward signal travels right to left; the reflected signal returns left
to right. Arrows and a textual step label accompany the colours. At the bottom
of this area, Play, Pause, Step and speed control the trace replay.

A side panel retains Messages, Settings and Signal access. The keyboard and
lampboard remain below the diagram. At the existing minimum window size of
860 by 600, the diagram may scroll; contacts remain readable and Mode stays
visible. Do not fit all 26 contacts by shrinking the lettering below 12 px.

## Colour and motion draft

| Element | Proposed colour | Additional cue |
|---|---|---|
| Background | `#15191F` | Separate component panels |
| Panels | `#202731` | Labelled borders |
| Text | `#F3F4F6` | Component and contact names |
| Inactive wiring | `#657080` | Thin lines |
| Forward path | `#F4C95D` | Right-to-left arrows, “Forward” label |
| Return path | `#66D9EF` | Left-to-right arrows, “Return” label |

Only the selected path is bright. The active contact and input/output letters
are labelled. Colour choices must be checked for contrast during implementation.
Animation changes presentation only; encryption completes independently.

## Mode behaviour

1. Start in Classic. Mode toggles Classic ↔ Diagram in the same window.
2. Preserve input, output, current rotor positions, applied configuration,
   Undo history and settings being edited, including unapplied edits.
3. Switching never encrypts a letter, steps a rotor or applies settings.
4. Pause the outgoing animation and release held-key presentation state.
   Display the last trace in the incoming view, paused and ready to replay.
5. Use one controller, one service and one shared set of view models.
6. Install physical keyboard handlers and shortcuts once per scene; hidden
   views cannot consume input. A held key must be released before it can
   encrypt again after a switch.
7. Persisting the selected mode between application runs is outside this stage.

Settings, Save/Load, Clear, Reset, Backspace and Decrypt keep their existing
meaning. Signal animation must use `SignalTrace`, including its post-stepping
positions and actual intermediate letters. Full wiring snapshots, if needed,
must use existing domain transforms rather than formulas copied into the UI.

## Acceptance scenarios for later implementation

- Type `AAAAA` with default settings: output is `BDZGO`; switch twice and
  verify both messages and positions remain unchanged.
- Switch 20 times, then press A once: one output letter and one rotor advance.
- Edit settings without Apply, switch twice: edits persist and the running
  configuration is unchanged.
- Switch while animating or holding a key: no extra encryption and no stuck key.
- Backspace, Reset, Clear, Decrypt and JSON Save/Load work in either mode.
- A trace ends at the letter shown in output and lampboard.
- Inspect both views at 1100×760 and 860×600, including focus and scrolling.

## Stage 1 checklist

- [x] Specify layout, component order, colour draft and control placement.
- [x] Produce a static layout draft.
- [x] Specify Mode behaviour and state preservation.
- [ ] Select the exact video frame with the user and compare the composition.
- [ ] Confirm the diagram composition before implementing stage 2.

No application code, dependencies or encryption behaviour change in this stage.
