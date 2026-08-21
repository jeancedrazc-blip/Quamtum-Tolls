# Quantum Tools

Source repository for the Minecraft **26.1.2 / NeoForge** mod **Quantum Tools**.

## Current development state

- Source version: **3.0.6-dev.10**
- Minecraft: **26.1.2**
- NeoForge: **26.1.2.95**
- Java toolchain: **25**
- Mod id: `rftoolsbuilder`
- Canonical stable branch: `main` (**3.0.6-dev.9**)
- Active integration base: `source/3.0.6-dev.10-complete-reconstruction`
- Active UI/placement work: `feature/schematic-create-parity-ui` / PR #12

The editable source in `src/main/java` and `src/main/resources` is the only development source of truth. Compiled JARs are validation outputs and must never be patched as a parallel implementation.

## Active validation gate

The dev.10 source restores the complete Builder/Quarry source and adds the current schematic placement and Quantum UI phase. The branch compiles successfully in GitHub Actions, but remains a development build until the runtime checklist is completed in Minecraft.

Required before promotion:

1. test schematic import and written-card deployment;
2. validate move, rotate, mirror, precise coordinates, apply, cancel and clear;
3. validate Constructor material, energy, replacement and Block Entity policies;
4. test save/reload and Builder/Miner regressions;
5. validate UI at GUI scales 1–4 and with shaders/resource packs.

The approved Constructor V5 assets remain frozen unless a visual change is explicitly requested.

## Development standard

Read `PADRAO_DE_DESENVOLVIMENTO_QUANTUM_TOOLS.txt` before making changes. It defines the required stage-gate process, modeling quality, schematic workflow, validation rules, definition of done and prohibited shortcuts.

## Build

```bash
gradle build
```

A change is complete only after clean compilation and validation of the affected behavior in Minecraft.
