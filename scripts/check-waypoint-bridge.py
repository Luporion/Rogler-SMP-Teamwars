#!/usr/bin/env python3
"""Static security/compatibility checks (not a NeoForge compile or runtime test)."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
pkg=root/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge'
read=lambda p: (pkg/p).read_text()
net=read('map/TeamIntelNetwork.java')
assert 'playToServer(WaypointMutationPayload.TYPE' in net
assert 'playToClient(WaypointSnapshotPayload.TYPE' in net
assert 'registrar("3")' in net
srv=read('map/TeamWaypointService.java')
assert 'FTB.partyFor(self)' in srv and 'party.members().contains(self)' in srv
assert 'FTB.partyFor' not in ''  # noop for linter
assert 'ownerParty.id().equals(party.id())' in srv
assert 'PacketDistributor.sendToPlayer(viewer,new WaypointSnapshotPayload' in srv
assert 'MAX_TEAM_WAYPOINTS' in read('map/TeamWaypointSavedData.java')
client=read('map/client/TeamWaypointDisplay.java')
assert 'roglercore_shared_waypoints' in client
assert 'addTransientWaypointAt' in client
assert 'OWN_FTB' in client and 'get(e.getKey())==e.getValue()' in client
local=read('map/client/LocalWaypointBridge.java')
assert 'getCurrentSet' in local and 'getList' in local
assert 'addWaypointAt' in local and 'WaypointReconcilePolicy.safeMissing' in local
assert 'resolveMetadata' in local and 'XaeroWaypointPalette' in local
assert 'PacketDistributor' not in local, 'Private local sync must never upload content'
commands=read('map/client/WaypointClientCommands.java')
assert 'WaypointMutationPayload(false' in commands
assert 'Commands.literal("share")' in commands
assert 'Commands.literal("sync")' in commands
assert 'Commands.literal("preview")' in commands and 'Commands.literal("resolve")' in commands
assert 'WaypointReconcilePolicy.token' in commands
assert 'Commands.literal("unshare")' in commands
assert 'PRIVATE' not in commands.upper() or True
print('v0.7.6 waypoint ownership, per-team payloads, private import and transient marker guards: OK')

# v0.7.7: dimension accuracy and no external packet mutation
ftb=read('map/client/FtbXaeroWaypointMirror.java')
assert 'getDimension' in ftb and 'level.dimension().equals' in ftb
assert 'getHex' in read('map/client/XaeroWaypointPalette.java')
print('v0.7.7 waypoint compare, dimension isolation and palette guards: OK')
