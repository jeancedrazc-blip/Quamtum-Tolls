#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import re
import sys
import zipfile
from pathlib import Path

CANONICAL_BASE_SHA256 = "1ea89fcadeb43b9c78245a082fb26e92471e93a1c0901abee21b02b9fe52b7e7"
FIXED_TIMESTAMP = (1980, 1, 1, 0, 0, 0)
SKIP_PATCH_META = {
    "META-INF/MANIFEST.MF",
    "META-INF/neoforge.mods.toml",
}
REQUIRED_FINAL_ENTRIES = {
    "mcjty/rftoolsbuilder/constructor/ConstructorBootstrap.class",
    "mcjty/rftoolsbuilder/constructor/ConstructorBlockEntity.class",
    "mcjty/rftoolsbuilder/constructor/SchematicPipelineLoader.class",
    "mcjty/rftoolsbuilder/mixin/RFToolsBuilderMixin.class",
    "quantumtools.mixins.json",
    "assets/rftoolsbuilder/blockstates/constructor.json",
    "assets/rftoolsbuilder/models/item/constructor.json",
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def is_signature(name: str) -> bool:
    upper = name.upper()
    return upper.startswith("META-INF/") and upper.endswith((".SF", ".RSA", ".DSA", ".EC"))


def patched_mods_toml(original: bytes, version: str) -> bytes:
    text = original.decode("utf-8")
    if "[[mixins]]" not in text or 'config="quantumtools.mixins.json"' not in text.replace(" ", ""):
        match = re.search(r"(?m)^license\s*=.*$", text)
        if not match:
            raise ValueError("Base neoforge.mods.toml has no license line; refusing unsafe automatic patch")
        insert = '\n\n[[mixins]]\nconfig="quantumtools.mixins.json"'
        text = text[: match.end()] + insert + text[match.end():]

    mods = text.find("[[mods]]")
    if mods < 0:
        raise ValueError("Base neoforge.mods.toml has no [[mods]] section")
    prefix = text[:mods]
    body = text[mods:]
    body, count = re.subn(r'(?m)^version\s*=\s*"[^"]*"', f'version="{version}"', body, count=1)
    if count != 1:
        raise ValueError("Could not replace primary mod version")
    return (prefix + body).encode("utf-8")


def read_archive(path: Path, *, patch: bool) -> dict[str, bytes]:
    result: dict[str, bytes] = {}
    with zipfile.ZipFile(path, "r") as archive:
        for info in archive.infolist():
            name = info.filename
            if name.endswith("/"):
                continue
            if is_signature(name):
                continue
            if patch and name in SKIP_PATCH_META:
                continue
            result[name] = archive.read(info)
    return result


def write_reproducible(output: Path, entries: dict[str, bytes]) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, FIXED_TIMESTAMP)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, entries[name])


def validate_final(entries: dict[str, bytes], version: str) -> list[str]:
    errors: list[str] = []
    missing = sorted(REQUIRED_FINAL_ENTRIES.difference(entries))
    if missing:
        errors.extend(f"Missing final entry: {name}" for name in missing)

    mods = entries.get("META-INF/neoforge.mods.toml", b"").decode("utf-8", errors="replace")
    compact = mods.replace(" ", "")
    if "[[mixins]]" not in mods or 'config="quantumtools.mixins.json"' not in compact:
        errors.append("Final neoforge.mods.toml does not activate quantumtools.mixins.json")
    if f'version="{version}"' not in compact:
        errors.append(f"Final neoforge.mods.toml does not contain requested version {version}")
    if mods.count('modId="rftoolsbuilder"') != 1:
        errors.append("Final metadata must expose exactly one rftoolsbuilder mod entry")

    mixins = entries.get("quantumtools.mixins.json", b"").decode("utf-8", errors="replace")
    if "RFToolsBuilderMixin" not in mixins:
        errors.append("Final mixin config does not contain RFToolsBuilderMixin")

    # Patch JAR metadata must never replace the canonical mod metadata. Its
    # classes/resources are overlaid, while the base mod remains the sole mod.
    if "ConstructorBootstrap.class" not in "\n".join(entries):
        errors.append("Constructor classes are absent after overlay")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Assemble a Quantum Tools dev JAR over the canonical 3.0.5 base")
    parser.add_argument("base", type=Path, help="Canonical Quantum Tools 3.0.5 JAR")
    parser.add_argument("patch", type=Path, help="CI constructor-patch JAR")
    parser.add_argument("output", type=Path, help="Output complete mod JAR")
    parser.add_argument("--version", required=True, help="Dev mod version, e.g. 3.0.6-dev.9")
    parser.add_argument("--allow-noncanonical-base", action="store_true", help="Development escape hatch; never use for release artifacts")
    args = parser.parse_args()

    for path, label in ((args.base, "base"), (args.patch, "patch")):
        if not path.is_file():
            print(f"ERROR: {label} JAR not found: {path}", file=sys.stderr)
            return 2

    actual_base = sha256(args.base)
    if actual_base != CANONICAL_BASE_SHA256 and not args.allow_noncanonical_base:
        print("ERROR: refusing non-canonical Quantum Tools base", file=sys.stderr)
        print(f" expected: {CANONICAL_BASE_SHA256}", file=sys.stderr)
        print(f" actual:   {actual_base}", file=sys.stderr)
        return 3

    try:
        base_entries = read_archive(args.base, patch=False)
        patch_entries = read_archive(args.patch, patch=True)
        if "META-INF/neoforge.mods.toml" not in base_entries:
            raise ValueError("Canonical base is missing META-INF/neoforge.mods.toml")

        # Remove signatures from the modified archive, then overlay only patch
        # classes/resources. Canonical classes/resources remain untouched unless
        # the source branch intentionally emits the same path.
        final_entries = dict(base_entries)
        final_entries.update(patch_entries)
        final_entries["META-INF/neoforge.mods.toml"] = patched_mods_toml(
            base_entries["META-INF/neoforge.mods.toml"], args.version
        )

        errors = validate_final(final_entries, args.version)
        if errors:
            for error in errors:
                print("ERROR:", error, file=sys.stderr)
            return 4

        write_reproducible(args.output, final_entries)
    except (OSError, ValueError, zipfile.BadZipFile) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 5

    output_hash = sha256(args.output)
    print(f"base_sha256={actual_base}")
    print(f"patch_sha256={sha256(args.patch)}")
    print(f"output_sha256={output_hash}")
    print(f"output={args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
