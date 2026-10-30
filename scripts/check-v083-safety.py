#!/usr/bin/env python3
"""v0.8.3 regression guards for the observed 2026-10-30 C2ME save deadlock.
Static checks augment (do not replace) NeoForge compilation and real multiplayer tests.
"""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
def source(rel):
    return (root / rel).read_text(encoding='utf-8')
kinetic = source('mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/base/CoreKineticBlockEntity.java')
owner = source('mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/base/CoreInfrastructureService.java')
screen = source('mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/ui/client/ControlCenterScreen.java')

def body(code, signature):
    start = code.index(signature)
    brace = code.index('{', start)
    depth = 0
    for pos in range(brace, len(code)):
        if code[pos] == '{':
            depth += 1
        elif code[pos] == '}':
            depth -= 1
            if depth == 0:
                raw=code[brace + 1:pos]
                return re.sub(r"/\*.*?\*/|//[^\n]*", "", raw, flags=re.S)
    raise AssertionError(f'Unterminated method: {signature}')

# Critical: all calls possibly invoked during async serialization are cached-only.
write = body(kinetic, '@Override protected void write(')
read = body(kinetic, '@Override protected void read(')
speed = body(kinetic, '@Override public float getGeneratedSpeed()')
enabled = body(kinetic, 'private boolean enabled()')
for method, label in [(write, 'write'), (read, 'read'), (speed, 'getGeneratedSpeed'), (enabled, 'enabled')]:
    for forbidden in ('refreshDriveFromLedger(', 'owner(', 'recordedOwner(', 'getBlockState(', 'getChunk(', 'getChunkNow(', 'SavedData.get(', 'CoreInfrastructureSavedData.get(', 'enabled()'):
        assert forbidden not in method, f'Unsafe {forbidden} in {label}'
assert 'tag.putBoolean("TeamWarsDriveEnabled",cachedDriveEnabled)' in write
assert 'cachedDriveEnabled=tag.getBoolean("TeamWarsDriveEnabled")' in read
assert 'cachedDriveEnabled?CoreKineticPolicy.RPM:0' in speed
assert 'return cachedDriveEnabled;' in enabled
assert 'refreshDriveFromLedger()' in body(kinetic, '@Override public void tick()')
assert 'refreshDriveFromLedger()' in body(kinetic, 'public void refreshEngine()')
assert 'super.write(tag,registries,clientPacket);' in write
# Keep physical proof in owner(), but loaded-only: don't reintroduce a synchronous level getBlockState.
physical = body(owner, 'public static UUID owner(ServerLevel level,BlockPos pos)')
ledger = body(owner, 'public static UUID recordedOwner(ServerLevel level,BlockPos pos)')
assert 'getChunkNow(' in physical and 'chunk==null' in physical
assert 'chunk.getBlockState(pos).is(' in physical
for forbidden in ('level.getBlockState(', 'level.getChunk(', 'level.getBlockEntity('):
    assert forbidden not in physical and forbidden not in ledger, f'Unsafe {forbidden} in owner lookup'
assert 'return recordedOwner(level,pos);' in physical
assert 'return e.teamId();' in ledger
# UI feature must remain OP gated and filtered from the policy whitelist, never arbitrary file edits.
assert 'ADMIN_SEARCH' in screen and 'private void searchSettings()' in screen
assert 'if(!admin()){page(Page.START);return;}' in body(screen, 'private void searchSettings()')
assert 'ControlCenterPolicy.FIELDS.stream()' in body(screen, 'private void searchSettings()')
assert 'renderSettings(matching,true)' in screen
assert 'ControlCenterClient.request("set",field.group()+"."+field.key(),field.normalize(value))' in screen
print('v0.8.3 C2ME async serialization, loaded-only owner and admin search guards: OK')
