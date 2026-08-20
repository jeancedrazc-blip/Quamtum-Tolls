# Current Quantum Tools development build

## 3.0.6-dev.9 — schematic pipeline runtime test

This is a **runtime-test build, not a final release**.

- Released baseline: Quantum Tools 3.0.5
- Baseline SHA-256: `1ea89fcadeb43b9c78245a082fb26e92471e93a1c0901abee21b02b9fe52b7e7`
- Functional source commit: `820a3e1ad7f2b9ea1555a64acee34a126e7387e4`
- GitHub Actions run: `32332355954`
- GitHub Actions job: `96315300479`
- CI result: **success** (asset/reference validation + Java 25 / NeoForge compile + patch artifact)
- CI patch artifact id: `9393492501`
- CI patch artifact SHA-256 (GitHub): `4e44285bb9993c10db01386398cc8f57a76da9d95eeaffd5cf199240676e301f`
- Assembled runtime-test JAR: `QuantumTools-3.0.6-dev9-schematic-pipeline-runtime-test.jar`
- Assembled JAR SHA-256: `514e8bf8e35ad94b94697fac89406fb576d7232ce97199b9c836ac731c40086e`

The assembled JAR is the previous verified dev.8 merged JAR overlaid with the CI-validated dev.9 patch. Existing language dictionaries are merged rather than replaced, the original RFToolsBuilder core classes remain present, `quantumtools.mixins.json` remains registered through `META-INF/neoforge.mods.toml`, and metadata version is `3.0.6-dev.9`.

### Included functional checkpoint

- Schematic Table input/output workflow and streamed client -> server upload.
- Upload path/size/byte-count/SHA/parser validation.
- Abandoned upload cleanup with reserved-card recovery.
- Portable written card metadata and server-authoritative schematic storage.
- Local preview with SHA validation plus rate-limited server -> client fallback/cache.
- Deployment anchor/rotation/mirror using one shared transform implementation.
- Declared schematic bounds preserved, including outer air margins.
- `.nbt`, `.schem`, `.litematic`, `.schematic` adapters normalized to one plan.
- Persistent `BLOCKS -> DEFERRED_BLOCKS -> ENTITIES -> COMPLETE` print cursor.
- Transactional adjacent-inventory material reservation.
- Requirement registries for multi-count/component-sensitive blocks.
- Safe Block Entity data/state-filter extension points.
- Supported entity requirements/materialization (Item Frames, Glow Item Frames, Armor Stands) and entity hologram preview.
- FE/material reservation before shot and authoritative world mutation on impact.
- BlockState projectile rendering and entity-stage item projectile rendering.
- Functional Constructor UI now distinguishes block/entity targets and displays the actual missing material.
- Slower test firing cycle: aim + charge + distance flight + impact + cooldown.

### Not yet claimed

CI proves compilation/API compatibility only. The build is not considered runtime-validated until the in-game acceptance checklist in `PROJECT_STATE.md` / `docs/SCHEMATIC_PIPELINE_DEV8.md` is completed.

The final visual redesign of the UI is intentionally not part of this build; it will use the user's later UI reference images.
