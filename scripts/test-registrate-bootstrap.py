#!/usr/bin/env python3
"""Offline regression tests for pinned Create JarJar extraction and integrity."""
import hashlib
import importlib.util
import io
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile

module_path = Path(__file__).with_name("bootstrap-registrate.py")
spec = importlib.util.spec_from_file_location("bootstrap_registrate", module_path)
assert spec and spec.loader
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def zipped(name: str, content: bytes) -> bytes:
    buffer = io.BytesIO()
    with ZipFile(buffer, "w") as archive:
        archive.writestr(name, content)
    return buffer.getvalue()


with TemporaryDirectory(prefix="registrate-regression-") as tmp:
    root = Path(tmp)
    mod = root / "mod-neoforge"
    mod.mkdir()
    (mod / "gradle.properties").write_text("\n".join(f"{k}={v}" for k, v in module.PINNED.items()))
    inner = zipped(module.REGISTRATE_CLASS, b"synthetic test class")
    outer = zipped("META-INF/jarjar/Registrate-MC1.21-1.3.0+67.jar", inner)
    jar = root / "test-create.jar"
    jar.write_bytes(outer)
    digest = hashlib.sha256(inner).hexdigest()

    # Synthetic test bytes require synthetic expected digest; the production
    # caller always uses the pinned fingerprint from the user's actual JAR.
    result = module.bootstrap(root, jar, expected_sha256=digest)
    assert result.read_bytes() == inner
    assert result.with_suffix(".pom").is_file()
    assert (root / ".ci-maven").is_dir()

    try:
        module.bootstrap(root, jar, expected_sha256="0" * 64)
    except ValueError as exc:
        assert "CHECKSUM MISMATCH" in str(exc)
    else:
        raise AssertionError("Corrupt Registrate accepted")
    # Mismatch must not overwrite the previously checked JAR.
    assert result.read_bytes() == inner

    (mod / "gradle.properties").write_text("minecraft_version=1.21.2\n")
    try:
        module.bootstrap(root, jar, expected_sha256=digest)
    except ValueError as exc:
        assert "Version mismatch" in str(exc)
    else:
        raise AssertionError("Unsupported MC version accepted")

print("Registrate CI bootstrap regression checks OK")
