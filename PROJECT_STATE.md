# Quantum Tools — Project State

## Source of truth

The editable GitHub project is the source of truth for all future Quantum Tools development.

- Current source version: **3.0.6-dev.9**
- Minecraft: **26.1.2**
- Loader: **NeoForge**
- NeoForge dependency: **26.1.2.95**
- Java toolchain: **25**
- Mod id: `rftoolsbuilder`
- Java source: `src/main/java`
- Resources/assets: `src/main/resources`

Compiled JARs are outputs used for testing and release validation. They must not diverge from the source tree or be used as the primary editing base.

## Constructor visual

The user-approved Constructor visual is **V5**. Its models and textures are stored directly in `src/main/resources/assets/rftoolsbuilder`.

V5 is frozen. Schematic, UI, networking, material, energy or gameplay corrections must not alter this model unless a visual change is explicitly requested.

The approved visual includes the low/wide stabilized base, multipart turret/barrel layout, cleaned white/gunmetal/cyan textures, emissive energy elements and the shortened rear profile approved in-game.

## Schematic work — current stage

Work is stage-gated. The current schematic stage is **Placement Editor / deployment usability**.

Implemented in 3.0.6-dev.9:

- mandatory G/R/M schematic keybinds removed;
- mandatory Shift+scroll placement control removed;
- right-clicking a written Schematic Card opens a placement UI;
- crosshair is sampled only for initial/explicit targeting instead of continuously dragging the schematic;
- anchor remains fixed until changed deliberately;
- X/Y/Z movement via -10, -1, +1 and +10 controls;
- rotation 0°, 90°, 180°, 270°;
- mirror None / Left-Right / Front-Back;
- explicit target, place-in-front and center controls;
- reset, confirm, cancel and clear deployment;
- hologram remains visible while the non-pausing placement screen is open;
- deployment confirmation synchronizes anchor/rotation/mirror/deployed state to the server.

No later schematic problem should be treated as the active stage until this editor is validated in-game.

## Build validation

The source with the Placement Editor and approved V5 Constructor assets has compiled successfully with Gradle/NeoForge before promotion.

For every future change:

1. implement in source;
2. compile;
3. validate the changed system;
4. test in Minecraft when behavior or visuals depend on the game runtime;
5. fix regressions before moving to the next stage;
6. update source and documentation together.

## Mandatory development standard

`PADRAO_DE_DESENVOLVIMENTO_QUANTUM_TOOLS.txt` is the project process specification. It defines:

- one-stage-at-a-time development;
- required modeling fidelity and in-game validation;
- prohibition on unauthorized simplification;
- no placeholder or half-implemented features being called complete;
- full schematic pipeline expectations;
- UI-first placement controls without mandatory conflicting keybinds;
- client/server authority and networking validation;
- regression testing;
- source synchronization and definition of done.

This standard must be followed for subsequent Quantum Tools work.
