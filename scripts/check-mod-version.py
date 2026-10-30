#!/usr/bin/env python3
"""Verify Gradle and internal NeoForge metadata match; optionally inspect a built mod JAR."""
from pathlib import Path
from argparse import ArgumentParser
from zipfile import ZipFile
import re
ROOT = Path(__file__).resolve().parents[1]
prop = (ROOT/'mod-neoforge/gradle.properties').read_text(encoding='utf-8')
version = re.search(r'^mod_version=([^\s]+)$', prop, re.M)
assert version, 'Missing mod_version in mod-neoforge/gradle.properties'
version = version.group(1)
metadata = 'META-INF/neoforge.mods.toml'
plain = (ROOT/'mod-neoforge/src/main/resources'/metadata).read_text(encoding='utf-8')

def check(content, label):
    match = re.search(r'^version\s*=\s*"([^"\n]+)"', content, re.M)
    assert match, f'{label}: missing top-level static mod version'
    assert match.group(1)==version, f'{label}: internal {match.group(1)} != Gradle {version}'
check(plain, 'source metadata')
arg = ArgumentParser()
arg.add_argument('--jar',nargs='*',default=[])
args=arg.parse_args()
for path in args.jar:
    with ZipFile(path) as z:
        check(z.read(metadata).decode('utf-8'),path)
    # Build artifact must actually be named for the same version.
    assert version in Path(path).name, f'{path}: JAR filename disagrees with {version}'
print(f'NeoForge metadata agrees with Gradle mod_version={version}; JARs checked={len(args.jar)}')
