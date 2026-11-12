#!/usr/bin/env python3
"""Structural guards supplement unit tests. Full NeoForge runtime verification still required."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge'
read=lambda p:(src/p).read_text(encoding='utf8')
service=read('ui/ControlCenterService.java')
admin=read('ui/ControlCenterAdminService.java')
network=read('map/TeamIntelNetwork.java')
client=read('ui/client/ControlCenterClient.java')
screen=read('ui/client/ControlCenterScreen.java')
request=read('ui/ControlCenterRequestPayload.java')
proto=read('ui/ControlCenterAdminPayload.java')
policy=(r/'core/src/main/java/dev/rogler/teamwars/core/ui/AdminActionPolicy.java').read_text(encoding='utf8')
assert 'adminview' in service and 'adminrun' in service and 'cas' in service
assert 'AdminActionPolicy.command(request.key(),request.value())' in service
assert 'ControlCenterPolicy.allowed(permission,field)' in service
assert 'withSource(capture)' in service
assert 'RoglerTeamWarsMod.assignmentFor(p)' in admin
assert 'viewer.createCommandSourceStack().hasPermission(2)' in admin
assert 'BaseCoreSavedData.get(' in admin and 'RaidSavedData.get(' in admin
assert 'Files.writeString(file,line' in admin and 'MAX_LOG_BYTES' in admin
assert 'reloadGroup(server,field.group())' in service and 'ROLLED_BACK' in service
assert 'ControlCenterAdminPayload.TYPE' in network and 'onAdminState' in network
assert 'ControlCenterClient.requestAdmin' in screen and 'AdminActionPolicy.command(id,value)' in screen
assert 'request("cas",id,before+"|"+after)' in screen
assert 'ControlCenterClient.request("adminrun",id,value)' in screen
assert 'readUtf(96)' in request and 'readUtf(180)' in proto
assert 'getChunk(' not in admin and 'getBlockState(' not in admin
assert 'sendCommand("teamwars admin "' not in screen
assert 'new ControlCenterAdminPayload' in admin
assert 'registrar("5")' in network
print('v0.8.4 UI admin snapshot, allowlist, compare-and-set, audit and privacy guards: OK')
