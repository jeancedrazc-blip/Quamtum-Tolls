#!/usr/bin/env python3
from __future__ import annotations

import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/rftoolsbuilder"

ERRORS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def validate_json() -> None:
    for path in ASSETS.rglob("*.json"):
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:
            fail(f"Invalid JSON: {path.relative_to(ROOT)}: {exc}")


def png_size(path: Path) -> tuple[int, int] | None:
    try:
        data = path.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n" or len(data) < 24:
            return None
        return struct.unpack(">II", data[16:24])
    except OSError:
        return None


def validate_cards() -> None:
    cards = [
        "schematiccarditem.png",
        "shapecarditem.png",
        "shapecardquarryitem.png",
        "shapecardcquarryitem.png",
        "shapecardfortuneitem.png",
        "shapecardcfortuneitem.png",
        "shapecardsilkitem.png",
        "shapecardcsilkitem.png",
    ]
    folder = ASSETS / "textures/item"
    for name in cards:
        path = folder / name
        if not path.is_file():
            fail(f"Missing approved card texture: {path.relative_to(ROOT)}")
            continue
        size = png_size(path)
        if size != (32, 32):
            fail(f"Card texture must be 32x32: {path.relative_to(ROOT)} is {size}")


def validate_table_model() -> None:
    path = ASSETS / "models/block/schematic_table.json"
    try:
        model = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"Cannot read Schematic Table model: {exc}")
        return
    elements = model.get("elements", [])
    if not elements:
        fail("Schematic Table model has no elements")
        return
    for idx, element in enumerate(elements):
        for key in ("from", "to"):
            coords = element.get(key)
            if not isinstance(coords, list) or len(coords) != 3:
                fail(f"Schematic Table element {idx} has invalid {key}")
                continue
            for axis, value in zip("XYZ", coords):
                if not isinstance(value, (int, float)) or value < 0 or value > 16:
                    fail(f"Schematic Table element {idx} {key}.{axis}={value} leaves one-block bounds")
    # A table must have open space below its top instead of being a full cube.
    has_leg = any(e.get("from", [99,99,99])[1] <= 1 and e.get("to", [0,0,0])[1] >= 8 for e in elements)
    has_top = any(e.get("from", [99,99,99])[1] >= 9 and e.get("to", [0,0,0])[1] >= 12 for e in elements)
    if not has_leg or not has_top:
        fail("Schematic Table geometry does not contain both legs and a raised tabletop")


def validate_references() -> None:
    refs = ROOT / "docs/references"
    expected = [
        "constructor-ct01-approved-reference.webp",
        "schematic-table-approved-reference.webp",
        "miner-approved-reference.webp",
        "miner-concepts-reference.webp",
        "ui-visual-language-reference.webp",
        "cards-visual-language-reference.webp",
        "README.md",
        "REFERENCE_RULES.md",
        "IMPLEMENTATION_AUDIT.md",
    ]
    for name in expected:
        path = refs / name
        if not path.is_file() or path.stat().st_size == 0:
            fail(f"Missing/empty canonical reference: docs/references/{name}")


def main() -> int:
    validate_json()
    validate_cards()
    validate_table_model()
    validate_references()
    if ERRORS:
        print("ASSET/REFERENCE VALIDATION FAILED")
        for error in ERRORS:
            print(" -", error)
        return 1
    print("Asset/reference validation passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
