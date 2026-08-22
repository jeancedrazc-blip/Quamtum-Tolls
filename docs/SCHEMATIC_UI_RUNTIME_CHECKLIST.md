# Runtime validation checklist — schematic and UI

Do not merge this feature only because it compiles.

- Written card opens the world-visible placement editor without mandatory keybinds.
- First deployment samples the target once; the schematic does not chase the crosshair afterward.
- Deploy, Move X/Z, Move Y, Rotate, Mirror and Precise tools update the same draft transform.
- Exact X/Y/Z editor accepts signed integer coordinates and returns to the live preview without losing the draft.
- Rotation and mirror use the same pivot and bounds as the final server transform.
- APPLY synchronizes the final transform; CANCEL restores the card's previous deployment; CLEAR undeploys it.
- Preview renders the actual block models and keeps declared schematic bounds/origin behavior intact.
- Constructor replace modes match their labels and persist through world save/reload.
- Skip-missing and Block Entity policy persist and are server authoritative.
- Constructor UI does not overlap inventory slots at GUI scale 1–4.
- Schematic Table input/output slots align with their visual frames and upload progress/status remain readable.
- Shader/resource-pack test: UI remains legible and Constructor emissive effects do not wash out status colors.
- Regression: Builder/Miner and the rest of Quantum Tools remain registered and present in JEI/creative inventory.
