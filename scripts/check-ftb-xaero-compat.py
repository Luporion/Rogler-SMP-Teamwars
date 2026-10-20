#!/usr/bin/env python3
"""Release gate for observed v0.7.3 runtime packet regression and Xaero 26.4.2 API."""
from pathlib import Path
src=Path(__file__).resolve().parents[1] / 'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge'
ftb=(src/'compat/ftbvisibility/FtbVisibilityPacketMixin.java').read_text()
assert 'dev.architectury.networking.NetworkManager' in ftb
assert 'getMethod("sendToPlayer", ServerPlayer.class, CustomPacketPayload.class)' in ftb
assert 'send.invoke(null, recipient, outgoing)' in ftb
assert 'PacketDistributor.sendToPlayer(' not in ftb, 'Do not call native NeoForge packet dispatcher for FTB Architectury payloads'
assert 'ci.cancel();' in ftb, 'Fallback to unfiltered FTB packet is a privacy leak'
mirror=(src/'map/client/FtbXaeroWaypointMirror.java').read_text()
assert 'table instanceof Map<?, ?> map' in mirror
assert 'Map<Integer,Object>' in mirror
assert 'clearInserted(own)' in mirror
assert 'table.get(e.getKey())==e.getValue()' in mirror
print('FTB/Architectury payload + Xaero 26.4.2 Map API regression check: OK')

# v0.7.5 is additive: retain native NeoForge own payloads only;
# use Xaero's *separate* namespaced waypoint map, never personal sets.
markers=(src/'map/client/TeamXaeroMarkers.java').read_text()
assert 'getCustomWaypoints' in markers and 'roglercore_team_navigation' in markers
assert 'TeamIntelClientHandler.active()' in markers and 'TeamNavigationClient.active()' in markers
assert 'clear()' in markers and 'removeInserted()' in markers
hud=(src/'map/client/TeamIntelHud.java').read_text()
assert '!settings.enabled()' in hud, 'HUD must be opt-in'
nav=(src/'map/TeamIntelService.java').read_text()
assert 'party.members().contains(id)' in nav, 'Do not leak base coordinates to an unverified player'
assert 'viewer.getRespawnPosition()' in nav, 'Respawn targets must be per-viewer'
print('v0.7.5 map privacy, HUD opt-in and Xaero namespacing check: OK')
