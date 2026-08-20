# Quantum Tools — Development rules

These rules are project-level requirements.

1. **Do not silently simplify requested features.** If a reference implementation or an approved specification contains multiple stages, states, safeguards or compatibility behaviors, implement the full agreed scope rather than replacing it with a smaller approximation.
2. A simplification is allowed only when explicitly approved before implementation.
3. When a user names a reference mod (for example Create), inspect the actual implementation for the relevant version and use it as the behavioral reference. Do not rely on a remembered summary.
4. Approved concept art is a visual acceptance reference, not loose inspiration. Runtime model silhouette, proportions and visual hierarchy must be checked against it.
5. CI success means only that the code compiles/API signatures are valid. Never describe a development build as fully working until runtime tests pass.
6. Debug/prototype UI and placeholder textures must be labeled as such and must not be presented as final visual work.
7. Client/server ownership, persistence, reload behavior, duplication safety and failure recovery are part of a feature, not optional follow-up polish.
8. Universal schematic support means a neutral internal representation plus explicit format adapters. Never claim unknown arbitrary formats are supported without a parser/specification.
9. Existing released behavior must be preserved unless the requested change explicitly replaces it.
10. Development state, hashes and known limitations must be recorded in the repository so future work does not depend on conversation memory.
