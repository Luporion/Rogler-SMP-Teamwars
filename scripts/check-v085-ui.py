#!/usr/bin/env python3
"""No GUI-free Java test proves Minecraft UI integration. Static guards cover wiring and privacy."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
client=root/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge'
screen=(client/'ui/client/ControlCenterScreen.java').read_text(encoding='utf8')
hud=(client/'map/client/HudPositionScreen.java').read_text(encoding='utf8')
store=(client/'map/client/HudSettingsStore.java').read_text(encoding='utf8')
core=(root/'core/src/main/java/dev/rogler/teamwars/core/HudPositionPolicy.java').read_text(encoding='utf8')
assert 'case ADMIN_DETAIL -> adminDetail();' in screen
assert 'selectedTeam=row' in screen and 'selectedTeam.id().toString()' in screen
assert 'AdminActionPolicy::require' in screen and 'adminRequest(action.id(),value)' in screen
assert 'ControlCenterClient.request("adminrun",id,value)' in screen
assert 'actionFilter' in screen and 'draftSettings' in screen and 'pendingSettingKey' in screen
assert 'request("cas",id,before+"|"+after)' in screen
assert 'new HudPositionScreen(this)' in screen
assert 'HudSettingsStore.update(draft)' in hud and 'draft = original' in hud
assert 'mouseDragged' in hud and 'HudPositionPolicy.offsets(' in hud
assert 'PacketDistributor' not in hud and 'ServerLevel' not in hud
assert 'java.util.Properties' not in hud
assert 'getChunk(' not in core and 'getBlockState(' not in core
assert 'config/roglercore-hud.properties' in store
print('v0.8.5 contextual admin, action search, draft preservation and client-only HUD guards: OK')
