#!/usr/bin/env python3
"""Reproducible CI fallback for Registrate using Create's official embedded JarJar.

The pinned Create 6.0.10-280 release embeds Registrate MC1.21-1.3.0+67.
The extracted bytes MUST match the checksum independently obtained from the
user's original installed Create 6.0.10 JAR. Never silently accept a mismatch.
"""

from __future__ import annotations

import argparse
import hashlib
import io
from pathlib import Path
import tempfile
import time
from urllib.request import Request, urlopen
from zipfile import ZipFile

REGISTRATE_SHA256 = "510f4041c41739f1d8ea8850ab8364d3e3a5fada8529beed5d9f479e2523db52"
PINNED = {"minecraft_version": "1.21.1", "create_version": "6.0.10-280", "registrate_version": "MC1.21-1.3.0+67"}
REGISTRATE_CLASS = "com/tterrag/registrate/AbstractRegistrate.class"
MAX_CREATE_BYTES = 32 * 1024 * 1024
CREATE_URL = (
    "https://maven.createmod.net/com/simibubi/create/create-1.21.1/6.0.10-280/"
    "create-1.21.1-6.0.10-280.jar"
)


def properties(path: Path) -> dict[str, str]:
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def fetch_create() -> bytes:
    last_error = None
    for attempt in range(3):
        try:
            request = Request(CREATE_URL, headers={"User-Agent": "RoglerCore-CI/0.7.6"})
            with urlopen(request, timeout=50) as response:
                data = response.read(MAX_CREATE_BYTES + 1)
            if len(data) > MAX_CREATE_BYTES:
                raise ValueError("Official Create artifact exceeded safety limit")
            print(f"Create artifact fetched from official Maven: {len(data)} bytes")
            return data
        except (OSError, ValueError) as exc:
            last_error = exc
            if attempt < 2:
                time.sleep(3 * (attempt + 1))
    raise RuntimeError(f"Official Create artifact unavailable: {last_error}")


def write_atomic(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path.parent, prefix=".temp-", delete=False) as stream:
        temp = Path(stream.name)
        stream.write(data)
    try:
        temp.replace(path)
    finally:
        temp.unlink(missing_ok=True)


def bootstrap(root: Path, create_jar: Path | None = None, *, expected_sha256: str = REGISTRATE_SHA256) -> Path:
    values = properties(root / "mod-neoforge" / "gradle.properties")
    for key, expected in PINNED.items():
        if values.get(key) != expected:
            raise ValueError(f"Version mismatch: {key}={values.get(key)!r}; expected {expected!r}. Review bootstrap first.")

    create_data = create_jar.read_bytes() if create_jar else fetch_create()
    with ZipFile(io.BytesIO(create_data)) as outer:
        embedded_path = f"META-INF/jarjar/Registrate-{PINNED['registrate_version']}.jar"
        embedded = outer.read(embedded_path)

    actual_hash = hashlib.sha256(embedded).hexdigest()
    if actual_hash.lower() != expected_sha256.lower():
        raise ValueError(
            f"REGISTRATE CHECKSUM MISMATCH: expected {expected_sha256}, got {actual_hash}. "
            "Do not use this dependency until the artifact source is investigated."
        )
    with ZipFile(io.BytesIO(embedded)) as inner:
        if REGISTRATE_CLASS not in inner.namelist():
            raise ValueError("Embedded Registrate does not contain AbstractRegistrate.class")

    version = PINNED["registrate_version"]
    path = root / ".ci-maven" / "com" / "tterrag" / "registrate" / "Registrate" / version
    filename = f"Registrate-{version}"
    pom = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<project xmlns="http://maven.apache.org/POM/4.0.0">\n'
        '  <modelVersion>4.0.0</modelVersion>\n'
        '  <groupId>com.tterrag.registrate</groupId>\n'
        '  <artifactId>Registrate</artifactId>\n'
        f'  <version>{version}</version>\n'
        '  <packaging>jar</packaging>\n'
        '</project>\n'
    )
    write_atomic(path / (filename + ".jar"), embedded)
    write_atomic(path / (filename + ".pom"), pom.encode("utf-8"))
    print(f"Verified pinned Registrate ({actual_hash.upper()}) installed into local CI Maven repository")
    return path / (filename + ".jar")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--create-jar", type=Path, help="Offline build: use the installed official Create JAR")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    print(f"Bootstrapping Registrate for Rogler Core {properties(root / 'mod-neoforge' / 'gradle.properties').get('mod_version')}")
    bootstrap(root, args.create_jar)


if __name__ == "__main__":
    main()
