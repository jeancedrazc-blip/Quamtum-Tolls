# Quantum Tools

Source repository for the Minecraft **26.1.2 / NeoForge** mod **Quantum Tools**.

## Current development state

- Source version: **3.0.6-dev.9**
- Minecraft: **26.1.2**
- NeoForge: **26.1.2.95**
- Java toolchain: **25**
- Mod id: `rftoolsbuilder`
- Editable source: `src/main/java`
- Assets/resources: `src/main/resources`

The repository source is the development base. New features, fixes, models, textures, UIs and networking changes must be committed to source; a compiled JAR must not become a separate source of truth.

## Constructor

The approved Constructor V5 visual is stored directly in `src/main/resources` and is frozen unless a new visual change is explicitly requested.

The current schematic placement flow uses a mouse/UI-driven `SchematicPlacementScreen` rather than mandatory keybinds. It supports precise X/Y/Z nudging, rotation, mirror, repositioning, reset, confirm, cancel and clear deployment while keeping the hologram visible in the world.

## Development standard

Read `PADRAO_DE_DESENVOLVIMENTO_QUANTUM_TOOLS.txt` before making changes. It defines the required stage-gate process, modeling quality, schematic workflow, validation rules, definition of done and prohibited shortcuts.

## Build

```bash
gradle build
```

A change is not considered complete until the source compiles successfully and the relevant behavior is validated in Minecraft.
