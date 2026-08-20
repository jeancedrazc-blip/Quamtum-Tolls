# Quantum Tools — Approved Visual References

This folder is the visual source of truth for the current redesign. Implementation must be checked against these references before a build is called ready.

## Locked references

- `constructor_ct01.jpg` — approved Constructor CT-01 silhouette: long elevated cannon, central trunnion, compact stabilizer base, white/gunmetal/cyan.
- `schematic_table_concept.jpg` — approved physical Schematic Table silhouette and material language. Functional UI must follow the Create-style workflow described below, not every old mockup control shown in the concept art.
- `miner_concept01_approved.jpg` — approved Miner Concept 01 revised: compact body, nearly flush front, white/gunmetal/cyan, subtle orange accents.
- `ui_style_reference.jpg` — approved UI visual language: dark industrial sci-fi frame, cyan emissive hierarchy, precise spacing, readable cards/energy/status/control zones. Do not copy obsolete feature layout literally.
- `cards_visual_reference.jpg` — approved cards family: wide card silhouette, industrial frame, cyan base language, function-specific accents.

## Schematic Table behavior — Create is the behavioral reference

The table is not a schematic editor. It is the import/write station.

Required flow:
1. Input slot accepts an empty/rewriteable Schematic Card.
2. UI lists supported schematic files from the local `schematics/` folder.
3. User selects one file and confirms.
4. In multiplayer/dedicated-server mode, the file is transferred client -> server in chunks with size/path validation, then stored server-side.
5. The input card is consumed/moved and a written Schematic Card appears in a separate output slot.
6. The written card stores a stable server-side schematic identifier/reference plus metadata; the Constructor must not depend on reading the player's local filesystem at build time.
7. UI shows upload/write progress and completion state.
8. UI includes refresh and open-folder controls.

This mirrors the proven Create Schematic Table architecture while remaining format-agnostic. Supported adapters feed a neutral `ConstructionPlan`.

## Quality gate

Do not call a development JAR 'working correctly' from compilation alone. Before delivery, verify:
- registration and creative-tab visibility;
- block/item models and actual in-game scale/origin assumptions;
- menu slot count/positions and quick-move behavior;
- client/server authority and persistence;
- schematic import/write/read lifecycle;
- FE/material reservation and missing-material states;
- world reload while idle, while paused, and during a reserved shot;
- no item/card loss when blocks are broken;
- visual comparison against the images in this folder.

CI compilation is necessary but is not runtime validation.