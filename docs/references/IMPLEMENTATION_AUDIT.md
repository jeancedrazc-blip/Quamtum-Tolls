# Implementation audit — before next test build

This file is intentionally strict. A build is not ready for user testing until every item is either implemented or explicitly marked pending.

## Create Schematic Table parity

- Two machine slots: input blank/rewriteable Schematic Card and output written Schematic Card.
- Input is consumed/moved only when an upload/write actually starts.
- Output cannot accept manual insertion.
- Client scans the local schematic folder and owns the visible file list.
- Folder/open, refresh, selection and confirm controls are present.
- Writing is an upload/state transition, not direct mutation of the same input stack.
- Upload progress is synchronized to the table UI.
- On completion the server creates the written card in the output slot.
- Dedicated-server path uses client-to-server chunked transfer; integrated server may short-circuit but must use the same semantic result.
- File name/path is validated and normalized; path traversal is rejected.
- Size limits and packet-size limits are enforced.
- Universal adapters are applied after the file reaches the server-side schematic store.

## Constructor

- Physical model visually matches `constructor-ct01-approved-reference.webp`.
- Raised pivot/trunnion; barrel never rotates around floor level.
- Yaw + pitch aim at exact target.
- Idle / aiming / charging / firing / recoil / return states are visually distinct.
- Projectile uses the complete target BlockState model/texture.
- World placement happens only at projectile impact.
- FE/material reservation is deterministic and cannot duplicate/lose materials on pause/cancel.
- Missing-material behavior, replacement modes, Block Entity protection and deferred support blocks are testable from UI.
- UI spacing and hierarchy follows `ui-visual-language-reference.webp`.

## Schematic Table visual

- Physical block is a table/workbench silhouette, not a cube.
- White/light armor, gunmetal structure, cyan surface, restrained orange accents.
- In-game model dimensions/rotation are checked against the reference from at least front, side and three-quarter views.
- GUI is a file-writing station; do not reintroduce unrelated rotation/offset/replacement controls.

## Miner + cards

- Miner matches revised Concept 01: compact, near-flush front, orange accents.
- Base mining rate remains ~5 blocks/s unless gameplay testing changes it.
- Cards are 32x32 and preserve the approved family.

## Validation language

- GitHub Actions passing means **compiled successfully**.
- It does not mean visually correct or runtime-correct.
- Runtime-unverified builds must be labeled `compiled / pending runtime validation`.
