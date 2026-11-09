#!/usr/bin/env python3
"""Focused source guards; not a substitute for NeoForge CI or live profiling."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def source(path):return (root/path).read_text(encoding="utf-8")
index=source("core/src/main/java/dev/rogler/teamwars/core/ExplorationIndex.java")
session=source("core/src/main/java/dev/rogler/teamwars/core/TeamMapSession.java")
service=source("mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/map/ExplorationService.java")
claims=source("mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/protection/FtbClaimBridge.java")
mod=source("mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/RoglerTeamWarsMod.java")
assert "MAX_RECENT_CHANGES = 2048" in index and "discoveredSince(" in index
assert "personalRevision(member)" in session and "index.discoveredSince(member, oldRevision)" in session
assert "Set<ChunkKey> nextVisible = index.visibleTo(nextMembers)" in session
assert "dataSource != index" in session and "newMembership" in session
assert "pending += inFlightChanges" in service and "if (runningWrite != null) finishLastWrite()" in service
assert "getTickCount() % 6000" in mod and "BaseGuardService.reconcile(activeServer)" in mod
assert "if (created > 0 || failed > 0 || loggedExistingClaims.add(team))" in claims
assert "syncChunksToPlayer" in claims and "team.equals(assignment.partyId())" in claims
assert "FtbClaimBridge.refreshOwnClaimView(p, a.partyId())" in source("mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/protection/BaseGuardService.java")
assert "FtbClaimBridge.resetRuntimeState()" in source("mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/protection/BaseGuardService.java")
print("v0.8.7 exploration incremental / claim audit cadence / dirty-write recovery source guards OK")
