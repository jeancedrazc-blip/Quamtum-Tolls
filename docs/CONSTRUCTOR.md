# Constructor — Canonical Design and Implementation Spec

Status: approved implementation direction.  
Project baseline: Quantum Tools 3.0.5 / Minecraft 26.1.2 / NeoForge.

## 1. Identity and physical form

- Do not use `Quantum` in the machine name.
- Internal development id: `constructor` until the final display name is chosen.
- Visual form: compact sci-fi construction cannon, based on the approved concept art.
- The cannon body is the visual focus; the base must remain compact.
- Fixed lower base + rotating turret + independently elevating barrel.
- Full 360-degree yaw rotation.
- Pitch is calculated toward the actual target block; visual limits must not prevent valid construction targets.
- The barrel must visibly aim at the exact block position before firing.
- This is not a normal cube-shaped machine. Dynamic moving parts are rendered by a Block Entity Renderer.

## 2. Energy

The Constructor is an FE-powered machine. It does not use gunpowder or a secondary fuel.

- Uses NeoForge `Capabilities.Energy.BLOCK` / `EnergyHandler`.
- Energy is accepted from any side unless a later side-configuration system changes this.
- Separate energy storage from work logic, following the robust pattern already used by the current Builder.
- Construction only begins a shot after enough FE has been reserved for that placement.
- If FE is insufficient, state becomes `WAITING_ENERGY`; target and progress are preserved and the machine resumes automatically when power returns.
- Energy parameters must be config-driven, not hardcoded gameplay constants.

Initial balance defaults for implementation/testing:

- capacity: 5,000,000 FE
- max input: 250,000 FE/t
- base placement cost: 1,000 FE/block
- distance surcharge: 15 FE per block of muzzle-to-target distance
- Block Entity surcharge: 1,500 FE

These numbers are defaults for testing and can be balanced later without changing the architecture.

## 3. Construction pipeline

Canonical server-side pipeline:

1. `SchematicAdapter` reads the source format.
2. Source is normalized into an internal `ConstructionPlan`.
3. Block substitution rules are applied.
4. World replacement policy decides whether the target should be changed.
5. Dependencies are ordered/deferred.
6. Required material is resolved.
7. Material availability is simulated.
8. Energy availability is checked.
9. Material + FE are reserved/consumed atomically for the shot.
10. Cannon rotates and elevates toward the target.
11. Construction projectile is spawned.
12. Projectile renders the actual target `BlockState` model/textures.
13. On impact the server validates the target again and places the block.
14. Safe Block Entity data is applied after block creation.
15. Progress advances to the next target.

The authoritative placement always happens on the server.

## 4. Schematic format abstraction

The machine must not be tied to one mod or one file type.

Core interface concept:

`SchematicAdapter -> ConstructionPlan -> Constructor execution engine`

Planned adapters:

- native Quantum Tools blueprint/structure adapter
- vanilla structure NBT adapter
- Sponge `.schem` adapter
- Create schematic adapter when Create is present
- Litematica `.litematic` adapter
- legacy `.schematic` adapter if practical

Create support must be optional. Quantum Tools must still load if Create is absent.

A normalized plan entry stores at minimum:

- relative position
- desired `BlockState`
- source/original block identity
- optional sanitized Block Entity data
- placement phase/dependency information

## 5. Materials

The Constructor uses real building materials.

- It scans adjacent automation-accessible inventories through NeoForge item capabilities.
- A future internal buffer may be added, but adjacent inventory compatibility is the baseline behavior.
- Material search is registry-driven and must work with modded blocks/items where the block has a normal placeable item representation.
- Missing material does not destroy progress.
- Default behavior: `WAITING_MATERIAL` and automatically resume when the item becomes available.
- Optional `skip missing` mode can be exposed in the UI.

A material checklist is generated from the normalized plan after substitutions, so the list represents what will actually be consumed.

## 6. Block substitution

