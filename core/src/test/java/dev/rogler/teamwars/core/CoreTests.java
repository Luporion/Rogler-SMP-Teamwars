package dev.rogler.teamwars.core;

import java.nio.file.*;
import java.util.*;

/** Runs with java -ea; avoids JUnit/Gradle dependency for the P0 offline proof. */
public final class CoreTests {
    private static int tests = 0;
    private static UUID id(int n) { return new UUID(0, n); }
    private static void check(boolean condition, String message) {
        tests++;
        if (!condition) throw new AssertionError(message);
    }
    private static void fails(Runnable code, String message) {
        boolean thrown = false;
        try { code.run(); } catch (IllegalStateException | IllegalArgumentException expected) { thrown = true; }
        check(thrown, message);
    }
    public static void main(String[] args) throws Exception {
        UUID a = id(1), b = id(2), c = id(3), teamId = id(100);
        TeamDirectory teams = new TeamDirectory();
        teams.create(teamId, "Rot", a);
        teams.add(teamId, b);
        check(teams.members(teamId).size() == 2, "join");
        fails(() -> teams.add(teamId, b), "Cannot join twice");
        fails(() -> teams.remove(a), "Cannot remove leader without transfer");
        teams.rename(teamId, "Blau");
        check(teams.require(teamId).name().equals("Blau"), "rename retains identity");
        check(teams.teamOf(a).orElseThrow().equals(teamId), "Stable team id");

        ExplorationIndex index = new ExplorationIndex();
        ChunkKey ownA = new ChunkKey("minecraft:overworld", -1, 0);
        ChunkKey overlap = new ChunkKey("minecraft:overworld", 1, -2);
        ChunkKey ownB = new ChunkKey("minecraft:overworld", 8, 12);
        check(index.discover(a, ownA), "first visit");
        check(!index.discover(a, ownA), "dedup revisit");
        index.discover(a, overlap); index.discover(b, overlap); index.discover(b, ownB);
        check(index.visibleTo(Set.of(a,b)).size() == 3, "team union");
        var revoke = index.delta(Set.of(a,b), Set.of(a));
        check(revoke.revoked().equals(Set.of(ownB)), "revoke only B exclusive");
        check(revoke.revealed().isEmpty(), "no reveal after leave");
        check(index.personal(a).contains(ownA), "private exploration retained");
        var gain = index.delta(Set.of(a), Set.of(a,b));
        check(gain.revealed().equals(Set.of(ownB)), "new member contributes only new tiles");
        teams.remove(b);
        check(teams.members(teamId).equals(Set.of(a)), "team updated");
        check(index.visibleTo(teams.members(teamId)).equals(Set.of(ownA,overlap)), "membership affects visibility");
        check(ChunkKey.fromBlock("minecraft:overworld", -1, -17).equals(new ChunkKey("minecraft:overworld", -1, -2)), "floor division negative");
        index.discover(c, new ChunkKey("minecraft:the_nether", 0, 0));
        check(!index.visibleTo(Set.of(a,b)).contains(new ChunkKey("minecraft:the_nether", 0, 0)), "dimension separation");

        Path f = Files.createTempFile("teamwars", ".rtwe");
        try {
            ExplorationStore.write(f,index);
            ExplorationIndex read = ExplorationStore.read(f);
            check(read.exportSnapshot().equals(index.exportSnapshot()), "snapshot roundtrip");
            byte[] bytes = Files.readAllBytes(f); bytes[20] ^= 0x35; Files.write(f, bytes);
            boolean corruption = false;
            try { ExplorationStore.read(f); } catch (java.io.IOException expected) { corruption = true; }
            check(corruption, "corrupt snapshot rejected");
        } finally { Files.deleteIfExists(f); }

        BaseZone zone = new BaseZone(0,0,300,2000);
        check(!zone.placementAllowed(299,0), "inner restriction");
        check(zone.placementAllowed(300,0), "inner boundary included");
        check(zone.placementAllowed(2000,0), "outer boundary included");
        check(!zone.placementAllowed(2001,0), "outer restriction");
        check(!zone.placementAllowed(Integer.MAX_VALUE,Integer.MAX_VALUE), "overflow-safe");
        CoreLifecycle.Position valid = new CoreLifecycle.Position("minecraft:overworld", 1000, 80, 1000);
        CoreLifecycle.Position invalid = new CoreLifecycle.Position("minecraft:overworld", 3000, 80, 3000);
        CoreLifecycle lifecycle = new CoreLifecycle(zone, 3600, 600);
        for (int i=0;i<59;i++) lifecycle.tickCarried(60,valid);
        check(lifecycle.carriedSeconds()==3540,"carrying timer persisted");
        var carryEnd=lifecycle.tickCarried(60,invalid);
        check(carryEnd.action()==CoreLifecycle.Action.RETURN_TO_LAST_SAFE,"out of bounds last safe");
        check(carryEnd.target().equals(valid),"safe return target");
        check(lifecycle.mode()==CoreLifecycle.Mode.CARRIED, "placement never committed until adapter ACK");
        lifecycle.placeStationary(valid);
        check(lifecycle.mode()==CoreLifecycle.Mode.STATIONARY, "placement ACK");
        var ship = new CoreLifecycle(zone,3600,600);
        ship.placeStationary(valid); ship.attachToShip();
        for(int i=0;i<9;i++) ship.tickMobile(60,invalid);
        check(ship.mode()==CoreLifecycle.Mode.MOBILE, "no premature nuke");
        ship.tickMobile(60,valid);
        check(ship.mobileOutsideSeconds()==510,"outside budget decreases slowly");
        var restarted = CoreLifecycle.restore(zone, 3600, 600, ship.snapshot());
        check(restarted.mobileOutsideSeconds()==510 && restarted.mode()==CoreLifecycle.Mode.MOBILE,
                "mobile snapshot restore keeps countdown");
        var oneSecond = CoreLifecycle.restore(zone, 3600, 600, ship.snapshot());
        oneSecond.tickMobile(1,valid); oneSecond.tickMobile(1,valid);
        check(oneSecond.mobileOutsideSeconds()==509,"half-speed recovery independent of tick granularity");
        ship.tickMobile(60,invalid);
        CoreLifecycle.Decision detonation = ship.tickMobile(60,invalid);
        check(detonation.action()==CoreLifecycle.Action.DETONATE, "detonation event");
        check(ship.mode()==CoreLifecycle.Mode.DESTROYED, "accumulated budget nuke");
        fails(() -> ship.tickMobile(60,invalid), "destroyed core cannot tick again");
        ChunkLoadingBudget budget = new ChunkLoadingBudget(1,2);
        check(budget.addManual(ownA), "manual ticket");
        check(!budget.addManual(ownB), "manual quota");
        check(budget.addCore(ownA) && budget.addCore(ownB), "core tickets");
        check(budget.actualDistinctLoaded()==2, "overlap counts once");
        check(!budget.addCore(overlap), "core quota");
        budget.clearCore();
        check(budget.actualDistinctLoaded()==1, "core release");
        System.out.println("P0 CoreTests: "+tests+" checks OK");
    }
}
