from pathlib import Path
import base64
import hashlib
import json

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / "baseline" / "manifest.json").read_text(encoding="utf-8"))
parts = sorted((root / "baseline" / "parts").glob("QuantumTools-3.0.5.jar.b64.part*"))
if not parts:
    raise SystemExit("No baseline parts found")
payload = "".join(p.read_text(encoding="ascii").strip() for p in parts)
data = base64.b64decode(payload)
out = root / "baseline" / manifest["output"]
out.write_bytes(data)
actual = hashlib.sha256(data).hexdigest()
expected = manifest["sha256"]
if actual != expected:
    out.unlink(missing_ok=True)
    raise SystemExit(f"SHA-256 mismatch: {actual} != {expected}")
print(f"Restored {out} ({actual})")
