#!/usr/bin/env python3
"""Wiring/privacy guard complementing real adapter and request-policy tests."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge'
read=lambda p:(src/p).read_text(encoding='utf-8')
service=read('ui/ControlCenterReportService.java')
reader=read('protection/FtbChunkStatusReader.java')
network=read('map/TeamIntelNetwork.java')
client=read('ui/client/ControlCenterClient.java')
screen=read('ui/client/ControlCenterScreen.java')
payload=read('ui/ControlCenterReportPayload.java')
ui=read('ui/ControlCenterService.java')
assert 'registrar("4")' in network and 'ControlCenterReportPayload.STREAM_CODEC' in network
assert 'party.members().contains(p.getUUID())' in service
assert 'DiagnosticPolicy.parse(kind,value,permission(viewer),own)' in service
assert 'Objects.equals(report.target(),ownTeam(viewer))' in service
assert '!report.kind().equals("own") && permission(viewer)<2' in service
assert 'getChunkNow(x,z)!=null' in service and '.getChunk(' not in service and 'getBlockState(' not in service
assert 'getOrCreateData' not in reader.split('public final class',1)[1]
assert 'MAX_CLAIM_SCAN' in reader and 'MAX_TEAM_SCAN' in reader
assert 'throttle(LAST_READ,viewer,10)' in service and 'ControlCenterReportService.logout(id)' in ui
assert 'withSource(capture)' in ui and 'ControlCenterReportService.actionResult' in ui
assert 'withSuppressedOutput' not in ui
assert 'CommandCapture implements CommandSource' in service
assert 'readUtf(180)' in payload and 'MAX_PAGE' in payload
assert 'ClientPlayerNetworkEvent.LoggingOut' in client and 'reportData=null' in client
assert 'reportOffset=report.offset()' in screen and 'Math.min(reportLimit(),report.rows().size())' in screen
assert 'RoglerTeamWarsMod.VERSION' in screen and '"v0.8.5"' not in screen
print('v0.8.8 report authority, fresh team scope, no-load reads, bounded packets and command feedback wiring: OK')
