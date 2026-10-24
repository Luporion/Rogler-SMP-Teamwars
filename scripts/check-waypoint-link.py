#!/usr/bin/env python3
"""Guard local-only journal and add-only imports, not a substitute for NeoForge compilation."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
base=r/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/map/client'
read=lambda p:(base/p).read_text(encoding='utf-8')
client=read('WaypointLinkClient.java')
local=read('LocalWaypointBridge.java')
cmd=read('WaypointClientCommands.java')
assert 'PacketDistributor' not in client
assert 'WaypointMutationPayload' not in client
assert 'private-waypoint-links' in client and 'SHA-256' in client
assert 'WaypointLinkJournal.load' in client
assert 'linkImported' in client and 'WaypointLinkClient.linkImported(imported)' in local and 'WaypointLinkJournal.save' in client
assert 'safeToApply' in client and 'resolveMetadata' in client
assert 'reservedLocations()' in client
assert 'WaypointLinkClient.reservedLocations()' in local
# Java compile regression v0.8.0: reservedLocations() declares Exception,
# while syncPrivate() exposes only ReflectiveOperationException. The journal
# safety lookup must be handled locally and must fail closed (no import).
assert 'catch (Exception journalError)' in local
assert 'Privatimport abgebrochen' in local
assert local.index('return "Privatimport abgebrochen') < local.index('Method createFtb=')
assert 'filter(p->!blocked.contains(p.locationKey()))' in local
assert 'Commands.literal("link")' in cmd and 'Commands.literal("changes")' in cmd
assert 'Commands.literal("apply")' in cmd and 'Commands.literal("unlink")' in cmd
assert 'Commands.literal("acknowledge")' in cmd
assert 'registrar("3")' in (r/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/map/TeamIntelNetwork.java').read_text()
assert 'dev.architectury.networking.NetworkManager' in (r/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/compat/ftbvisibility/FtbVisibilityPacketMixin.java').read_text()
bridge=read('WaypointMutationBridge.java')
assert 'PacketDistributor' not in bridge and 'WaypointMutationPayload' not in bridge
assert 'safeToDelete' in client and 'safeToMove' in client
assert 'confirmDeletion' in client and 'confirmMove' in client
assert 'WaypointLinkJournal.load' in client
assert 'linkImported' in client and 'WaypointLinkClient.linkImported(imported)' in local
assert 'WaypointMutationBridge.deleteXaero' in client and 'WaypointMutationBridge.deleteFtb' in client
assert 'WaypointMutationBridge.moveXaero' in client and 'WaypointMutationBridge.moveFtb' in client
assert 'removeWaypoint' in bridge and 'setX' in bridge and 'setZ' in bridge
assert 'Commands.literal("delete-linked")' in cmd and 'Commands.literal("move-linked")' in cmd
assert 'Commands.literal("sync-linked")' in cmd and 'Commands.literal("autosync")' in cmd
assert 'Ungelöster Link' in client and 'WaypointLinkPolicy.State.MISSING_FTB' in client
assert 'ClientTickEvent.Post' in client and 'autoMetadata=false' in client
print('v0.8.1 local waypoint sync, explicit delete/move, opt-in metadata only and privacy guards: OK')
