# Schematic / UI reconstruction — Create-parity phase

This feature branch implements the schematic configuration and UI phase on source before any JAR is produced.

## Scope implemented in this branch

- Shared Quantum Tools UI kit (`QuantumUiTheme`, `QuantumButton`).
- Create-inspired schematic tool modes: deploy, horizontal move, vertical move, rotate, mirror and precise transform.
- Live world preview remains visible while editing.
- Exact transform editor with X/Y/Z, 0/90/180/270 rotation and none/X/Z mirror.
- Anchor/rotation/mirror remain local draft state until explicit APPLY; server sync happens only after confirmation.
- Persistent Constructor placement policy: four replace modes, skip-missing and block-entity replacement policy.
- Constructor UI rebuilt as a dark industrial sci-fi terminal using cyan structural accents, amber interaction accents and semantic green/red states.
- Schematic Table UI rebuilt with the same UI system while preserving folder listing, refresh and validated upload flow.
- Client menu data count aligned with the 14-field Constructor server data channel.

## Source policy

All work is performed in `src/main/java` / `src/main/resources`. Compiled JARs are validation outputs only. No binary patching or class injection is permitted.

## Validation gate

The branch must pass `gradle build` before an in-game test JAR is treated as valid. Runtime approval is still required for placement UX, hologram readability, UI spacing and behavior under shaders/resource packs.
