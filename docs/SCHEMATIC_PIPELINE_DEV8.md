# Quantum Tools — Schematic pipeline dev.8

Status: **architecture refactor in progress; CI/runtime validation required before packaging.**

## Non-negotiable pipeline

The schematic system follows the real Create 6.0.9 workflow as behavioral reference while preserving Quantum Tools FE power, block substitutions, multi-format adapters and approved sci-fi machine direction.

`schematics folder -> Schematic Table -> validated upload -> written Schematic Card -> world preview/deployment -> persistent Printer -> Constructor impact placement`

No stage may be silently collapsed into another.

## Schematic Table

The table is an import/write station only:

- one blank/rewriteable card input;
- one written-card output;
- client schematic file browser;
- refresh/open-folder/write actions;
- real progress;
- input reservation while upload is active;
- safe input recovery on error/cancel.

Position, rotation, mirror, material policy and Constructor settings do not belong in the table.

## Upload and card portability

Upload is chunked client -> server and validates path, format, size, byte count, SHA-256 and parser compatibility before a written card is emitted.

Written cards store:

- source/display name;
- authoritative server path;
- original client-relative path;
- format id;
- SHA-256;
- declared X/Y/Z bounds;
- deployed flag;
- anchor;
- rotation;
- mirror;
- substitution rules.

Preview is portable. The client first tries the original local schematic and verifies SHA-256. If the file is missing or does not match, it requests the authoritative server copy. The server streams it at a bounded per-tick rate. The client writes a temporary cache file and commits it only after exact byte-count and SHA-256 validation.

Internal paths `quantumtools_uploaded/` and `.quantumtools_cache/` are excluded from the table file browser.

## Source adapters

All formats normalize to one neutral `ConstructionPlan`:

- Create / vanilla structure `.nbt`;
- Sponge / WorldEdit `.schem`;
- Litematica `.litematic`;
- legacy MCEdit / Schematica `.schematic`.

Declared cuboids are preserved even when outer layers are air. Sparse plans must not be expanded into millions of AIR entries just to preserve bounds.

The normalized plan contains both block targets and schematic entity targets.

## Deployment preview

Holding a written card activates preview/editing. Deployment stores the world anchor and transform on the card and synchronizes it to the server. Rotation/mirror uses the same `SchematicTransform` that the Constructor uses; preview and printing must not have separate transform math.

A card must be deployed before the Constructor starts.

## Printer stages and persistence

`ConstructionJob` stages are:

`BLOCKS -> DEFERRED_BLOCKS -> ENTITIES -> COMPLETE`

The job persists:

- primary queue;
- deferred queue;
- stage;
- completed block count;
- entity cursor;
- deferred no-progress state.

The Constructor additionally persists active target, phase, projectile progress, cooldown, resource reservation and pause-after-shot state so save/reload does not silently restart the plan or double-charge FE/materials.

## Materials

Requirements are not `block.asItem()` shortcuts.

The requirement layer supports multi-count states and component-sensitive stacks. Adjacent inventory extraction is transactional across all six sides: a requirement is fully reservable or nothing is committed.

Substitution is applied before material resolution and placement.

Entity requirements follow Create-style defaults: supported entity types have explicit requirements; unsupported/unknown types are invalid until a compatibility provider is registered. Item frames include the frame and displayed item; armor stands include the stand and equipment.

## Block Entity safety

Raw Block Entity data is never trusted as a generic clone source. Safe-data and state-filter registries provide explicit compatibility. Inventory/energy/fluid/runtime contents are not copied simply because they were present in a schematic.

## Constructor execution

The Constructor validates, in order, card/deployment, range, chunk/world-border, replacement policy, support/dependency, material requirement and FE before reserving a shot.

Material and FE are reserved before launch. The world changes only at projectile impact. Block and supported entity targets use the same authoritative shot pipeline.

Current test balance retains a real cycle rather than instant placement: aim + charge + distance-based flight + cooldown.

## Renderer

Block shots render the real target BlockState model. Entity-stage shots render the real required item model. Turret yaw/pitch, interpolation, charge pulse, recoil and arced flight remain separate from server-authoritative placement state.

## Validation rule

CI success means only **compiled/API-valid**. A runtime-test JAR must not be described as fully working until in-game tests cover at least table upload/output, portable preview, deployment transforms, undeployed-card rejection, missing material/FE pauses, modded adjacent inventories, deferred blocks, Block Entity safety, save/reload during an active shot, entity stage and representative files from every supported adapter.
