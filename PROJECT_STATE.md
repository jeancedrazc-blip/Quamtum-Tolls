# Quantum Tools — Project State

## Canonical baseline

- Current released project version: **3.0.5**
- Minecraft: **26.1.2**
- Loader: **NeoForge**
- Mod id: `rftoolsbuilder`
- Canonical 3.0.5 SHA-256: `1ea89fcadeb43b9c78245a082fb26e92471e93a1c0901abee21b02b9fe52b7e7`
- Internal 3.0.5 JAR metadata version: `26.1.2-7.0.5-port.1`

The user-supplied 3.0.5 JAR identified by the SHA-256 above is the authoritative released baseline. Never infer a different baseline from conversation memory.

## Source-of-truth / development rules

Before changing Quantum Tools:

1. Read this file and `docs/DEVELOPMENT_RULES.md`.
2. Verify the current PR/branch and CI state.
3. Preserve released behavior unless an explicit request replaces it.
4. Never silently simplify an agreed feature.
5. If a reference mod is named, inspect its real implementation for the relevant version.
6. CI success means **compiled/API-valid**, not runtime-proven.
7. Do not call a development build final or fully working until the requested in-game tests pass.
8. Approved visual references are acceptance references, not loose inspiration.

## Constructor — permanent design rules

- Machine name must **not** use `Quantum`.
- Dedicated sci-fi cannon/turret, not a generic cube or robotic arm.
- Long horizontal cannon; white/light metallic armor; gunmetal structure; cyan emissive channel/muzzle.
- Raised central pivot/trunnion with smooth yaw + pitch aiming toward the exact target.
- Compact stabilizer base/feet.
- FE-powered; do not substitute Create gunpowder mechanics.
- Block projectiles render the full target BlockState model.
- Entity-stage projectiles render their actual required item representation.
- Server-authoritative world mutation occurs only at projectile impact.
- Block substitution is permanent functionality and is applied **before** material calculation and placement.
- Neutral `ConstructionPlan` must remain independent of source file format.

Approved CT-01 visual reference is stored under `docs/references/constructor-ct01-approved-reference.webp`.

## Current schematic architecture — dev.8 refactor

Canonical development branch / PR head: `agent/constructor-foundation`, draft PR #1.

The current architecture is one pipeline:

`schematics/ -> Schematic Table -> validated upload -> written Schematic Card -> world preview/deployment -> persistent Printer -> Constructor impact placement`

See `docs/SCHEMATIC_PIPELINE_DEV8.md` for the detailed implementation contract.

### Schematic Table

The table is an import/write station only. It has:

- one blank/rewriteable Schematic Card **input** slot;
- one written Schematic Card **output** slot;
- client-side file browser for user files under `schematics/`;
- refresh/open-folder/write controls;
- real upload progress;
- reserved input while upload is active;
- safe input restoration on cancel/error.

The table does **not** own position, rotation, mirror, Constructor replacement policy or construction offsets.

The physical table must keep the approved workbench/table concept stored in the reference assets. Final UI visual redesign is intentionally deferred until the user provides the new UI references.

### Upload / portability

Client -> server upload is implemented as chunked transfer with:

- path/filename sanitization;
- format validation;
- maximum size enforcement;
- exact received byte count;
- SHA-256 validation;
- parser validation before output-card emission;
- authoritative per-player server storage under `schematics/quantumtools_uploaded/`.

A written card stores:

- display/source name;
- authoritative server file path;
- original client-relative file path;
- format id;
- SHA-256;
- declared X/Y/Z bounds;
- deployed flag;
- anchor;
- rotation;
- mirror;
- substitution rules.

If a client holding a written card no longer has the original local file, preview requests the authoritative server copy. Server -> client preview transfer is rate-limited over server ticks. The client writes a temporary cache file under `schematics/.quantumtools_cache/`, checks exact byte count + SHA-256, and only then commits the cache. If atomic rename is unavailable, it safely falls back to a normal replacement move.

Internal `quantumtools_uploaded/` and `.quantumtools_cache/` files are excluded from the Schematic Table browser.

### Supported source adapters

All readers normalize to one `ConstructionPlan`:

- `.nbt` — Create / vanilla structure NBT;
- `.schem` — WorldEdit / Sponge v1-v3;
- `.litematic` — Litematica;
- `.schematic` — legacy MCEdit / Schematica, with best-effort legacy vanilla numeric mapping.

Unknown/proprietary future formats still require an explicit adapter/specification. Do not claim arbitrary bytes are supported.

The pipeline preserves each source file's **declared cuboid**, including outer air margins, without expanding sparse plans into millions of AIR entries. This prevents rotation/mirror anchor drift.

The normalized plan includes both block entries and entity entries.

### World preview / deployment

Holding a written card loads its verified schematic asynchronously and displays the structure in the world.

Deployment controls currently use functional placeholder key bindings:

- `G` — confirm deployment / enter edit mode;
- `R` — rotate 90 degrees;
- `M` — cycle mirror mode;
- `Shift + mouse wheel` — vertical adjustment.

Preview and Constructor use the exact same `SchematicTransform`; there is no separate transform implementation.

