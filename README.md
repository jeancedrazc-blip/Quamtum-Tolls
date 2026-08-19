# Quantum Tools

Canonical repository baseline for the Minecraft 26.1.2 NeoForge mod **Quantum Tools**.

## Current project version

- Project version: **3.0.5**
- Minecraft: **26.1.2**
- Loader: **NeoForge**
- Canonical baseline artifact: `baseline/QuantumTools-3.0.5.jar`
- SHA-256: `1ea89fcadeb43b9c78245a082fb26e92471e93a1c0901abee21b02b9fe52b7e7`

The JAR's internal `neoforge.mods.toml` still reports `26.1.2-7.0.5-port.1`. That internal metadata is historical and does **not** override the project version 3.0.5 confirmed for this baseline.

The exact baseline binary is stored in `baseline/parts/` as Base64 chunks because the connected GitHub API does not expose direct binary file upload. Run `scripts/restore_baseline.py` to reconstruct the JAR and verify its checksum.

Before any new development, read `PROJECT_STATE.md` and verify the baseline hash.
