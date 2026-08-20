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
- Planned adapters include Create `.nbt` and other schematic formats such as `.schem` where technically practical.

### Constructor visual correction after dev.3 runtime test

The dev.3 runtime model was rejected because the barrel rotated around a pivot only about half a block above the floor. The current branch raises the pivot/trunnion and aligns turret, barrel and cyan energy channel with the approved cannon silhouette. Preserve this raised configuration.

### Constructor UI + animation — dev.5+

- right-click opens a dedicated Constructor control UI;
- UI shows FE stored/capacity, machine status, schematic/job progress, current target block, target XYZ, current FE cost per shot and firing progress;
- UI exposes `START`, `PAUSE / RESUME` and safe `CLEAR` controls;
- CLEAR refuses while a shot already has FE + material reserved;
- turret yaw and barrel pitch interpolate smoothly toward the exact target;
- subtle standby motion only when idle/complete/paused;
- cyan energy channel remains visible and pulses during charging, firing and low-energy wait states;
- barrel has physical recoil on firing;
- projectile remains the full target BlockState model/texture, materializes from smaller scale, follows an arc, and rotates in flight;
- authoritative block placement remains server-side and happens only when the projectile reaches the target.

## Schematic Table — approved workflow, corrected in dev.6

Approved visual direction remains:

- real workbench/table silhouette, not a cubic machine;
- white/light metallic tabletop frame, dark structural legs/frame, cyan holographic/touch surface and small orange accents;
- same product-family language as Constructor and Miner.

The dev.4 interaction design is **rejected/superseded**. Do not restore the old FROM/TO slots, rotation/mirror/offset controls, PREVIEW/SEND buttons or fixed 5x3 test wall to this table.

Current dev.6 workflow intentionally follows the Create-style schematic-writing concept:

- the table has **one Schematic Card slot only** (plus the standard player inventory area);
- it scans the Minecraft instance `schematics/` directory and lists available `.nbt` schematic files;
- the list has selection, scrolling, refresh and `WRITE TO CARD`;
- writing stores the selected schematic filename and source type on the Schematic Card;
- rewriting a card resets stale dev.4 rotation/mirror/offset values to zero;
- block substitution data remains supported by the card/plan layer but is no longer edited on the Schematic Table;
- there is no synthetic test-wall SEND path in the normal workflow anymore.

## Schematic Card + Constructor — dev.6 real file flow

- `rftoolsbuilder:schematic_card` can now represent an actual schematic file reference instead of only test/config data;
- tooltip distinguishes an empty card from a card written with a schematic;
- Constructor now has its own Schematic Card slot;
- `START` reads the inserted card and loads the referenced schematic;
- the Create/vanilla structure-NBT adapter reads the `.nbt` palette and block list, reconstructs BlockStates/properties and normalizes them into `ConstructionPlan` entries;
- air entries are skipped;
- the existing Constructor engine then handles materials, FE, aiming, projectile animation and authoritative placement;
- the inserted card is persisted with the Constructor and is dropped if the machine is broken in survival.

### Current schematic-format limitations

- dev.6 currently implements the Create/vanilla **compressed `.nbt` structure format** from the local `schematics/` folder;
- `.schem`/Sponge-style files are not implemented yet;
- Block Entity NBT payload restoration is not implemented yet; dev.6 reconstructs BlockStates/properties;
- local/singleplayer/integrated-server flow is the current runtime test target;
- Create-style client-to-dedicated-server schematic upload/synchronization is **not implemented yet**. Do not claim dedicated-server parity until a payload/upload layer is added and tested.

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

## Current development checkpoint — 3.0.6-dev.6

Development branch: `agent/constructor-foundation`
Draft PR: #1

Create-style schematic workflow code commit validated by GitHub Actions (Java 25 / NeoForge 26.1.2.95):

`478cac278de41377327b25aa78417dd16fd5f9e2`

Validated merged development JAR SHA-256:

`17f4157172fbd17082e145d0a671efd8a861bd04dbd648b3a380a1584b16176f`

JAR build label: **3.0.6-dev.6**

The dev.6 JAR is built by overlaying the CI-validated patch over dev.5, preserving the verified 3.0.5 lineage, Constructor animation/UI, Miner, card redesign, translations and `quantumtools.mixins.json`.

## Binary baseline storage note

The repository previously claimed that the 3.0.5 JAR was fully stored as Base64 parts, but that upload was not completed. Do not rely on `baseline/parts/` unless a future commit explicitly records a verified complete upload. The canonical SHA-256 above remains the verification key.

## Versioning rule

The next final release must increment from verified 3.0.5. Development builds may use `3.0.6-dev.*`; do not call them final 3.0.6 until runtime testing is complete.
