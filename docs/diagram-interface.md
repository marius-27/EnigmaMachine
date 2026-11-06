# Diagram interface specification

Stage 1, commit 50. Branch: `feature/12-diagram-interface`.

This document specifies the second interface; it does not implement it.

## Reference and visual target

Inspiration: Coding Cassowary, [Coding the Enigma machine - Part 1](https://www.youtube.com/watch?v=sbm2dmkmqgQ).
Selected reference: [24:08 — Enciphering a letter](https://www.youtube.com/watch?v=sbm2dmkmqgQ&t=1448s).
The user delegated selection of the reference. This section was selected because
it covers a complete letter traversal rather than an isolated component.
The design target is the full component diagram described below. Video frames
could not be inspected directly; this is an independently specified layout
inspired by that section, not a verified reproduction of its exact appearance.

The target is a component diagram with visible contacts and wires. The
existing machine interface is called **Classic**, and the new view **Diagram**.
See [the selected static layout](diagram-interface-layout.svg).

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

A side panel retains Messages, Settings and Signal access. The keyboard
remains below the diagram. At the existing minimum window size of
860 by 600, the diagram may scroll; contacts remain readable and Mode stays
visible. Do not fit all 26 contacts by shrinking the lettering below 12 px.
A larger window enlarges the diagram to fill its area.

Revised in stage 3: the static draft placed a separate lampboard below the
diagram, which left the diagram about 80 px of height at the minimum window
size. The keys column of the diagram now doubles as the lampboard and lights
the output letter, and the keyboard keys are 36 px in this mode. Classic is
unchanged and keeps its lampboard.

## Colour and motion specification

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
- A trace ends at the letter shown in output and the lit key contact.
- Inspect both views at 1100×760 and 860×600, including focus and scrolling.

## Stage 1 checklist

- [x] Specify layout, component order, colour draft and control placement.
- [x] Produce a static layout draft.
- [x] Specify Mode behaviour and state preservation.
- [x] Select the reference section: 24:08, “Enciphering a letter”, under delegated design choice.
- [x] Define the implementation composition in the static layout and this specification.

Stage 1 is complete as a design specification. Exact visual fidelity to the
video has not been verified and is not claimed by this checklist. Stages 2–6
remain unimplemented.

No application code, dependencies or encryption behaviour change in this stage.
