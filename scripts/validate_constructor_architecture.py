#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/mcjty/rftoolsbuilder"
RES = ROOT / "src/main/resources"
ERRORS: list[str] = []


def text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        ERRORS.append(f"Cannot read {path.relative_to(ROOT)}: {exc}")
        return ""


def require(path: Path, needle: str, reason: str) -> None:
    body = text(path)
    if needle not in body:
        ERRORS.append(f"{path.relative_to(ROOT)}: missing {reason}: {needle!r}")


def forbid(path: Path, needle: str, reason: str) -> None:
    body = text(path)
    if needle in body:
        ERRORS.append(f"{path.relative_to(ROOT)}: forbidden {reason}: {needle!r}")


def main() -> int:
    constructor = JAVA / "constructor"
    bootstrap = constructor / "ConstructorBootstrap.java"
    mixin = JAVA / "mixin/RFToolsBuilderMixin.java"
    mixins_json = RES / "quantumtools.mixins.json"
    block_entity = constructor / "ConstructorBlockEntity.java"
    pipeline = constructor / "SchematicPipelineLoader.java"
    facade = constructor / "UniversalSchematicLoader.java"
    entity_compat = constructor / "ConstructorEntityDataCompat.java"
    material = constructor / "ConstructorMaterialAccess.java"
    job = constructor / "plan/ConstructionJob.java"

    # One mod, one loader lifecycle. Constructor is injected into the canonical
    # rftoolsbuilder entrypoint and must never become a second @Mod again.
    forbid(bootstrap, "@Mod(", "second mod entrypoint")
    forbid(bootstrap, "@Mod ", "second mod entrypoint")
    require(mixin, 'targets = "mcjty.rftoolsbuilder.RFToolsBuilder"', "primary mod target")
    require(mixin, "ConstructorBootstrap.init(modBus)", "Constructor registration through primary mod")
    require(mixins_json, '"RFToolsBuilderMixin"', "registration mixin declaration")

    # One authoritative schematic path shared by upload, preview and printing.
    require(facade, "SchematicPipelineLoader.load", "authoritative schematic pipeline delegation")
    require(block_entity, "UniversalSchematicLoader.loadCard", "Constructor card pipeline")
    require(pipeline, "declaredMin", "declared source bounds preservation")
    require(pipeline, "readLitematicEntities", "Litematica entity import")
    require(pipeline, "readSpongeEntities", "Sponge entity import")
    require(pipeline, "readVanillaEntities", "vanilla/Create entity import")
    require(pipeline, 'data.remove("Passengers")', "nested entity sanitization")

    # Save/reload and payment semantics are hard invariants: a reserved shot may
    # resume, but must not reserve FE/material twice.
    require(block_entity, 'output.putBoolean("ShotReserved", shotReserved)', "reserved-shot persistence")
    require(block_entity, 'output.store("ConstructionJob"', "construction cursor persistence")
    require(block_entity, 'input.read("ConstructionJob"', "construction cursor reload")
    require(block_entity, "pendingJobData", "reload reconstruction cursor")
    require(job, 'tag.put("Primary"', "primary queue persistence")
    require(job, 'tag.put("Deferred"', "deferred queue persistence")
    require(job, 'tag.putInt("EntityIndex"', "entity-stage persistence")
    require(material, "Transaction", "transactional material reservation")

    # External entity NBT must never recursively materialize passengers/root
    # vehicles from a schematic. We explicitly spawn only the paid top entity.
    require(entity_compat, 'data.remove("Passengers")', "passenger stripping")
    require(entity_compat, 'data.remove("RootVehicle")', "root vehicle stripping")
    require(entity_compat, "level.addFreshEntity(entity)", "single-entity authoritative spawn")
    forbid(entity_compat, "addFreshEntityWithPassengers", "recursive passenger spawn")

    if ERRORS:
        print("CONSTRUCTOR ARCHITECTURE VALIDATION FAILED")
        for error in ERRORS:
            print(" -", error)
        return 1

    print("Constructor architecture validation passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
