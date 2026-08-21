# Quantum Tools — Project State

## Source of truth

The editable GitHub source is the source of truth for Quantum Tools development.

- Active source version: **3.0.6-dev.10**
- Stable `main`: **3.0.6-dev.9**
- Minecraft: **26.1.2**
- Loader: **NeoForge 26.1.2.95**
- Java toolchain: **25**
- Mod id: `rftoolsbuilder`
- Integration base: `source/3.0.6-dev.10-complete-reconstruction`
- Active feature: `feature/schematic-create-parity-ui` (PR #12)
- Java source: `src/main/java`
- Resources: `src/main/resources`

Compiled JARs are build/test outputs. They must not diverge from or replace the editable source tree.

## Constructor visual

The user-approved Constructor visual is **V5** and remains frozen. Schematic, UI, networking, material, energy and gameplay corrections must not alter that model unless a visual change is explicitly requested.

## Current stage

The active stage is **schematic placement and Quantum UI runtime validation**.

Implemented in the current dev.10 feature branch:

- mouse/UI-first placement with no mandatory conflicting keybinds;
- deploy, horizontal move, vertical move, rotate, mirror and precise modes;
- exact signed X/Y/Z, 0°/90°/180°/270° rotation and mirror editing;
- live hologram while editing;
- draft transform synchronized only after explicit Apply;
- cancel and clear deployment behavior;
- shared Quantum UI components;
- rebuilt Constructor and Schematic Table screens;
- persistent replace mode, skip-missing and Block Entity policy controls;
- corrected 14-field Constructor menu synchronization;
- complete editable Builder/Quarry reconstruction in the dev.10 integration base.

## Verified

- GitHub Actions clean builds for the active PR are successful.
- The feature is source-based; no binary JAR patch is part of the implementation.
- The active PR is mergeable into its dev.10 integration base.

## Still required

Do not promote this stage solely because it compiles. Validate in Minecraft:

1. schematic upload, output card and server/client fallback;
2. placement pivot, bounds, move, rotation, mirror, apply, cancel and clear;
3. hologram readability and UI layout at GUI scales 1–4;
4. Constructor energy, materials, replacement policies and Block Entity handling;
5. persistence across save/reload;
6. Builder/Miner registration and runtime regression;
7. representative NBT, SCHEM, LITEMATIC and SCHEMATIC files.

The detailed checklist is in `docs/SCHEMATIC_UI_RUNTIME_CHECKLIST.md`.

## Mandatory development standard

`PADRAO_DE_DESENVOLVIMENTO_QUANTUM_TOOLS.txt` remains mandatory. Implement, compile, validate, test in Minecraft, fix regressions, and update source/documentation together before moving to the next stage.
