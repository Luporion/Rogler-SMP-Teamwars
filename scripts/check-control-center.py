#!/usr/bin/env python3
"""Static wiring/authority checks complement pure Java tests, not a NeoForge build."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
src=root/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge'
read=lambda p:(src/p).read_text(encoding='utf-8')
network=read('map/TeamIntelNetwork.java')
service=read('ui/ControlCenterService.java')
request=read('ui/ControlCenterRequestPayload.java')
state=read('ui/ControlCenterStatePayload.java')
client=read('ui/client/ControlCenterClient.java')
screen=read('ui/client/ControlCenterScreen.java')
mod=read('RoglerTeamWarsMod.java')
assert 'ControlCenterService::handle' in network and 'ControlCenterClient.onState' in network
assert 'context.enqueueWork' in service and 'ControlCenterPolicy.require(request.key())' in service
assert 'ControlCenterPolicy.allowed(permission,field)' in service
assert 'validateCombined(changed)' in service
assert service.index('validateCombined(changed)') < service.index('atomicSave(file(server')
assert 'RoglerTeamWarsMod.reloadUiSettings(server)' in service
assert 'new ControlCenterStatePayload(permission,team,message' in service
assert 'permission>=2?snapshot(server):Map.of()' in service
assert 'StandardCopyOption.ATOMIC_MOVE' in service
assert 'RoglerTeamWarsLogger.info("UI admin change by ' in service
assert 'readUtf(64)' in request and 'readUtf(32)' in state
assert 'Commands.literal("roglerui")' in client and 'RegisterKeyMappingsEvent' in client
assert 'ConfirmScreen' in screen and 'ControlCenterPolicy.sensitive(field)' in screen
assert 'sendCommand("teamwars admin "' in screen and 'matches("[A-Za-z0-9_]{1,16}")' in screen
assert 'Commands.literal("rogler")' in mod and 'BaseGuardService.reconcile(server)' in mod
assert 'registrar("3")' in network
assert 'private-waypoint-links' in read('map/client/WaypointLinkClient.java')
print('v0.8.2 Control Center network, policy, UI and admin authority guards: OK')
