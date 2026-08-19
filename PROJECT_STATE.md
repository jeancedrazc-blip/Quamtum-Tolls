# Quantum Tools — Project State

## Canonical baseline

- Current released project version: **3.0.5**
- Minecraft: **26.1.2**
- Loader: **NeoForge**
- Mod id preserved by the current build: `rftoolsbuilder`
- Canonical baseline SHA-256: `1ea89fcadeb43b9c78245a082fb26e92471e93a1c0901abee21b02b9fe52b7e7`
- Internal 3.0.5 JAR metadata version: `26.1.2-7.0.5-port.1`

The user-supplied 3.0.5 JAR identified by the SHA-256 above is the authoritative release baseline. Never infer a newer/older baseline from memory.

## Source-of-truth rule

Before changing Quantum Tools in any conversation:

1. Read this file.
2. Verify the latest repository branch/PR/release state.
3. Verify the canonical baseline hash when the 3.0.5 artifact is involved.
4. Never replace a newer baseline with an older remembered version.
5. Never invent missing changelog entries.
6. Preserve existing behavior unless an explicit change request says otherwise.

## Constructor — approved direction

- Actual machine name must **not** use the word `Quantum`.
- Dedicated sci-fi cannon/turret, not a generic cube or robotic arm.
- Long horizontal cannon with white/light metallic armor, gunmetal structure and cyan emissive channel/muzzle.
- Mechanical center pivot/trunnion above the base; 360-degree horizontal aiming plus vertical elevation toward the exact target block.
- Compact turret base with splayed stabilizer feet.
- FE-powered.
- Projectile renders the full target BlockState model/texture.
- Neutral `ConstructionPlan` execution layer, independent of schematic source.
- Block substitution is required (example cobblestone -> smooth stone).
- Planned real schematic adapters include Create and `.schem`; those readers are not complete in dev.4.

### Constructor visual correction after dev.3 runtime test

The dev.3 runtime model was rejected because the barrel rotated around a pivot only about half a block above the floor. The current branch raises the pivot/trunnion and aligns turret, barrel and cyan energy channel with the approved cannon silhouette. This change must be preserved and visually re-tested in dev.4.

## Schematic Table — approved concept and dev.4 implementation

Approved visual direction:

- It must read clearly as a **real workbench/table**, not a cubic machine.
- Wide white/light metallic tabletop frame, dark structural legs/frame, cyan holographic/touch surface and front control module.
- Small orange technical/warning accents.
- Same product-family language as the Constructor and Miner.

Implemented in **3.0.6-dev.4**:

- registered block + BlockItem + Block Entity + Menu + client Screen;
- dedicated `rftoolsbuilder:schematic_table` item/block;
- dedicated `rftoolsbuilder:schematic_card` item;
- card slot plus FROM/TO block-sample slots for substitution mapping;
- card-persisted rotation (0/90/180/270), mirror (off/X/Z) and X/Y/Z offsets (-64..64);
- up to 8 exact block replacement mappings stored on the Schematic Card;
- UI buttons for rotation, mirror, offset, preview/test, save mapping, clear mappings and send;
- nearest Constructor lookup within 16 horizontal blocks / 5 vertical blocks;
- SEND converts the current test schematic into the existing `ConstructionPlan` engine and applies the card's transformations/replacements before starting the Constructor;
- the current PREVIEW/SEND source is intentionally a deterministic 5x3 test wall (cobblestone border + stone center) so energy, materials, aiming, placement, transformations and substitution can be runtime-tested before real Create/`.schem` readers are attached.

Important: dev.4 does **not** yet claim real Create schematic or `.schem` file loading. The table/card/execution pipeline is now testable; format readers are the next integration layer.

## Miner — approved Concept 1 visual

- Same visual family as Constructor, distinct compact mining silhouette.
- White/light armor over gunmetal chassis, cyan mining/energy core, small orange accents.
- Front must remain nearly flush; do not restore a large protruding drill/nose.
- Existing UI, Quarry Cards, hologram, energy and orientation behavior should remain unless explicitly changed.
- Pre-redesign 3.0.5 appearance remains a fallback reference if requested.

## Miner balance

- base work interval: **4 ticks**;
- ideal maximum approximately **5 mined blocks/second**, reduced from approximately 20/s;
- implemented by throttling mining `work`, not the whole Block Entity tick.

This remains a gameplay-test value.

## Cards — dev.4 visual redesign implemented

All existing card textures plus the Schematic Card use the approved family:

- true **32x32 RGBA** textures;
- visually wider/horizontal card silhouette inside the 32x32 canvas;
- white/gunmetal casing;
- cyan central display/emissive language;
- function-specific accent colors/icons;
- redesigned files: Shape Card, Quarry, Clear Quarry, Fortune, Clear Fortune, Silk, Clear Silk and Schematic Card.

## Current development checkpoint — 3.0.6-dev.4

Development branch: `agent/constructor-foundation`
Draft PR: #1

Code commit validated by GitHub Actions (Java 25 / NeoForge 26.1.2.95):

`35e21a650243e5bc47b9a45adbbcd2a76002f07f`

Validated merged development JAR SHA-256:

`aeba1e53325c5ee94be74b870d82fa2275fbce625b9a807f99b9ec45d5a9c913`

JAR build label: **3.0.6-dev.4**

The JAR was built by overlaying the CI-validated branch patch over the existing dev.3 JAR (which itself preserves the verified 3.0.5 baseline), merging translations by key, retaining `quantumtools.mixins.json`, and replacing the corrected Constructor/card/table resources.

## Binary baseline storage note

The repository previously claimed that the 3.0.5 JAR was fully stored as Base64 parts, but that upload was not completed. Do not rely on `baseline/parts/` unless a future commit explicitly records a verified complete upload. The canonical SHA-256 above remains the verification key.

## Versioning rule

The next final release must increment from verified 3.0.5. Development builds may use `3.0.6-dev.*`; do not call them final 3.0.6 until runtime testing is complete.
