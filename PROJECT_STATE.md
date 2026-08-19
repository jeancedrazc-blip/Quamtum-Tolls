# Quantum Tools — Project State

## Canonical baseline

- Current project version: **3.0.5**
- Minecraft: **26.1.2**
- Loader: **NeoForge**
- Mod id preserved by the current build: `rftoolsbuilder`
- Canonical JAR: `baseline/QuantumTools-3.0.5.jar`
- SHA-256: `1ea89fcadeb43b9c78245a082fb26e92471e93a1c0901abee21b02b9fe52b7e7`
- Internal JAR metadata version: `26.1.2-7.0.5-port.1`

## Source-of-truth rule

Before changing Quantum Tools in any conversation:

1. Read this file.
2. Verify the latest repository tag/release/commit.
3. Verify the canonical baseline hash when the baseline JAR is involved.
4. Never replace a newer baseline with an older remembered version.
5. Never invent missing changelog entries.
6. Preserve existing behavior unless an explicit change request says otherwise.

## Current implementation visible in the 3.0.5 baseline

The supplied JAR contains the Builder/Quarry implementation, Quarry Card classes, filter UI/menu classes, Builder UI/menu classes, renderer classes, translations, recipes, models and textures.

## Current design direction for the next construction machine

These decisions are approved project state and should be preserved unless explicitly changed later:

- The new schematic-driven construction machine must have its **own physical model/form**, not look like a normal cubic block.
- Do **not** use the word `Quantum` in the machine's name.
- The machine should be format-agnostic through adapters/readers instead of being locked to one schematic source.
- Planned compatibility includes Create schematic data and other schematic formats where technically practical.
- The construction projectile must use the **full texture of the block being placed**, not an approximate color.
- The machine needs a **block substitution system**, e.g. schematic cobblestone -> build with smooth stone.
- The placement animation should be a distinctive part of the machine's identity.
- The physical layout/model is the next design task before implementation.

## Binary baseline storage

The exact 3.0.5 JAR is preserved losslessly in `baseline/parts/` as ordered Base64 chunks. Run `scripts/restore_baseline.py` to rebuild `baseline/QuantumTools-3.0.5.jar`; the script verifies the SHA-256 above.

## Versioning rule

The next release must be created from this verified 3.0.5 baseline and must increment from 3.0.5. Do not reuse 3.0.4 or another earlier build as the implementation base.
