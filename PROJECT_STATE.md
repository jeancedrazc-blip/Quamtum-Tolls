# Quantum Tools — Project State

## Canonical baseline

- Current released project version: **3.0.5**
- Minecraft: **26.1.2**
- Loader: **NeoForge**
- Mod id preserved by the current build: `rftoolsbuilder`
- Canonical baseline SHA-256: `1ea89fcadeb43b9c78245a082fb26e92471e93a1c0901abee21b02b9fe52b7e7`
- Internal 3.0.5 JAR metadata version: `26.1.2-7.0.5-port.1`

The user-supplied 3.0.5 JAR identified by the SHA-256 above is the authoritative release baseline. Do not infer a baseline from older commits or remembered versions.

## Source-of-truth rule

Before changing Quantum Tools in any conversation:

1. Read this file.
2. Verify the latest repository branch/PR/release state.
3. Verify the canonical baseline hash when the 3.0.5 artifact is involved.
4. Never replace a newer baseline with an older remembered version.
5. Never invent missing changelog entries.
6. Preserve existing behavior unless an explicit change request says otherwise.

## Current implementation visible in the 3.0.5 baseline

The supplied JAR contains the Miner/Builder implementation, Quarry Card classes, filter UI/menu classes, Miner UI/menu classes, renderer classes, translations, recipes, models and textures.

## Constructor — approved direction

- The schematic-driven construction machine must have its **own physical model/form**, not look like a normal cubic block.
- Do **not** use the word `Quantum` in the machine's name.
- The machine should be format-agnostic through adapters/readers instead of being locked to one schematic source.
- Planned compatibility includes Create schematic data and other schematic formats where technically practical.
- The construction projectile must use the **full model/texture of the BlockState being placed**, not an approximate color.
- The machine needs a **block substitution system**, e.g. schematic cobblestone -> build with smooth stone.
- The placement animation should be a distinctive part of the machine's identity.
- It is an FE-powered machine.

### Approved Constructor physical model

- Dedicated sci-fi cannon/turret, not a generic industrial cube or robotic arm.
- Long horizontal cannon body with layered futuristic armor and cyan energy channel.
- Real aiming assembly: horizontal turret rotation plus vertical elevation toward the exact placement position.
- Exposed mechanical pivot/joint.
- Compact low-profile base with stabilizer geometry; avoid bulky pedestal forms.
- Main materials/colors: light metallic/white armor, dark gunmetal structure, cyan emissive accents.
- Dedicated energy-emitter muzzle.

## Miner — approved Concept 1 visual

Approved on 2026-08-19 after concept exploration.

- Same product-family language as the Constructor, but a distinct mining silhouette.
- Compact, robust body rather than a cannon.
- White/light metallic armor over a gunmetal technical chassis.
- Cyan mining/energy core integrated into the front.
- **Front must remain nearly flush with the main body**; do not restore the large protruding drill/nose from rejected concepts.
- Cyan upper energy detail/core.
- Small **orange warning/accent details** for identity and contrast.
- Low reinforced base.
- Preserve existing Miner UI, Quarry Cards, hologram/area renderer, energy system and orientation behavior unless separately requested.
- The pre-redesign 3.0.5 Miner appearance remains a fallback reference if the user asks to restore it.

## Miner balance — current development decision

The 3.0.5 Miner can complete up to one successful mining operation per game tick in ideal conditions. This was judged too fast.

Current development balance:

- base work interval: **4 ticks**;
- effective ideal maximum: approximately **5 mined blocks per second** instead of approximately 20/s;
- implemented by throttling the mining `work` cycle, while leaving normal Block Entity tick/update behavior intact.

This value is a development balance point and can be adjusted after gameplay testing.

## Cards — next visual round, not implemented yet

Approved visual direction to preserve for the next card pass:

- 32×32 item textures;
- slightly wider card proportions;
- white/gunmetal casing;
- cyan central display/emissive language;
- small function-specific colored accents;
- do not implement this card redesign until the user asks for that round.

## Current development checkpoint — 3.0.6-dev.3

Development branch: `agent/constructor-foundation`
Draft PR: #1

Current checkpoint includes:

- Constructor FE machine foundation;
- Constructor yaw/pitch aiming and BlockState projectile renderer;
- material lookup/consumption and authoritative placement on impact;
- neutral `ConstructionPlan` layer and exact block-substitution rules;
- Constructor registration integrated into the main `rftoolsbuilder` mod bootstrap through a Mixin instead of relying on a separate mod entrypoint;
- Constructor explicitly added to the `rftoolsbuilder:main` creative tab through `BuildCreativeModeTabContentsEvent` so it appears in creative/item browsers;
- Miner Concept 1 visual with cyan + orange accents;
- Miner speed throttled to one work cycle every four ticks;
- Quarry Card visual redesign intentionally deferred.

Validated development JAR SHA-256 for 3.0.6-dev.3:

`0d7f8505cf8a3d39c868437d4426d192cba7c9838ae8ea777c8af363f46330bf`

The development JAR for this checkpoint is built by merging the validated patch over the canonical 3.0.5 JAR and enabling `quantumtools.mixins.json` in `META-INF/neoforge.mods.toml`.

## Binary baseline storage note

The repository previously claimed that the 3.0.5 JAR was fully stored as Base64 parts, but that upload was not completed. Do **not** rely on `baseline/parts/` as a complete artifact source unless a later commit explicitly records a successful verified upload. The canonical SHA-256 above remains the verification key for the user-supplied baseline artifact.

## Versioning rule

The next final release must be created from the verified 3.0.5 baseline and must increment from 3.0.5. Development builds may use `3.0.6-dev.*`; do not call them the final 3.0.6 until runtime testing is complete.