Block substitution is independent from the schematic source.

Example:

`minecraft:cobblestone -> minecraft:smooth_stone`

Substitution happens before material lookup and before the projectile is created.

Rule priority:

1. exact BlockState rule
2. exact block rule
3. tag rule
4. fallback to original schematic block

Initial UI can expose exact block replacement first, while the internal rule model remains ready for BlockState/tag expansion.

Rules belong to the loaded construction job/profile, not globally to every schematic.

## 7. World replacement policy

This is separate from material substitution.

Modes:

- `AIR_ONLY`: place only into air/replaceable targets
- `REPLACE`: replace differing blocks
- `MATCH_ONLY`: only replace when the existing block matches the schematic's original expected block
- `SKIP_OCCUPIED`: never destroy an occupied target

The machine must never silently behave like a quarry.

## 8. Placement ordering and dependencies

To avoid torches, rails, doors and other dependent blocks failing:

- primary traversal is bottom-to-top with locality preserved
- blocks that cannot survive at the current moment are moved to a deferred queue
- deferred entries are retried after support blocks are placed
- fluids are processed late
- Block Entity data is applied only after the block exists
- if an entry remains impossible after the retry budget, the job pauses with a diagnosable status instead of looping forever

## 9. Modded block compatibility and safe NBT

Directly copying arbitrary Block Entity NBT is unsafe and can duplicate inventories, fluids, energy or nested items.

Default safe policy:

- place the requested BlockState
- create the destination Block Entity normally
- apply only sanitized configuration data
- strip inventory contents, item stacks, fluid contents, energy storage, ownership/session ids and other known mutable/resource-bearing payloads by default
- allow adapter-specific safe handlers for mods that require configuration data

The architecture must permit per-mod placement hooks without hardcoding every mod into the core engine.

## 10. Projectile and animation

The projectile must visually use the complete block appearance, not an approximate color.

Preferred rendering approach:

- projectile carries/syncs the target `BlockState`
- client renders that BlockState using Minecraft's normal block model renderer
- therefore vanilla and modded textures/models are preserved, including slabs/stairs/non-full cubes when possible

Shot sequence:

1. turret slews toward target
2. barrel elevation locks on target
3. muzzle/core charge ramps up
4. target block model appears in the chamber/muzzle
5. block-model projectile launches on a smooth curved/accelerating path
6. target gets a brief wireframe/placement marker
7. impact pulse occurs
8. authoritative block appears

The shot is visual timing around a server-authoritative placement transaction; visual effects must never be able to duplicate materials or cause placement twice.

## 11. State machine

Suggested states:

- `IDLE`
- `LOADING_PLAN`
- `READY`
- `AIMING`
- `CHARGING`
- `FIRING`
- `WAITING_ENERGY`
- `WAITING_MATERIAL`
- `WAITING_CHUNK`
- `PAUSED`
- `BLOCKED`
- `COMPLETE`
- `ERROR`

The current target, progress index and substitution profile must persist across save/reload.

## 12. Chunk policy

Default behavior should not permanently force-load an entire schematic.

- process only the current target area
- if target chunk is unavailable, enter `WAITING_CHUNK`
- resume when it is available
- optional controlled chunk-ticket behavior can be added later behind config/upgrade if required

This avoids a large schematic becoming an uncontrolled chunk loader.

## 13. UI baseline

Main screen should expose:

- schematic/blueprint source slot
- energy bar
- start / pause / stop
- current state
- total progress and placed/remaining blocks
- current target block
- missing material when blocked
- material checklist
- block substitutions
- world replacement mode
- skip-missing toggle

No quarry-card controls belong in this machine.

## 14. Implementation rule

Do not copy the Create Schematicannon, RFTools Builder, or Building Gadgets implementation. Use them only as behavioral references. The Constructor has its own normalized-plan engine, FE transaction, substitution layer, safe Block Entity policy, aiming renderer and block-model projectile.
