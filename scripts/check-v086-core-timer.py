#!/usr/bin/env python3
"""Static integration guards for v0.8.6; gameplay + NeoForge tests remain required."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def src(p):return (root/p).read_text(encoding='utf-8')
service=src('mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/base/BaseCoreService.java')
data=src('mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/base/BaseCoreSavedData.java')
item=src('mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/base/BaseCoreItem.java')
ledger=src('core/src/main/java/dev/rogler/teamwars/core/base/BaseCoreLedger.java')
clock=src('core/src/main/java/dev/rogler/teamwars/core/base/CoreTransportClockPolicy.java')
assert 'for (var snapshot : new ArrayList<>(saved.ledger().all()))' in service, 'Must tick all issued cores, not inventory carriers only'
assert 'saved.transportSecondsDue(team, snapshot.token(), System.currentTimeMillis())' in service
assert 'advanceUnplaced(team, snapshot.token(), seconds, safe)' in service
assert 'notifyTeam(server, team, "Core transport expired' in service
assert 'public static BaseCoreLedger.Entry accountIssuedTeam(' in service
assert 'BaseCoreService.accountIssuedTeam(level.getServer(), team)' in item
assert item.index('BaseCoreService.accountIssuedTeam') < item.index('var reason = data.ledger().placementFailure')
assert 'TransportClocks' in data and 'AccountedAtMillis' in data
assert 'tag.putInt("Schema", 2)' in data
assert 'new CoreTransportClockPolicy.Checkpoint' in data
assert 'public Entry advanceUnplaced(' in ledger
assert 'current.token().equals(token)' in ledger
assert 'Math.min(remaining, seconds)' in ledger
assert 'currentToken.equals(previous.token())' in clock
print('v0.8.6 server-authoritative unplaced timer, persisted wall clock and placement guards: OK')
