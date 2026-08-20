# Quantum Tools — Project State

## Canonical baseline

- Current project version: **3.0.6-dev.8**
- Minecraft: **26.1.2**
- Loader: **NeoForge**
- Minimum NeoForge declared by the current JAR: **26.1.2.95**
- Mod id preserved by the current build: `rftoolsbuilder`
- Canonical baseline metadata: `baseline/3.0.6-dev.8/BASELINE.json`
- Canonical JAR identity: `QuantumTools-3.0.6-dev.8.jar`
- SHA-256: `ba9a16dd48d8229768d8cb927c0fa6b09a1560cec934ab60386afe3944f3f85b`
- Internal mod metadata version: `3.0.6-dev.8`
- Compiled classes in canonical JAR: **85**
- Non-class resources in canonical JAR: **68**
- Original `.java` files embedded in canonical JAR: **0**

## Source-of-truth rule

The user supplied the 3.0.6-dev.8 JAR on 2026-08-20 and explicitly approved it as the base for all future Quantum Tools work. It supersedes the former 3.0.5 baseline.

Before changing Quantum Tools in any conversation:

1. Read this file and `baseline/3.0.6-dev.8/BASELINE.json`.
2. Treat **3.0.6-dev.8** as the canonical implementation unless the user explicitly approves a newer baseline.
3. Verify the canonical SHA-256 whenever a candidate baseline JAR is available.
4. Never replace this baseline with 3.0.5 or another older remembered version.
5. Never invent missing changelog entries.
6. Preserve existing behavior unless an explicit change request says otherwise.
7. Do **not** rebuild unrelated classes from guessed/decompiled Java. The supplied JAR contains no original Java source text, so the unchanged compiled classes are authoritative.
8. For a future change, reconstruct/edit only the explicitly requested class/resource and preserve every untouched entry from the canonical JAR.
9. Before accepting a new build, compare changed entries against the intended change set and reject unrelated removals or modifications.

## Current implementation visible in the 3.0.6-dev.8 baseline

The supplied JAR contains the Builder/Quarry implementation, Quarry Card classes, filter UI/menu classes, Builder UI/menu classes, renderer classes, translations, recipes, models and textures. It also contains the current Constructor/schematic implementation, including construction plan/job classes, schematic loading/upload handling, substitution rules, safe block-entity handling, Constructor networking/UI/rendering and related client classes.

A source-file inventory recovered from class debug metadata is stored at `baseline/3.0.6-dev.8/source_file_summary.txt`.

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

## Baseline storage and integrity

The GitHub connector used in this session cannot directly upload arbitrary local binary ZIP/JAR files. The repository therefore records the canonical binary identity by exact SHA-256 and stores the recovered engineering/source inventory and preservation rules. Do not claim a repository binary is canonical unless its SHA-256 matches the value above.

The complete locally generated engineering baseline for 3.0.6-dev.8 contains the exact supplied JAR, 85 exact compiled classes, 68 exact resources, per-entry hashes, API signatures and `javap` bytecode references. Those artifacts are the reconstruction reference; guessed Java is never allowed to override an unchanged canonical class.

## Versioning rule

The next release must be created from the verified **3.0.6-dev.8** baseline and must increment from it. Do not reuse 3.0.5 or another earlier build as the implementation base.
