# Changelog

This changelog records verified repository state. Runtime-dependent behavior remains marked as pending until tested in Minecraft.

## 3.0.6-dev.10 — active development

### Verified in source/build

- Complete editable Builder and Quarry source reconstructed on the dev.10 integration base.
- Quantum Tools UI kit added with shared themed components.
- Schematic placement tools added for deploy, X/Z move, Y move, rotate, mirror and precise transform.
- Exact X/Y/Z, rotation and mirror editor added.
- Live hologram remains visible during placement editing.
- Draft placement state is synchronized only after explicit Apply.
- Constructor and Schematic Table screens rebuilt on the shared UI system.
- Constructor menu synchronization corrected to 14 fields.
- Replace modes, skip-missing and Block Entity policy exposed as persistent controls.
- Clean GitHub Actions builds passed for the active PR.

### Pending runtime approval

- Full schematic import/deployment workflow.
- Pivot, bounds, rotation and mirror parity.
- Constructor energy/material/impact behavior and persistence.
- UI layout at GUI scales 1–4 and under shaders/resource packs.
- Builder/Miner regression pass.

## 3.0.6-dev.9 — stable main

- Approved Constructor V5 assets promoted to editable source.
- Mouse-driven placement editor replaced mandatory schematic keybinds.
- Precise position, rotation, mirror, targeting, apply/cancel/clear controls added.
- Source project promoted as canonical development base.

## 3.0.5 — preserved baseline

- Baseline supplied and verified by SHA-256.
- Minecraft 26.1.2 / NeoForge.
- Existing Builder/Quarry, card, filter, UI and renderer implementation preserved from the supplied baseline artifact.
