#!/usr/bin/env python3
"""Fail CI if a normal runtime service is accidentally placed in a Mixin package.

Mixin config 'package' is reserved for mixin classes; ordinary services must live
outside that package to avoid mixin IllegalClassLoadError at startup.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'mod-neoforge/src/main/java'
CONFIG = ROOT / 'mod-neoforge/src/main/resources/roglercore-sable-lifecycle.mixins.json'
config = json.loads(CONFIG.read_text(encoding='utf-8'))
mixin_package = config['package']
mixin_path = SOURCE.joinpath(*mixin_package.split('.'))
assert mixin_path.is_dir(), f'Missing mixin package directory: {mixin_path}'

mixins = set(config.get('mixins', []) + config.get('client', []) + config.get('server', []))
assert mixins, 'No declared Mixin in Sable configuration'
for entry in mixins:
    expected = mixin_path.joinpath(*entry.split('.')).with_suffix('.java')
    assert expected.is_file(), f'Configured Mixin does not exist: {expected}'

for path in mixin_path.rglob('*.java'):
    source = path.read_text(encoding='utf-8')
    assert re.search(r'(?m)^package\s+' + re.escape(mixin_package) + r'\s*;', source), (
        f'Java package does not match configured Mixin package: {path}'
    )
    assert re.search(r'(?m)^@Mixin\s*\(', source), (
        f'Ordinary non-Mixin class in reserved Mixin package: {path}'
    )

service = SOURCE / 'dev/rogler/teamwars/neoforge/compat/SableRemovedSublevelGuard.java'
assert service.is_file(), 'Runtime lifecycle service must be outside the Mixin package'
assert f'package {mixin_package};' not in service.read_text(encoding='utf-8')

old_name = mixin_package + '.SableRemovedSublevelGuard'
for path in SOURCE.rglob('*.java'):
    assert old_name not in path.read_text(encoding='utf-8'), f'Stale Mixin-package service reference: {path}'

print('Mixin package boundary regression check: OK (only actual @Mixin classes in reserved package).')
