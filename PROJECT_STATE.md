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

## Constructor — permanent design rules

- Actual machine name must **not** use the word `Quantum`.
- Dedicated sci-fi cannon/turret, not a generic cube or robotic arm.
- Long horizontal cannon with white/light metallic armor, gunmetal structure and cyan emissive channel/muzzle.
- Mechanical center pivot/trunnion above the base; 360-degree horizontal aiming plus vertical elevation toward the exact target block.
- Compact turret base with splayed stabilizer feet.
- FE-powered; do not replace FE with the Create Schematicannon gunpowder system.
- Projectile renders the full target BlockState model/texture.
- Block placement is authoritative server-side and occurs on projectile impact.
- Neutral `ConstructionPlan` execution layer must remain independent of schematic source.
- The Constructor must **not be locked to one schematic format**. File readers are adapters that normalize external formats into `ConstructionPlan`.
- Block substitution remains a required system (example cobblestone -> smooth stone).

### Approved visual correction

The dev.3 runtime model was rejected because the barrel pivot was too low. Preserve the raised trunnion/pivot and approved cannon silhouette from dev.4+.

### Constructor UI + animation

- right-click opens the dedicated Constructor control UI;
- UI shows FE, status, job progress, current target block, target XYZ, FE cost and firing progress;
- controls include `START`, `PAUSE / RESUME`, safe `CLEAR`, replacement mode, skip-missing toggle and Block Entity protection/replacement toggle;
- turret yaw and barrel pitch interpolate smoothly toward the exact target;
- idle movement is subtle;
- cyan energy channel pulses during charge/firing/low-FE state;
- barrel has recoil;
- projectile materializes, rotates and follows an arc;
- projectile flight duration scales with target distance instead of using one constant duration.

## Schematic Table — permanent workflow

Approved visual direction:

- real workbench/table silhouette, not a cubic machine;
- white/light metallic tabletop frame, dark structural legs/frame, cyan holographic/touch surface and small orange accents;
- same product-family language as Constructor and Miner.

Interaction rule:

- the table has **one Schematic Card slot only** plus the normal player inventory;
- the main panel is a browser for files inside the Minecraft instance `schematics/` directory;
- it supports selection, scrolling, refresh and `WRITE TO CARD`;
- it must not restore the rejected dev.4 FROM/TO slots, manual rotation/mirror/offset panel, synthetic preview wall or SEND-to-nearest-Constructor flow;
- rewriting a card resets stale hidden placement transforms from dev.4.

## Universal schematic pipeline — dev.7

The table recursively scans `schematics/` and currently recognizes these adapters:

- `.nbt` — Create / vanilla structure NBT;
- `.schem` — WorldEdit / Sponge schematic v1-v3;
- `.litematic` — Litematica;
- `.schematic` — legacy MCEdit/Schematica, with best-effort vanilla numeric-ID conversion.

The Schematic Card stores the selected relative filename plus detected format. The Constructor reads the card and dispatches to the corresponding adapter. All adapters normalize to `ConstructionPlan`; adding another format must not require rewriting the Constructor engine.

Current readers include BlockState properties. Create/vanilla, Sponge and Litematica readers also retain available Block Entity payloads for best-effort restoration after placement through the Minecraft 26.1 `ValueInput` compatibility bridge. A malformed/incompatible Block Entity payload must not abort the remaining schematic.

Legacy `.schematic` modded numeric IDs are inherently ambiguous without the original old registry mapping; unknown legacy IDs are skipped rather than guessed as another block.

## Create Schematicannon behavior adopted in dev.7

Use the Create Schematicannon as the behavioral reference, while retaining Quantum Tools visuals and FE power:

- materials are sourced through NeoForge item capabilities from inventories on all six adjacent sides, allowing normal and modded inventories to feed the Constructor;
- missing material pauses the machine by default;
- `SKIP MISSING` can skip unavailable materials instead;
- replacement modes mirror the Schematicannon concept: air-only/no replacement, solid-aware replacement, replace-any, and include-air/clear mode;
- existing Block Entities are protected by default and can be explicitly allowed for replacement;
- already-correct blocks are skipped;
- unbreakable targets and unsupported virtual/non-item states are skipped safely;
- blocks whose support is not ready are deferred to the back of the construction queue and retried after other blocks, instead of failing immediately;
- duplicate item consumption is avoided for paired/multi-block parts such as upper double-block halves, bed heads and piston heads;
- FE/material are reserved before firing and the world mutation happens at projectile impact;
- flight duration uses a distance-based curve similar to the Create Schematicannon projectile rather than a fixed 8-tick shot.

## Current limitations after dev.7

- Runtime validation is still required for representative files of every format; CI validates compilation/API compatibility, not every third-party file variant.
- Entity spawning from schematic files is not implemented yet; dev.7 focuses on blocks and Block Entity payloads.
- Create-style client-to-dedicated-server schematic upload/synchronization is not implemented yet. The current file browser targets the local/server `schematics/` directory.
- Unknown/proprietary future schematic formats require a new adapter; do not claim arbitrary bytes can be understood without a format specification.

## Miner — approved Concept 1

- Same visual family as Constructor, compact mining silhouette.
- White/light armor over gunmetal chassis, cyan mining/energy core, small orange accents.
- Front remains nearly flush; do not restore the large protruding drill/nose.
- Preserve existing Miner UI, Quarry Cards, hologram, energy and orientation behavior unless explicitly changed.
- Pre-redesign 3.0.5 appearance remains a fallback reference if requested.

## Miner balance

- base work interval: **4 ticks**;
- ideal maximum approximately **5 mined blocks/second**, reduced from approximately 20/s;
- throttle mining `work`, not the whole Block Entity tick.

## Cards

Approved/implemented visual family:

- true 32x32 RGBA textures;
- visually wider/horizontal silhouette;
- white/gunmetal casing;
- cyan central display/emissive language;
- function-specific accent colors/icons;
- Shape Card, Quarry, Clear Quarry, Fortune, Clear Fortune, Silk, Clear Silk and Schematic Card follow this family.

## Current development checkpoint — 3.0.6-dev.7

Development branch: `agent/constructor-foundation`
Draft PR: #1

Functional dev.7 code commit validated by GitHub Actions (Java 25 / NeoForge 26.1.2.95):

`211a9e48b79f87b7ec4073ebf85ca1f8fa5da1fc`

The following branch commit only clarifies comments and does not change dev.7 behavior:

`0517f899aee32111403389c4b989ddafd691d70d`

Validated merged development JAR SHA-256:

`95502d17101abb0b537bd5ac219be309b20059bc8bfd4d5aa7dceacc05235d66`

JAR build label: **3.0.6-dev.7**

The dev.7 JAR is built by overlaying the CI-validated universal-format/Schematicannon patch over dev.6, preserving the verified 3.0.5 lineage, Constructor approved model/animation, Miner, cards, table resources, translations and `quantumtools.mixins.json`.

## Binary baseline storage note

The repository previously claimed that the 3.0.5 JAR was fully stored as Base64 parts, but that upload was not completed. Do not rely on `baseline/parts/` unless a future commit explicitly records a verified complete upload. The canonical SHA-256 above remains the verification key.

## Versioning rule

The next final release must increment from verified 3.0.5. Development builds may use `3.0.6-dev.*`; do not call them final 3.0.6 until runtime testing is complete.
