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

## Approved construction-machine direction

These decisions are approved project state and should be preserved unless explicitly changed later:

- The new schematic-driven construction machine must have its **own physical model/form**, not look like a normal cubic block.
- Do **not** use the word `Quantum` in the machine's name.
- The machine should be format-agnostic through adapters/readers instead of being locked to one schematic source.
- Planned compatibility includes Create schematic data and other schematic formats where technically practical.
- The construction projectile must use the **full texture of the block being placed**, not an approximate color.
- The machine needs a **block substitution system**, e.g. schematic cobblestone -> build with smooth stone.
- The placement animation should be a distinctive part of the machine's identity.

### Approved physical model — locked on 2026-08-19

The user approved the sci-fi cannon concept shown in the final concept sheet. Treat this as the canonical visual direction for implementation:

- Overall silhouette is a **dedicated sci-fi cannon/turret**, not a generic industrial machine or robotic arm.
- Long horizontal cannon body with layered futuristic armor panels and an exposed cyan energy channel along the barrel.
- Cannon must be mounted on a real aiming assembly: **horizontal turret rotation plus vertical elevation**, so the barrel visibly points toward the exact block-placement position.
- Central pivot/joint is visually exposed and should read as the mechanical aiming axis.
- Base is **compact, low-profile and turret-like**, with several articulated/splayed stabilizer feet around it; avoid the large bulky circular pedestal from rejected concepts.
- Main materials/colors: light metallic/white armor, dark gunmetal structure and cyan emissive accents. Keep the design clean and engineered rather than overdecorated.
- The muzzle is a dedicated energy emitter, visually distinct from a conventional firearm barrel.
- The approved concept is a visual reference only; incidental text/spec numbers rendered in the concept image (range, reload, energy naming, etc.) are **not gameplay requirements unless explicitly approved later**.

## Miner visual redesign — approved direction 2026-08-19

The existing Miner/Builder block must be visually aligned with the Constructor as part of the same machine family, while keeping a distinct mining silhouette.

- Preserve all existing Miner/Builder behavior, UI, quarry cards, hologram/area renderer and energy logic; this redesign is visual only unless explicitly expanded later.
- Replace the old full cube appearance with a dedicated sci-fi mining unit.
- Reuse the Constructor material language: light metallic/white armor, dark gunmetal chassis and cyan emissive accents.
- Miner silhouette is more vertical/compact than the Constructor, with a heavy central chassis rather than a cannon body.
- Front face has a recessed cyan mining/emitter chamber framed by separate white armor pylons.
- Top section has a dark technical cap with a cyan energy core/strip to visually connect it to the Constructor energy channel.
- Side housings/vents provide industrial mass and make the machine readable from all angles.
- The blockstate facing still defines the front of the machine, so existing placement/orientation behavior remains unchanged.
- The block/item model should share the same visual model so inventory appearance remains consistent.

## Binary baseline storage

The exact 3.0.5 JAR is preserved losslessly in `baseline/parts/` as ordered Base64 chunks. Run `scripts/restore_baseline.py` to rebuild `baseline/QuantumTools-3.0.5.jar`; the script verifies the SHA-256 above.

## Versioning rule

The next release must be created from this verified 3.0.5 baseline and must increment from 3.0.5. Do not reuse 3.0.4 or another earlier build as the implementation base.
