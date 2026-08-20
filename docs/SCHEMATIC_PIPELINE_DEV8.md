# Quantum Tools — Schematic pipeline dev.8

Status: **compiled and packaged for runtime testing; not yet runtime-validated.**

## Reference architecture

The implementation follows the full Create 6.0.9 schematic pipeline as the behavioral reference, while preserving Quantum Tools FE power, visual identity, block substitution and multi-format adapters.

The workflow is intentionally split into distinct responsibilities:

1. Schematic Table discovers client-side schematic files.
2. A blank Schematic Card is inserted in the table input.
3. The selected file is validated and uploaded client -> server in chunks.
4. The server validates path, declared size, actual byte count, SHA-256 and parser compatibility before issuing a written card in the output slot.
5. The written card stores source metadata, bounds and deployment transform; it does not embed arbitrary raw schematic bytes.
6. Holding a written card activates world preview/deployment.
7. The player positions, rotates and mirrors the holographic plan and confirms deployment.
8. Only a deployed card can start the Constructor.
9. The Constructor converts the source through an adapter into a neutral ConstructionPlan and executes a persistent ConstructionJob.

## Schematic Table

The table now uses Create-style input -> processing -> output semantics.

- one input Schematic Card slot;
- one output written-card slot;
- file browser for the schematics directory;
- refresh and selection;
- real upload progress;
- input is reserved while an upload is active;
- interrupted uploads restore/drop the reserved input safely instead of duplicating it.

The table must not contain the rejected dev.4 manual offset/rotation/substitution workflow.

## Upload protocol

`ConstructorNetworking` and `ClientSchematicUploader` provide dedicated BeginUpload, UploadChunk, FinishUpload and CancelUpload payloads.

Server upload rules include:

- sanitized server-side path;
- per-player storage;
- maximum upload size;
- declared-size enforcement;
- byte-count enforcement;
- SHA-256 verification;
- parser validation before card creation;
- cancellation/cleanup for incomplete uploads.

## Schematic Card data

Schema version: 2.

The card stores:

- source/display name;
- server-authoritative file path;
- original client-relative file path for preview;
- detected source format;
- SHA-256;
- size X/Y/Z;
- deployed flag;
- anchor;
- rotation;
- mirror;
- block substitution rules.

Writing a new schematic clears the obsolete dev.4 offset fields and resets deployment.

## Universal source adapters

The neutral loader architecture remains independent of the Constructor engine.

Current supported source families:

- Create / vanilla structure `.nbt`;
- Sponge / WorldEdit `.schem`;
- Litematica `.litematic`;
- legacy MCEdit / Schematica `.schematic`.

All readers normalize to `ConstructionPlan` / `ConstructionEntry`.

## World preview and deployment

`SchematicPlacementHandler` loads the client copy asynchronously and renders the real BlockState models as a world hologram.

Deployment state supports:

- world anchor;
- horizontal positioning by target/look;
- vertical offset;
- 90-degree rotation;
- mirror none / left-right / front-back;
- confirm/clear deployment;
- server synchronization of the selected hotbar card, guarded by the card SHA-256.

The Constructor must reject cards that have not been deployed.

## Persistent construction job

`ConstructionJob` is the Create-style persistent print cursor.

Stages:

`BLOCKS -> DEFERRED_BLOCKS -> ENTITIES -> COMPLETE`

The saved job includes primary/deferred queues, stage, completed count and deferred-attempt state so reloading a world does not restart the schematic.

Block substitution is applied before material requirement calculation and before placement, ensuring checklist, consumption and final BlockState agree.

## Material requirements

`ConstructorRequirement` and `ConstructorRequirementRegistry` replace the old `block.asItem()` simplification.

The system supports block- and BlockEntity-specific providers and special multi-count/use semantics. Material access simulates extraction before committing it and uses NeoForge inventory capabilities so modded adjacent inventories can participate.

## Safe Block Entity data

`ConstructorSafeBlockEntityData` and `ConstructorStateFilterRegistry` exist specifically to prevent blind NBT duplication.

Unsafe inventory/energy/fluid/runtime state must not be copied simply because it existed in the schematic. Sanitizers/state filters can be registered for special blocks and Block Entities.

## Constructor behavior

The Constructor retains FE rather than Create gunpowder.

Execution validates:

- deployed schematic;
- range;
- loaded target;
- world placement policy;
- replacement mode;
- existing Block Entity policy;
- already-correct target;
- material requirement;
- FE requirement;
- support/dependency availability.

Support-sensitive entries may be moved to the deferred pass instead of failing the whole job.

FE and material reservation happen before launch. World mutation remains server-authoritative and occurs at projectile impact.

## Renderer / firing

The approved sci-fi cannon remains the visual target.

Runtime renderer includes target yaw/pitch, interpolation, charge/emissive animation, recoil, distance-scaled flight time and a projectile rendered from the real target BlockState model.

## Validation rule

GitHub Actions success means **compiled/API-valid only**.

A dev build becomes runtime-validated only after in-game testing confirms at minimum:

- table file listing;
- upload and output card;
- preview rendering;
- rotation/mirror/height positioning;
- deployment persistence;
- Constructor rejection of undeployed cards;
- material pause and resume;
- FE pause and resume;
- adjacent modded inventory extraction;
- deferred support blocks;
- safe Block Entity handling;
- world reload during an active job/projectile;
- representative `.nbt`, `.schem`, `.litematic` and `.schematic` files.

Do not describe dev.8 as fully working until those runtime checks pass.