Supported schematic entities are also rendered in preview using the real entity renderer. Unknown/unsupported entity types are not shown as constructible.

A card must be `deployed=true` with a valid anchor before Constructor execution can start.

### Printer stages / persistence

The persistent print cursor is:

`BLOCKS -> DEFERRED_BLOCKS -> ENTITIES -> COMPLETE`

`ConstructionJob` persists:

- primary block queue;
- deferred block queue;
- current stage;
- completed block count;
- entity cursor;
- deferred no-progress/deadlock state.

`ConstructorBlockEntity` additionally persists target/phase/projectile progress/cooldown/resource reservation/pause-after-shot state. Save/reload must reconstruct the plan from the card and resume the cursor instead of resetting the job or charging twice.

### Materials

Materials are resolved through `ConstructorRequirement` / registries, **not** a generic `block.asItem()` shortcut.

Current built-in requirement handling includes multi-count/state-sensitive cases such as double slabs, snow layers, candles, sea pickles and turtle eggs, plus farmland/path -> dirt and paired-block no-double-consumption rules.

Inventory access uses NeoForge item capabilities on all six adjacent sides. Reservation is transactional across the complete requirement: either the whole requirement can be reserved or no partial extraction is committed.

Requirements can preserve ItemStack components when an exact configured/contained item is needed.

### Entity stage

The entity stage is implemented for explicitly supported entity types and has a public compatibility registry.

Built-in Create-style behavior currently includes:

- Item Frame / Glow Item Frame -> frame item + displayed item;
- Armor Stand -> armor stand + equipment in main hand, off hand, feet, legs, chest and head slots.

Unknown entity types are treated as invalid until a requirement provider is registered. OP-only/custom-data-sensitive entity types are not materialized.

Entity NBT identity is sanitized; UUID/dimension/runtime transport state is not replayed. Entity orientation follows vanilla StructureTemplate rotation/mirror semantics, and authoritative entity creation happens at impact.

### Block Entity safety

Raw Block Entity data is never treated as a generic clone source. Safe-data and BlockState filter registries provide explicit compatibility points. Inventory/energy/fluid/runtime contents are not blindly restored because they existed in a schematic.

### Constructor firing / balance

Current test balance:

- energy buffer: 5,000,000 FE;
- max receive: 250,000 FE/t;
- base placement cost: 1,000 FE;
- distance cost: 15 FE per distance unit;
- Block Entity surcharge: 1,500 FE;
- entity surcharge: 750 FE;
- aim: 4 ticks;
- charge: 5 ticks;
- minimum flight: 10 ticks;
- post-shot cooldown: 4 ticks;
- max target distance: 256 blocks.

These are development balance values, not final release balance. The machine intentionally no longer places blocks at an effectively instant cadence; each target goes through aim -> charge -> flight -> impact -> cooldown.

## Miner — approved Concept 1

- Same visual family as Constructor, compact mining silhouette.
- White/light armor over gunmetal chassis, cyan mining/energy core, small orange accents.
- Front remains nearly flush; do not restore the large protruding drill/nose.
- Preserve existing Miner behavior unless explicitly changed.
- Exact pre-redesign 3.0.5 appearance remains the fallback reference.

### Miner balance

- base work interval: **4 ticks**;
- ideal maximum approximately **5 mined blocks/second**;
- throttle mining work, not the whole Block Entity tick.

## Cards

Approved visual family:

- true 32x32 RGBA textures;
- wider/horizontal silhouette;
- white/gunmetal casing;
- cyan central display/emissive language;
- function-specific accent colors/icons.

Final card/UI visual work is not part of the current schematic architecture round unless separately requested.

## Development build status

- **3.0.6-dev.7** is historical/obsolete and must not be described as the current correct implementation.
- Current code is the **dev.8 schematic/Constructor architecture refactor**.
- First full dev.8 compile gate failed only on obsolete `ArmorStand#getAllSlots()` usage.
- That was replaced with the real 26.1 equipment-slot API and the next CI gate passed.
- Additional commits after that pass add atomic-cache fallback and entity hologram rendering; they require their own CI gate before packaging.
- No dev.8 JAR should be described as runtime-validated until in-game tests are completed.

## Runtime acceptance checklist before final 3.0.6

At minimum test:

1. Schematic Table browsing with all supported extensions and no internal cache/storage duplicates.
2. Real upload progress, output card and error recovery.
3. Preview with local source file.
4. Preview after deleting/moving the local source, forcing authoritative server download/cache.
5. Move/Y/rotation/mirror deployment and anchor stability, including even-sized structures and outer air margins.
6. Constructor rejection of an undeployed card.
7. FE and material pause/resume behavior.
8. Adjacent vanilla and modded inventory extraction.
9. Substitution affecting both checklist/material use and final placed state.
10. Deferred support-dependent blocks.
11. Block Entity safety.
12. Save/reload during an active projectile and during a larger job.
13. Item Frame and Armor Stand entity stage.
14. Representative `.nbt`, `.schem`, `.litematic` and `.schematic` files.

## Versioning rule

The next final release increments from verified 3.0.5. Development builds may use `3.0.6-dev.*`; do not call them final 3.0.6 until runtime testing is complete.
