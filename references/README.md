# Quantum Tools — Visual Reference Lock

This directory is the canonical visual reference set for the current Quantum Tools redesign. These files are not moodboards. They are acceptance references for implementation.

## Locked references

- `constructor_ct01_approved.jpg` — approved Constructor cannon silhouette, proportions, raised central pivot, compact splayed base, white/gunmetal shell and cyan energy channel. Runtime model must be compared against this image before a build is described as visually finished.
- `schematic_table_approved.jpg` — approved physical Schematic Table form language. The UI shown inside this early concept is **not** the current functional specification; only the physical table/material language is locked from this sheet.
- `miner_concept01_approved.jpg` — approved Miner Concept 01 revision: compact body, nearly flush front, cyan core, white/gunmetal body and restrained orange accents.
- `miner_concepts_board.jpg` — retained design exploration board for comparison/history. Concept 01 is the selected direction.
- `ui_system_reference.jpg` — locked UI finish standard: dark machined panel, cyan energy accents, controlled typography, proper spacing and hierarchy. Do not ship placeholder rectangles/text-only developer UI as final art.
- `cards_visual_reference.jpg` — locked card family reference: wide sci-fi card chassis, 32x32 final texture target, white/gunmetal/cyan base with function-specific accent colors.

## Functional reference rules

The Schematic Table and Schematic Card must follow the **Create schematic workflow semantics**, adapted to our universal-format requirement:

1. Local client `schematics/` browser.
2. Real file transfer/upload to server; never store only a client-local filename and pretend the server can resolve it.
3. Upload progress and validation.
4. Card becomes valid only after server-side acceptance/storage.
5. Constructor consumes the server-valid card and normalized `ConstructionPlan`.
6. Universal adapter layer remains independent of source format (`.nbt`, `.schem`, `.litematic`, `.schematic`, future adapters).

Create source files used as behavioral reference:
- `SchematicTableBlockEntity`
- `SchematicTableMenu`
- `SchematicTableScreen`
- `ClientSchematicLoader`
- `ServerSchematicLoader`

## Definition of done

A dev build may be called **compiled** after CI. It may only be called **working** after runtime verification of the relevant path. It may only be called **visually approved/final** after in-game comparison against the locked reference image.
