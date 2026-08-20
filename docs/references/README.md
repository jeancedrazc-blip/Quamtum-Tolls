# Quantum Tools — Approved Visual References

These files are the canonical visual references for future implementation work. Runtime models, textures and GUIs must be compared against them before a development build is called ready for testing.

## Approved references

- `constructor-ct01-approved-reference.webp` — approved Constructor silhouette: sci-fi cannon, raised trunnion/pivot, long barrel, white/gunmetal armor, cyan emissives and compact stabilizer base.
- `schematic-table-approved-reference.webp` — approved physical Schematic Table: real workbench/table silhouette, not a cube; white/gunmetal body, cyan surface, orange accents.
- `miner-approved-reference.webp` — approved Miner Concept 01 revised: compact body, near-flush front, white/gunmetal, cyan and orange accents.
- `miner-concepts-reference.webp` — comparison board used to preserve the evolution/fallback context for Miner.
- `ui-visual-language-reference.webp` — approved UI visual language: dark industrial sci-fi frame, cyan information hierarchy, clean spacing, restrained orange accents.
- `cards-visual-language-reference.webp` — card-family visual direction and functional color differentiation.

## Non-negotiable implementation rule

A compile-successful build is not automatically a visually approved build. Before handing a build to the user, verify that the in-game geometry, pivots, proportions, slot locations, text hierarchy and textures match these references. If runtime validation has not happened, label the build as **compiled / pending runtime validation**, never as “working correctly”.

## Schematic Table interaction reference

For behavior, use Create's `SchematicTableBlockEntity`, `SchematicTableMenu`, `SchematicTableScreen`, `ClientSchematicLoader` and `ServerSchematicLoader` as the interaction/networking reference. Our UI skin and physical block model remain Quantum Tools-specific.
